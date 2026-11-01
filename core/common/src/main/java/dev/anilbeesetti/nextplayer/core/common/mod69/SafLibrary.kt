package dev.anilbeesetti.nextplayer.core.common.mod69

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 69 mod: folders picked through Android's folder picker (for example RSAF / rclone remotes,
 * which never appear in MediaStore). Each picked folder shows up in the library as its own
 * storage volume at the virtual path "/storage/☁ <name>".
 *
 * The file list is scanned on demand (adding a folder, or "Rescan") and saved in the app's
 * files folder, so the library opens instantly and does not hit the network every time.
 */
object SafLibrary {

    const val ROOT_PREFIX = "/storage/☁ "

    data class Root(val treeUri: String, val label: String) {
        val virtualPath: String get() = ROOT_PREFIX + label
    }

    data class Entry(
        val uri: String,
        val path: String,
        val name: String,
        val size: Long,
        val modified: Long,
        val duration: Long,
        val width: Int,
        val height: Int,
    ) {
        val parentPath: String get() = path.substringBeforeLast('/')

        /** Stable, negative id so it never clashes with MediaStore ids. */
        val id: Long get() {
            var h = -0x340d631b7bdddcdbL
            for (c in uri) {
                h = h xor c.code.toLong()
                h *= 0x100000001b3L
            }
            return h or Long.MIN_VALUE
        }
    }

