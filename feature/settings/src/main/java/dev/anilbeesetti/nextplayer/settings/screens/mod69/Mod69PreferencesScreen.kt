package dev.anilbeesetti.nextplayer.settings.screens.mod69

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import dev.anilbeesetti.nextplayer.core.common.mod69.SafLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSlider
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

private val SWATCHES = listOf(
    0xFF000000L, 0xFF424242L, 0xFFFFFFFFL, 0xFF2196F3L, 0xFF03A9F4L, 0xFF009688L,
    0xFF4CAF50L, 0xFFFFC107L, 0xFFFF9800L, 0xFFF44336L, 0xFFE91E63L, 0xFF9C27B0L,
)

private val QUICK_IDS = listOf(
    "tools", "speed", "screenshot", "bookmark", "night", "background", "rotate", "loop", "shuffle", "pip",
)

@Composable
private fun quickLabel(id: String): String = when (id) {
    "speed" -> stringResource(R.string.mod69_quick_speed)
    "background" -> stringResource(R.string.mod69_quick_background)
    "rotate" -> stringResource(R.string.mod69_quick_rotate)
    "loop" -> stringResource(R.string.mod69_quick_loop)
    "shuffle" -> stringResource(R.string.mod69_quick_shuffle)
    "pip" -> stringResource(R.string.mod69_quick_pip)
    "tools" -> "Tools (sleep, A-B, picture, info…)"
    "screenshot" -> "Screenshot"
    "bookmark" -> "Add bookmark"
    "night" -> "Night mode"
    else -> id
}

