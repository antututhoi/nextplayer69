package dev.anilbeesetti.nextplayer.feature.player.mod69

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
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
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Audiotrack
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

private enum class ToolsPage { MAIN, SLEEP, AB, BOOKMARKS, PICTURE, ASPECT, NIGHT, INFO, AUDIO, SUBTITLE, CUT, HELP }

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
    onOpenOverlay: (dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView) -> Unit = {},
) {
    var page by remember { mutableStateOf(ToolsPage.MAIN) }
    LaunchedEffect(show) {
        if (!show) {
            page = ToolsPage.MAIN
        } else {
            Mod69Session.toolsStart?.let { start ->
                page = runCatching { ToolsPage.valueOf(start) }.getOrDefault(ToolsPage.MAIN)
                Mod69Session.toolsStart = null
            }
        }
    }
    val title = when (page) {
        ToolsPage.HELP -> "Gesture help"
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
    val side = Mod69Flags.toolsLayout
    if (side != 0) {
        // 69 mod: slim column at the screen edge instead of a big sheet over the video
        androidx.compose.animation.AnimatedVisibility(
            visible = show && page == ToolsPage.MAIN,
            modifier = Modifier.align(if (side == 1) Alignment.CenterStart else Alignment.CenterEnd),
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
        ) {
            val entries = toolEntries(player = player, onOpen = { page = it }, onOpenOverlay = onOpenOverlay)
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                // 69 mod: no panel behind the side buttons, they float over the video like the player buttons
                color = Color.Transparent,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(88.dp)
                    .toolsColumnHeight(Mod69Flags.toolsVisible),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White) }
                    entries.forEach { ToolButton(it.icon, it.label, floating = true, onClick = it.onClick) }
                    ToolButton(Icons.Rounded.SkipPrevious, "Frame back", floating = true) { frameStep(player, -1) }
                    ToolButton(Icons.Rounded.SkipNext, "Frame next", floating = true) { frameStep(player, 1) }
                }
            }
        }
    }
    OverlayView(show = show && (side == 0 || page != ToolsPage.MAIN), title = title) {
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
                ToolsPage.MAIN -> MainPage(player = player, onOpen = { page = it }, onDismiss = onDismiss, onOpenOverlay = onOpenOverlay)
                ToolsPage.HELP -> HelpPage()
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
private fun MainPage(
    player: Player,
    onOpen: (ToolsPage) -> Unit,
    onDismiss: () -> Unit,
    onOpenOverlay: (dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView) -> Unit = {},
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 4,
        modifier = Modifier.fillMaxWidth(),
    ) {
        toolEntries(player = player, onOpen = onOpen, onOpenOverlay = onOpenOverlay).forEach {
            ToolButton(it.icon, it.label, onClick = it.onClick)
        }
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

private class ToolEntry(val id: String, val icon: ImageVector, val label: String, val onClick: () -> Unit)

/** Height that fits [count] tool buttons (the column scrolls for the rest). */
private fun Modifier.toolsColumnHeight(count: Int): Modifier =
    this.then(Modifier.heightIn(max = (count.coerceIn(2, 20) * 80 + 56).dp))

@Composable
private fun toolEntries(
    player: Player,
    onOpen: (ToolsPage) -> Unit,
    onOpenOverlay: (dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView) -> Unit,
): List<ToolEntry> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return listOf(
        ToolEntry("chapters", Icons.AutoMirrored.Rounded.List, "Chapters") { onOpenOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.CHAPTERS) },
        ToolEntry("audio_track", Icons.Rounded.Audiotrack, "Audio track") { onOpenOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.AUDIO_SELECTOR) },
        ToolEntry("subtitle_track", Icons.Rounded.Subtitles, "Subtitle track") { onOpenOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.SUBTITLE_SELECTOR) },
        ToolEntry("speed", Icons.Rounded.Speed, "Speed") { onOpenOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.PLAYBACK_SPEED) },
        ToolEntry("decoder", Icons.Rounded.Memory, "Decoder") { onOpenOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.DECODER_SELECTOR) },
        ToolEntry("aspect", Icons.Rounded.AspectRatio, "Aspect ratio") { onOpen(ToolsPage.ASPECT) },
        ToolEntry("zoom", Icons.Rounded.ZoomOutMap, "Zoom / fit") { onOpenOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.VIDEO_CONTENT_SCALE) },
        ToolEntry("playlist", Icons.AutoMirrored.Rounded.PlaylistPlay, "Playlist") { onOpenOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.PLAYLIST) },
        ToolEntry("sleep", Icons.Rounded.Bedtime, "Sleep timer") { onOpen(ToolsPage.SLEEP) },
        ToolEntry("ab", Icons.Rounded.Repeat, "A-B repeat") { onOpen(ToolsPage.AB) },
        ToolEntry("screenshot", Icons.Rounded.CameraAlt, "Screenshot") { scope.launch { takeScreenshot(context, player) } },
        ToolEntry("bookmark", Icons.Rounded.Bookmark, "Bookmarks") { onOpen(ToolsPage.BOOKMARKS) },
        ToolEntry("night", Icons.Rounded.NightsStay, "Night mode") { onOpen(ToolsPage.NIGHT) },
        ToolEntry("picture", Icons.Rounded.Tune, "Picture") { onOpen(ToolsPage.PICTURE) },
        ToolEntry("equalizer", Icons.Rounded.GraphicEq, "Equalizer") { onOpen(ToolsPage.AUDIO) },
        ToolEntry("sub_look", Icons.Rounded.ClosedCaption, "Subtitle look") { onOpen(ToolsPage.SUBTITLE) },
        ToolEntry("info", Icons.Rounded.Info, "Information") { onOpen(ToolsPage.INFO) },
        ToolEntry("cut", Icons.Rounded.ContentCut, "Cut / MP3") { onOpen(ToolsPage.CUT) },
        ToolEntry("share", Icons.Rounded.Share, "Share") {
            player.currentMediaItem?.localConfiguration?.uri?.let { uri ->
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching {
                    context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        },
        ToolEntry("help", Icons.Rounded.TouchApp, "Gesture help") { onOpen(ToolsPage.HELP) },
        ToolEntry("loop", Icons.Rounded.Repeat, "Loop") {
            player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            android.widget.Toast.makeText(context, if (player.repeatMode == Player.REPEAT_MODE_OFF) "Loop off" else "Loop this video", android.widget.Toast.LENGTH_SHORT).show()
        },
        ToolEntry("shuffle", Icons.Rounded.Shuffle, "Shuffle") {
            player.shuffleModeEnabled = !player.shuffleModeEnabled
            android.widget.Toast.makeText(context, if (player.shuffleModeEnabled) "Shuffle on" else "Shuffle off", android.widget.Toast.LENGTH_SHORT).show()
        },
        ToolEntry("mute", Icons.AutoMirrored.Rounded.VolumeOff, "Mute") {
            if (player.volume > 0f) {
                Mod69Session.volumeBeforeMute = player.volume
                player.volume = 0f
            } else {
                player.volume = Mod69Session.volumeBeforeMute.takeIf { it > 0f } ?: 1f
            }
        },
    ).filter { it.id !in Mod69Session.placedItems && it.id !in Mod69Session.hiddenItems }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ToolButton(icon: ImageVector, label: String, floating: Boolean = false, onClick: () -> Unit) {
    val context = LocalContext.current
    // MX-style tile: round icon with the name under it; long-press always tells the name
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { android.widget.Toast.makeText(context, label, android.widget.Toast.LENGTH_SHORT).show() },
            )
            .padding(vertical = 4.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                // 69 mod: on the side bar the circle uses the player button background setting (can be see-through)
                .background(
                    if (floating) LocalMod69ButtonBackground.current else MaterialTheme.colorScheme.secondaryContainer,
                ),
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (floating) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        if (Mod69Session.buttonLabels) {
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f),
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

internal fun frameStep(player: Player, direction: Int) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookmarksPage(player: Player) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Mod69Session.loadBookmarks(context, player.mod69Key()) }
    val title = player.mediaMetadata.title?.toString() ?: player.mod69Key().orEmpty()
    var editing by remember { mutableStateOf<Long?>(null) }
    var askClear by remember { mutableStateOf(false) }
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        dev.anilbeesetti.nextplayer.core.common.mod69.Mod69PickerContracts.Create("text/plain"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { it.write(Mod69Session.bookmarksText(title).toByteArray()) }
                android.widget.Toast.makeText(context, "Bookmarks exported", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        dev.anilbeesetti.nextplayer.core.common.mod69.Mod69PickerContracts.Open(),
    ) { uri ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)!!.bufferedReader().readText() }.getOrNull()
            val n = if (text == null) 0 else Mod69Session.importBookmarks(context, text)
            android.widget.Toast.makeText(context, "$n bookmark(s) added", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilledTonalButton(onClick = { Mod69Session.addBookmark(context, player.currentPosition) }) {
            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
            Text("  Add here")
        }
        OutlinedButton(enabled = Mod69Session.bookmarks.isNotEmpty(), onClick = {
            runCatching { exportLauncher.launch("$title bookmarks.txt") }
        }) { Text("Export") }
        OutlinedButton(onClick = { runCatching { importLauncher.launch(arrayOf("text/*", "*/*")) } }) { Text("Import") }
        OutlinedButton(enabled = Mod69Session.bookmarks.isNotEmpty(), onClick = { exportBookmarks(context, player) }) { Text("Share") }
        if (Mod69Session.bookmarks.isNotEmpty()) {
            TextButton(onClick = { askClear = true }) { Text("Delete all") }
        }
    }
    if (Mod69Session.bookmarks.isEmpty()) {
        Text(
            "No bookmarks yet. Tap \"Add here\" or the bookmark button. They show as marks on the seek bar.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    val now = player.currentPosition
    Mod69Session.bookmarks.toList().forEachIndexed { index, ms ->
        val name = Mod69Session.bookmarkNames[ms]
        val isNear = kotlin.math.abs(now - ms) < 1500
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = if (isNear) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { player.seekTo(ms) },
                    ) {
                        Text(getStringForTime(ms), style = MaterialTheme.typography.titleMedium)
                        Text(
                            name ?: "Tap to jump · ✎ to name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { editing = ms }) { Icon(Icons.Rounded.Edit, contentDescription = "Rename") }
                    IconButton(onClick = { Mod69Session.removeBookmark(context, ms) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                    }
                }
                // Move: nudge the time, or set it to where the video is now
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Move", style = MaterialTheme.typography.labelSmall)
                    listOf(-5000L to "−5s", -1000L to "−1s", 1000L to "+1s", 5000L to "+5s").forEach { (d, label) ->
                        TextButton(
                            onClick = { Mod69Session.moveBookmark(context, ms, ms + d) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                        ) { Text(label) }
                    }
                    TextButton(
                        onClick = { Mod69Session.moveBookmark(context, ms, player.currentPosition) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                    ) { Text("To now") }
                }
            }
        }
    }
    editing?.let { ms ->
        var text by remember(ms) { mutableStateOf(Mod69Session.bookmarkNames[ms].orEmpty()) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Name for " + getStringForTime(ms)) },
            text = {
                androidx.compose.material3.OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    Mod69Session.renameBookmark(context, ms, text)
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    if (askClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { askClear = false },
            title = { Text("Delete all bookmarks of this video?") },
            confirmButton = {
                TextButton(onClick = {
                    Mod69Session.clearBookmarks(context)
                    askClear = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { askClear = false }) { Text("Cancel") } },
        )
    }
}

private fun exportBookmarks(context: Context, player: Player) {
    val name = player.mediaMetadata.title?.toString() ?: player.mod69Key().orEmpty()
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Bookmarks – $name")
        putExtra(Intent.EXTRA_TEXT, Mod69Session.bookmarksText(name))
    }
    context.startActivity(Intent.createChooser(send, "Share bookmarks").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
    var w by remember { mutableStateOf("") }
    var h by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Text("Custom ")
        androidx.compose.material3.OutlinedTextField(
            value = w, onValueChange = { w = it.filter { c -> c.isDigit() || c == '.' } },
            modifier = Modifier.width(70.dp), singleLine = true, label = { Text("W") },
        )
        Text(" : ")
        androidx.compose.material3.OutlinedTextField(
            value = h, onValueChange = { h = it.filter { c -> c.isDigit() || c == '.' } },
            modifier = Modifier.width(70.dp), singleLine = true, label = { Text("H") },
        )
        TextButton(onClick = {
            val r = (w.toFloatOrNull() ?: 0f) / (h.toFloatOrNull() ?: 0f)
            if (r.isFinite() && r > 0f) Mod69Session.aspect = r
        }) { Text("Set") }
    }
    SwitchRow("Apply to all videos", Mod69Session.aspectAll) { on ->
        Mod69Session.aspectAll = on
        val keep = if (on) Mod69Session.aspect ?: 0f else 0f
        Mod69Session.savePrefs { it.copy(mod69AspectAll = keep) }
    }
    LaunchedEffect(Mod69Session.aspect) {
        if (Mod69Session.aspectAll) {
            val keep = Mod69Session.aspect ?: 0f
            Mod69Session.savePrefs { it.copy(mod69AspectAll = keep) }
        }
    }
    Text(
        "Stretches the picture to this shape. Use the aspect button for fit / fill / zoom.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun HelpPage() {
    listOf(
        "Double-tap left / right" to "Rewind / fast forward",
        "Double-tap middle" to "Play / pause (when double-tap is set to both)",
        "Swipe up/down on the left half" to "Brightness",
        "Swipe up/down on the right half" to "Volume (sides can be swapped in 69 settings)",
        "Swipe left / right" to "Seek",
        "Pinch" to "Zoom, then drag to move the picture",
        "Long press" to "Fast forward at the long-press speed",
        "Tap" to "Show / hide controls (or play/pause, see 69 settings)",
        "Lock button" to "Locks the screen. With kids lock, long-press the lock to unlock",
        "Back twice" to "Close (when double back is on)",
    ).forEach { (g, a) ->
        Text(g, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
        Text(a, style = MaterialTheme.typography.bodySmall)
    }
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
