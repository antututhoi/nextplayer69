package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 69 mod: when the list title bar sits at the very bottom of the home screen, the floating
 * home-screen buttons are lifted by its height so they sit on top of it.
 */
object Mod69NavLift {
    var bottom: Dp by mutableStateOf(0.dp)

    /** The screen that set [bottom]; only it may clear it (a closing folder screen used to clear the home's lift). */
    private var owner: Any? = null

    fun set(token: Any, value: Dp) {
        owner = token
        bottom = value
    }

    fun clear(token: Any) {
        if (owner == null || owner === token) {
            owner = null
            bottom = 0.dp
        }
    }
}
