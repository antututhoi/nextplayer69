package dev.anilbeesetti.nextplayer.core.common.mod69

/** 69 mod: background work (thumbnails, reading video details) waits while a video plays. */
object Mod69Busy {
    @Volatile var playerOpen: Boolean = false

    suspend fun waitWhilePlaying() {
        while (playerOpen) kotlinx.coroutines.delay(700)
    }
}

/** 69 mod: the last video the player was asked to play (even if it never got to play), for the play button. */
object Mod69LastOpened {
    private const val PREFS = "mod69_last_opened"

    fun set(context: android.content.Context, uri: android.net.Uri?) {
        if (uri == null) return
        context.applicationContext.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString("uri", uri.toString()).apply()
    }

    fun get(context: android.content.Context): String? =
        context.applicationContext.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getString("uri", null)
}
