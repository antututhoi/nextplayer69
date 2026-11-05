package dev.anilbeesetti.nextplayer.feature.player.mod69

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class Mod69SubStyle(
    val textColor: Int = 0xFFFFFFFF.toInt(),
    /** 0 none, 1 outline, 2 drop shadow, 3 raised, 4 depressed (CaptionStyleCompat edge types). */
    val edgeType: Int = 2,
    val edgeColor: Int = 0xFF000000.toInt(),
    /** Box behind the text; 0 = none (the old "background" switch still works). */
    val bgColor: Int = 0,
    /** Distance from the bottom, in % of the video height. */
    val bottomPercent: Int = 8,
    /** Copied font file, or empty for the font chosen in settings. */
    val fontPath: String = "",
    /** Extra folder (real path) searched for subtitles with the same name. */
    val subtitleFolder: String = "",
    /** Extra size in % (100 = as set in Settings → Subtitle). */
    val scalePercent: Int = 100,
    /** 0 centre, 1 left, 2 right. */
    val align: Int = 0,
    /** Lines starting with "/" show in italics (MicroDVD style). */
    val italicSlash: Boolean = true,
    /** Force left-to-right for right-to-left subtitles. */
    val forceLtr: Boolean = false,
    /** Default subtitle delay for every video, in ms. */
    val defaultDelayMs: Long = 0L,
)

object Mod69SubStore {
    private val stateInternal = MutableStateFlow(Mod69SubStyle())
    val state: StateFlow<Mod69SubStyle> = stateInternal.asStateFlow()
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("mod69_subs", Context.MODE_PRIVATE)
        prefs = p
        val o = runCatching { JSONObject(p.getString("cfg", "{}")!!) }.getOrDefault(JSONObject())
        val d = Mod69SubStyle()
        stateInternal.value = Mod69SubStyle(
            textColor = o.optInt("textColor", d.textColor),
            edgeType = o.optInt("edgeType", d.edgeType),
            edgeColor = o.optInt("edgeColor", d.edgeColor),
            bgColor = o.optInt("bgColor", d.bgColor),
            bottomPercent = o.optInt("bottomPercent", d.bottomPercent),
            fontPath = o.optString("fontPath", ""),
            scalePercent = o.optInt("scalePercent", d.scalePercent),
            align = o.optInt("align", d.align),
            italicSlash = o.optBoolean("italicSlash", d.italicSlash),
            forceLtr = o.optBoolean("forceLtr", d.forceLtr),
            defaultDelayMs = o.optLong("defaultDelayMs", d.defaultDelayMs),
            subtitleFolder = p.getString("folder", "") ?: "",
        )
    }

    fun update(transform: (Mod69SubStyle) -> Mod69SubStyle) {
        val s = transform(stateInternal.value)
        stateInternal.value = s
        val o = JSONObject().put("textColor", s.textColor).put("edgeType", s.edgeType)
            .put("edgeColor", s.edgeColor).put("bgColor", s.bgColor)
            .put("bottomPercent", s.bottomPercent).put("fontPath", s.fontPath)
            .put("scalePercent", s.scalePercent).put("align", s.align)
            .put("italicSlash", s.italicSlash).put("forceLtr", s.forceLtr)
            .put("defaultDelayMs", s.defaultDelayMs)
        // "folder" is kept as its own key so core code can read it without this class.
        prefs?.edit()?.putString("cfg", o.toString())?.putString("folder", s.subtitleFolder)?.apply()
    }

    private var cachedFont: Pair<String, Typeface?>? = null

    fun typeface(path: String): Typeface? {
        if (path.isEmpty()) return null
        cachedFont?.let { if (it.first == path) return it.second }
        val tf = runCatching { Typeface.createFromFile(path) }.getOrNull()
        cachedFont = path to tf
        return tf
    }
}

