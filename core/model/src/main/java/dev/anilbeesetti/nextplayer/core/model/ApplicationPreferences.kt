package dev.anilbeesetti.nextplayer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationPreferences(
    val sortBy: Sort.By = Sort.By.TITLE,
    val sortOrder: Sort.Order = Sort.Order.ASCENDING,
    val themeConfig: ThemeConfig = ThemeConfig.SYSTEM,
    val useHighContrastDarkTheme: Boolean = false,
    val useDynamicColors: Boolean = true,
    val markLastPlayedMedia: Boolean = true,
    val isHistoryPaused: Boolean = false,
    val includeNetworkWatchHistory: Boolean = true,
    val excludeFolders: List<String> = emptyList(),
    val mediaViewMode: MediaViewMode = MediaViewMode.FOLDERS,
    val mediaLayoutMode: MediaLayoutMode = MediaLayoutMode.LIST,

    // Network browser
    val networkSortBy: Sort.By = Sort.By.TITLE,
    val networkSortOrder: Sort.Order = Sort.Order.ASCENDING,
    val networkMediaLayoutMode: MediaLayoutMode = MediaLayoutMode.LIST,

    // Fields
    val showDurationField: Boolean = true,
    val showFolderDurationField: Boolean = true,
    val showExtensionField: Boolean = false,
    val showPathField: Boolean = true,
    val showResolutionField: Boolean = false,
    val showSizeField: Boolean = false,
    val showThumbnailField: Boolean = true,
    val showPlayedProgress: Boolean = true,

    val newVideoThresholdDays: Int = 7,

    // Thumbnail generation
    val thumbnailGenerationStrategy: ThumbnailGenerationStrategy =
        ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE,
    val thumbnailFramePosition: Float = DEFAULT_THUMBNAIL_FRAME_POSITION,

    // 69 mod: grid columns in portrait (0 = automatic, as before)
    val mod69VideoColumns: Int = 2,
    val mod69FolderColumns: Int = 2,
    // Long side of saved thumbnails in pixels (0 = original size, old behaviour).
    val mod69ThumbnailMaxPx: Int = 384,
    val mod69MarkLastInFolder: Boolean = true,
    val mod69ScrollToLast: Boolean = true,
    /** Folder path bar at the bottom of the video list. */
    val mod69PathBar: Boolean = true,
    /** App accent colour (ARGB). 0 = theme default / dynamic. */
    val mod69AccentColor: Long = 0L,

) {

    companion object {
        const val DEFAULT_THUMBNAIL_FRAME_POSITION = 0.33f
    }
}
