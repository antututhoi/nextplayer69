package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.videopicker.state.SelectionItem
import dev.anilbeesetti.nextplayer.feature.videopicker.state.SelectionManager
import dev.anilbeesetti.nextplayer.feature.videopicker.state.remove

/** 69 mod: the small play button shown on each video while marking. */
@Composable
internal fun Mod69SelectionPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    marked: Boolean = false,
    preferences: dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences? = null,
) {
    val bg = preferences?.let { if (marked) it.mod69PlayBgMarked else it.mod69PlayBgUnmarked }
    val fg = preferences?.let { if (marked) it.mod69PlayIconMarked else it.mod69PlayIconUnmarked }
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = bg?.let { Color(it.toInt()) } ?: Color.Black.copy(alpha = 0.55f),
            contentColor = fg?.let { Color(it.toInt()) } ?: Color.White,
        ),
    ) {
        Icon(
            imageVector = NextIcons.Play,
            contentDescription = stringResource(R.string.play),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 69 mod: every marked video from every folder, to watch or untick. */
@Composable
internal fun Mod69MarkedListDialog(
    selectionManager: SelectionManager,
    onPlay: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.mod69_marked_title)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(items = selectionManager.selectionItems.toList(), key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (item is SelectionItem.Video) {
                            IconButton(onClick = { onPlay(item.uriString) }) {
                                Icon(imageVector = NextIcons.Play, contentDescription = stringResource(R.string.play))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            val folder = when (item) {
                                is SelectionItem.Video -> item.path.substringBeforeLast('/')
                                is SelectionItem.Folder -> item.path
                            }
                            Text(
                                text = folder,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { selectionManager.remove(item) }) {
                            Icon(imageVector = NextIcons.Close, contentDescription = stringResource(R.string.remove))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.done)) }
        },
    )
}
