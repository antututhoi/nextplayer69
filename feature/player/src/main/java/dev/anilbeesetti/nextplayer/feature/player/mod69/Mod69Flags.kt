package dev.anilbeesetti.nextplayer.feature.player.mod69

/** 69 mod: settings read by non-Compose player code; refreshed from the preferences by the controls. */
object Mod69Flags {
    @Volatile var swapGestureSides: Boolean = false
    @Volatile var gestureEdgePercent: Int = 0
    /** Tools menu: 0 = grid, 1 = left column, 2 = right column. */
    @Volatile var toolsLayout: Int = 0
    @Volatile var toolsVisible: Int = 6
    @Volatile var chapterGaps: Boolean = false
}
