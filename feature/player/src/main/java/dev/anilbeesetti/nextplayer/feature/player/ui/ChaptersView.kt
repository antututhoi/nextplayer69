package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.extractor.metadata.Chapter
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextSegmentedListItem
import dev.anilbeesetti.nextplayer.feature.player.extensions.formatted
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxScope.ChaptersView(
    show: Boolean,
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    onChapterSelected: (Chapter) -> Unit,
) {
    OverlayView(
        show = show,
        title = stringResource(R.string.chapters) + "  (${chapters.size})",
    ) {
        // 69 mod: search box, number, start time and length of every chapter
        var query by remember { mutableStateOf("") }
        val titles = chapters.mapIndexed { index, chapter -> chapter.titleOrDefault(index = index) }
        val shown = remember(chapters, query, titles) {
            chapters.indices.filter { query.isBlank() || titles[it].contains(query, ignoreCase = true) }
        }
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            if (chapters.size > 6) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search chapters") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = rememberLazyListState(
                    initialFirstVisibleItemIndex = currentChapterIndex.coerceAtLeast(minimumValue = 0),
                ),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            ) {
                itemsIndexed(
                    items = shown,
                    key = { _, index -> chapters[index].startTimeMs },
                ) { pos, index ->
                    val chapter = chapters[index]
                    val lengthMs = (chapter.endTimeMs - chapter.startTimeMs).coerceAtLeast(0L)
                    NextSegmentedListItem(
                        selected = index == currentChapterIndex,
                        isFirstItem = pos == 0,
                        isLastItem = pos == shown.lastIndex,
                        onClick = { onChapterSelected(chapter) },
                        content = {
                            Text(
                                text = "${index + 1}.  " + titles[index],
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = chapter.startTimeMs.milliseconds.formatted() +
                                    if (lengthMs > 0) "  ·  " + lengthMs.milliseconds.formatted() + " long" else "",
                            )
                        },
                    )
                }
            }
        }
    }
}

/** 69 mod: non-composable title for use inside strings. */
object ChapterTitles {
    fun Chapter.mod69Title(index: Int): String = title?.value?.takeIf { it.isNotBlank() } ?: "Chapter ${index + 1}"
}

@Composable
internal fun Chapter.titleOrDefault(index: Int): String =
    title?.value?.takeIf { it.isNotBlank() } ?: stringResource(
        R.string.chapter_number,
        index + 1,
    )
