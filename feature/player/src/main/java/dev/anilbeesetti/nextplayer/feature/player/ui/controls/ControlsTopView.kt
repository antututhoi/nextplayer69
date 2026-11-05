package dev.anilbeesetti.nextplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton
import dev.anilbeesetti.nextplayer.feature.player.model.labelRes
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

@OptIn(UnstableApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    videoDecoderMode: DecoderMode?,
    onBackClick: () -> Unit = {},
    onDecoderClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    /** 69 mod: only back + title; the buttons are in the rows below. */
    hideButtons: Boolean = false,
    compactButtons: Boolean = false,
) {
    // 69 mod: tap the title to read all of it
    var titleExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var menuOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val firstControlFocusRequester = remember { FocusRequester() }
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val decoderDescription = stringResource(R.string.select_decoders)
    // Add top spacing only when the system bars don't already provide it (e.g. on TV / landscape).
    val extraTopPadding = if (systemBarsPadding.calculateTopPadding() == 0.dp) 16.dp else 0.dp
    Row(
        modifier = modifier
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .padding(top = extraTopPadding)
            .focusProperties { onEnter = { firstControlFocusRequester.requestFocus() } }
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlayerButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(lineBreak = LineBreak.Heading),
            color = Color.White,
            maxLines = if (titleExpanded) 8 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(weight = 1f)
                .clickable { titleExpanded = !titleExpanded },
        )

        if (hideButtons) {
            // 69 mod: buttons live in Row 1 / Row 2 / Tools
        } else if (compactButtons) {
            androidx.compose.foundation.layout.Box {
                PlayerButton(
                    modifier = Modifier.focusRequester(focusRequester = firstControlFocusRequester),
                    onClick = { menuOpen = true },
                ) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Rounded.MoreVert, contentDescription = null)
                }
                androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Decoder: " + stringResource((videoDecoderMode ?: DecoderMode.HARDWARE).labelRes)) },
                        onClick = { menuOpen = false; onDecoderClick() },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Playlist") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_playlist), contentDescription = null) },
                        onClick = { menuOpen = false; onPlaylistClick() },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Audio track") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_audio_track), contentDescription = null) },
                        onClick = { menuOpen = false; onAudioClick() },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Subtitles") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_subtitle_track), contentDescription = null) },
                        onClick = { menuOpen = false; onSubtitleClick() },
                    )
                }
            }
        } else Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayerButton(
                modifier = Modifier
                    .semantics { contentDescription = decoderDescription }
                    .focusRequester(focusRequester = firstControlFocusRequester),
                onClick = onDecoderClick,
            ) {
                Text(
                    text = stringResource((videoDecoderMode ?: DecoderMode.HARDWARE).labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
            PlayerButton(onClick = onPlaylistClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_playlist),
                    contentDescription = null,
                )
            }
            PlayerButton(onClick = onAudioClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_audio_track),
                    contentDescription = null,
                )
            }
            PlayerButton(onClick = onSubtitleClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_subtitle_track),
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ControlsTopViewPreview() {
    NextPlayerTheme(darkTheme = true) {
        Surface {
            ControlsTopView(
                title = "Title",
                videoDecoderMode = DecoderMode.HARDWARE,
                onBackClick = {},
            )
        }
    }
}
