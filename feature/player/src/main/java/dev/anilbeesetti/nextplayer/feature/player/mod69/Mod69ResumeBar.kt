package dev.anilbeesetti.nextplayer.feature.player.mod69

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** MX-style "Continue from where you stopped · START OVER" bar. */
@Composable
internal fun BoxScope.Mod69ResumeBar(onStartOver: () -> Unit, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .navigationBarsPadding()
            .padding(start = 12.dp, bottom = 120.dp)
            .background(Color(0xE6202020), RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
        }
        Text("Continue from where you\nstopped.", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onStartOver) { Text("START OVER") }
    }
}
