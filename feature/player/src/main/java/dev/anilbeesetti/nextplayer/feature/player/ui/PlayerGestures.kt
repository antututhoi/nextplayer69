package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.feature.player.extensions.detectCustomHorizontalDragGestures
import dev.anilbeesetti.nextplayer.feature.player.extensions.detectCustomTransformGestures
import dev.anilbeesetti.nextplayer.feature.player.extensions.detectCustomVerticalDragGestures
import dev.anilbeesetti.nextplayer.feature.player.state.ControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.PictureInPictureState
import dev.anilbeesetti.nextplayer.feature.player.state.SeekGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.TapGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.anilbeesetti.nextplayer.feature.player.state.VolumeAndBrightnessGestureState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerGestures(
    modifier: Modifier = Modifier,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    pictureInPictureState: PictureInPictureState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    volumeAndBrightnessGestureState: VolumeAndBrightnessGestureState,
    onTapPlayPause: (() -> Unit)? = null,
    // 69: MX-style gestures
    mod69DoubleTapZoom: Boolean = false,
    mod69TwoFingerSpeed: Boolean = false,
) {
    // Keep the system's swipe-to-reveal areas reserved even while the bars are hidden.
    val gestureInsets = WindowInsets.systemGestures.union(WindowInsets.systemBarsIgnoringVisibility)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    BoxWithConstraints {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(pictureInPictureState.isInPictureInPictureMode) {
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectTapGestures(
                        onTap = {
                            if (tapGestureState.seekMillis != 0L) return@detectTapGestures
                            if (onTapPlayPause != null && !controlsVisibilityState.controlsLocked) {
                                onTapPlayPause()
                            } else {
                                controlsVisibilityState.toggleControlsVisibility()
                            }
                        },
                        onDoubleTap = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            // 69: double-tap the middle = zoom in / out
                            val third = it.x / size.width
                            if (mod69DoubleTapZoom && third > 0.35f && third < 0.65f) {
                                videoZoomAndContentScaleState.mod69ToggleQuickZoom()
                                return@detectTapGestures
                            }
                            tapGestureState.handleDoubleTap(
                                offset = it,
                                size = size,
                            )
                        },
                        onPress = {
                            tryAwaitRelease()
                            tapGestureState.handleOnLongPressRelease()
                        },
                        onLongPress = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            tapGestureState.handleLongPress(offset = it)
                        },
                    )
                }
                .pointerInput(
                    seekGestureState,
                    gestureInsets,
                    density,
                    layoutDirection,
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomHorizontalDragGestures(
                        shouldStartGesture = {
                            gestureInsets.isPlayerGestureStart(
                                it,
                                size,
                                density,
                                layoutDirection,
                            )
                        },
                        onDragStart = seekGestureState::onDragStart,
                        onHorizontalDrag = seekGestureState::onDrag,
                        onDragCancel = seekGestureState::onDragEnd,
                        onDragEnd = seekGestureState::onDragEnd,
                    )
                }
                .pointerInput(
                    gestureInsets,
                    density,
                    layoutDirection,
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomVerticalDragGestures(
                        shouldStartGesture = {
                            gestureInsets.isPlayerGestureStart(
                                it,
                                size,
                                density,
                                layoutDirection,
                            )
                        },
                        onDragStart = {
                            volumeAndBrightnessGestureState.onDragStart(
                                offset = it,
                                size = size,
                            )
                        },
                        onVerticalDrag = volumeAndBrightnessGestureState::onDrag,
                        onDragCancel = volumeAndBrightnessGestureState::onDragEnd,
                        onDragEnd = volumeAndBrightnessGestureState::onDragEnd,
                    )
                }
                .pointerInput(
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                    mod69TwoFingerSpeed,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    // 69: what the two fingers are doing: 0 = not known yet, 1 = zoom / pan, 2 = speed
                    var mode = 0
                    var movedY = 0f
                    var zoomed = 1f
                    val decideAt = 24.dp.toPx()
                    detectCustomTransformGestures(
                        onGestureStart = {
                            mode = if (mod69TwoFingerSpeed) 0 else 1
                            movedY = 0f
                            zoomed = 1f
                            tapGestureState.mod69SpeedGestureStart()
                        },
                        onGesture = { _, panChange, zoomChange, _ ->
                            if (tapGestureState.isLongPressGestureInAction) {
                                return@detectCustomTransformGestures
                            }
                            if (mode == 0) {
                                // fingers moving together up / down (no pinch) = speed, anything else = zoom
                                movedY += panChange.y
                                zoomed *= zoomChange
                                if (kotlin.math.abs(zoomed - 1f) > 0.06f) {
                                    mode = 1
                                } else if (kotlin.math.abs(movedY) > decideAt) {
                                    mode = 2
                                }
                                if (mode == 0) return@detectCustomTransformGestures
                            }
                            if (mode == 2) {
                                movedY += panChange.y
                                tapGestureState.mod69SpeedGestureMove(-movedY / size.height)
                                return@detectCustomTransformGestures
                            }
                            videoZoomAndContentScaleState.onZoomPanGesture(
                                constraints = this@BoxWithConstraints.constraints,
                                panChange = panChange,
                                zoomChange = zoomChange,
                            )
                        },
                        onGestureEnd = {
                            if (mode == 2) {
                                tapGestureState.mod69SpeedGestureEnd()
                            } else {
                                videoZoomAndContentScaleState.onZoomPanGestureEnd()
                            }
                            mode = 1
                        },
                    )
                },
        )
    }
}

internal fun WindowInsets.isPlayerGestureStart(
    position: Offset,
    size: IntSize,
    density: Density,
    layoutDirection: LayoutDirection,
): Boolean = position.x >= getLeft(
    density,
    layoutDirection,
) &&
    position.x < size.width - getRight(
        density,
        layoutDirection,
    ) &&
    position.y >= getTop(density) &&
    position.y < size.height - getBottom(density)
