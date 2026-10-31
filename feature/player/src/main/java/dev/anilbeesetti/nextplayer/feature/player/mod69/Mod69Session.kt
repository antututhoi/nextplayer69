package dev.anilbeesetti.nextplayer.feature.player.mod69

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.anilbeesetti.nextplayer.feature.player.service.mod69
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** 69 mod: state of the player tools, shared by the controls, the tools panel and the video. */
object Mod69Session {
    // Night mode: 0 = off, up to 0.75 = very dark. Warm adds an orange tint.
    var nightDim by mutableFloatStateOf(0f)
    var nightWarm by mutableStateOf(false)

    // Aspect ratio preset (width / height), null = the video's own.
    var aspect by mutableStateOf<Float?>(null)

    // Picture adjustments
    var mirror by mutableStateOf(false)
    var flip by mutableStateOf(false)
    var rotate by mutableIntStateOf(0)
    var brightness by mutableFloatStateOf(0f)
    var contrast by mutableFloatStateOf(0f)
    var saturation by mutableFloatStateOf(0f)
    var hue by mutableFloatStateOf(0f)
    var gamma by mutableFloatStateOf(1f)

    // A-B repeat (ms, -1 = not set)
    var aMs by mutableLongStateOf(-1L)
    var bMs by mutableLongStateOf(-1L)

    // Bookmarks of the current video
    var bookmarkKey by mutableStateOf<String?>(null)
    val bookmarks = mutableStateListOf<Long>()

    val hasEffects: Boolean
        get() = brightness != 0f || contrast != 0f || saturation != 0f || hue != 0f ||
            gamma != 1f || rotate != 0 || mirror || flip

    suspend fun sendEffects(player: Player?) {
        val c = player as? MediaController ?: return
        c.mod69("effects") {
            putFloat("brightness", brightness)
            putFloat("contrast", contrast)
            putFloat("saturation", saturation)
            putFloat("hue", hue)
            putFloat("gamma", gamma)
            putInt("rotate", rotate)
            putBoolean("mirror", mirror)
            putBoolean("flip", flip)
        }
    }

    fun resetPicture() {
        brightness = 0f
        contrast = 0f
        saturation = 0f
        hue = 0f
        gamma = 1f
        rotate = 0
        mirror = false
        flip = false
    }

    // ---- bookmarks storage: filesDir/mod69_bookmarks.json  { key: [ms, ms, ...] } ----

    private fun file(context: Context) = File(context.applicationContext.filesDir, "mod69_bookmarks.json")

    private fun readAll(context: Context): JSONObject =
        runCatching { JSONObject(file(context).readText()) }.getOrDefault(JSONObject())

    fun loadBookmarks(context: Context, key: String?) {
        if (key == bookmarkKey) return
        bookmarkKey = key
        bookmarks.clear()
        if (key == null) return
        val arr = readAll(context).optJSONArray(key) ?: return
        for (i in 0 until arr.length()) bookmarks += arr.optLong(i)
        bookmarks.sort()
    }

    private fun saveBookmarks(context: Context) {
        val key = bookmarkKey ?: return
        val all = readAll(context)
        if (bookmarks.isEmpty()) {
            all.remove(key)
        } else {
            all.put(key, JSONArray(bookmarks.toList()))
        }
        runCatching { file(context).writeText(all.toString()) }
    }

    fun addBookmark(context: Context, ms: Long) {
        if (bookmarks.any { kotlin.math.abs(it - ms) < 1000 }) return
        bookmarks += ms
        bookmarks.sort()
        saveBookmarks(context)
    }

    fun removeBookmark(context: Context, ms: Long) {
        bookmarks.remove(ms)
        saveBookmarks(context)
    }
}