    private val VIDEO_EXT = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "3g2", "ts", "m2ts", "mts", "flv",
        "wmv", "mpg", "mpeg", "ogv", "vob", "rm", "rmvb", "divx", "f4v", "asf",
    )

    private val SUB_EXT = setOf("srt", "ssa", "ass", "vtt", "ttml")

    /** Subtitle files found in picked folders: virtual path -> document uri. */
    private var subs: Map<String, String> = emptyMap()

    /** Subtitles next to a cloud video with the same name (or "name.lang.srt"). */
    fun subtitlesFor(context: Context, videoPath: String): List<Uri> {
        ensureLoaded(context)
        val dir = videoPath.substringBeforeLast('/')
        val base = videoPath.substringAfterLast('/').substringBeforeLast('.')
        return subs.filter { (path, _) ->
            val pdir = path.substringBeforeLast('/')
            val name = path.substringAfterLast('/')
            (pdir == dir || pdir == "$dir/Subs" || pdir == "$dir/subs") &&
                (name.substringBeforeLast('.') == base || name.startsWith("$base."))
        }.entries.sortedBy { it.key.length }.map { Uri.parse(it.value) }
    }

    private val versionInternal = MutableStateFlow(0)
    val version: StateFlow<Int> = versionInternal.asStateFlow()

    private val scanStateInternal = MutableStateFlow<String?>(null)
    private var scanLabel = ""

    /** Null when idle, otherwise a short progress text. */
    val scanState: StateFlow<String?> = scanStateInternal.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Rescans in the background (keeps going when the screen is closed). */
    fun rescanInBackground(context: Context, only: Root? = null) {
        if (scanStateInternal.value != null) return
        scanStateInternal.value = "…"
        val app = context.applicationContext
        scope.launch { runCatching { rescan(app, only) }; scanStateInternal.value = null }
    }

    fun rootForPath(context: Context, path: String): Root? =
        roots(context).firstOrNull { path == it.virtualPath || path.startsWith(it.virtualPath + "/") }

    @Volatile private var loaded = false
    private var roots: List<Root> = emptyList()
    private var entries: List<Entry> = emptyList()
    private var byUri: Map<String, Entry> = emptyMap()
    private lateinit var dir: File

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            dir = context.applicationContext.filesDir
            roots = runCatching {
                val arr = JSONArray(File(dir, "mod69_saf_roots.json").readText())
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Root(o.getString("uri"), o.getString("label"))
                }
            }.getOrDefault(emptyList())
            setEntries(
                runCatching {
                    val arr = JSONArray(File(dir, "mod69_saf_index.json").readText())
                    List(arr.length()) { i ->
                        val o = arr.getJSONObject(i)
                        Entry(
                            uri = o.getString("u"),
                            path = o.getString("p"),
                            name = o.getString("n"),
                            size = o.optLong("s"),
                            modified = o.optLong("m"),
                            duration = o.optLong("d"),
                            width = o.optInt("w"),
                            height = o.optInt("h"),
                        )
                    }
                }.getOrDefault(emptyList()),
            )
            subs = runCatching {
                val o = JSONObject(File(dir, "mod69_saf_subs.json").readText())
                o.keys().asSequence().associateWith { o.getString(it) }
            }.getOrDefault(emptyMap())
            loaded = true
        }
    }

    private fun saveSubs() {
        val o = JSONObject()
        subs.forEach { (k, v) -> o.put(k, v) }
        runCatching { File(dir, "mod69_saf_subs.json").writeText(o.toString()) }
    }

    private fun setEntries(list: List<Entry>) {
        entries = list
        byUri = list.associateBy { it.uri }
    }

    private fun saveRoots() {
        val arr = JSONArray()
        roots.forEach { arr.put(JSONObject().put("uri", it.treeUri).put("label", it.label)) }
        File(dir, "mod69_saf_roots.json").writeText(arr.toString())
    }

    private fun saveEntries() {
        val arr = JSONArray()
        entries.forEach {
            arr.put(
                JSONObject().put("u", it.uri).put("p", it.path).put("n", it.name)
                    .put("s", it.size).put("m", it.modified).put("d", it.duration)
                    .put("w", it.width).put("h", it.height),
            )
        }
        val tmp = File(dir, "mod69_saf_index.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(File(dir, "mod69_saf_index.json"))
    }

    private fun bump() {
        versionInternal.value = versionInternal.value + 1
    }

    fun roots(context: Context): List<Root> {
        ensureLoaded(context)
        return roots
    }

    fun entries(context: Context): List<Entry> {
        ensureLoaded(context)
        return entries
    }

    fun findByUri(context: Context, uri: Uri): Entry? {
        ensureLoaded(context)
        return byUri[uri.toString()]
    }

    fun isVirtualPath(path: String?): Boolean = path != null && path.startsWith(ROOT_PREFIX)

    /** Entries under [folderPath] (recursively), or all when null. */
    fun entriesUnder(context: Context, folderPath: String?): List<Entry> {
        val all = entries(context)
        if (folderPath == null) return all
        val prefix = "$folderPath/"
        return all.filter { it.path.startsWith(prefix) }
    }

    fun addRoot(context: Context, treeUri: Uri): Root {
        ensureLoaded(context)
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.onFailure {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        roots.firstOrNull { it.treeUri == treeUri.toString() }?.let { return it }
        val base = displayNameOfTree(context, treeUri)?.replace('/', '_')?.ifBlank { null } ?: "Cloud"
        var label = base
        var n = 2
        while (roots.any { it.label == label }) label = "$base ${n++}"
        val root = Root(treeUri.toString(), label)
        synchronized(this) {
            roots = roots + root
            saveRoots()
        }
        bump()
        return root
    }

    fun removeRoot(context: Context, root: Root) {
        ensureLoaded(context)
        synchronized(this) {
            roots = roots.filterNot { it.treeUri == root.treeUri }
            setEntries(entries.filterNot { it.path.startsWith(root.virtualPath + "/") })
            saveRoots()
            saveEntries()
        }
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(root.treeUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        bump()
    }

    fun removeEntries(context: Context, uris: Collection<String>) {
        ensureLoaded(context)
        synchronized(this) {
            setEntries(entries.filterNot { it.uri in uris })
            saveEntries()
        }
        bump()
    }

    fun replaceEntry(context: Context, oldUri: String, newEntry: Entry) {
        ensureLoaded(context)
        synchronized(this) {
            setEntries(entries.map { if (it.uri == oldUri) newEntry else it })
            saveEntries()
        }
        bump()
    }

    private fun displayNameOfTree(context: Context, treeUri: Uri): String? = runCatching {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        context.contentResolver.query(
            docUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    /**
     * Lists every video in all picked folders (or just [only]). Keeps already-known
     * duration / size info for files that did not change, then reads missing durations.
     */
    suspend fun rescan(context: Context, only: Root? = null) = withContext(Dispatchers.IO) {
        ensureLoaded(context)
        val targets = if (only != null) listOf(only) else roots
        val old = byUri
        val found = mutableListOf<Entry>()
        val foundSubs = mutableMapOf<String, String>()
        for (root in targets) {
            scanStateInternal.value = "${root.label}: looking for videos…"
            scanLabel = root.label
            val tree = Uri.parse(root.treeUri)
            val rootId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: continue
            walk(context, tree, rootId, root.virtualPath, old, found, depth = 0, subsOut = foundSubs)
        }
        synchronized(this@SafLibrary) {
            val keep = entries.filterNot { e -> targets.any { e.path.startsWith(it.virtualPath + "/") } }
            setEntries(keep + found)
            saveEntries()
            subs = subs.filterKeys { k -> targets.none { k.startsWith(it.virtualPath + "/") } } + foundSubs
            saveSubs()
        }
        bump()

        // Read durations / sizes for new files (slow over network, so done after listing).
        val missing = found.filter { it.duration <= 0L }
        missing.forEachIndexed { i, e ->
            scanStateInternal.value = "Reading video details ${i + 1} / ${missing.size}  (${(i + 1) * 100 / missing.size}%)"
            val meta = runCatching {
                MediaMetadataRetriever().use { r ->
                    r.setDataSource(context, Uri.parse(e.uri))
                    Triple(
                        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                    )
                }
            }.getOrNull() ?: return@forEachIndexed
            synchronized(this@SafLibrary) {
                setEntries(
                    entries.map {
                        if (it.uri == e.uri) it.copy(duration = meta.first, width = meta.second, height = meta.third) else it
                    },
                )
                if (i % 20 == 19 || i == missing.lastIndex) {
                    saveEntries()
                    bump()
                }
            }
        }
        scanStateInternal.value = null
    }

    private fun walk(
        context: Context,
        tree: Uri,
        docId: String,
        virtualDir: String,
        old: Map<String, Entry>,
        out: MutableList<Entry>,
        depth: Int,
        subsOut: MutableMap<String, String>,
    ) {
        if (depth > 30) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val dirs = mutableListOf<Pair<String, String>>()
        val files = mutableListOf<Entry>()
        var hasNoMedia = false
        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2) ?: ""
                    if (name == ".nomedia") {
                        hasNoMedia = true
                        continue
                    }
                    if (name.startsWith(".")) continue
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        dirs += id to name
                        continue
                    }
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in SUB_EXT) {
                        subsOut["$virtualDir/$name"] = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString()
                        continue
                    }
                    if (!mime.startsWith("video/") && ext !in VIDEO_EXT) continue
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString()
                    val size = if (c.isNull(3)) 0L else c.getLong(3)
                    val modified = if (c.isNull(4)) 0L else c.getLong(4)
                    val prev = old[uri]
                    val same = prev != null && prev.size == size && prev.modified == modified
                    files += Entry(
                        uri = uri,
                        path = "$virtualDir/$name",
                        name = name,
                        size = size,
                        modified = modified,
                        duration = if (same) prev!!.duration else 0L,
                        width = if (same) prev!!.width else 0,
                        height = if (same) prev!!.height else 0,
                    )
                }
            }
        }
        if (hasNoMedia) return
        out += files
        scanStateInternal.value = "$scanLabel: ${out.size} videos found · folder ${virtualDir.substringAfterLast('/')}"
        for ((id, name) in dirs) {
            walk(context, tree, id, "$virtualDir/$name", old, out, depth + 1, subsOut)
        }
    }
}
