package dev.anilbeesetti.nextplayer.feature.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
                Column {
                    ControlsTopView(
                        title = title,
                        videoDecoderMode = videoDecoderMode,
                        onDecoderClick = { onShowOverlay(OverlayView.DECODER_SELECTOR) },
                        onAudioClick = { onShowOverlay(OverlayView.AUDIO_SELECTOR) },
                        onSubtitleClick = { onShowOverlay(OverlayView.SUBTITLE_SELECTOR) },
                        onPlaylistClick = { onShowOverlay(OverlayView.PLAYLIST) },
                        onBackClick = onBackClick,
                        compactButtons = playerPreferences.mod69TopButtonsMenu,
                    )
                    if (mod69Layout && !controlsVisibilityState.controlsLocked) {
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
