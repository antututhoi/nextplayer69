package dev.anilbeesetti.nextplayer.feature.player.mod69

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.feature.player.buttons.LoopButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.NextButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayPauseButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlaybackSpeedButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PreviousButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.RotateButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.ShuffleButton
import dev.anilbeesetti.nextplayer.feature.player.extensions.drawableRes
import dev.anilbeesetti.nextplayer.feature.player.state.ChaptersState
import dev.anilbeesetti.nextplayer.feature.player.ui.controls.PlayerSeekbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** 69 mod: background colour behind every player button (transparent by default). */
val LocalMod69ButtonBackground = compositionLocalOf { Color.Transparent }

/** Background picked in 69 settings, or null to keep the normal (Material You) look. */
val LocalMod69ButtonBgOverride = compositionLocalOf<Color?> { null }

/** Same size for every round player button in the 69 layout (null = original sizes). */
val LocalMod69ButtonSize = compositionLocalOf<androidx.compose.ui.unit.Dp?> { null }

fun Long.toArgbColor(): Color = Color(this.toInt())

/** 69 mod: player actions the buttons need (lock, zoom mode). */
class Mod69Actions(
    val lock: () -> Unit = {},
    val scale: () -> Unit = {},
    val scaleLong: () -> Unit = {},
    val scaleIcon: Int = 0,
)

val LocalMod69Actions = compositionLocalOf { Mod69Actions() }

/** The quick icons that can show in the row under the title. */
enum class QuickItem(val id: String) {
    SPEED("speed"),
    BACKGROUND("background"),
    ROTATE("rotate"),
    LOOP("loop"),
    SHUFFLE("shuffle"),
    PIP("pip"),
    TOOLS("tools"),
    SCREENSHOT("screenshot"),
    BOOKMARK("bookmark"),
    NIGHT("night"),
    MUTE("mute"),
    EQUALIZER("equalizer"),
    SLEEP("sleep"),
    AB("ab"),
    HELP("help"),
    DECODER("decoder"),
    PLAYLIST("playlist"),
    AUDIO_TRACK("audio_track"),
    SUBTITLE_TRACK("subtitle_track"),
    ASPECT("aspect"),
    ZOOM("zoom"),
    PICTURE("picture"),
    INFO("info"),
    SUB_LOOK("sub_look"),
    CUT("cut"),
    SHARE("share"),
    CHAPTERS("chapters"),
    LOCK("lock"),
    SCALE("scale"),
    FRAME_BACK("frame_back"),
    FRAME_NEXT("frame_next"),
    ;

    val label: String
        get() = when (this) {
            SPEED -> "Speed"
            BACKGROUND -> "Background"
            ROTATE -> "Rotation"
            LOOP -> "Loop"
            SHUFFLE -> "Shuffle"
            PIP -> "Picture-in-picture"
            TOOLS -> "Tools"
            SCREENSHOT -> "Screenshot"
            BOOKMARK -> "Bookmark"
            NIGHT -> "Night mode"
            MUTE -> "Mute"
            EQUALIZER -> "Equalizer"
            SLEEP -> "Sleep timer"
            AB -> "A-B repeat"
            HELP -> "Gesture help"
            DECODER -> "Decoder"
            PLAYLIST -> "Playlist"
            AUDIO_TRACK -> "Audio"
            SUBTITLE_TRACK -> "Subtitles"
            ASPECT -> "Aspect"
            ZOOM -> "Zoom"
            PICTURE -> "Picture"
            INFO -> "Info"
            SUB_LOOK -> "Sub look"
            CUT -> "Cut"
            SHARE -> "Share"
            CHAPTERS -> "Chapters"
            LOCK -> "Lock"
            SCALE -> "Zoom mode"
            FRAME_BACK -> "Frame back"
            FRAME_NEXT -> "Frame next"
        }

    /** Short name under the button. */
    val shortLabel: String
        get() = when (this) {
            BACKGROUND -> "Background"
            PIP -> "PiP"
            SCREENSHOT -> "Shot"
            NIGHT -> "Night"
            EQUALIZER -> "EQ"
            SLEEP -> "Sleep"
            AB -> "A-B"
            HELP -> "Help"
            else -> label
        }