@Composable
fun Mod69PreferencesScreen(viewModel: Mod69PreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(viewModel.exportSettings(context).toByteArray()) }
            }.isSuccess
            android.widget.Toast.makeText(context, if (ok) "Settings exported" else "Export failed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()
            val ok = text != null && viewModel.importSettings(context, text)
            android.widget.Toast.makeText(
                context,
                if (ok) "Settings imported. Restart the app for audio / subtitle look." else "Not a 69-Next Player settings file",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }
    Mod69PreferencesContent(
        state = state,
        onAction = viewModel::onAction,
        onExport = { runCatching { exporter.launch("69-next-player-settings.json") } },
        onImport = { runCatching { importer.launch(arrayOf("application/json", "text/plain", "*/*")) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Mod69PreferencesContent(
    state: Mod69PreferencesUiState,
    onAction: (Mod69PreferencesUiEvent) -> Unit,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
) {
    val p = state.player
    val a = state.app
    fun player(t: (PlayerPreferences) -> PlayerPreferences) = onAction(Mod69PreferencesUiEvent.UpdatePlayer(t))

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(R.string.mod69_settings),
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(Mod69PreferencesUiEvent.NavigateUp) }) {
                        Icon(NextIcons.ArrowBack, contentDescription = stringResource(R.string.navigate_up))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(R.string.mod69_player_look))
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                PreferenceSwitch(
                    title = stringResource(R.string.mod69_bottom_controls),
                    description = stringResource(R.string.mod69_bottom_controls_description),
                    icon = NextIcons.ButtonsPosition,
                    isChecked = p.mod69BottomPlayControls,
                    onClick = { player { it.copy(mod69BottomPlayControls = !it.mod69BottomPlayControls) } },
                    isFirstItem = true,
                )
                ColorCard(
                    title = stringResource(R.string.mod69_button_background),
                    description = stringResource(R.string.mod69_button_background_description),
                    color = p.mod69ButtonBackgroundColor,
                    showAlpha = true,
                    onChange = { c -> player { it.copy(mod69ButtonBackgroundColor = c) } },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.mod69_seek_preview),
                    description = stringResource(R.string.mod69_seek_preview_description),
                    icon = NextIcons.Image,
                    isChecked = p.mod69SeekPreview,
                    onClick = { player { it.copy(mod69SeekPreview = !it.mod69SeekPreview) } },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.mod69_mini_progress),
                    description = stringResource(R.string.mod69_mini_progress_description),
                    icon = NextIcons.Length,
                    isChecked = p.mod69MiniProgressBar,
                    onClick = { player { it.copy(mod69MiniProgressBar = !it.mod69MiniProgressBar) } },
                )
                if (p.mod69MiniProgressBar) {
                    ColorCard(
                        title = stringResource(R.string.mod69_mini_progress_color),
                        description = null,
                        color = p.mod69MiniProgressColor,
                        showAlpha = false,
                        onChange = { c -> player { it.copy(mod69MiniProgressColor = c) } },
                    )
                }
                PreferenceSwitch(
                    title = stringResource(R.string.mod69_corner_time),
                    description = stringResource(R.string.mod69_corner_time_description),
                    icon = NextIcons.Timer,
                    isChecked = p.mod69CornerTime,
                    onClick = { player { it.copy(mod69CornerTime = !it.mod69CornerTime) } },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.mod69_corner_battery),
                    description = stringResource(R.string.mod69_corner_battery_description),
                    icon = NextIcons.Info,
                    isChecked = p.mod69CornerBatteryClock,
                    onClick = { player { it.copy(mod69CornerBatteryClock = !it.mod69CornerBatteryClock) } },
                    isLastItem = true,
                )
            }

            ListSectionTitle(text = "Gestures & playback")
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                PreferenceSwitch(
                    title = "Swap brightness and volume sides",
                    description = "Volume on the left, brightness on the right",
                    icon = NextIcons.SwipeVertical,
                    isChecked = p.mod69SwapGestureSides,
                    onClick = { player { it.copy(mod69SwapGestureSides = !it.mod69SwapGestureSides) } },
                    isFirstItem = true,
                )
                var edge by remember(p.mod69GestureEdgePercent) { mutableFloatStateOf(p.mod69GestureEdgePercent.toFloat()) }
                PreferenceSlider(
                    title = "Dead zone at the screen edges",
                    description = "Swipes starting this close to the left/right edge are ignored (avoids the system back gesture)",
                    icon = NextIcons.Pan,
                    value = edge,
                    valueRange = 0f..15f,
                    steps = 14,
                    onValueChange = { edge = it },
                    onValueChangeFinished = { player { it.copy(mod69GestureEdgePercent = edge.toInt()) } },
                    trailingContent = { Text("${edge.toInt()}%") },
                )
                PreferenceSwitch(
                    title = "Press back twice to close the player",
                    icon = NextIcons.ArrowBack,
                    isChecked = p.mod69DoubleBackToExit,
                    onClick = { player { it.copy(mod69DoubleBackToExit = !it.mod69DoubleBackToExit) } },
                )
                PreferenceSwitch(
                    title = "Resume only the video I opened",
                    description = "Next videos in the list start from the beginning",
                    icon = NextIcons.Resume,
                    isChecked = p.mod69ResumeFirstOnly,
                    onClick = { player { it.copy(mod69ResumeFirstOnly = !it.mod69ResumeFirstOnly) } },
                    isLastItem = true,
                )
            }

            ListSectionTitle(text = stringResource(R.string.mod69_quick_items))
            QuickItemsCard(
                selected = p.mod69QuickItems,
                onChange = { list -> player { it.copy(mod69QuickItems = list) } },
            )

            ListSectionTitle(text = stringResource(R.string.mod69_list))
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                ColumnsSlider(
                    title = stringResource(R.string.mod69_video_columns),
                    value = a.mod69VideoColumns,
                    isFirst = true,
                    onDone = { v -> onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69VideoColumns = v) }) },
                )
                ColumnsSlider(
                    title = stringResource(R.string.mod69_folder_columns),
                    value = a.mod69FolderColumns,
                    isFirst = false,
                    onDone = { v -> onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69FolderColumns = v) }) },
                )
                PreferenceSwitch(
                    title = "Mark the last watched video in each folder",
                    icon = NextIcons.History,
                    isChecked = a.mod69MarkLastInFolder,
                    onClick = { onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69MarkLastInFolder = !it.mod69MarkLastInFolder) }) },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = "Scroll to the last watched video",
                    description = "When you open a folder",
                    icon = NextIcons.ArrowDownward,
                    isChecked = a.mod69ScrollToLast,
                    onClick = { onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69ScrollToLast = !it.mod69ScrollToLast) }) },
                    isLastItem = true,
                )
            }
            ListSectionTitle(text = stringResource(R.string.mod69_cloud_folders))
            CloudFoldersCard()

            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                var px by remember(a.mod69ThumbnailMaxPx) { mutableFloatStateOf(a.mod69ThumbnailMaxPx.toFloat()) }
                PreferenceSlider(
                    title = stringResource(R.string.mod69_thumb_size),
                    description = stringResource(R.string.mod69_thumb_size_description),
                    icon = NextIcons.Image,
                    value = px,
                    valueRange = 192f..768f,
                    steps = 11,
                    onValueChange = { px = it },
                    onValueChangeFinished = {
                        val v = px.toInt()
                        onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69ThumbnailMaxPx = v) })
                    },
                    isFirstItem = true,
                    isLastItem = true,
                    trailingContent = { Text("${px.toInt()} px") },
                )
            }
            ListSectionTitle(text = "Backup")
            SectionSurface {
                Text(
                    "Save all settings to a file (for example in Download), or load them back after reinstalling.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Button(onClick = onExport) { Text("Export settings") }
                    OutlinedButton(onClick = onImport) { Text("Import") }
                }
            }
            Text(
                text = "",
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun ColumnsSlider(title: String, value: Int, isFirst: Boolean, onDone: (Int) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    PreferenceSlider(
        title = title,
        description = stringResource(R.string.mod69_columns_description),
        icon = NextIcons.DashBoard,
        value = v,
        valueRange = 0f..6f,
        steps = 5,
        onValueChange = { v = it },
        onValueChangeFinished = { onDone(v.toInt()) },
        isFirstItem = isFirst,
        isLastItem = !isFirst,
        trailingContent = { Text(if (v.toInt() == 0) "Auto" else v.toInt().toString()) },
    )
}

@Composable
private fun SectionSurface(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun ColorCard(
    title: String,
    description: String?,
    color: Long,
    showAlpha: Boolean,
    onChange: (Long) -> Unit,
) {
    val rgb = color and 0x00FFFFFFL
    val alpha = ((color ushr 24) and 0xFF).toInt()
    var alphaF by remember(alpha) { mutableFloatStateOf(alpha / 255f) }
    SectionSurface {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SWATCHES.forEach { sw ->
                val swRgb = sw and 0x00FFFFFFL
                val selected = swRgb == rgb
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(sw.toInt()))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                        .clickable {
                            val a = if (showAlpha) alpha.toLong() else 0xFFL
                            onChange((a shl 24) or swRgb)
                        },
                )
            }
        }
        if (showAlpha) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.mod69_transparency), modifier = Modifier.padding(end = 8.dp))
                androidx.compose.material3.Slider(
                    value = 1f - alphaF,
                    onValueChange = { alphaF = 1f - it },
                    onValueChangeFinished = {
                        val a = (alphaF * 255).toInt().coerceIn(0, 255).toLong()
                        onChange((a shl 24) or rgb)
                    },
                    modifier = Modifier.weight(1f),
                )
                Text("${((1f - alphaF) * 100).toInt()}%", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun QuickItemsCard(selected: List<String>, onChange: (List<String>) -> Unit) {
    // Selected items first (in their order), then the unselected ones.
    val ordered = selected.filter { it in QUICK_IDS } + QUICK_IDS.filter { it !in selected }
    SectionSurface {
        Text(
            stringResource(R.string.mod69_quick_items_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ordered.forEach { id ->
            val on = id in selected
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = on,
                    onCheckedChange = {
                        onChange(if (on) selected - id else selected + id)
                    },
                )
                Text(quickLabel(id), modifier = Modifier.weight(1f))
                if (on) {
                    val i = selected.indexOf(id)
                    IconButton(enabled = i > 0, onClick = {
                        val m = selected.toMutableList(); m.removeAt(i); m.add(i - 1, id); onChange(m)
                    }) { Icon(NextIcons.ArrowUpward, contentDescription = null) }
                    IconButton(enabled = i < selected.size - 1, onClick = {
                        val m = selected.toMutableList(); m.removeAt(i); m.add(i + 1, id); onChange(m)
                    }) { Icon(NextIcons.ArrowDownward, contentDescription = null) }
                }
            }
        }
    }
}

@Composable
private fun CloudFoldersCard() {
    val context = LocalContext.current
    val version by SafLibrary.version.collectAsState()
    val scanning by SafLibrary.scanState.collectAsState()
    val roots = remember(version) { SafLibrary.roots(context) }
    val counts = remember(version) {
        val all = SafLibrary.entries(context)
        roots.associate { r -> r.treeUri to all.count { it.path.startsWith(r.virtualPath + "/") } }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val root = SafLibrary.addRoot(context, uri)
            SafLibrary.rescanInBackground(context, root)
        }
    }
    SectionSurface {
        Text(
            stringResource(R.string.mod69_cloud_folders_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        roots.forEach { root ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(NextIcons.Cloud, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(root.label, style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.mod69_cloud_count, counts[root.treeUri] ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { SafLibrary.removeRoot(context, root) }) {
                    Text(stringResource(R.string.mod69_cloud_remove))
                }
            }
        }
        scanning?.let {
            Text(
                stringResource(R.string.mod69_cloud_scanning, it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            Button(onClick = { runCatching { picker.launch(null) } }) {
                Text(stringResource(R.string.mod69_cloud_add))
            }
            if (roots.isNotEmpty()) {
                OutlinedButton(enabled = scanning == null, onClick = { SafLibrary.rescanInBackground(context) }) {
                    Text(stringResource(R.string.mod69_cloud_rescan))
                }
            }
        }
    }
}
