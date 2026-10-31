package dev.anilbeesetti.nextplayer.feature.player.mod69

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.Util.getStringForTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 69 mod: small picture above the seekbar while dragging (MX style). Frames are read with a
 * separate retriever so the video itself is not disturbed. Only for files on the phone or
 * picked folders (content:// and file://), not network streams.
 */
@Composable
internal fun Mod69SeekPreview(player: Player?, positionMs: Long?, fraction: Float) {
    val context = LocalContext.current
    val uri = player?.currentMediaItem?.localConfiguration?.uri
    val usable = uri != null && (uri.scheme == "content" || uri.scheme == "file" || uri.scheme == null)
    var retriever by remember(uri) { mutableStateOf<MediaMetadataRetriever?>(null) }
    LaunchedEffect(uri) {
        if (!usable) return@LaunchedEffect
        retriever = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().apply {
                    if (uri!!.scheme == "content") setDataSource(context, uri) else setDataSource(uri!!.path)
                }
            }.getOrNull()
        }
    }
    DisposableEffect(uri) { onDispose { runCatching { retriever?.release() } } }
    var frame by remember(uri) { mutableStateOf<Bitmap?>(null) }

    if (positionMs == null) return
    LaunchedEffect(retriever, positionMs / 500) {
        val r = retriever ?: return@LaunchedEffect
        delay(80)
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                val us = positionMs * 1000
                if (Build.VERSION.SDK_INT >= 27) {
                    r.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 320)
                } else {
                    r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            }.getOrNull()
        }
        if (bmp != null) frame = bmp
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(if (usable) 112.dp else 28.dp)) {
        val boxW = if (usable) 168.dp else 72.dp
        val x = ((maxWidth - boxW) * fraction).coerceAtLeast(0.dp)
        Column(
            modifier = Modifier
                .offset(x = x)
                .width(boxW),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (usable) {
                Box(
                    modifier = Modifier
                        .width(boxW)
                        .height(94.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
                ) {
                    frame?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(94.dp),
                        )
                    }
                }
            }
            Text(
                text = getStringForTime(positionMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}
