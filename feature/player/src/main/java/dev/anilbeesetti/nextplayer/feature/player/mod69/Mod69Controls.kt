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
import androidx.compose.material.icons.rounded.BookmarkAdd
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
) {
    val items = QuickItem.fromIds(preferences.mod69QuickItems)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var customise by remember { mutableStateOf(false) }
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()

    @Composable
    fun ItemButton(item: QuickItem) {
        when (item) {
            QuickItem.SPEED -> PlaybackSpeedButton(player = player, onClick = onPlaybackSpeedClick)

            QuickItem.BACKGROUND -> PlayerButton(onClick = onPlayInBackgroundClick) {
                Icon(painter = painterResource(R.drawable.ic_headset), contentDescription = null)
            }

            QuickItem.ROTATE -> if (!isTv) RotateButton()

            QuickItem.LOOP -> LoopButton(player = player)

            QuickItem.SHUFFLE -> ShuffleButton(player = player)

            QuickItem.PIP -> if (isPipSupported) {
                PlayerButton(onClick = onPictureInPictureClick) {
                    Icon(painter = painterResource(R.drawable.ic_pip), contentDescription = null)
                }
            }

            QuickItem.TOOLS -> PlayerButton(onClick = onToolsClick) {
                Icon(imageVector = Icons.Rounded.Apps, contentDescription = null)
            }

            QuickItem.SCREENSHOT -> PlayerButton(onClick = {
                player?.let { p -> scope.launch { takeScreenshot(context, p) } }
            }) {
                Icon(imageVector = Icons.Rounded.CameraAlt, contentDescription = null)
            }

            QuickItem.BOOKMARK -> PlayerButton(onClick = {
                player?.let { p ->
                    Mod69Session.loadBookmarks(context, p.mod69Key())
                    Mod69Session.addBookmark(context, p.currentPosition)
                    android.widget.Toast.makeText(context, "Bookmark added", android.widget.Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(imageVector = Icons.Rounded.BookmarkAdd, contentDescription = null)
            }

            QuickItem.NIGHT -> PlayerButton(onClick = {
                Mod69Session.nightDim = if (Mod69Session.nightDim > 0f) 0f else 0.4f
            }) {
                Icon(
                    imageVector = Icons.Rounded.NightsStay,
                    contentDescription = null,
                    tint = if (Mod69Session.nightDim > 0f) MaterialTheme.colorScheme.primary else Color.White,
                )
            }

            QuickItem.MUTE -> {
                var muted by remember { mutableStateOf(player?.volume == 0f) }
                PlayerButton(onClick = {
                    player?.let { p ->
                        if (p.volume > 0f) {
                            Mod69Session.volumeBeforeMute = p.volume
                            p.volume = 0f
                        } else {
                            p.volume = Mod69Session.volumeBeforeMute.takeIf { it > 0f } ?: 1f
                        }
                        muted = p.volume == 0f
                    }
                }) {
                    Icon(
                        imageVector = if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = if (muted) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
            }

            QuickItem.EQUALIZER -> PlayerButton(onClick = {
                Mod69Session.toolsStart = "AUDIO"
                onToolsClick()
            }) { Icon(imageVector = Icons.Rounded.GraphicEq, contentDescription = null) }

            QuickItem.SLEEP -> PlayerButton(onClick = {
                Mod69Session.toolsStart = "SLEEP"
                onToolsClick()
            }) { Icon(imageVector = Icons.Rounded.Bedtime, contentDescription = null) }

            QuickItem.AB -> PlayerButton(onClick = {
                Mod69Session.toolsStart = "AB"
                onToolsClick()
            }) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = if (Mod69Session.aMs >= 0) MaterialTheme.colorScheme.primary else Color.White,
                )
            }

            QuickItem.HELP -> PlayerButton(onClick = {
                Mod69Session.toolsStart = "HELP"
                onToolsClick()
            }) { Icon(imageVector = Icons.Rounded.TouchApp, contentDescription = null) }
        }
    }

    val rest = QuickItem.entries.filter { it !in items }
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
                .padding(horizontal = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { ItemButton(it) }
            PlayerButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
            if (expanded) {
                rest.forEach { ItemButton(it) }
                PlayerButton(onClick = { customise = true }) {
                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
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
                    Mod69Session.savePrefs { it.copy(mod69QuickItems = ids) }
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
    frameStepButtons: Boolean = true,
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
                    previewPos = it.toLong()
                    onSeek(it.toLong())
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
                Canvas(modifier = Modifier.matchParentSize()) {
                    val pad = 10.dp.toPx()
                    val w = size.width - 2 * pad
                    val y = size.height / 2 - 7.dp.toPx()
                    marks.forEach { ms ->
                        drawCircle(Color(0xFFFFC107), radius = 2.5.dp.toPx(), center = Offset(pad + w * ms / duration, y))
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
            PlayerButton(onClick = onLockControlsClick) {
                Icon(painter = painterResource(R.drawable.ic_lock_open), contentDescription = null)
            }
            PlayerButton(onClick = { player?.let { it.seekTo((it.currentPosition - seekIncrementMs).coerceAtLeast(0)) } }) {
                Icon(imageVector = Icons.Rounded.Replay10, contentDescription = null)
            }
            PreviousButton(player = player)
            PlayPauseButton(player = player)
            NextButton(player = player)
            PlayerButton(onClick = {
                player?.let {
                    val target = it.currentPosition + seekIncrementMs
                    it.seekTo(if (it.duration != C.TIME_UNSET) target.coerceAtMost(it.duration) else target)
                }
            }) {
                Icon(imageVector = Icons.Rounded.Forward10, contentDescription = null)
            }
            PlayerButton(onClick = onVideoContentScaleClick, onLongClick = onVideoContentScaleLongClick) {
                Icon(painter = painterResource(videoContentScale.drawableRes()), contentDescription = null)
            }
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

