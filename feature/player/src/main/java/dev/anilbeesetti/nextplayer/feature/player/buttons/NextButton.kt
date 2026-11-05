package dev.anilbeesetti.nextplayer.feature.player.buttons

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberNextButtonState
import dev.anilbeesetti.nextplayer.core.ui.R as coreUiR
import dev.anilbeesetti.nextplayer.feature.player.LocalControlsVisibilityState

@OptIn(UnstableApi::class)
@Composable
internal fun NextButton(modifier: Modifier = Modifier, player: Player?, onLongClick: (() -> Unit)? = null) {
    val state = rememberNextButtonState(player = player)
    val controlsVisibilityState = LocalControlsVisibilityState.current

    PlayerButton(
        modifier = modifier.size(dev.anilbeesetti.nextplayer.feature.player.mod69.LocalMod69ButtonSize.current ?: 48.dp),
        enabled = state.isEnabled || onLongClick != null,
        onClick = {
            if (state.isEnabled) state.onClick()
            controlsVisibilityState?.showControls()
        },
        // 69 mod: long press jumps between chapters
        onLongClick = onLongClick?.let { lc ->
            {
                lc()
                controlsVisibilityState?.showControls()
            }
        },
    ) {
        Icon(
            painter = painterResource(coreUiR.drawable.ic_skip_next),
            contentDescription = stringResource(coreUiR.string.player_controls_next),
            modifier = Modifier.size(28.dp),
        )
    }
}