private val TEXT_COLORS = listOf(
    0xFFFFFFFF, 0xFFFFEB3B, 0xFFFFC107, 0xFF00E5FF, 0xFF76FF03, 0xFFFF80AB, 0xFFBDBDBD, 0xFF000000,
).map { it.toInt() }
private val EDGE_COLORS = listOf(0xFF000000, 0xFF424242, 0xFF0D47A1, 0xFFB71C1C, 0xFFFFFFFF).map { it.toInt() }
private val BG_COLORS = listOf(0x00000000, 0x80000000, 0xCC000000, 0xFF000000, 0x80424242).map { it.toInt() }

/** Turns a picked folder (tree uri) on the phone's own storage into a normal path. */
private fun treeToPath(uri: Uri): String? = runCatching {
    val id = DocumentsContract.getTreeDocumentId(uri)
    val volume = id.substringBefore(':')
    val rel = id.substringAfter(':', "")
    val base = if (volume.equals("primary", true)) "/storage/emulated/0" else "/storage/$volume"
    if (rel.isEmpty()) base else "$base/$rel"
}.getOrNull()

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Mod69SubtitlePage() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Mod69SubStore.init(context) }
    val s by Mod69SubStore.state.collectAsState()

    val fontPicker = rememberLauncherForActivityResult(dev.anilbeesetti.nextplayer.core.common.mod69.Mod69PickerContracts.Open()) { uri ->
        if (uri != null) {
            runCatching {
                val name = "mod69_font_" + System.currentTimeMillis() + ".ttf"
                val dest = File(context.filesDir, name)
                context.contentResolver.openInputStream(uri)!!.use { input -> dest.outputStream().use { input.copyTo(it) } }
                if (Typeface.createFromFile(dest) != null) {
                    context.filesDir.listFiles()?.filter { it.name.startsWith("mod69_font_") && it != dest }?.forEach { it.delete() }
                    Mod69SubStore.update { it.copy(fontPath = dest.path) }
                }
            }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(dev.anilbeesetti.nextplayer.core.common.mod69.Mod69PickerContracts.Tree()) { uri ->
        uri?.let { treeToPath(it) }?.let { path -> Mod69SubStore.update { it.copy(subtitleFolder = path) } }
    }

    Text("Text colour", style = MaterialTheme.typography.labelLarge)
    ColorRow(TEXT_COLORS, s.textColor) { c -> Mod69SubStore.update { it.copy(textColor = c) } }

    Text("Edge", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("None", "Outline", "Shadow", "Raised", "Sunken").forEachIndexed { i, name ->
            FilterChip(
                selected = s.edgeType == i,
                onClick = { Mod69SubStore.update { it.copy(edgeType = i) } },
                label = { Text(name) },
            )
        }
    }
    if (s.edgeType != 0) {
        Text("Edge colour", style = MaterialTheme.typography.labelLarge)
        ColorRow(EDGE_COLORS, s.edgeColor) { c -> Mod69SubStore.update { it.copy(edgeColor = c) } }
    }

    Text("Background box", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    ColorRow(BG_COLORS, s.bgColor) { c -> Mod69SubStore.update { it.copy(bgColor = c) } }

    var pos by remember(s.bottomPercent) { mutableFloatStateOf(s.bottomPercent.toFloat()) }
    Text("Position from bottom: ${pos.toInt()}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    Slider(
        value = pos,
        onValueChange = { pos = it },
        onValueChangeFinished = { Mod69SubStore.update { it.copy(bottomPercent = pos.toInt()) } },
        valueRange = 0f..45f,
    )
    var scale by remember(s.scalePercent) { mutableFloatStateOf(s.scalePercent.toFloat()) }
    Text("Scale: ${scale.toInt()}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    Slider(
        value = scale,
        onValueChange = { scale = it },
        onValueChangeFinished = { Mod69SubStore.update { it.copy(scalePercent = scale.toInt()) } },
        valueRange = 50f..250f,
    )
    Text("Alignment", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Centre", "Left", "Right").forEachIndexed { i, name ->
            FilterChip(selected = s.align == i, onClick = { Mod69SubStore.update { it.copy(align = i) } }, label = { Text(name) })
        }
    }
    var delay by remember(s.defaultDelayMs) { mutableFloatStateOf(s.defaultDelayMs / 1000f) }
    Text(
        "Default sync for every video: " + String.format(java.util.Locale.US, "%+.1f s", delay),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
    Slider(
        value = delay,
        onValueChange = { delay = (it * 10).toInt() / 10f },
        onValueChangeFinished = { Mod69SubStore.update { it.copy(defaultDelayMs = (delay * 1000).toLong()) } },
        valueRange = -10f..10f,
    )
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        androidx.compose.material3.Checkbox(checked = s.italicSlash, onCheckedChange = { v -> Mod69SubStore.update { it.copy(italicSlash = v) } })
        Text("Italic for lines starting with \"/\"")
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        androidx.compose.material3.Checkbox(checked = s.forceLtr, onCheckedChange = { v -> Mod69SubStore.update { it.copy(forceLtr = v) } })
        Text("Force left-to-right (RTL subtitles)")
    }
    Text("Size and bold: Settings → Subtitle. Sync for this video: subtitle menu (±0.1 s).", style = MaterialTheme.typography.bodySmall)

    Text("Font file", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    Text(if (s.fontPath.isEmpty()) "Using the font from settings" else "Custom font in use", style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { runCatching { fontPicker.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream", "*/*")) } }) {
            Text("Pick .ttf / .otf")
        }
        if (s.fontPath.isNotEmpty()) {
            OutlinedButton(onClick = { Mod69SubStore.update { it.copy(fontPath = "") } }) { Text("Default") }
        }
    }

    Text("Subtitle folder", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    Text(
        (if (s.subtitleFolder.isEmpty()) "None. " else s.subtitleFolder + "\n") +
            "Subtitles with the same name as the video load by themselves (next to the video, in a Subs folder, or here).",
        style = MaterialTheme.typography.bodySmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { runCatching { folderPicker.launch(null) } }) { Text("Pick folder") }
        if (s.subtitleFolder.isNotEmpty()) {
            OutlinedButton(onClick = { Mod69SubStore.update { it.copy(subtitleFolder = "") } }) { Text("Clear") }
        }
    }
    OutlinedButton(
        onClick = { Mod69SubStore.update { Mod69SubStyle(subtitleFolder = it.subtitleFolder) } },
        modifier = Modifier.padding(top = 8.dp),
    ) { Text("Reset look") }
}

@Composable
private fun ColorRow(colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(if (c == selected) 3.dp else 1.dp, if (c == selected) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                    .clickable { onPick(c) },
            )
        }
    }
}

/** 69 mod: applies italic "/" lines, force LTR and alignment to text cues. */
internal fun mod69Cues(cues: List<androidx.media3.common.text.Cue>?, style: Mod69SubStyle): List<androidx.media3.common.text.Cue>? {
    if (cues == null) return null
    if (!style.italicSlash && !style.forceLtr && style.align == 0) return cues
    return cues.map { cue ->
        val text = cue.text ?: return@map cue
        val b = cue.buildUpon()
        if (style.italicSlash || style.forceLtr) {
            val out = android.text.SpannableStringBuilder(text)
            var lineStart = 0
            while (lineStart <= out.length) {
                var lineEnd = out.indexOf('\n', lineStart).let { if (it < 0) out.length else it }
                if (style.italicSlash && lineStart < lineEnd && out[lineStart] == '/') {
                    out.delete(lineStart, lineStart + 1)
                    lineEnd--
                    if (lineEnd > lineStart) {
                        out.setSpan(android.text.style.StyleSpan(Typeface.ITALIC), lineStart, lineEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                if (style.forceLtr) {
                    out.insert(lineStart, "\u200E")
                    lineEnd++
                }
                lineStart = lineEnd + 1
            }
            b.setText(out)
        }
        when (style.align) {
            1 -> b.setTextAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setPosition(0.05f).setPositionAnchor(androidx.media3.common.text.Cue.ANCHOR_TYPE_START)
            2 -> b.setTextAlignment(android.text.Layout.Alignment.ALIGN_OPPOSITE)
                .setPosition(0.95f).setPositionAnchor(androidx.media3.common.text.Cue.ANCHOR_TYPE_END)
        }
        b.build()
    }
}
