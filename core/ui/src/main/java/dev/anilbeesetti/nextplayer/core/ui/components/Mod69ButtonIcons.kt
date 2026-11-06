package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.ui.graphics.vector.ImageVector

/** 69 mod: icons of the player buttons, for the settings list. */
object Mod69ButtonIcons {
    fun icon(id: String): ImageVector = when (id) {
        "decoder" -> Icons.Rounded.Memory
        "playlist" -> Icons.AutoMirrored.Rounded.PlaylistPlay
        "chapters" -> Icons.AutoMirrored.Rounded.List
        "audio_track" -> Icons.Rounded.Audiotrack
        "subtitle_track" -> Icons.Rounded.Subtitles
        "tools" -> Icons.Rounded.Widgets
        "speed" -> Icons.Rounded.Speed
        "screenshot" -> Icons.Rounded.CameraAlt
        "bookmark" -> Icons.Rounded.Bookmark
        "night" -> Icons.Rounded.NightsStay
        "background" -> Icons.Rounded.Headphones
        "rotate" -> Icons.Rounded.ScreenRotation
        "loop" -> Icons.Rounded.RepeatOne
        "shuffle" -> Icons.Rounded.Shuffle
        "pip" -> Icons.Rounded.PictureInPicture
        "mute" -> Icons.AutoMirrored.Rounded.VolumeOff
        "equalizer" -> Icons.Rounded.GraphicEq
        "sleep" -> Icons.Rounded.Bedtime
        "ab" -> Icons.Rounded.Repeat
        "aspect" -> Icons.Rounded.AspectRatio
        "zoom" -> Icons.Rounded.ZoomOutMap
        "picture" -> Icons.Rounded.Tune
        "info" -> Icons.Rounded.Info
        "sub_look" -> Icons.Rounded.ClosedCaption
        "cut" -> Icons.Rounded.ContentCut
        "share" -> Icons.Rounded.Share
        "help" -> Icons.Rounded.TouchApp
        "lock" -> Icons.Rounded.Lock
        "scale" -> Icons.Rounded.FitScreen
        "frame_back" -> Icons.AutoMirrored.Rounded.KeyboardArrowLeft
        "frame_next" -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
        else -> Icons.Rounded.Widgets
    }
}
