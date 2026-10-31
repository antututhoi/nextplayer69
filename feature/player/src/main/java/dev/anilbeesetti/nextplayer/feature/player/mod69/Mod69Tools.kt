package dev.anilbeesetti.nextplayer.feature.player.mod69

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import androidx.media3.session.MediaController
import dev.anilbeesetti.nextplayer.feature.player.service.mod69
import dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ToolsPage { MAIN, SLEEP, AB, BOOKMARKS, PICTURE, ASPECT, NIGHT, INFO, AUDIO, SUBTITLE, CUT }

/** The key bookmarks are stored under: the media uri (or id). */
internal fun Player.mod69Key(): String? =
    currentMediaItem?.localConfiguration?.uri?.toString() ?: currentMediaItem?.mediaId?.ifBlank { null }

@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun BoxScope.Mod69ToolsView(
    show: Boolean,
    player: Player,
    videoDecoderMode: DecoderMode?,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableStateOf(ToolsPage.MAIN) }
    LaunchedEffect(show) { if (!show) page = ToolsPage.MAIN }
    val title = when (page) {
        ToolsPage.MAIN -> "Tools"
        ToolsPage.SLEEP -> "Sleep timer"
        ToolsPage.AB -> "A-B repeat"
        ToolsPage.BOOKMARKS -> "Bookmarks"
        ToolsPage.PICTURE -> "Picture"
        ToolsPage.ASPECT -> "Aspect ratio"
        ToolsPage.NIGHT -> "Night mode"
        ToolsPage.INFO -> "Information"
        ToolsPage.AUDIO -> "Audio & equalizer"
        ToolsPage.SUBTITLE -> "Subtitle look"
        ToolsPage.CUT -> "Cut / MP3"
    }
    OverlayView(show = show, title = title) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            if (page != ToolsPage.MAIN) {
                TextButton(onClick = { page = ToolsPage.MAIN }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    Text("  Back")
                }
            }
            when (page) {
                ToolsPage.MAIN -> MainPage(player = player, onOpen = { page = it }, onDismiss = onDismiss)
                ToolsPage.SLEEP -> SleepPage(player)
                ToolsPage.AB -> AbPage(player)
                ToolsPage.BOOKMARKS -> BookmarksPage(player)
                ToolsPage.PICTURE -> PicturePage(player, videoDecoderMode)
                ToolsPage.ASPECT -> AspectPage()
                ToolsPage.NIGHT -> NightPage()
                ToolsPage.INFO -> InfoPage(player, videoDecoderMode)
                ToolsPage.AUDIO -> Mod69AudioPage()
                ToolsPage.SUBTITLE -> Mod69SubtitlePage()
                ToolsPage.CUT -> Mod69CutPage(player)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainPage(player: Player, onOpen: (ToolsPage) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToolButton(Icons.Rounded.Bedtime, "Sleep timer") { onOpen(ToolsPage.SLEEP) }
        ToolButton(Icons.Rounded.Repeat, "A-B repeat") { onOpen(ToolsPage.AB) }
        ToolButton(Icons.Rounded.CameraAlt, "Screenshot") {
            scope.launch { takeScreenshot(context, player) }
        }
        ToolButton(Icons.Rounded.Bookmark, "Bookmarks") { onOpen(ToolsPage.BOOKMARKS) }
        ToolButton(Icons.Rounded.NightsStay, "Night mode") { onOpen(ToolsPage.NIGHT) }
        ToolButton(Icons.Rounded.Tune, "Picture") { onOpen(ToolsPage.PICTURE) }
        ToolButton(Icons.Rounded.AspectRatio, "Aspect ratio") { onOpen(ToolsPage.ASPECT) }
        ToolButton(Icons.Rounded.Info, "Information") { onOpen(ToolsPage.INFO) }
        ToolButton(Icons.Rounded.GraphicEq, "Audio / EQ") { onOpen(ToolsPage.AUDIO) }
        ToolButton(Icons.Rounded.ClosedCaption, "Subtitle look") { onOpen(ToolsPage.SUBTITLE) }
        ToolButton(Icons.Rounded.ContentCut, "Cut / MP3") { onOpen(ToolsPage.CUT) }
    }
    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    Text("Frame step (pauses the video)", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        OutlinedButton(onClick = { frameStep(player, -1) }) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = null)
            Text(" 1 frame")
        }
        OutlinedButton(onClick = { frameStep(player, 1) }) {
            Text("1 frame ")
            Icon(Icons.Rounded.SkipNext, contentDescription = null)
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.width(150.dp)) {
        Icon(icon, contentDescription = null)
        Text("  $label", maxLines = 1)
    }
}

