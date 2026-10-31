package dev.anilbeesetti.nextplayer.feature.videopicker.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.anilbeesetti.nextplayer.core.model.Folder
import dev.anilbeesetti.nextplayer.core.model.Video
import java.io.Serializable

@Composable
fun rememberSelectionManager(): SelectionManager =
    rememberSaveable(saver = SelectionManager.Saver) { SelectionManager() }

/**
 * 69 mod: one marking session for the whole video list, so marks stay while you open other folders,
 * go back up, or watch a video. It ends only with the close button or after an action (move, delete...).
 */
object SharedSelection {
    val manager = SelectionManager()
}

/** How many different folders the marked videos come from. */
fun SelectionManager.markedFolderCount(): Int = selectionItems
    .map { item ->
        when (item) {
            is SelectionItem.Video -> item.path.substringBeforeLast('/')
            is SelectionItem.Folder -> item.path.substringBeforeLast('/')
        }
    }
    .toSet()
    .size

fun SelectionManager.remove(item: SelectionItem) {
    when (item) {
        is SelectionItem.Video -> if (isVideoSelectedById(item.id)) toggleById(item)
        is SelectionItem.Folder -> if (isVideoSelectedById(item.id)) toggleById(item)
    }
}

@Stable
class SelectionManager(
    initialSelectionItems: Set<SelectionItem> = emptySet(),
    initialIsInSelectionMode: Boolean = false,
) {
    var selectionItems: Set<SelectionItem> by mutableStateOf(value = initialSelectionItems)
        private set

    var isInSelectionMode: Boolean by mutableStateOf(value = initialIsInSelectionMode)
        private set

    val isSingleVideoSelected: Boolean by derivedStateOf {
        selectionItems.size == 1 &&
            selectionItems.first() is SelectionItem.Video
    }

    fun toggleFolderSelection(folder: Folder) {
        val selectedFolder = selectionItems.find { it.id == folder.path }
        selectionItems = if (selectedFolder != null) {
            selectionItems - selectedFolder
        } else {
            selectionItems + folder.toSelectedFolder()
        }
        if (selectionItems.isNotEmpty()) {
            enterSelectionMode()
        } else {
            exitSelectionMode()
        }
    }

    fun toggleVideoSelection(video: Video) {
        val selectedVideo = selectionItems.find { it.id == video.uriString }
        selectionItems = if (selectedVideo != null) {
            selectionItems - selectedVideo
        } else {
            selectionItems + video.toSelectedVideo()
        }
        if (selectionItems.isNotEmpty()) {
            enterSelectionMode()
        } else {
            exitSelectionMode()
        }
    }

    fun selectFolder(folder: Folder) {
        enterSelectionMode()
        selectionItems = selectionItems + folder.toSelectedFolder()
    }

    fun selectVideo(video: Video) {
        enterSelectionMode()
        selectionItems = selectionItems + video.toSelectedVideo()
    }

    internal fun isVideoSelectedById(id: String): Boolean = selectionItems.any { it.id == id }

    internal fun toggleById(item: SelectionItem) {
        selectionItems = selectionItems.filterNot { it.id == item.id }.toSet()
        if (selectionItems.isEmpty()) exitSelectionMode()
    }

    fun clearSelection() {
        selectionItems = emptySet()
    }

    fun enterSelectionMode() {
        isInSelectionMode = true
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectionItems = emptySet()
    }

    fun isFolderSelected(folder: Folder): Boolean = selectionItems.find {
        it.id == folder.path
    } != null

    fun isVideoSelected(video: Video): Boolean = selectionItems.find {
        it.id == video.uriString
    } != null

    companion object {
        @Suppress("UNCHECKED_CAST")
        val Saver = Saver<SelectionManager, Map<String, Any>>(
            save = {
                mapOf(
                    "selectionItems" to it.selectionItems,
                    "isInSelectionMode" to it.isInSelectionMode,
                )
            },
            restore = {
                SelectionManager(
                    initialSelectionItems =
                        (it["selectionItems"] as? Set<SelectionItem>) ?: emptySet(),
                    initialIsInSelectionMode = it["isInSelectionMode"] as? Boolean ?: false,
                )
            },
        )
    }
}

sealed interface SelectionItem : Serializable {
    @Stable
    data class Folder(override val name: String, val path: String) : SelectionItem {
        override val id: String = path
    }

    @Stable
    data class Video(override val name: String, val uriString: String, val path: String) :
        SelectionItem {
        override val id: String = uriString
    }

    val id: String
    val name: String
}

fun Folder.toSelectedFolder() = SelectionItem.Folder(
    name = name,
    path = path,
)

fun Video.toSelectedVideo() = SelectionItem.Video(
    name = displayName,
    uriString = uriString,
    path = path,
)
