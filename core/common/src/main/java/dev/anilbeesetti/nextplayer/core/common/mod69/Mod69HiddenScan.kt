package dev.anilbeesetti.nextplayer.core.common.mod69

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.File
import org.json.JSONObject

/**
 * 69 mod (MX Player "Show hidden files and folders" / "Recognize .nomedia"):
 * Android's media library never lists videos in dot-folders, dot-files, or folders with a ".nomedia" file.
 * When the setting is on (and the app has "All files access") those folders are scanned directly.
 */
object Mod69HiddenScan {
    private const val PREFS = "mod69_library"
    private val VIDEO_EXT = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "3g2", "ts", "m2ts", "mts", "flv", "wmv",
        "mpg", "mpeg", "vob", "ogv", "rmvb", "rm", "divx", "asf", "f4v", "m2v", "mxf",
    )

    fun showHidden(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("hidden", false)

    fun setShowHidden(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("hidden", on).apply()
        cacheAt = 0
    }

    fun hasAccess(): Boolean = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()

    @Volatile private var cache: List<SafLibrary.Entry> = emptyList()

    @Volatile private var cacheAt = 0L

    /** Hidden / .nomedia videos under [folderPath] (all when null). Empty when the setting is off. */
    @Synchronized
    fun entriesUnder(context: Context, folderPath: String?): List<SafLibrary.Entry> {
        if (!showHidden(context) || !hasAccess()) return emptyList()
        if (System.currentTimeMillis() - cacheAt > 30_000) {
            cache = runCatching { scan(context) }.getOrDefault(emptyList())
            cacheAt = System.currentTimeMillis()
        }
        if (folderPath == null) return cache
        val prefix = "$folderPath/"
        return cache.filter { it.path.startsWith(prefix) }
    }

    private fun scan(context: Context): List<SafLibrary.Entry> {
        val root = Environment.getExternalStorageDirectory() ?: return emptyList()
        val metaFile = File(context.cacheDir, "mod69_hidden_meta.json")
        val old = runCatching { JSONObject(metaFile.readText()) }.getOrDefault(JSONObject())
        val fresh = JSONObject()
        val out = ArrayList<SafLibrary.Entry>()
        val started = System.currentTimeMillis()
        var probed = 0

        // (folder, isHiddenArea): hidden once a parent is a dot-folder or holds .nomedia
        val stack = ArrayDeque<Pair<File, Boolean>>()
        stack.add(root to false)
        while (stack.isNotEmpty()) {
            if (System.currentTimeMillis() - started > 20_000) break
            val (dir, parentHidden) = stack.removeLast()
            val children = dir.listFiles() ?: continue
            val hidden = parentHidden || children.any { it.name == ".nomedia" }
            for (c in children) {
                val name = c.name
                if (c.isDirectory) {
                    // app-private data and system folders are not the user's videos
                    if (dir == root && name == "Android") continue
                    if (name == ".thumbnails" || name == ".trashed" || name == ".69RecycleBin") continue
                    stack.add(c to (hidden || name.startsWith(".")))
                } else if ((hidden || name.startsWith(".")) && name.substringAfterLast('.', "").lowercase() in VIDEO_EXT) {
                    if (name.startsWith(".trashed-") || name.startsWith(".pending-")) continue
                    val key = c.absolutePath + "|" + c.length() + "|" + c.lastModified()
                    var meta = old.optJSONArray(key)
                    if (meta == null && probed < 60) {
                        probed++
                        meta = probe(c)
                    }
                    if (meta != null) fresh.put(key, meta)
                    out.add(
                        SafLibrary.Entry(
                            uri = Uri.fromFile(c).toString(),
                            path = c.absolutePath,
                            name = name,
                            size = c.length(),
                            modified = c.lastModified(),
                            duration = meta?.optLong(0) ?: 0L,
                            width = meta?.optInt(1) ?: 0,
                            height = meta?.optInt(2) ?: 0,
                        ),
                    )
                }
            }
        }
        runCatching { metaFile.writeText(fresh.toString()) }
        return out
    }

    private fun probe(f: File): org.json.JSONArray? = runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(f.absolutePath)
            org.json.JSONArray().apply {
                put(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L)
                put(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0)
                put(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0)
            }
        } finally {
            r.release()
        }
    }.getOrNull()
}