private fun frameStep(player: Player, direction: Int) {
    player.pause()
    val fps = player.currentTracks.groups
        .firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
        ?.let { g -> (0 until g.length).firstOrNull { g.isTrackSelected(it) }?.let { g.getTrackFormat(it).frameRate } }
        ?.takeIf { it > 0f } ?: 30f
    val frameMs = (1000f / fps).toLong().coerceAtLeast(1L)
    val target = (player.currentPosition + direction * frameMs).coerceAtLeast(0L)
    player.seekTo(target)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepPage(player: Player) {
    val scope = rememberCoroutineScope()
    var left by remember { mutableLongStateOf(0L) }
    var eof by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val r = (player as? MediaController)?.mod69("sleep_get")
            left = r?.getLong("sleep_left") ?: 0L
            eof = r?.getBoolean("sleep_eof") ?: false
            delay(1000)
        }
    }
    Text(
        when {
            eof -> "Stops at the end of this video"
            left > 0 -> "Stops in " + getStringForTime(left)
            else -> "Off"
        },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        listOf(0 to "Off", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1 h", 90 to "1 h 30", 120 to "2 h", -1 to "End of video")
            .forEach { (m, label) ->
                FilterChip(
                    selected = false,
                    onClick = {
                        scope.launch {
                            val r = (player as? MediaController)?.mod69("sleep_set") { putInt("minutes", m) }
                            left = r?.getLong("sleep_left") ?: 0L
                            eof = r?.getBoolean("sleep_eof") ?: false
                        }
                    },
                    label = { Text(label) },
                )
            }
    }
    Text(
        "The sound fades out, then the video pauses. Works in background play too.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun AbPage(player: Player) {
    val scope = rememberCoroutineScope()
    fun send() {
        scope.launch {
            (player as? MediaController)?.mod69("ab_set") {
                putLong("a", Mod69Session.aMs)
                putLong("b", Mod69Session.bMs)
            }
        }
    }
    Text(
        "A: " + (Mod69Session.aMs.takeIf { it >= 0 }?.let { getStringForTime(it) } ?: "—") +
            "    B: " + (Mod69Session.bMs.takeIf { it >= 0 }?.let { getStringForTime(it) } ?: "—"),
        style = MaterialTheme.typography.titleMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        FilledTonalButton(onClick = {
            Mod69Session.aMs = player.currentPosition
            if (Mod69Session.bMs <= Mod69Session.aMs) Mod69Session.bMs = -1L
            send()
        }) { Text("Set A here") }
        FilledTonalButton(onClick = {
            val now = player.currentPosition
            if (Mod69Session.aMs in 0 until now) {
                Mod69Session.bMs = now
                send()
            }
        }) { Text("Set B here") }
        OutlinedButton(onClick = {
            Mod69Session.aMs = -1L
            Mod69Session.bMs = -1L
            send()
        }) { Text("Clear") }
    }
    Text(
        "Set A, play to where it should end, then set B. The part between repeats.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun BookmarksPage(player: Player) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Mod69Session.loadBookmarks(context, player.mod69Key()) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = { Mod69Session.addBookmark(context, player.currentPosition) }) {
            Icon(Icons.Rounded.Bookmark, contentDescription = null)
            Text("  Add here")
        }
        OutlinedButton(
            enabled = Mod69Session.bookmarks.isNotEmpty(),
            onClick = { exportBookmarks(context, player) },
        ) { Text("Export") }
    }
    if (Mod69Session.bookmarks.isEmpty()) {
        Text("No bookmarks yet. They show as dots on the seekbar.", modifier = Modifier.padding(top = 12.dp))
    }
    Mod69Session.bookmarks.toList().forEach { ms ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { player.seekTo(ms) }, modifier = Modifier.weight(1f)) {
                Text(getStringForTime(ms), modifier = Modifier.fillMaxWidth())
            }
            IconButton(onClick = { Mod69Session.removeBookmark(context, ms) }) {
                Icon(Icons.Rounded.Close, contentDescription = null)
            }
        }
    }
}

