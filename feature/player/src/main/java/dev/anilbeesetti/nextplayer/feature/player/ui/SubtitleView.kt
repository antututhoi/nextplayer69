package dev.anilbeesetti.nextplayer.feature.player.ui

import android.graphics.Typeface
import android.util.TypedValue
import android.view.accessibility.CaptioningManager
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69SubStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getSystemService
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.feature.player.extensions.toTypeface
import dev.anilbeesetti.nextplayer.feature.player.state.rememberCuesState

@OptIn(UnstableApi::class)
@Composable
fun SubtitleView(
    modifier: Modifier = Modifier,
    player: Player,
    isInPictureInPictureMode: Boolean,
    configuration: SubtitleConfiguration,
) {
    val cuesState = rememberCuesState(player = player)
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { Mod69SubStore.init(context) }
    // 69 mod: colours, edge, background box, position and custom font
    val mod69 by Mod69SubStore.state.collectAsState()

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx -> SubtitleView(ctx) },
        update = { subtitleView ->
            val captioningManager = getSystemService(subtitleView.context, CaptioningManager::class.java)
            if (configuration.useSystemCaptionStyle && captioningManager != null) {
                subtitleView.setStyle(CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle))
            } else {
                val base = Mod69SubStore.typeface(mod69.fontPath) ?: configuration.font.toTypeface()
                val bg = when {
                    mod69.bgColor != 0 -> mod69.bgColor
                    configuration.showBackground -> android.graphics.Color.BLACK
                    else -> android.graphics.Color.TRANSPARENT
                }
                subtitleView.setStyle(
                    CaptionStyleCompat(
                        mod69.textColor,
                        bg,
                        android.graphics.Color.TRANSPARENT,
                        mod69.edgeType,
                        mod69.edgeColor,
                        Typeface.create(base, if (configuration.textBold) Typeface.BOLD else Typeface.NORMAL),
                    ),
                )
            }
            subtitleView.setApplyEmbeddedStyles(configuration.applyEmbeddedStyles)
            subtitleView.setBottomPaddingFraction(mod69.bottomPercent / 100f)
            subtitleView.setCues(cuesState.currentCues?.cues)
            if (isInPictureInPictureMode) {
                subtitleView.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION)
            } else if (!configuration.useSystemCaptionStyle) {
                subtitleView.setFixedTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    configuration.textSize.toFloat(),
                )
            }
        },
    )
}

@Stable
data class SubtitleConfiguration(
    val useSystemCaptionStyle: Boolean,
    val showBackground: Boolean,
    val font: Font,
    val textSize: Int,
    val textBold: Boolean,
    val applyEmbeddedStyles: Boolean,
)
