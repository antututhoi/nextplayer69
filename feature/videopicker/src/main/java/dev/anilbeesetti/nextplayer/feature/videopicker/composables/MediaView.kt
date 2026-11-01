package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.domain.MediaHolder
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.Folder
import dev.anilbeesetti.nextplayer.core.model.MediaLayoutMode
import dev.anilbeesetti.nextplayer.core.model.MediaViewMode
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.RestorableFocusState
import dev.anilbeesetti.nextplayer.core.ui.components.rememberRestorableFocusState
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusGroup
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusItem
import dev.anilbeesetti.nextplayer.core.ui.extensions.plus
import dev.anilbeesetti.nextplayer.feature.videopicker.state.SelectionManager
import dev.anilbeesetti.nextplayer.feature.videopicker.state.rememberSelectionManager
import kotlin.math.abs

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalPermissionsApi::class,
)
@Composable
fun MediaView(
    mediaHolder: MediaHolder,
    recentlyPlayedVideo: Video?,
    recentlyPlayedFolder: Folder?,
    preferences: ApplicationPreferences,
    showHeaders: Boolean = preferences.mediaViewMode == MediaViewMode.FOLDER_TREE,
    contentPadding: PaddingValues = PaddingValues(),
    selectionManager: SelectionManager = rememberSelectionManager(),
    lazyGridState: LazyGridState = rememberLazyGridState(),
    modifier: Modifier = Modifier,
    focusState: RestorableFocusState = rememberRestorableFocusState(),
    autoFocus: Boolean = true,
    onFolderClick: (String) -> Unit,
    onVideoClick: (Uri) -> Unit,
    onVideoPlayInSelection: ((Uri) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current

    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val folderMinWidth = if (isTv) 160.dp else 90.dp
    val videoMinWidth = if (isTv) 240.dp else 130.dp

    BoxWithConstraints {
        val contentHorizontalPadding = when (preferences.mediaLayoutMode) {
            MediaLayoutMode.LIST -> 8.dp
            MediaLayoutMode.GRID -> 8.dp
        }
        val itemSpacing = when (preferences.mediaLayoutMode) {
            MediaLayoutMode.LIST -> 2.dp
            MediaLayoutMode.GRID -> 2.dp
        }
        val maxWidth = this.maxWidth - (contentHorizontalPadding * 2) - itemSpacing
        // 69 mod: fixed column counts from settings (doubled in landscape); 0 = automatic as before
        val isLandscape = this.maxWidth > this.maxHeight
        val maxFolders = if (preferences.mod69FolderColumns > 0 && !isTv) {
            preferences.mod69FolderColumns * (if (isLandscape) 2 else 1)
        } else {
            (maxWidth / folderMinWidth).toInt().coerceAtLeast(minimumValue = 1)
        }
        val maxVideos = if (preferences.mod69VideoColumns > 0 && !isTv) {
            preferences.mod69VideoColumns * (if (isLandscape) 2 else 1)
        } else {
            (maxWidth / videoMinWidth).toInt().coerceAtLeast(minimumValue = 1)
        }
        val spans = when (preferences.mediaLayoutMode) {
            MediaLayoutMode.LIST -> 1

            MediaLayoutMode.GRID -> lcm(
                a = maxFolders,
                b = maxVideos,
            )
        }

        val singleFolderSpan = when (preferences.mediaLayoutMode) {
            MediaLayoutMode.LIST -> 1
            MediaLayoutMode.GRID -> spans / maxFolders
        }
        val singleVideoSpan = when (preferences.mediaLayoutMode) {
            MediaLayoutMode.LIST -> 1
            MediaLayoutMode.GRID -> spans / maxVideos
        }

        // 69 mod: last watched video in this folder (marked, and scrolled to on open)
        val lastInFolder = remember(mediaHolder.videos) {
            mediaHolder.videos.filter { it.lastPlayedAt != null }.maxByOrNull { it.lastPlayedAt!!.time }
        }
        var mod69Scrolled by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(lastInFolder?.uriString) {
            if (mod69Scrolled || !preferences.mod69ScrollToLast || lastInFolder == null) return@LaunchedEffect
            mod69Scrolled = true
            val videoIndex = mediaHolder.videos.indexOf(lastInFolder)
            if (videoIndex < 0) return@LaunchedEffect
            var before = 0
            if (showHeaders && mediaHolder.folders.isNotEmpty()) before++
            before += mediaHolder.folders.size
            if (preferences.mediaViewMode == MediaViewMode.FOLDER_TREE && mediaHolder.folders.isNotEmpty()) before++
            if (showHeaders) before++
            val target = before + videoIndex
            val visible = lazyGridState.layoutInfo.visibleItemsInfo
            if (visible.none { it.index == target }) {
                runCatching { lazyGridState.scrollToItem((target - 1).coerceAtLeast(0)) }
            }
        }

        LazyVerticalGrid(
            modifier = modifier.fillMaxSize().restorableFocusGroup(
                state = focusState,
                ready =
                    autoFocus &&
                        (mediaHolder.folders.isNotEmpty() || mediaHolder.videos.isNotEmpty()),
            ),
            state = lazyGridState,
            columns = GridCells.Fixed(count = spans),
            contentPadding =
                contentPadding +
                    PaddingValues(
                        horizontal = contentHorizontalPadding,
                        vertical = 8.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(space = itemSpacing),
            horizontalArrangement = Arrangement.spacedBy(space = itemSpacing),
        ) {
            if (showHeaders && mediaHolder.folders.isNotEmpty()) {
                item(span = { GridItemSpan(currentLineSpan = maxLineSpan) }) {
                    ListSectionTitle(
                        text =
                            stringResource(
                                id = R.string.folders,
                            ) + " (${mediaHolder.folders.size})",
                    )
                }
            }
            itemsIndexed(
                items = mediaHolder.folders,
                key = { _, folder -> folder.path },
                span = { _, _ -> GridItemSpan(currentLineSpan = singleFolderSpan) },
            ) { index, folder ->
                val selected by remember {
                    derivedStateOf { selectionManager.isFolderSelected(folder = folder) }
                }
                FolderItem(
                    folder = folder,
                    isRecentlyPlayedFolder = folder.path == recentlyPlayedFolder?.path,
                    preferences = preferences,
                    modifier = Modifier.restorableFocusItem(
                        state = focusState,
                        folder.path,
                    ),
                    selected = selected,
                    isFirstItem = index == 0,
                    isLastItem = index == mediaHolder.folders.lastIndex,
                    onClick = {
                        // 69 mod: folders always open, even while marking (long-press marks a folder)
                        if (selectionManager.isInSelectionMode && onVideoPlayInSelection == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            selectionManager.toggleFolderSelection(folder = folder)
                        } else {
                            onFolderClick(folder.path)
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectionManager.toggleFolderSelection(folder = folder)
                    },
                )
            }

            if (preferences.mediaViewMode == MediaViewMode.FOLDER_TREE &&
                mediaHolder.folders.isNotEmpty()
            ) {
                item(span = { GridItemSpan(currentLineSpan = maxLineSpan) }) {
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }

            if (showHeaders && mediaHolder.videos.isNotEmpty()) {
                item(span = { GridItemSpan(currentLineSpan = maxLineSpan) }) {
                    ListSectionTitle(
                        text =
                            stringResource(id = R.string.videos) + " (${mediaHolder.videos.size})",
                    )
                }
            }

            itemsIndexed(
                items = mediaHolder.videos,
                key = { _, video -> video.uriString },
                span = { _, _ -> GridItemSpan(currentLineSpan = singleVideoSpan) },
            ) { index, video ->
                val selected by remember {
                    derivedStateOf { selectionManager.isVideoSelected(video = video) }
                }
                val mod69PlayOverlay: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? =
                    if (selectionManager.isInSelectionMode && onVideoPlayInSelection != null) {
                        {
                            Mod69SelectionPlayButton(
                                onClick = { onVideoPlayInSelection(video.uriString.toUri()) },
                                marked = selected,
                                preferences = preferences,
                                modifier = Modifier.align(Alignment.BottomStart).padding(5.dp),
                            )
                        }
                    } else {
                        null
                    }
                Box {
                androidx.compose.runtime.CompositionLocalProvider(LocalMod69ThumbOverlay provides mod69PlayOverlay) {
                VideoItem(
                    video = video,
                    preferences = preferences,
                    isRecentlyPlayedVideo = video.path == recentlyPlayedVideo?.path ||
                        (preferences.mod69MarkLastInFolder && video.uriString == lastInFolder?.uriString),
                    modifier = Modifier.restorableFocusItem(
                        state = focusState,
                        video.uriString,
                    ),
                    isFirstItem = index == 0,
                    isLastItem = index == mediaHolder.videos.lastIndex,
                    selected = selected,
                    onClick = {
                        if (selectionManager.isInSelectionMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            selectionManager.toggleVideoSelection(video = video)
                        } else {
                            onVideoClick(video.uriString.toUri())
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectionManager.toggleVideoSelection(video = video)
                    },
                )
                }
                }
            }
        }
    }
}

fun lcm(a: Int, b: Int): Int = abs(n = a * b) / gcd(
    a = a,
    b = b,
)

fun gcd(a: Int, b: Int): Int = if (b == 0) {
    a
} else {
    gcd(
        a = b,
        b = a % b,
    )
}