private fun exportBookmarks(context: Context, player: Player) {
    val name = player.mediaMetadata.title?.toString() ?: player.mod69Key().orEmpty()
    val text = buildString {
        appendLine(name)
        Mod69Session.bookmarks.forEach { appendLine(getStringForTime(it)) }
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Bookmarks – $name")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Export bookmarks").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun PicturePage(player: Player, videoDecoderMode: DecoderMode?) {
    val scope = rememberCoroutineScope()
    fun push() {
        scope.launch { Mod69Session.sendEffects(player) }
    }
    Text(
        "These need the HW decoder (Decoder menu → HW). They stay on until you press Reset.",
        style = MaterialTheme.typography.bodySmall,
    )
    SwitchRow("Mirror (left ↔ right)", Mod69Session.mirror) {
        Mod69Session.mirror = it
        push()
    }
    SwitchRow("Flip (upside down)", Mod69Session.flip) {
        Mod69Session.flip = it
        push()
    }
    Text("Rotate", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 90, 180, 270).forEach { deg ->
            FilterChip(
                selected = Mod69Session.rotate == deg,
                onClick = {
                    Mod69Session.rotate = deg
                    push()
                },
                label = { Text("$deg°") },
            )
        }
    }
    AdjustSlider("Brightness", Mod69Session.brightness, -0.5f..0.5f, { Mod69Session.brightness = it }, ::push)
    AdjustSlider("Contrast", Mod69Session.contrast, -0.5f..0.5f, { Mod69Session.contrast = it }, ::push)
    AdjustSlider("Saturation", Mod69Session.saturation, -100f..100f, { Mod69Session.saturation = it }, ::push)
    AdjustSlider("Hue", Mod69Session.hue, -180f..180f, { Mod69Session.hue = it }, ::push)
    AdjustSlider("Gamma", Mod69Session.gamma, 0.5f..2f, { Mod69Session.gamma = it }, ::push)
    OutlinedButton(
        onClick = {
            Mod69Session.resetPicture()
            push()
        },
        modifier = Modifier.padding(top = 8.dp),
    ) { Text("Reset") }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
) {
    Text(
        "$label: " + String.format(java.util.Locale.US, "%.2f", value),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
    Slider(value = value, onValueChange = onChange, onValueChangeFinished = onDone, valueRange = range)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AspectPage() {
    val presets = listOf(
        null to "Video's own",
        16f / 9f to "16:9",
        4f / 3f to "4:3",
        21f / 9f to "21:9",
        2.35f to "2.35:1",
        18f / 9f to "18:9",
        5f / 4f to "5:4",
        1f to "1:1",
        9f / 16f to "9:16",
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (ratio, label) ->
            FilterChip(
                selected = Mod69Session.aspect == ratio,
                onClick = { Mod69Session.aspect = ratio },
                label = { Text(label) },
            )
        }
    }
    Text(
        "Stretches the picture to this shape. Use the aspect button for fit / fill / zoom.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun NightPage() {
    Text("Darkness: ${(Mod69Session.nightDim * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
    Slider(value = Mod69Session.nightDim, onValueChange = { Mod69Session.nightDim = it }, valueRange = 0f..0.75f)
    SwitchRow("Warm colour (less blue light)", Mod69Session.nightWarm) { Mod69Session.nightWarm = it }
    OutlinedButton(onClick = {
        Mod69Session.nightDim = 0f
        Mod69Session.nightWarm = false
    }) { Text("Off") }
}

@OptIn(UnstableApi::class)
@Composable
private fun InfoPage(player: Player, videoDecoderMode: DecoderMode?) {
    val context = LocalContext.current
    val uri = player.currentMediaItem?.localConfiguration?.uri
    var fileSize by remember(uri) { mutableLongStateOf(-1L) }
    LaunchedEffect(uri) {
        fileSize = withContext(Dispatchers.IO) { uri?.let { querySize(context, it) } ?: -1L }
    }
    val rows = mutableListOf<Pair<String, String>>()
    rows += "Title" to (player.mediaMetadata.title?.toString() ?: uri?.lastPathSegment.orEmpty())
    rows += "Location" to (uri?.let { Uri.decode(it.toString()) } ?: "—")
    if (fileSize >= 0) rows += "Size" to formatSize(fileSize)
    if (player.duration != C.TIME_UNSET) rows += "Length" to getStringForTime(player.duration)
    rows += "Decoder" to when (videoDecoderMode) {
        null -> "—"
        else -> videoDecoderMode.name
    }
    player.currentTracks.groups.forEach { g ->
        for (i in 0 until g.length) {
            if (!g.isTrackSelected(i)) continue
            val f = g.getTrackFormat(i)
            when (g.type) {
                C.TRACK_TYPE_VIDEO -> {
                    rows += "Video" to listOfNotNull(
                        f.codecs ?: f.sampleMimeType,
                        if (f.width > 0) "${f.width}×${f.height}" else null,
                        if (f.frameRate > 0) String.format(java.util.Locale.US, "%.3f fps", f.frameRate) else null,
                        if (f.bitrate > 0) "${f.bitrate / 1000} kbps" else null,
                    ).joinToString(" · ")
                }
                C.TRACK_TYPE_AUDIO -> {
                    rows += "Audio" to listOfNotNull(
                        f.codecs ?: f.sampleMimeType,
                        f.language,
                        if (f.channelCount > 0) "${f.channelCount} ch" else null,
                        if (f.sampleRate > 0) "${f.sampleRate} Hz" else null,
                        if (f.bitrate > 0) "${f.bitrate / 1000} kbps" else null,
                    ).joinToString(" · ")
                }
                C.TRACK_TYPE_TEXT -> {
                    rows += "Subtitle" to listOfNotNull(f.label, f.language, f.sampleMimeType).joinToString(" · ")
                }
            }
        }
    }
    val audioCount = player.currentTracks.groups.count { it.type == C.TRACK_TYPE_AUDIO }
    val textCount = player.currentTracks.groups.count { it.type == C.TRACK_TYPE_TEXT }
    rows += "Tracks" to "$audioCount audio · $textCount subtitle"
    rows.forEach { (k, v) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(k, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(84.dp))
            Text(v, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun querySize(context: Context, uri: Uri): Long = runCatching {
    if (uri.scheme == "file" || uri.scheme == null) {
        java.io.File(uri.path!!).length()
    } else {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
            if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else -1L
        } ?: -1L
    }
}.getOrDefault(-1L)

private fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1L shl 20).toDouble())
    else -> "${bytes / 1024} KB"
}

/** Saves the current frame (full size) to Pictures/69-Next Player. */
internal suspend fun takeScreenshot(context: Context, player: Player) {
    val uri = player.currentMediaItem?.localConfiguration?.uri
    val posMs = player.currentPosition
    val name = (player.mediaMetadata.title?.toString() ?: "video")
        .replace(Regex("[\\\\/:*?\"<>|]"), "_").take(60)
    val ok = withContext(Dispatchers.IO) {
        if (uri == null || !(uri.scheme == "content" || uri.scheme == "file" || uri.scheme == null)) return@withContext false
        val bmp: Bitmap = runCatching {
            MediaMetadataRetriever().use { r ->
                if (uri.scheme == "content") r.setDataSource(context, uri) else r.setDataSource(uri.path)
                r.getFrameAtTime(posMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
            }
        }.getOrNull() ?: return@withContext false
        val fileName = "${name}_${getStringForTime(posMs).replace(':', '.')}.jpg"
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/69-Next Player")
                }
                val out = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                context.contentResolver.openOutputStream(out)!!.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            } else {
                @Suppress("DEPRECATION")
                val dir = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "69-Next Player")
                dir.mkdirs()
                java.io.File(dir, fileName).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            }
            bmp.recycle()
            true
        }.getOrDefault(false)
    }
    Toast.makeText(
        context,
        if (ok) "Screenshot saved to Pictures/69-Next Player" else "Screenshot failed (only works for files, not network streams)",
        Toast.LENGTH_SHORT,
    ).show()
}
