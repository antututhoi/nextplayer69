package dev.anilbeesetti.nextplayer.core.common.extensions

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun File.getSubtitles(extraFolder: String? = null): List<File> = withContext(Dispatchers.IO) {
    val mediaName = this@getSubtitles.nameWithoutExtension
    val parentDir = this@getSubtitles.parentFile
    val subtitleExtensions = setOf(
        "srt",
        "ssa",
        "ass",
        "vtt",
        "ttml",
    )
    // 69 mod: also "Movie.en.srt" style names, a "Subs" folder next to the video,
    // and an extra subtitle folder chosen in the player.
    val folders = buildList {
        parentDir?.let { dir ->
            add(dir)
            listOf("Subs", "subs", "Subtitles", "subtitles", "Sub", "sub").forEach { add(File(dir, it)) }
            add(File(dir, "Subs/$mediaName"))
        }
        extraFolder?.takeIf { it.isNotBlank() }?.let { add(File(it)) }
    }.distinctBy { it.path }
    folders.flatMap { dir ->
        (dir.listFiles() ?: emptyArray()).filter { file ->
            file.isFile &&
                file.extension.lowercase() in subtitleExtensions &&
                (file.nameWithoutExtension == mediaName || file.name.startsWith("$mediaName."))
        }.sortedBy { it.name.length }
    }
}

suspend fun File.getLocalSubtitles(
    context: Context,
    excludeSubsList: List<Uri> = emptyList(),
): List<Uri> = withContext(Dispatchers.Default) {
    val excludeSubsPathSet = excludeSubsList.mapNotNull { context.getPath(uri = it) }.toSet()

    val extraFolder = context.getSharedPreferences("mod69_subs", Context.MODE_PRIVATE).getString("folder", null)
    getSubtitles(extraFolder = extraFolder).mapNotNull { file ->
        if (file.path !in excludeSubsPathSet) {
            file.toUri()
        } else {
            null
        }
    }
}

fun String.getThumbnail(): File? {
    val filePathWithoutExtension = this.substringBeforeLast(delimiter = ".")
    val imageExtensions = listOf(
        "png",
        "jpg",
        "jpeg",
    )
    for (imageExtension in imageExtensions) {
        val file = File("$filePathWithoutExtension.$imageExtension")
        if (file.exists()) return file
    }
    return null
}

fun File.isSubtitle(): Boolean {
    val subtitleExtensions = listOf(
        "srt",
        "ssa",
        "ass",
        "vtt",
        "ttml",
    )
    return extension.lowercase() in subtitleExtensions
}

fun File.deleteFiles() {
    try {
        listFiles()?.onEach { it.delete() }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

val File.prettyName: String
    get() = name.takeIf { parent != "/storage/emulated" } ?: "Internal Storage"
