package dev.anilbeesetti.nextplayer.feature.player

import dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69ResumeBar
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import dev.anilbeesetti.nextplayer.feature.player.extensions.nameRes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.ui.components.requestFocusUntilLanded
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.feature.player.state.ControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.SeekGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.TapGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberChaptersState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberMediaPresentationState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberMetadataState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberPlayerKeyInputState
import dev.anilbeesetti.nextplayer.feature.player.mod69.LocalMod69ButtonBackground
import dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69CornerInfo
import dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69MiniProgressBar
import dev.anilbeesetti.nextplayer.feature.player.mod69.toArgbColor
import dev.anilbeesetti.nextplayer.feature.player.ui.DpadSeekIndicator
import dev.anilbeesetti.nextplayer.feature.player.ui.OverlayShowView
import dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerGestureIndicators
import kotlin.time.Duration.Companion.seconds

/** Controls and overlays shared by live playback and the previewable content. */
@OptIn(UnstableApi::class)
@Composable
internal fun MediaPlayerControls(
    player: Player,
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    isPipSupported: Boolean,
    onPictureInPictureClick: () -> Unit,
    modifier: Modifier = Modifier,
    dpadUpDown: Int? = null,
) {
    val playerPreferences = state.playerPreferences
    dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Flags.swapGestureSides = playerPreferences.mod69SwapGestureSides
    dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Flags.gestureEdgePercent = playerPreferences.mod69GestureEdgePercent
    dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Flags.toolsLayout = playerPreferences.mod69ToolsLayout
    dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Flags.chapterGaps = playerPreferences.mod69ChapterGaps
    dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Flags.toolsVisible = playerPreferences.mod69ToolsVisible
    var lastBackPress by remember { mutableStateOf(0L) }
    val metadataState = rememberMetadataState(player = player)
    val mediaPresentationState = rememberMediaPresentationState(player = player)
    val progressState = rememberProgressStateWithTickInterval(player = player)
    val chaptersState = rememberChaptersState(
        player = player,
        progressState = progressState,
    )
    val keyInputState = rememberPlayerKeyInputState(
        player = player,
        controls = controlsVisibilityState,
        seekIncrementMs = playerPreferences.seekIncrement.seconds.inWholeMilliseconds,
        dpadUpDown = dpadUpDown,
    )
    var overlayView by remember { mutableStateOf<OverlayView?>(value = null) }
    LaunchedEffect(Unit) {
        if (!playerPreferences.mod69ShowUiAtStart) controlsVisibilityState.hideControls()
    }
    dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Session.savePrefs = { t -> onAction(PlayerAction.Mod69Update(t)) }
    LaunchedEffect(Unit) {
        val keep = playerPreferences.mod69AspectAll
        if (keep > 0f) {
            dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Session.aspectAll = true
            if (dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Session.aspect == null) {
                dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Session.aspect = keep
            }
        }
    }
    var mod69ResumeBar by remember { mutableStateOf(false) }
    LaunchedEffect(metadataState.mediaId) {
        mod69ResumeBar = false
        if (!playerPreferences.mod69ResumeBar ||
            playerPreferences.resume != dev.anilbeesetti.nextplayer.core.model.Resume.YES
        ) {
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(1500)
        if (player.currentPosition > 4000) {
            mod69ResumeBar = true
            kotlinx.coroutines.delay(7000)
            mod69ResumeBar = false
        }
    }
    LaunchedEffect(metadataState.mediaId) {
        if (overlayView == OverlayView.CHAPTERS) overlayView = null
    }

    fun showOverlay(overlay: OverlayView) {
        controlsVisibilityState.hideControls()
        overlayView = overlay
    }

    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val handleDpadKeys = isTv || dpadUpDown != null
    val rootFocusRequester = remember { FocusRequester() }
    val middleControlsFocusRequester = remember { FocusRequester() }
    val unlockFocusRequester = remember { FocusRequester() }
    var isMiddleControlsFocused by remember { mutableStateOf(value = false) }
    var isUnlockFocused by remember { mutableStateOf(value = false) }
    if (handleDpadKeys) {
        LaunchedEffect(
            controlsVisibilityState.controlsVisible,
            controlsVisibilityState.controlsLocked,
            key3 = overlayView,
        ) {
            if (overlayView != null) return@LaunchedEffect
            if (!isTv || !controlsVisibilityState.controlsVisible) {
                runCatching { rootFocusRequester.requestFocus() }
                return@LaunchedEffect
            }
            val locked = controlsVisibilityState.controlsLocked
            val target = if (locked) unlockFocusRequester else middleControlsFocusRequester
            target.requestFocusUntilLanded(attempts = 20) {
                if (locked) isUnlockFocused else isMiddleControlsFocused
            }
        }
    }

    CompositionLocalProvider(
        LocalControlsVisibilityState provides controlsVisibilityState,
        LocalUseMaterialYouControls provides playerPreferences.useMaterialYouControls,
        LocalMod69ButtonBackground provides playerPreferences.mod69ButtonBackgroundColor.toArgbColor(),
        dev.anilbeesetti.nextplayer.feature.player.mod69.LocalMod69ButtonBgOverride provides
            (if (playerPreferences.mod69ButtonBgCustom) playerPreferences.mod69ButtonBackgroundColor.toArgbColor() else null),
        dev.anilbeesetti.nextplayer.feature.player.mod69.LocalMod69ButtonSize provides
            (if (playerPreferences.mod69BottomPlayControls) 46.dp else null),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .thenIf(condition = handleDpadKeys) {
                    focusRequester(focusRequester = rootFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            if (overlayView != null ||
                                (
                                    !isTv && keyEvent.key != Key.DirectionUp &&
                                        keyEvent.key != Key.DirectionDown
                                    )
                            ) {
                                false
                            } else {
                                keyInputState.onKeyEvent(keyEvent = keyEvent)
                            }
                        }
                },
        ) {
            AnimatedVisibility(
                visible =
                    controlsVisibilityState.controlsVisible &&
                        !controlsVisibilityState.controlsLocked,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                )
            }
            if (mediaPresentationState.isBuffering && playerPreferences.mod69LoadingCircle) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .size(72.dp),
                )
            }
            // 69 mod: thin progress line and corner info while the controls are hidden
            if (!controlsVisibilityState.controlsVisible && overlayView == null) {
                if (playerPreferences.mod69MiniProgressBar) {
                    Mod69MiniProgressBar(progressState = progressState, preferences = playerPreferences)
                }
                Mod69CornerInfo(progressState = progressState, preferences = playerPreferences)
            }
            PlayerGestureIndicators(tapGestureState = tapGestureState)
            // 69 mod: always say which zoom / aspect mode was just picked
            var mod69ModeText by remember { mutableStateOf<String?>(null) }
            val scaleName = androidx.compose.ui.res.stringResource(
                videoZoomAndContentScaleState.videoContentScale.nameRes(),
            )
            var mod69FirstScale by remember { mutableStateOf(true) }
            LaunchedEffect(videoZoomAndContentScaleState.videoContentScale) {
                if (mod69FirstScale) {
                    mod69FirstScale = false
                    return@LaunchedEffect
                }
                mod69ModeText = scaleName
                kotlinx.coroutines.delay(1500)
                mod69ModeText = null
            }
            var mod69FirstAspect by remember { mutableStateOf(true) }
            val aspectNow = dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69Session.aspect
            LaunchedEffect(aspectNow) {
                if (mod69FirstAspect) {
                    mod69FirstAspect = false
                    return@LaunchedEffect
                }
                mod69ModeText = aspectNow?.let { "Aspect " + String.format(java.util.Locale.US, "%.2f", it).trimEnd('0').trimEnd('.') + ":1" }
                    ?: "Video's own aspect"
                kotlinx.coroutines.delay(1500)
                mod69ModeText = null
            }
            mod69ModeText?.let { text ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = text,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                }
            }
            if (mod69ResumeBar) {
                Mod69ResumeBar(
                    onStartOver = {
                        player.seekTo(0)
                        mod69ResumeBar = false
                    },
                    onClose = { mod69ResumeBar = false },
                )
            }
            DpadSeekIndicator(
                visible = keyInputState.isSeeking && keyInputState.seekOffsetMs != 0L,
                offsetMs = keyInputState.seekOffsetMs,
                positionMs = keyInputState.seekPositionMs,
            )

            if (controlsVisibilityState.controlsVisible && controlsVisibilityState.controlsLocked) {
                PlayerLockedControls(
                    onUnlockControls = controlsVisibilityState::unlockControls,
                    kidsLock = playerPreferences.mod69KidsLock,
                    unlockButtonModifier = Modifier.thenIf(condition = isTv) {
                        focusRequester(focusRequester = unlockFocusRequester)
                            .onFocusChanged { isUnlockFocused = it.hasFocus }
                    },
                )
            } else {
                PlayerControls(
                    player = player,
                    title = metadataState.title.orEmpty(),
                    playerPreferences = playerPreferences,
                    videoDecoderMode = state.decoderServiceState.videoMode,
                    controlsVisibilityState = controlsVisibilityState,
                    seekGestureState = seekGestureState,
                    videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                    progressState = progressState,
                    chaptersState = chaptersState,
                    isPipSupported = isPipSupported,
                    onShowOverlay = ::showOverlay,
                    onBackClick = { onAction(PlayerAction.NavigateUp) },
                    onPlayInBackgroundClick = { onAction(PlayerAction.PlayInBackground) },
                    onToggleTimeDisplay = { onAction(PlayerAction.ToggleTimeDisplay) },
                    onPictureInPictureClick = onPictureInPictureClick,
                    middleControlsModifier = Modifier.thenIf(condition = isTv) {
                        focusRequester(focusRequester = middleControlsFocusRequester)
                            .onFocusChanged { isMiddleControlsFocused = it.hasFocus }
                    },
                )
            }

            OverlayShowView(
                player = player,
                overlayView = overlayView,
                onOpenOverlay = { overlayView = it },
                chapters = chaptersState.chapters,
                currentChapterIndex = chaptersState.currentChapterIndex,
                onChapterSelected = { chapter ->
                    if (player.isCurrentMediaItemSeekable) {
                        player.seekTo(chapter.startTimeMs)
                        overlayView = null
                        controlsVisibilityState.showControls()
                    }
                },
                videoDecoderMode = state.decoderServiceState.videoMode,
                audioDecoderMode = state.decoderServiceState.audioMode,
                videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                onDismiss = { overlayView = null },
                onVideoDecoderModeSelected = {
                    onAction(PlayerAction.SetVideoDecoderMode(mode = it))
                },
                onAudioDecoderModeSelected = {
                    onAction(PlayerAction.SetAudioDecoderMode(mode = it))
                },
                onSelectSubtitleClick = { onAction(PlayerAction.SelectSubtitle) },
                onSelectAudioClick = { onAction(PlayerAction.SelectAudio) },
                onSubtitleOptionEvent = {
                    onAction(PlayerAction.OnSubtitleOptionEvent(event = it))
                },
                onVideoContentScaleChanged =
                    videoZoomAndContentScaleState::onVideoContentScaleChanged,
            )
        }
    }

    BackHandler {
        when {
            playerPreferences.mod69KidsLock && controlsVisibilityState.controlsLocked ->
                controlsVisibilityState.showControls()

            overlayView != null -> overlayView = null

            isTv && controlsVisibilityState.controlsVisible ->
                controlsVisibilityState.hideControls()

            playerPreferences.mod69DoubleBackToExit &&
                android.os.SystemClock.elapsedRealtime() - lastBackPress > 2000 -> {
                lastBackPress = android.os.SystemClock.elapsedRealtime()
                android.widget.Toast.makeText(context, "Press back again to close", android.widget.Toast.LENGTH_SHORT).show()
            }

            else -> onAction(PlayerAction.NavigateUp)
        }
    }
}
