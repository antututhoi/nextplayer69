package dev.anilbeesetti.nextplayer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerPreferences(
    val resume: Resume = Resume.YES,
    val rememberPlayerBrightness: Boolean = false,
    val playerBrightness: Float = 0.5f,
    val minDurationForFastSeek: Long = 120000L,
    val rememberSelections: Boolean = true,
    val playerScreenOrientation: ScreenOrientation = ScreenOrientation.VIDEO_ORIENTATION,
    val playerVideoZoom: VideoContentScale = VideoContentScale.BEST_FIT,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoplay: Boolean = true,
    val enableTunneledPlayback: Boolean = false,
    val autoPip: Boolean = true,
    val autoBackgroundPlay: Boolean = false,
    val loopMode: LoopMode = LoopMode.OFF,

    // Controls (Gestures)
    @Deprecated(
        message =
            "Use individual enableVolumeSwipeGesture and enableBrightnessSwipeGesture instead",
    )
    val useSwipeControls: Boolean = true,
    val enableVolumeSwipeGesture: Boolean = true,
    val enableBrightnessSwipeGesture: Boolean = true,
    val useSeekControls: Boolean = true,
    val useZoomControls: Boolean = true,
    val enablePanGesture: Boolean = false,
    val doubleTapGesture: DoubleTapGesture = DoubleTapGesture.BOTH,
    val useLongPressControls: Boolean = false,
    val longPressControlsSpeed: Float = 2.0f,
    val seekIncrement: Int = DEFAULT_SEEK_INCREMENT,
    val seekSensitivity: Float = DEFAULT_SEEK_SENSITIVITY,
    val volumeGestureSensitivity: Float = DEFAULT_VOLUME_GESTURE_SENSITIVITY,
    val brightnessGestureSensitivity: Float = DEFAULT_BRIGHTNESS_GESTURE_SENSITIVITY,

    // Player Interface
    val controllerAutoHideTimeout: Int = DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT,
    val controlButtonsPosition: ControlButtonsPosition = ControlButtonsPosition.LEFT,
    val hidePlayerButtonsBackground: Boolean = false,
    val useMaterialYouControls: Boolean = false,
    val showRemainingTime: Boolean = false,

    // Audio Preferences
    val preferredAudioLanguage: String = "",
    val pauseOnHeadsetDisconnect: Boolean = true,
    val requireAudioFocus: Boolean = true,
    val showSystemVolumePanel: Boolean = true,
    val enableVolumeBoost: Boolean = false,

    // Subtitle Preferences
    val useSystemCaptionStyle: Boolean = false,
    val preferredSubtitleLanguage: String = "",
    val subtitleTextEncoding: String = "",
    val subtitleTextSize: Int = DEFAULT_SUBTITLE_TEXT_SIZE,
    val subtitleBackground: Boolean = false,
    val subtitleFont: Font = Font.DEFAULT,
    val subtitleTextBold: Boolean = true,
    val applyEmbeddedStyles: Boolean = true,

    // 69 mod: player look
    /** MX-style: play/pause, previous and next sit in the bottom row instead of the middle of the video. */
    val mod69BottomPlayControls: Boolean = true,
    val mod69SeekPreview: Boolean = true,
    val mod69TopBarAtBottom: Boolean = false,
    val mod69SwapGestureSides: Boolean = false,
    val mod69GestureEdgePercent: Int = 0,
    val mod69DoubleBackToExit: Boolean = false,
    val mod69ResumeFirstOnly: Boolean = false,
    /** Background colour (ARGB) behind the player buttons. Fully transparent by default. */
    val mod69ButtonBackgroundColor: Long = 0x00000000L,
    /** Thin progress bar along the bottom edge while the controls are hidden. */
    val mod69MiniProgressBar: Boolean = true,
    val mod69MiniProgressColor: Long = 0xFF2196F3L,
    val mod69MiniProgressHeightDp: Int = 3,
    /** Elapsed / total time in the top-left corner while the controls are hidden. */
    val mod69CornerTime: Boolean = false,
    /** Battery and clock in the top-right corner while the controls are hidden. */
    val mod69CornerBatteryClock: Boolean = false,
    val mod69CornerTextColor: Long = 0xCCFFFFFFL,
    /** Which quick icons show in the row under the title, in order. */
    val mod69QuickItems: List<String> = DEFAULT_QUICK_ITEMS,
    /** Single tap: 0 show/hide controls, 1 play/pause, 2 play/pause + show controls. */
    val mod69TapAction: Int = 0,
    /** Kids lock: while locked the back button does nothing and unlocking needs a long press. */
    val mod69KidsLock: Boolean = false,
    val mod69ShowRemaining: Boolean = false,
    val mod69ShowUiAtStart: Boolean = true,
    val mod69LoadingCircle: Boolean = true,
    val mod69SuppressErrors: Boolean = false,
    /** Seek to the nearest key frame (faster, less exact). */
    val mod69FastSeek: Boolean = false,
    /** Show the "Continue from where you stopped / Start over" bar when a video resumes. */
    val mod69ResumeBar: Boolean = true,
    val mod69MediaButtons: Boolean = true,
    /** Headset next/previous buttons jump forward/back instead of changing video. */
    val mod69NextPrevAsSeek: Boolean = false,
    /** Aspect ratio kept for all videos (width / height), 0 = each video's own. */
    val mod69AspectAll: Float = 0f,
    val mod69UserAgent: String = "",
    /** Accent colour for the player (seek bar, highlights). 0 = app colour. */
    val mod69PlayerAccent: Long = 0L,
    /** True once the button background was picked in 69 settings (overrides Material You buttons). */
    val mod69ButtonBgCustom: Boolean = false,
    /** Cut the seek bar into pieces at chapters (off = one smooth bar, chapters still in the menu). */
    val mod69ChapterGaps: Boolean = false,
    val mod69ChapterSnap: Boolean = false,
    /** Buttons in the row under the title (same catalog as the quick row and the tools). */
    val mod69TopRowItems: List<String> = listOf("decoder", "playlist", "audio_track", "subtitle_track"),
    /** Buttons not shown anywhere. Everything not in a row and not hidden is in Tools. */
    val mod69HiddenItems: List<String> = emptyList(),
    /** Small names under the player buttons (long-press always shows the name). */
    val mod69ButtonLabels: Boolean = true,
    /** Thickness of the thin progress line shown while the controls are hidden (same as the seek bar line by default). */
    val mod69MiniBarHeight: Int = 4,
    /** Frame-step buttons at both ends of the seek bar. */
    val mod69FrameStepOnBar: Boolean = true,
    /** Tools menu layout: 0 = grid at the bottom, 1 = column on the left, 2 = column on the right. */
    val mod69ToolsLayout: Int = 0,
    /** How many tool buttons fit in the side column before it scrolls. */
    val mod69ToolsVisible: Int = 6,
    /** Decoder / playlist / audio / subtitle buttons in one ⋮ menu so the title has room. */
    val mod69TopButtonsMenu: Boolean = true,
) {

    companion object {
        const val DEFAULT_SEEK_INCREMENT = 10
        const val DEFAULT_SEEK_SENSITIVITY = 0.50f
        const val DEFAULT_VOLUME_GESTURE_SENSITIVITY = 0.50f
        const val DEFAULT_BRIGHTNESS_GESTURE_SENSITIVITY = 0.50f
        const val DEFAULT_SUBTITLE_TEXT_SIZE = 20
        const val DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT = 4

        /** 69 mod: quick icon ids, see the player's QuickItem. */
        val DEFAULT_QUICK_ITEMS = listOf("tools", "speed", "screenshot", "background", "rotate", "loop", "pip")
    }
}
