package dev.anilbeesetti.nextplayer.core.media.network.datasource

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import dev.anilbeesetti.nextplayer.core.media.network.NetworkUri
import org.koin.core.annotation.Single

/**
 * The player's data source factory: Media3's own [DefaultDataSource] for local and http(s) media,
 * and [NetworkDataSource] for the `smb`/`ftp`/`sftp`/`webdav` schemes it doesn't handle.
 */
@UnstableApi
@Single
class NextDataSourceFactory(private val context: Context, private val sessions: NetworkSessions) :
    DataSource.Factory {

    override fun createDataSource(): DataSource = SchemeDispatchingDataSource(
        default = Mod69SafeDataSource(userAgent.trim().let { ua ->
            if (ua.isEmpty()) {
                DefaultDataSource.Factory(context)
            } else {
                DefaultDataSource.Factory(
                    context,
                    androidx.media3.datasource.DefaultHttpDataSource.Factory()
                        .setUserAgent(ua)
                        .setAllowCrossProtocolRedirects(true),
                )
            }
        }.createDataSource()),
        network = NetworkDataSource(sessions = sessions),
    )

    companion object {
        /** 69 mod: custom HTTP User-Agent, empty = Media3 default. Set by the player service. */
        @Volatile
        var userAgent: String = ""
    }

    /** Disconnects the network client held for playback. Call when the player is released. */
    suspend fun release() = sessions.release()
}

/**
 * Picks the delegate on the first [open], since the scheme is only known then. Media3 asks the
 * factory for a source before it knows which item it will play.
 */
@UnstableApi
private class SchemeDispatchingDataSource(
    private val default: DataSource,
    private val network: DataSource,
) : DataSource {

    private var delegate: DataSource? = null

    override fun open(dataSpec: DataSpec): Long {
        val target = if (NetworkUri.isNetworkUri(dataSpec.uri)) network else default
        delegate = target
        return target.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(value = delegate) { "read() before open()" }.read(
            buffer,
            offset,
            length,
        )

    override fun getUri(): Uri? = delegate?.uri

    override fun close() {
        delegate?.close()
        delegate = null
    }

    override fun addTransferListener(transferListener: TransferListener) {
        default.addTransferListener(transferListener)
        network.addTransferListener(transferListener)
    }

    override fun getResponseHeaders(): Map<String, List<String>> =
        delegate?.responseHeaders ?: emptyMap()
}


/**
 * 69 mod: rclone / RSAF files sometimes report a wrong length, and the content reader then fails
 * with "ArrayIndexOutOfBounds … regionLength=-1" ("Source error"), mostly after a seek.
 * On such a failure this re-opens the file at the current position with an unknown length and
 * carries on; if that fails too it reports an IOException, which the player retries.
 */
@UnstableApi
private class Mod69SafeDataSource(private val inner: DataSource) : DataSource {
    private var spec: DataSpec? = null
    private var position = 0L
    private var reopened = 0

    override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) =
        inner.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        spec = dataSpec
        position = dataSpec.position
        reopened = 0
        return try {
            inner.open(dataSpec)
        } catch (e: RuntimeException) {
            throw java.io.IOException(e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length <= 0) return 0
        while (true) {
            try {
                val n = inner.read(buffer, offset, length)
                if (n > 0) position += n
                return n
            } catch (e: RuntimeException) {
                val s = spec
                if (s == null || s.uri.scheme != "content" || reopened >= 3) throw java.io.IOException(e)
                reopened++
                runCatching { inner.close() }
                inner.open(s.buildUpon().setPosition(position).setLength(androidx.media3.common.C.LENGTH_UNSET.toLong()).build())
            }
        }
    }

    override fun getUri(): android.net.Uri? = inner.uri

    override fun getResponseHeaders(): Map<String, List<String>> = inner.responseHeaders

    override fun close() {
        spec = null
        inner.close()
    }
}
