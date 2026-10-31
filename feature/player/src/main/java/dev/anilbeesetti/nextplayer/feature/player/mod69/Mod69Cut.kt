package dev.anilbeesetti.nextplayer.feature.player.mod69

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import dev.anilbeesetti.nextplayer.core.common.extensions.getFilenameFromUri

/** 69 mod: cut a part of the video, or save its sound as MP3, using 69-Media Helper's FFmpeg. */
object Mod69Cut {
    private const val HELPER = "com.gor69.mediahelper"
    private const val ACTION = "com.gor69.mediahelper.PROCESS"
    private const val MSG_CUT = 5
    private const val MSG_CUT_RESULT = 6

    var running by mutableStateOf(false)
        private set

    fun isHelperInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(HELPER, 0)
        true
    }.getOrDefault(false)

    /**
     * @param mode "copy" (fast, no quality loss, starts at the nearest key frame),
     *             "exact" (re-encodes, exact start) or "mp3" (sound only).
     */
    fun start(context: Context, input: Uri, title: String, startMs: Long, endMs: Long, mode: String) {
        val app = context.applicationContext
        if (running) return
        val inExt = title.substringAfterLast('.', "mp4").lowercase().takeIf { it.length in 2..4 } ?: "mp4"
        val ext = when (mode) {
            "mp3" -> "mp3"
            "exact" -> "mp4"
            else -> if (inExt in setOf("mp4", "mkv", "webm", "mov", "m4v")) inExt else "mkv"
        }
        val base = title.substringBeforeLast('.').replace(Regex("[\\\\/:*?\"<>|]"), "_").take(70)
        val name = "${base}_${getStringForTime(startMs).replace(':', '.')}" +
            (if (endMs > 0) "-" + getStringForTime(endMs).replace(':', '.') else "") + "." + ext
        val outUri = createOutput(app, name, mode == "mp3", ext) ?: run {
            Toast.makeText(app, "Could not create the output file", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            app.grantUriPermission(HELPER, input, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            app.grantUriPermission(HELPER, outUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        running = true
        Toast.makeText(app, "Working… you can keep watching", Toast.LENGTH_SHORT).show()

        val main = Handler(Looper.getMainLooper())
        lateinit var conn: ServiceConnection
        fun finish(ok: Boolean, error: String?) {
            running = false
            runCatching { app.unbindService(conn) }
            runCatching { app.revokeUriPermission(input, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            if (ok) {
                publish(app, outUri)
                val where = if (mode == "mp3") "Music/69-Next Player" else "Movies/69-Next Player"
                Toast.makeText(app, "Saved to $where", Toast.LENGTH_LONG).show()
            } else {
                runCatching { app.contentResolver.delete(outUri, null, null) }
                Toast.makeText(app, "Failed: " + (error ?: "unknown error"), Toast.LENGTH_LONG).show()
            }
        }
        val replyTo = Messenger(
            Handler(Looper.getMainLooper()) { msg ->
                if (msg.what == MSG_CUT_RESULT) {
                    finish(msg.data.getBoolean("ok"), msg.data.getString("error"))
                }
                true
            },
        )
        conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val m = Message.obtain(null, MSG_CUT)
                m.data = Bundle().apply {
                    putParcelable("in", input)
                    putParcelable("out", outUri)
                    putLong("start_ms", startMs)
                    putLong("end_ms", endMs)
                    putString("mode", mode)
                    putString("out_ext", ext)
                }
                m.replyTo = replyTo
                runCatching { Messenger(service).send(m) }.onFailure { main.post { finish(false, it.message) } }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                if (running) finish(false, "69-Media Helper stopped")
            }
        }
        val ok = runCatching {
            app.bindService(Intent(ACTION).setPackage(HELPER), conn, Context.BIND_AUTO_CREATE)
        }.getOrDefault(false)
        if (!ok) finish(false, "69-Media Helper is not installed or has a different signing key")
    }

    private fun createOutput(context: Context, name: String, audio: Boolean, ext: String): Uri? = runCatching {
        val mime = when (ext) {
            "mp3" -> "audio/mpeg"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            else -> "video/mp4"
        }
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    (if (audio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES) + "/69-Next Player",
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = if (audio) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            context.contentResolver.insert(collection, values)
        } else {
            null
        }
    }.getOrNull()

    private fun publish(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= 29) {
            runCatching {
                context.contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun Mod69CutPage(player: Player) {
    val context = LocalContext.current
    val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
    var start by remember { mutableLongStateOf(Mod69Session.aMs.takeIf { it >= 0 } ?: 0L) }
    var end by remember { mutableLongStateOf(Mod69Session.bMs.takeIf { it > 0 } ?: -1L) }
    var mode by remember { mutableStateOf("copy") }
    val uri = player.currentMediaItem?.localConfiguration?.uri

    if (!Mod69Cut.isHelperInstalled(context)) {
        Text("Needs 69-Media Helper (it has FFmpeg). Install it, then come back.", color = MaterialTheme.colorScheme.error)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Start", style = MaterialTheme.typography.labelLarge)
            Text(getStringForTime(start), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = { start = player.currentPosition }) { Text("Set to now") }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Column(Modifier.weight(1f)) {
            Text("End", style = MaterialTheme.typography.labelLarge)
            Text(if (end > 0) getStringForTime(end) else "End of video", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = { end = player.currentPosition }) { Text("Set to now") }
        OutlinedButton(onClick = { end = -1L }, modifier = Modifier.padding(start = 4.dp)) { Text("End") }
    }
    Text("Tip: A-B repeat points are used as start / end.", style = MaterialTheme.typography.bodySmall)
    Text("Type", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("copy" to "Fast cut", "exact" to "Exact cut", "mp3" to "MP3 sound").forEach { (id, label) ->
            FilterChip(selected = mode == id, onClick = { mode = id }, label = { Text(label) })
        }
    }
    Text(
        when (mode) {
            "copy" -> "Same quality, very fast. May start up to a few seconds early (at a key frame)."
            "exact" -> "Starts exactly where you set it. Re-encodes as MP4, so it is slower."
            else -> "Saves only the sound as MP3 in Music/69-Next Player."
        },
        style = MaterialTheme.typography.bodySmall,
    )
    val maxMs = if (duration > 0) duration else Long.MAX_VALUE
    val valid = uri != null && (end < 0 || end > start) && start < maxMs
    FilledTonalButton(
        enabled = valid && !Mod69Cut.running && uri?.scheme == "content",
        onClick = {
            val title = runCatching { context.getFilenameFromUri(uri!!) }.getOrNull()
                ?: player.mediaMetadata.title?.toString() ?: "video.mp4"
            Mod69Cut.start(context, uri!!, title, start, end, mode)
        },
        modifier = Modifier.padding(top = 10.dp),
    ) { Text(if (Mod69Cut.running) "Working…" else "Save") }
}
