package dev.anilbeesetti.nextplayer.core.common.mod69

import android.content.Context
import java.io.File

/**
 * 69 mod (MX "Delete subtitle files together"): when a video is deleted, subtitle files next to it
 * with the same name (movie.srt, movie.en.srt, movie.ass …) are deleted too.
 * Needs "All files access"; without it nothing happens.
 */
object Mod69SubtitleCleanup {
    private const val PREFS = "mod69_library"
    private val SUB_EXT = setOf("srt", "ass", "ssa", "vtt", "sub", "idx", "smi", "sami", "sup", "ttml", "lrc", "txt")

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("delete_subs", false)

    fun setEnabled(context: Context, on: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("delete_subs", on).apply()

    /** Subtitle files that belong to the video at [videoPath]. */
    fun subtitlesOf(videoPath: String): List<File> {
        val video = File(videoPath)
        val dir = video.parentFile ?: return emptyList()
        val base = video.nameWithoutExtension
        if (base.isEmpty()) return emptyList()
        return dir.listFiles()?.filter { f ->
            f.isFile && f != video &&
                f.extension.lowercase() in SUB_EXT &&
                (f.nameWithoutExtension == base || f.name.startsWith("$base."))
        }.orEmpty()
    }

    /** Called after the videos were deleted. Returns how many subtitle files were removed. */
    fun deleteFor(context: Context, videoPaths: List<String>): Int {
        if (!enabled(context) || !Mod69HiddenScan.hasAccess()) return 0
        var n = 0
        for (p in videoPaths) {
            if (File(p).exists()) continue // the video is still there (delete failed or cancelled)
            for (s in subtitlesOf(p)) if (runCatching { s.delete() }.getOrDefault(false)) n++
        }
        return n
    }
}
