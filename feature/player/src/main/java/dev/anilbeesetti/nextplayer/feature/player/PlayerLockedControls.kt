package dev.anilbeesetti.nextplayer.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton

@Composable
fun PlayerLockedControls(
    onUnlockControls: () -> Unit,
    modifier: Modifier = Modifier,
    kidsLock: Boolean = false,
    unlockButtonModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(top = 24.dp),
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        if (kidsLock) {
            androidx.compose.foundation.layout.Box(
                modifier = unlockButtonModifier
                    .padding(start = 16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                android.widget.Toast.makeText(context, "Kids lock: long-press to unlock", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onLongPress = { onUnlockControls() },
                        )
                    },
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = stringResource(R.string.controls_unlock),
                    tint = Color.White,
                )
            }
            return@Column
        }
        PlayerButton(
            modifier = unlockButtonModifier,
            containerColor = Color.Black.copy(alpha = 0.5f),
            onClick = onUnlockControls,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.controls_unlock),
            )
        }
    }
}
