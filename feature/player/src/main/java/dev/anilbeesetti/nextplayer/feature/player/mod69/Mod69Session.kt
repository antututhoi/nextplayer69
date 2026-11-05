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

    var volumeBeforeMute = 1f

    // Player buttons placement (from the settings): ids in Row 1/Row 2, hidden ids, labels on/off
    var placedItems: Set<String> = emptySet()
    var hiddenItems: Set<String> = emptySet()
    var buttonLabels by mutableStateOf(true)

    // Aspect kept for all videos (saved in settings)
    var aspectAll by mutableStateOf(false)

    // Tools page to open next time the tools panel shows (ToolsPage name)
    var toolsStart by mutableStateOf<String?>(null)

    // Saves player settings from inside the player (set by the controls)
    var savePrefs: ((dev.anilbeesetti.nextplayer.core.model.PlayerPreferences) -> dev.anilbeesetti.nextplayer.core.model.PlayerPreferences) -> Unit = {}

    // Bookmarks of the current video
    var bookmarkKey by mutableStateOf<String?>(null)
    val bookmarks = mutableStateListOf<Long>()
    /** Optional names of bookmarks (ms -> name). */
    val bookmarkNames = androidx.compose.runtime.mutableStateMapOf<Long, String>()

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
        bookmarkNames.clear()
        if (key == null) return
        val all = readAll(context)
        val arr = all.optJSONArray(key) ?: return
        for (i in 0 until arr.length()) bookmarks += arr.optLong(i)
        bookmarks.sort()
        all.optJSONObject("$key#names")?.let { names ->
            names.keys().forEach { k -> k.toLongOrNull()?.let { bookmarkNames[it] = names.optString(k) } }
        }
    }

    private fun saveBookmarks(context: Context) {
        val key = bookmarkKey ?: return
        val all = readAll(context)
        if (bookmarks.isEmpty()) {
            all.remove(key)
            all.remove("$key#names")
        } else {
            all.put(key, JSONArray(bookmarks.toList()))
            val names = JSONObject()
            bookmarkNames.forEach { (ms, n) -> if (ms in bookmarks && n.isNotBlank()) names.put(ms.toString(), n) }
            if (names.length() > 0) all.put("$key#names", names) else all.remove("$key#names")
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
        bookmarkNames.remove(ms)
        saveBookmarks(context)
    }

    fun renameBookmark(context: Context, ms: Long, name: String) {
        if (name.isBlank()) bookmarkNames.remove(ms) else bookmarkNames[ms] = name.trim()
        saveBookmarks(context)
    }

    /** Moves a bookmark to a new time, keeping its name. */
    fun moveBookmark(context: Context, from: Long, to: Long) {
        val target = to.coerceAtLeast(0L)
        if (target == from || bookmarks.contains(target)) return
        val name = bookmarkNames.remove(from)
        bookmarks.remove(from)
        bookmarks += target
        bookmarks.sort()
        if (name != null) bookmarkNames[target] = name
        saveBookmarks(context)
    }

    fun clearBookmarks(context: Context) {
        bookmarks.clear()
        bookmarkNames.clear()
        saveBookmarks(context)
    }

    /** Text export: one "h:mm:ss  name" per line. */
    fun bookmarksText(title: String): String = buildString {
        appendLine("# Bookmarks – $title")
        bookmarks.forEach { ms ->
            val s = ms / 1000
            append(String.format(java.util.Locale.US, "%d:%02d:%02d.%03d", s / 3600, (s / 60) % 60, s % 60, ms % 1000))
            bookmarkNames[ms]?.let { append("  ").append(it) }
            appendLine()
        }
    }

    /** Reads lines like "1:02:03 name", "02:03", "123.5s" (MX / own export). @return how many were added. */
    fun importBookmarks(context: Context, text: String): Int {
        var added = 0
        val time = Regex("""^\s*(?:(\d+):)?(\d{1,2}):(\d{2})(?:[.,](\d{1,3}))?\s*(.*)$""")
        text.lineSequence().forEach { line ->
            if (line.trimStart().startsWith("#")) return@forEach
            val m = time.find(line) ?: return@forEach
            val h = m.groupValues[1].toLongOrNull() ?: 0
            val min = m.groupValues[2].toLong()
            val sec = m.groupValues[3].toLong()
            val frac = m.groupValues[4].padEnd(3, '0').take(3).toLongOrNull() ?: 0
            val ms = ((h * 3600 + min * 60 + sec) * 1000) + frac
            if (bookmarks.none { kotlin.math.abs(it - ms) < 500 }) {
                bookmarks += ms
                m.groupValues[5].trim().takeIf { it.isNotEmpty() }?.let { bookmarkNames[ms] = it }
                added++
            }
        }
        bookmarks.sort()
        saveBookmarks(context)
        return added
    }
}