    companion object {
        fun fromIds(ids: List<String>): List<QuickItem> = ids.mapNotNull { id -> entries.find { it.id == id } }
    }
}

/** The row of quick icons under the title (MX's quick shortcuts). */
@OptIn(UnstableApi::class)
@Composable
internal fun Mod69QuickRow(
    player: Player?,
    preferences: PlayerPreferences,
    isPipSupported: Boolean,
    isTv: Boolean,
    onPlaybackSpeedClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToolsClick: () -> Unit = {},
    /** 69 mod: which buttons this row shows; null = the quick row (Row 2). */
    itemIds: List<String>? = null,
    /** Row 2 has the "›" arrow with the buttons that are in no row. */
    expandable: Boolean = true,
    onOverlay: (dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView) -> Unit = {},
    decoderLabel: String = "HW",
    /** 69 mod: shown inside the title line (no own width / side padding). */
    inline: Boolean = false,
    /** 69 mod: a column (side bar) instead of a row. */
    vertical: Boolean = false,
) {
    val items = QuickItem.fromIds(itemIds ?: preferences.mod69QuickItems)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var customise by remember { mutableStateOf(false) }
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val labels = preferences.mod69ButtonLabels

    fun toast(item: QuickItem) {
        android.widget.Toast.makeText(context, item.label, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun openTools(page: String) {
        Mod69Session.toolsStart = page
        onToolsClick()
    }

    @Composable
    fun B(item: QuickItem, onClick: () -> Unit, content: @Composable () -> Unit) {
        PlayerButton(onClick = onClick, onLongClick = { toast(item) }) { content() }
    }

    @Composable
    fun Ic(v: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean = false) {
        Icon(imageVector = v, contentDescription = null, tint = if (on) MaterialTheme.colorScheme.primary else Color.White)
    }

    @Composable
    fun ItemButton(item: QuickItem) {
        when (item) {
            QuickItem.SPEED -> PlaybackSpeedButton(player = player, onClick = onPlaybackSpeedClick)
            QuickItem.BACKGROUND -> B(item, onPlayInBackgroundClick) {
                Icon(painter = painterResource(R.drawable.ic_headset), contentDescription = null)
            }
            QuickItem.ROTATE -> if (!isTv) RotateButton()
            QuickItem.LOOP -> LoopButton(player = player)
            QuickItem.SHUFFLE -> ShuffleButton(player = player)
            QuickItem.PIP -> if (isPipSupported) {
                B(item, onPictureInPictureClick) { Icon(painter = painterResource(R.drawable.ic_pip), contentDescription = null) }
            }
            QuickItem.TOOLS -> B(item, onToolsClick) { Ic(Icons.Rounded.Apps) }
            QuickItem.SCREENSHOT -> B(item, { player?.let { p -> scope.launch { takeScreenshot(context, p) } } }) {
                Ic(Icons.Rounded.CameraAlt)
            }
            // 69 mod: like MX: tap = the bookmark list (jump, add, rename, delete, move, import/export),
            // hold = add a bookmark here at once
            QuickItem.BOOKMARK -> PlayerButton(
                onClick = { openTools("BOOKMARKS") },
                onLongClick = {
                    player?.let { p ->
                        Mod69Session.loadBookmarks(context, p.mod69Key())
                        Mod69Session.addBookmark(context, p.currentPosition)
                        android.widget.Toast.makeText(context, "Bookmark added", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
            ) { Ic(Icons.Rounded.Bookmarks) }
            QuickItem.NIGHT -> B(item, { Mod69Session.nightDim = if (Mod69Session.nightDim > 0f) 0f else 0.4f }) {
                Ic(Icons.Rounded.NightsStay, Mod69Session.nightDim > 0f)
            }
            QuickItem.MUTE -> {
                var muted by remember { mutableStateOf(player?.volume == 0f) }
                B(item, {
                    player?.let { p ->
                        if (p.volume > 0f) {
                            Mod69Session.volumeBeforeMute = p.volume
                            p.volume = 0f
                        } else {
                            p.volume = Mod69Session.volumeBeforeMute.takeIf { it > 0f } ?: 1f
                        }
                        muted = p.volume == 0f
                    }
                }) { Ic(if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, muted) }
            }
            QuickItem.EQUALIZER -> B(item, { openTools("AUDIO") }) { Ic(Icons.Rounded.GraphicEq) }
            QuickItem.SLEEP -> B(item, { openTools("SLEEP") }) { Ic(Icons.Rounded.Bedtime) }
            QuickItem.AB -> B(item, { openTools("AB") }) { Ic(Icons.Rounded.Repeat, Mod69Session.aMs >= 0) }
            QuickItem.HELP -> B(item, { openTools("HELP") }) { Ic(Icons.Rounded.TouchApp) }
            QuickItem.DECODER -> B(item, { onOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.DECODER_SELECTOR) }) {
                Text(decoderLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
            QuickItem.CHAPTERS -> B(item, { onOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.CHAPTERS) }) {
                Ic(Icons.AutoMirrored.Rounded.List)
            }
            QuickItem.PLAYLIST -> B(item, { onOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.PLAYLIST) }) {
                Icon(painter = painterResource(R.drawable.ic_playlist), contentDescription = null)
            }
            QuickItem.AUDIO_TRACK -> B(item, { onOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.AUDIO_SELECTOR) }) {
                Icon(painter = painterResource(R.drawable.ic_audio_track), contentDescription = null)
            }
            QuickItem.SUBTITLE_TRACK -> B(item, { onOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.SUBTITLE_SELECTOR) }) {
                Icon(painter = painterResource(R.drawable.ic_subtitle_track), contentDescription = null)
            }
            QuickItem.ZOOM -> B(item, { onOverlay(dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView.VIDEO_CONTENT_SCALE) }) {
                Ic(Icons.Rounded.ZoomOutMap)
            }
            QuickItem.ASPECT -> B(item, { openTools("ASPECT") }) { Ic(Icons.Rounded.AspectRatio) }
            QuickItem.PICTURE -> B(item, { openTools("PICTURE") }) { Ic(Icons.Rounded.Tune) }
            QuickItem.INFO -> B(item, { openTools("INFO") }) { Ic(Icons.Rounded.Info) }
            QuickItem.SUB_LOOK -> B(item, { openTools("SUBTITLE") }) { Ic(Icons.Rounded.ClosedCaption) }
            QuickItem.CUT -> B(item, { openTools("CUT") }) { Ic(Icons.Rounded.ContentCut) }
            QuickItem.SHARE -> B(item, { player?.let { mod69Share(context, it) } }) { Ic(Icons.Rounded.Share) }
            QuickItem.LOCK -> B(item, LocalMod69Actions.current.lock) {
                Icon(painter = painterResource(R.drawable.ic_lock_open), contentDescription = null)
            }
            QuickItem.SCALE -> {
                val actions = LocalMod69Actions.current
                PlayerButton(onClick = actions.scale, onLongClick = actions.scaleLong) {
                    if (actions.scaleIcon != 0) {
                        Icon(painter = painterResource(actions.scaleIcon), contentDescription = null)
                    } else {
                        Ic(Icons.Rounded.ZoomOutMap)
                    }
                }
            }
            QuickItem.FRAME_BACK -> B(item, { player?.let { frameStep(it, -1) } }) {
                Ic(Icons.AutoMirrored.Rounded.KeyboardArrowLeft)
            }
            QuickItem.FRAME_NEXT -> B(item, { player?.let { frameStep(it, 1) } }) {
                Ic(Icons.AutoMirrored.Rounded.KeyboardArrowRight)
            }
        }
    }

    @Composable
    fun Labelled(item: QuickItem) {
        if (!labels) {
            ItemButton(item)
            return
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ItemButton(item)
            Text(
                item.shortLabel,
                color = Color.White,
                fontSize = 9.sp,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }

    val placed = (preferences.mod69TopRowItems + preferences.mod69QuickItems + preferences.mod69BottomItems).toSet()
    val rest = QuickItem.entries.filter { it.id !in placed && it.id !in preferences.mod69HiddenItems && it != QuickItem.TOOLS }
    if (vertical) {
        androidx.compose.foundation.layout.Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEach { Labelled(it) }
        }
    } else
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        // 69 mod: Row 1 (under the title) is centred; Row 2 starts at the left
        androidx.compose.foundation.layout.Box(
            modifier = if (inline) {
                Modifier
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
                    .padding(horizontal = 12.dp)
            },
            contentAlignment = if (expandable) Alignment.TopStart else Alignment.TopCenter,
        ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            items.forEach { Labelled(it) }
            if (expandable && rest.isNotEmpty()) {
                PlayerButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
                if (expanded) {
                    rest.forEach { Labelled(it) }
                }
            }
            if (expandable && expanded) {
                PlayerButton(onClick = { customise = true }) {
                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                }
            }
        }
        }
    }

    if (customise) {
        val chosen = remember { androidx.compose.runtime.mutableStateListOf<QuickItem>().apply { addAll(items) } }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { customise = false },
            title = { Text("Customise items") },
            text = {
                androidx.compose.foundation.layout.Column(
                    Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text("Ticked items show in the row, in the order you tick them. The rest are behind the arrow.", style = MaterialTheme.typography.bodySmall)
                    QuickItem.entries.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (item in chosen) chosen.remove(item) else chosen.add(item) },
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = item in chosen,
                                onCheckedChange = { if (it) chosen.add(item) else chosen.remove(item) },
                            )
                            Text(item.label)
                            if (item in chosen) {
                                Text("  #" + (chosen.indexOf(item) + 1), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val ids = chosen.map { it.id }
                    Mod69Session.savePrefs {
                        it.copy(
                            mod69QuickItems = ids,
                            mod69TopRowItems = it.mod69TopRowItems - ids.toSet(),
                            mod69HiddenItems = it.mod69HiddenItems - ids.toSet(),
                        )
                    }
                    customise = false
                }) { Text("Apply") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { customise = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * MX-style bottom controls: time | seekbar | duration, then one evenly spread row with
 * lock, -10s, previous, play/pause, next, +10s and the aspect button.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun Mod69BottomControls(
    player: Player?,
    progressState: ProgressStateWithTickInterval,
    chaptersState: ChaptersState,
    seekIncrementMs: Long,
    showRemainingTime: Boolean,
    videoContentScale: VideoContentScale,
    onToggleTimeDisplay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
    onLockControlsClick: () -> Unit,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    seekPreview: Boolean = true,
    frameStepButtons: Boolean = false,
    /** 69 mod: the chosen bottom-bar buttons, left and right of the play buttons. */
    bottomLeft: @Composable () -> Unit = {},
    bottomRight: @Composable () -> Unit = {},
) {
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(systemBarsPadding.copy(top = 0.dp))
            .padding(horizontal = 12.dp)
            .padding(bottom = if (systemBarsPadding.calculateBottomPadding() == 0.dp) 8.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        var previewPos by remember { mutableStateOf<Long?>(null) }
        val previewDuration = progressState.durationMs
        if (seekPreview) {
            Mod69SeekPreview(
                player = player,
                positionMs = previewPos?.takeIf { previewDuration > 0 },
                fraction = ((previewPos ?: 0L).toFloat() / previewDuration.coerceAtLeast(1L)).coerceIn(0f, 1f),
            )
        }
        // 69 mod: chapter name above the finger while dragging the seek bar
        val dragChapters = chaptersState.chapters
        val dragPos = previewPos
        if (dragPos != null && dragChapters.isNotEmpty() && previewDuration > 0) {
            val idx = dragChapters.indexOfLast { it.startTimeMs <= dragPos }.coerceAtLeast(0)
            val frac = (dragPos.toFloat() / previewDuration).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${idx + 1}/${dragChapters.size}  " + with(dev.anilbeesetti.nextplayer.feature.player.ui.ChapterTitles) { dragChapters[idx].mod69Title(idx) },
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(androidx.compose.ui.BiasAlignment(frac * 2f - 1f, 0f))
                        .widthIn(max = 260.dp)
                        .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val position = progressState.currentPositionMs
            val duration = progressState.durationMs
            Text(
                text = getStringForTime(position),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(end = 8.dp),
            )
            val ctx = LocalContext.current
            val key = player?.currentMediaItem?.localConfiguration?.uri?.toString()
            LaunchedEffect(key) { player?.let { Mod69Session.loadBookmarks(ctx, it.mod69Key()) } }
            // 69 mod: give cloud folders the duration the player found (so the list shows length + progress)
            LaunchedEffect(key, duration > 0) {
                if (key != null && duration > 0 && key.startsWith("content://")) {
                    dev.anilbeesetti.nextplayer.core.common.mod69.SafLibrary.rememberDuration(ctx, key, duration)
                }
            }
            // 69 mod: frame-step buttons at both ends of the seek bar (pause and move one frame)
            if (frameStepButtons && player != null) {
                PlayerButton(
                    onClick = { frameStep(player, -1) },
                    contentPadding = PaddingValues(4.dp),
                    fixedSize = false,
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous frame")
                }
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = if (frameStepButtons) 2.dp else 0.dp)) {
            PlayerSeekbar(
                modifier = Modifier.fillMaxWidth(),
                position = position.toFloat(),
                duration = duration.toFloat(),
                chapters = chaptersState.chapters,
                onSeek = {
                    // 69 mod: optional snap to the nearest chapter start
                    var target = it.toLong()
                    val chs = chaptersState.chapters
                    if (Mod69Flags.chapterSnap && chs.isNotEmpty() && duration > 0) {
                        val near = chs.minByOrNull { c -> kotlin.math.abs(c.startTimeMs - target) }
                        if (near != null && kotlin.math.abs(near.startTimeMs - target) <= duration / 60) target = near.startTimeMs
                    }
                    previewPos = target
                    onSeek(target)
                },
                onSeekFinished = {
                    previewPos = null
                    onSeekEnd()
                },
            )
            // 69 mod: bookmark dots and A-B markers on the seekbar
            if (duration > 0 && (Mod69Session.bookmarks.isNotEmpty() || Mod69Session.aMs >= 0)) {
                val marks = Mod69Session.bookmarks.toList()
                val a = Mod69Session.aMs
                val b = Mod69Session.bMs
                val bookmarkColor = MaterialTheme.colorScheme.tertiary
                Canvas(modifier = Modifier.matchParentSize()) {
                    val pad = 10.dp.toPx()
                    val w = size.width - 2 * pad
                    val y = size.height / 2 - 7.dp.toPx()
                    marks.forEach { ms ->
                        // 69 mod: a clear tick in the app colour (was a tiny yellow dot)
                        val x = pad + w * ms / duration
                        drawRoundRect(
                            color = bookmarkColor,
                            topLeft = Offset(x - 2.dp.toPx(), y - 7.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 14.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                        )
                        drawCircle(Color.White, radius = 1.5.dp.toPx(), center = Offset(x, y))
                    }
                    if (a >= 0) {
                        val xa = pad + w * a / duration
                        val xb = if (b > a) pad + w * b / duration else xa
                        drawLine(Color(0xFF4CAF50), Offset(xa, y), Offset(xb, y), strokeWidth = 3.dp.toPx())
                        drawCircle(Color(0xFF4CAF50), radius = 3.dp.toPx(), center = Offset(xa, y))
                        if (b > a) drawCircle(Color(0xFF4CAF50), radius = 3.dp.toPx(), center = Offset(xb, y))
                    }
                }
            }
            }
            if (frameStepButtons && player != null) {
                PlayerButton(
                    onClick = { frameStep(player, 1) },
                    contentPadding = PaddingValues(4.dp),
                    fixedSize = false,
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next frame")
                }
            }
            PlayerButton(
                onClick = onToggleTimeDisplay,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                fixedSize = false,
            ) {
                Text(
                    text = if (showRemainingTime && duration != C.TIME_UNSET) {
                        "-" + getStringForTime(duration - position)
                    } else {
                        getStringForTime(duration)
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomLeft()
            PlayerButton(onClick = { player?.let { it.seekTo((it.currentPosition - seekIncrementMs).coerceAtLeast(0)) } }) {
                Icon(imageVector = Icons.Rounded.Replay10, contentDescription = null)
            }
            val chapterCtx = LocalContext.current
            val chs = chaptersState.chapters
            PreviousButton(
                player = player,
                onLongClick = if (chs.isNotEmpty() && player != null) { { mod69JumpChapter(chapterCtx, player, chs, -1) } } else null,
            )
            PlayPauseButton(player = player)
            NextButton(
                player = player,
                onLongClick = if (chs.isNotEmpty() && player != null) { { mod69JumpChapter(chapterCtx, player, chs, 1) } } else null,
            )
            PlayerButton(onClick = {
                player?.let {
                    val target = it.currentPosition + seekIncrementMs
                    it.seekTo(if (it.duration != C.TIME_UNSET) target.coerceAtMost(it.duration) else target)
                }
            }) {
                Icon(imageVector = Icons.Rounded.Forward10, contentDescription = null)
            }
            bottomRight()
        }
    }
}

/** Thin progress line along the bottom edge while the controls are hidden. */
@OptIn(UnstableApi::class)
@Composable
internal fun BoxScope.Mod69MiniProgressBar(progressState: ProgressStateWithTickInterval, preferences: PlayerPreferences) {
    val duration = progressState.durationMs
    if (duration <= 0 || duration == C.TIME_UNSET) return
    val fraction = (progressState.currentPositionMs.toFloat() / duration).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .height(preferences.mod69MiniBarHeight.coerceIn(1, 12).dp)
            .background(Color.White.copy(alpha = 0.15f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(preferences.mod69MiniBarHeight.coerceIn(1, 12).dp)
                .background(preferences.mod69MiniProgressColor.toArgbColor()),
        )
    }
}

/** Elapsed / total time (top left) and battery + clock (top right) while the controls are hidden. */
@OptIn(UnstableApi::class)
@Composable
internal fun BoxScope.Mod69CornerInfo(progressState: ProgressStateWithTickInterval, preferences: PlayerPreferences) {
    if (!preferences.mod69CornerTime && !preferences.mod69CornerBatteryClock) return
    val color = preferences.mod69CornerTextColor.toArgbColor()
    val padding = WindowInsets.displayCutout.asPaddingValues()
    if (preferences.mod69CornerTime) {
        Text(
            text = getStringForTime(progressState.currentPositionMs) + " / " + getStringForTime(progressState.durationMs),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
    if (preferences.mod69CornerBatteryClock) {
        val context = LocalContext.current
        var text by remember { mutableStateOf(batteryAndClock(context)) }
        LaunchedEffect(Unit) {
            while (true) {
                text = batteryAndClock(context)
                delay(15_000)
            }
        }
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun batteryAndClock(context: Context): String {
    val battery: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) "${level * 100 / scale}%  " else ""
    return percent + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}


/** 69 mod: share the playing video. */
internal fun mod69Share(context: android.content.Context, player: Player) {
    val uri = player.currentMediaItem?.localConfiguration?.uri ?: return
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "video/*"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(
            android.content.Intent.createChooser(send, "Share").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}


/** 69 mod: jump to the next / previous chapter (previous = start of this one unless within its first 3 s). */
internal fun mod69JumpChapter(
    context: android.content.Context,
    player: Player,
    chapters: List<androidx.media3.extractor.metadata.Chapter>,
    direction: Int,
) {
    if (chapters.isEmpty()) return
    val pos = player.currentPosition
    val cur = chapters.indexOfLast { it.startTimeMs <= pos }
    val target = if (direction > 0) {
        (cur + 1).takeIf { it <= chapters.lastIndex }
    } else {
        when {
            cur < 0 -> 0
            pos - chapters[cur].startTimeMs < 3000 -> (cur - 1).coerceAtLeast(0)
            else -> cur
        }
    } ?: run {
        android.widget.Toast.makeText(context, "Last chapter", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    player.seekTo(chapters[target].startTimeMs)
    val name = chapters[target].title?.value?.takeIf { it.isNotBlank() } ?: "Chapter ${target + 1}"
    android.widget.Toast.makeText(context, "${target + 1}/${chapters.size}  $name", android.widget.Toast.LENGTH_SHORT).show()
}

/** 69 mod: icon of the current zoom mode (for the zoom-mode button). */
internal fun mod69ScaleIcon(scale: VideoContentScale): Int = scale.drawableRes()
