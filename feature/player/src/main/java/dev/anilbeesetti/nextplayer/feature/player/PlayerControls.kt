package dev.anilbeesetti.nextplayer.feature.player

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.feature.player.model.labelRes
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import dev.anilbeesetti.nextplayer.core.model.ControlButtonsPosition
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69BottomControls
import dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69QuickRow
import dev.anilbeesetti.nextplayer.feature.player.state.ChaptersState
import dev.anilbeesetti.nextplayer.feature.player.state.ControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.SeekGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.anilbeesetti.nextplayer.feature.player.state.seekAmountFormatted
import dev.anilbeesetti.nextplayer.feature.player.state.seekToPositionFormated
import dev.anilbeesetti.nextplayer.feature.player.ui.InfoView
import dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView
import dev.anilbeesetti.nextplayer.feature.player.ui.controls.ControlsBottomView
import dev.anilbeesetti.nextplayer.feature.player.ui.controls.ControlsMiddleView
import dev.anilbeesetti.nextplayer.feature.player.ui.controls.ControlsTopView
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

@OptIn(UnstableApi::class)
@Composable
fun PlayerControls(
    player: Player?,
    title: String,
    playerPreferences: PlayerPreferences,
    videoDecoderMode: DecoderMode?,
    controlsVisibilityState: ControlsVisibilityState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    progressState: ProgressStateWithTickInterval,
    chaptersState: ChaptersState,
    isPipSupported: Boolean,
    onShowOverlay: (OverlayView) -> Unit,
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onToggleTimeDisplay: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    modifier: Modifier = Modifier,
    middleControlsModifier: Modifier = Modifier,
) {
    val mod69Layout = playerPreferences.mod69BottomPlayControls
    // 69 mod: row buttons in a side column; soft black shade behind the buttons
    val mod69Vertical = mod69Layout && playerPreferences.mod69RowsVertical
    val mod69ShadeColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)
    val mod69TopShade = if (playerPreferences.mod69ButtonShade) {
        Modifier.background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(mod69ShadeColor, androidx.compose.ui.graphics.Color.Transparent)))
    } else {
        Modifier
    }
    val mod69BottomShade = if (playerPreferences.mod69ButtonShade) {
        Modifier.background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, mod69ShadeColor)))
    } else {
        Modifier
    }
    val isTv = androidx.compose.ui.platform.LocalContext.current.let { ctx ->
        androidx.compose.runtime.remember { ctx.isTelevision }
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        @androidx.compose.runtime.Composable
        fun TopBlock() {
            AnimatedVisibility(
                visible = controlsVisibilityState.controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth().then(mod69TopShade)) {
                    val mod69DecoderLabel = if (playerPreferences.mod69HwPlus && (videoDecoderMode == null || videoDecoderMode == io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode.HARDWARE)) "HW+" else androidx.compose.ui.res.stringResource((videoDecoderMode ?: io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode.HARDWARE).labelRes)
                    ControlsTopView(
                        title = title,
                        videoDecoderMode = videoDecoderMode,
                        onDecoderClick = { onShowOverlay(OverlayView.DECODER_SELECTOR) },
                        onAudioClick = { onShowOverlay(OverlayView.AUDIO_SELECTOR) },
                        onSubtitleClick = { onShowOverlay(OverlayView.SUBTITLE_SELECTOR) },
                        onPlaylistClick = { onShowOverlay(OverlayView.PLAYLIST) },
                        onBackClick = onBackClick,
                        compactButtons = playerPreferences.mod69TopButtonsMenu,
                        hideButtons = mod69Layout,
                        // 69 mod: Row 1 on the title line, so nothing covers the video
                        trailing = if (mod69Layout && !mod69Vertical && !controlsVisibilityState.controlsLocked && playerPreferences.mod69TopRowItems.isNotEmpty()) {
                            {
                                Mod69QuickRow(
                                    player = player,
                                    preferences = playerPreferences,
                                    isPipSupported = isPipSupported,
                                    isTv = isTv,
                                    onPlaybackSpeedClick = { onShowOverlay(OverlayView.PLAYBACK_SPEED) },
                                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                                    onPictureInPictureClick = onPictureInPictureClick,
                                    onToolsClick = { onShowOverlay(OverlayView.MOD69_TOOLS) },
                                    itemIds = playerPreferences.mod69TopRowItems,
                                    expandable = false,
                                    onOverlay = onShowOverlay,
                                    decoderLabel = mod69DecoderLabel,
                                    inline = true,
                                )
                            }
                        } else {
                            null
                        },
                    )
                    if (mod69Layout && !mod69Vertical && !controlsVisibilityState.controlsLocked) {
                        // 69 mod: MX-style quick icons under the title
                        Mod69QuickRow(
                            player = player,
                            preferences = playerPreferences,
                            isPipSupported = isPipSupported,
                            isTv = isTv,
                            onPlaybackSpeedClick = { onShowOverlay(OverlayView.PLAYBACK_SPEED) },
                            onPlayInBackgroundClick = onPlayInBackgroundClick,
                            onPictureInPictureClick = onPictureInPictureClick,
                            onToolsClick = { onShowOverlay(OverlayView.MOD69_TOOLS) },
                            onOverlay = onShowOverlay,
                            decoderLabel = if (playerPreferences.mod69HwPlus && (videoDecoderMode == null || videoDecoderMode == io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode.HARDWARE)) "HW+" else androidx.compose.ui.res.stringResource((videoDecoderMode ?: io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode.HARDWARE).labelRes),
                        )
                    }
                }
            }
        }
        Column {
            TopBlock()
            Spacer(modifier = Modifier.weight(weight = 1f))
            // 69 mod: optional - title bar and quick icons just above the bottom controls
            AnimatedVisibility(
                visible =
                    mod69Layout && controlsVisibilityState.controlsVisible &&
                        !controlsVisibilityState.controlsLocked,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                // 69 mod: play/pause, previous and next at the bottom, nothing over the middle of the video
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().then(mod69BottomShade)) {
                Mod69BottomControls(
                    player = player,
                    progressState = progressState,
                    chaptersState = chaptersState,
                    seekIncrementMs = playerPreferences.seekIncrement * 1000L,
                    showRemainingTime = playerPreferences.showRemainingTime,
                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                    onToggleTimeDisplay = onToggleTimeDisplay,
                    onSeek = seekGestureState::onSeek,
                    onSeekEnd = seekGestureState::onSeekEnd,
                    seekPreview = playerPreferences.mod69SeekPreview,
                    frameStepButtons = playerPreferences.mod69FrameStepOnBar,
                    onLockControlsClick = {
                        controlsVisibilityState.showControls()
                        controlsVisibilityState.lockControls()
                    },
                    onVideoContentScaleClick = {
                        controlsVisibilityState.showControls()
                        videoZoomAndContentScaleState.switchToNextVideoContentScale()
                    },
                    onVideoContentScaleLongClick = { onShowOverlay(OverlayView.VIDEO_CONTENT_SCALE) },
                )
                }
            }
            AnimatedVisibility(
                visible =
                    !mod69Layout && controlsVisibilityState.controlsVisible &&
                        !controlsVisibilityState.controlsLocked,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ControlsBottomView(
                    player = player,
                    progressState = progressState,
                    chaptersState = chaptersState,
                    onChaptersClick = { onShowOverlay(OverlayView.CHAPTERS) },
                    controlsAlignment = when (playerPreferences.controlButtonsPosition) {
                        ControlButtonsPosition.LEFT -> Alignment.Start
                        ControlButtonsPosition.RIGHT -> Alignment.End
                    },
                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                    isPipSupported = isPipSupported,
                    showRemainingTime = playerPreferences.showRemainingTime,
                    onToggleTimeDisplay = onToggleTimeDisplay,
                    onSeek = seekGestureState::onSeek,
                    onSeekEnd = seekGestureState::onSeekEnd,
                    onPlaybackSpeedClick = { onShowOverlay(OverlayView.PLAYBACK_SPEED) },
                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                    onLockControlsClick = {
                        controlsVisibilityState.showControls()
                        controlsVisibilityState.lockControls()
                    },
                    onVideoContentScaleClick = {
                        controlsVisibilityState.showControls()
                        videoZoomAndContentScaleState.switchToNextVideoContentScale()
                    },
                    onVideoContentScaleLongClick = {
                        onShowOverlay(OverlayView.VIDEO_CONTENT_SCALE)
                    },
                    onPictureInPictureClick = onPictureInPictureClick,
                )
            }
        }
        // 69 mod: Row 1 + Row 2 as a column at the side, in the middle of the screen
        if (mod69Vertical) {
            AnimatedVisibility(
                visible = controlsVisibilityState.controlsVisible && !controlsVisibilityState.controlsLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(if (playerPreferences.mod69RowsVerticalLeft) Alignment.CenterStart else Alignment.CenterEnd),
            ) {
                val left = playerPreferences.mod69RowsVerticalLeft
                val sideShade = if (playerPreferences.mod69ButtonShade) {
                    Modifier.background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            if (left) {
                                listOf(mod69ShadeColor, androidx.compose.ui.graphics.Color.Transparent)
                            } else {
                                listOf(androidx.compose.ui.graphics.Color.Transparent, mod69ShadeColor)
                            },
                        ),
                    )
                } else {
                    Modifier
                }
                Mod69QuickRow(
                    player = player,
                    preferences = playerPreferences,
                    isPipSupported = isPipSupported,
                    isTv = isTv,
                    onPlaybackSpeedClick = { onShowOverlay(OverlayView.PLAYBACK_SPEED) },
                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                    onPictureInPictureClick = onPictureInPictureClick,
                    onToolsClick = { onShowOverlay(OverlayView.MOD69_TOOLS) },
                    itemIds = (playerPreferences.mod69TopRowItems + playerPreferences.mod69QuickItems).distinct(),
                    expandable = false,
                    onOverlay = onShowOverlay,
                    decoderLabel = if (playerPreferences.mod69HwPlus && (videoDecoderMode == null || videoDecoderMode == io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode.HARDWARE)) "HW+" else androidx.compose.ui.res.stringResource((videoDecoderMode ?: io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode.HARDWARE).labelRes),
                    vertical = true,
                    modifier = sideShade
                        .fillMaxHeight(0.62f)
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        when {
            seekGestureState.seekAmount != null -> InfoView(
                info =
                    "${seekGestureState.seekAmountFormatted}\n" +
                        "[${seekGestureState.seekToPositionFormated}]",
            )

            videoZoomAndContentScaleState.isZooming -> InfoView(
                info = "${(videoZoomAndContentScaleState.zoom * 100).toInt()}%",
            )

            controlsVisibilityState.controlsVisible && !mod69Layout -> ControlsMiddleView(
                player = player,
                modifier = middleControlsModifier,
            )
        }
    }
}
