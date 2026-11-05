package dev.anilbeesetti.nextplayer.settings.screens.mod69

import kotlinx.coroutines.launch
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
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
import androidx.compose.runtime.mutableStateOf
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
    0xFF3F51B5L, 0xFF673AB7L, 0xFF00BCD4L, 0xFF8BC34AL, 0xFFCDDC39L, 0xFFFFEB3BL,
    0xFFFF5722L, 0xFF795548L, 0xFF607D8BL, 0xFFFF4081L, 0xFF7C4DFFL, 0xFF00E676L,
)

private val QUICK_IDS = listOf(
    "tools", "speed", "screenshot", "bookmark", "night", "background", "rotate", "loop", "shuffle", "pip",
    "mute", "equalizer", "sleep", "ab", "help",
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
    "mute" -> "Mute"
    "equalizer" -> "Equalizer"
    "sleep" -> "Sleep timer"
    "ab" -> "A-B repeat"
    "help" -> "Gesture help"
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
                    onChange = { c -> player { it.copy(mod69ButtonBackgroundColor = c, mod69ButtonBgCustom = true) } },
                )
                if (p.mod69ButtonBgCustom) {
                    androidx.compose.material3.TextButton(onClick = {
                        player { it.copy(mod69ButtonBgCustom = false, mod69ButtonBackgroundColor = 0x00000000L) }
                    }) { Text("Default button look") }
                }
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
                    var barH by remember(p.mod69MiniBarHeight) { mutableFloatStateOf(p.mod69MiniBarHeight.toFloat()) }
                    PreferenceSlider(
                        title = "Progress line thickness",
                        description = "The thin line at the bottom while the buttons are hidden",
                        icon = NextIcons.Length,
                        value = barH,
                        valueRange = 1f..12f,
                        steps = 10,
                        onValueChange = { barH = it },
                        onValueChangeFinished = { player { it.copy(mod69MiniBarHeight = barH.toInt()) } },
                        trailingContent = { Text("${barH.toInt()} dp") },
                    )
                }
                PreferenceSwitch(
                    title = "Frame-step buttons on the seek bar",
                    description = "Arrows at both ends of the bar: pause and move one frame",
                    icon = NextIcons.ButtonsPosition,
                    isChecked = p.mod69FrameStepOnBar,
                    onClick = { player { it.copy(mod69FrameStepOnBar = !it.mod69FrameStepOnBar) } },
                )
                PreferenceSwitch(
                    title = "Title bar buttons in one ⋮ menu",
                    description = "Decoder, playlist, audio and subtitles go in a menu so the video title has room. Tap the title to read all of it",
                    icon = NextIcons.ButtonsPosition,
                    isChecked = p.mod69TopButtonsMenu,
                    onClick = { player { it.copy(mod69TopButtonsMenu = !it.mod69TopButtonsMenu) } },
                )
                PreferenceSwitch(
                    title = "Tools as a side column",
                    description = "A slim column at the screen edge instead of a big panel over the video",
                    icon = NextIcons.ButtonsPosition,
                    isChecked = p.mod69ToolsLayout != 0,
                    onClick = { player { it.copy(mod69ToolsLayout = if (it.mod69ToolsLayout == 0) 2 else 0) } },
                )
                if (p.mod69ToolsLayout != 0) {
                    PreferenceSwitch(
                        title = "Put the tools column on the left",
                        description = "Off = right side",
                        icon = NextIcons.SwipeVertical,
                        isChecked = p.mod69ToolsLayout == 1,
                        onClick = { player { it.copy(mod69ToolsLayout = if (it.mod69ToolsLayout == 1) 2 else 1) } },
                    )
                    var toolsN by remember(p.mod69ToolsVisible) { mutableFloatStateOf(p.mod69ToolsVisible.toFloat()) }
                    PreferenceSlider(
                        title = "Tool buttons shown at once",
                        description = "Scroll the column for the rest",
                        icon = NextIcons.Length,
                        value = toolsN,
                        valueRange = 3f..12f,
                        steps = 8,
                        onValueChange = { toolsN = it },
                        onValueChangeFinished = { player { it.copy(mod69ToolsVisible = toolsN.toInt()) } },
                        trailingContent = { Text("${toolsN.toInt()}") },
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

            ListSectionTitle(text = "Player behaviour")
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                SectionSurface {
                    Text("Single tap on the video", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("Show/hide controls", "Play/pause", "Play/pause + show").forEachIndexed { i, name ->
                            androidx.compose.material3.FilterChip(
                                selected = p.mod69TapAction == i,
                                onClick = { player { it.copy(mod69TapAction = i) } },
                                label = { Text(name) },
                            )
                        }
                    }
                }
                PreferenceSwitch(
                    title = "Kids lock",
                    description = "While locked, back does nothing and the lock needs a long press to open",
                    icon = NextIcons.Lock,
                    isChecked = p.mod69KidsLock,
                    onClick = { player { it.copy(mod69KidsLock = !it.mod69KidsLock) } },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = "Show controls when a video starts",
                    icon = NextIcons.Player,
                    isChecked = p.mod69ShowUiAtStart,
                    onClick = { player { it.copy(mod69ShowUiAtStart = !it.mod69ShowUiAtStart) } },
                )
                PreferenceSwitch(
                    title = "\"Continue / Start over\" bar",
                    description = "Shows when a video resumes, tap START OVER to play from the beginning",
                    icon = NextIcons.Resume,
                    isChecked = p.mod69ResumeBar,
                    onClick = { player { it.copy(mod69ResumeBar = !it.mod69ResumeBar) } },
                )
                PreferenceSwitch(
                    title = "Fast seeking",
                    description = "Jump to the nearest key frame. Faster, but less exact",
                    icon = NextIcons.FastForward,
                    isChecked = p.mod69FastSeek,
                    onClick = { player { it.copy(mod69FastSeek = !it.mod69FastSeek) } },
                )
                PreferenceSwitch(
                    title = "Loading circle",
                    description = "Spinner while the video buffers",
                    icon = NextIcons.Timer,
                    isChecked = p.mod69LoadingCircle,
                    onClick = { player { it.copy(mod69LoadingCircle = !it.mod69LoadingCircle) } },
                )
                PreferenceSwitch(
                    title = "Suppress error messages",
                    description = "Skip to the next video instead of showing an error",
                    icon = NextIcons.BugReport,
                    isChecked = p.mod69SuppressErrors,
                    onClick = { player { it.copy(mod69SuppressErrors = !it.mod69SuppressErrors) } },
                )
                PreferenceSwitch(
                    title = "Media buttons",
                    description = "React to headset / Bluetooth buttons",
                    icon = NextIcons.Headset,
                    isChecked = p.mod69MediaButtons,
                    onClick = { player { it.copy(mod69MediaButtons = !it.mod69MediaButtons) } },
                )
                PreferenceSwitch(
                    title = "Next / previous buttons seek",
                    description = "Headset next/previous jump forward/back by the seek step instead of changing video",
                    icon = NextIcons.SwipeHorizontal,
                    isChecked = p.mod69NextPrevAsSeek,
                    onClick = { player { it.copy(mod69NextPrevAsSeek = !it.mod69NextPrevAsSeek) } },
                    isLastItem = true,
                )
                SectionSurface {
                    Text("HTTP User-Agent", style = MaterialTheme.typography.titleMedium)
                    Text("For network links. Leave empty for the default", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    var ua by remember(p.mod69UserAgent) { mutableStateOf(p.mod69UserAgent) }
                    androidx.compose.material3.OutlinedTextField(
                        value = ua,
                        onValueChange = { ua = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        trailingIcon = {
                            androidx.compose.material3.TextButton(onClick = { player { it.copy(mod69UserAgent = ua.trim()) } }) { Text("Save") }
                        },
                    )
                }
            }

            ListSectionTitle(text = "Colours")
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                ColorCard(
                    title = "App accent colour",
                    description = if (a.mod69AccentColor == 0L) "Using the theme colour" else "Tap Default to go back to the theme colour",
                    color = a.mod69AccentColor,
                    showAlpha = false,
                    onChange = { c -> onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69AccentColor = c) }) },
                )
                if (a.mod69AccentColor != 0L) {
                    androidx.compose.material3.TextButton(onClick = { onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69AccentColor = 0L) }) }) { Text("Default app colour") }
                }
                ColorCard(
                    title = "Player accent colour",
                    description = "Seek bar and highlights in the player",
                    color = p.mod69PlayerAccent,
                    showAlpha = false,
                    onChange = { c -> player { it.copy(mod69PlayerAccent = c) } },
                )
                if (p.mod69PlayerAccent != 0L) {
                    androidx.compose.material3.TextButton(onClick = { player { it.copy(mod69PlayerAccent = 0L) } }) { Text("Default player colour") }
                }
                Text(
                    "Pure black (AMOLED): Settings → Appearance → high contrast dark theme.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
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
                )
                PreferenceSwitch(
                    title = "Title bar at the bottom",
                    description = "The folder / video list bar (title, search, menu) moves to the bottom of the screen",
                    icon = NextIcons.ArrowDownward,
                    isChecked = a.mod69ListBarAtBottom,
                    onClick = { onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69ListBarAtBottom = !it.mod69ListBarAtBottom) }) },
                )
                PreferenceSwitch(
                    title = "Folders always A–Z",
                    description = "Folders are sorted by name; the sort choice only changes videos (like MX)",
                    icon = NextIcons.Folder,
                    isChecked = a.mod69FoldersByName,
                    onClick = { onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69FoldersByName = !it.mod69FoldersByName) }) },
                )
                PreferenceSwitch(
                    title = "Video names on the left",
                    description = "Names under grid thumbnails start at the left (like MX) instead of centred",
                    icon = NextIcons.Title,
                    isChecked = a.mod69TitleLeft,
                    onClick = { onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69TitleLeft = !it.mod69TitleLeft) }) },
                )
                PreferenceSwitch(
                    title = "Hide words on the long-press bar",
                    description = "Only icons in the bottom bar while marking, so all buttons fit",
                    icon = NextIcons.SelectAll,
                    isChecked = a.mod69HideActionLabels,
                    onClick = { onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69HideActionLabels = !it.mod69HideActionLabels) }) },
                )
                ColorCard(
                    title = "Play button while marking – not marked",
                    description = "Icon colour",
                    color = a.mod69PlayIconUnmarked,
                    showAlpha = false,
                    onChange = { c -> onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69PlayIconUnmarked = c) }) },
                )
                ColorCard(
                    title = "Play button while marking – not marked",
                    description = "Background colour and transparency",
                    color = a.mod69PlayBgUnmarked,
                    showAlpha = true,
                    onChange = { c -> onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69PlayBgUnmarked = c) }) },
                )
                ColorCard(
                    title = "Play button while marking – marked",
                    description = "Icon colour",
                    color = a.mod69PlayIconMarked,
                    showAlpha = false,
                    onChange = { c -> onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69PlayIconMarked = c) }) },
                )
                ColorCard(
                    title = "Play button while marking – marked",
                    description = "Background colour and transparency",
                    color = a.mod69PlayBgMarked,
                    showAlpha = true,
                    onChange = { c -> onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69PlayBgMarked = c) }) },
                )
                PreferenceSwitch(
                    title = "Folder path bar",
                    description = "Shows the folder path at the bottom of the list",
                    icon = NextIcons.Folder,
                    isChecked = a.mod69PathBar,
                    onClick = { onAction(Mod69PreferencesUiEvent.UpdateApp { it.copy(mod69PathBar = !it.mod69PathBar) }) },
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
            ListSectionTitle(text = "Debug log")
            ClickablePreferenceItem(
                title = "Share debug log",
                description = "Saves this app's recent log to Download/69-Next Player log.txt and opens Share. " +
                    "Play the problem video first, then tap this.",
                icon = NextIcons.BugReport,
                onClick = { mod69ShareLog(context) },
                isFirstItem = true,
                isLastItem = true,
            )
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

/** 69 mod: an app may read its own log without adb; save it and share it. */
private fun mod69ShareLog(context: android.content.Context) {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    scope.launch {
        val text = runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "threadtime", "-t", "6000"))
            p.inputStream.bufferedReader().use { it.readText() }
        }.getOrElse { "Could not read the log: ${it.message}" }
        val header = "69-Next Player log\n" +
            "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n" +
            "Time: ${java.util.Date()}\n\n"
        val uri = runCatching {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "69-Next Player log.txt")
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
            }
            val cr = context.contentResolver
            val u = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
            cr.openOutputStream(u)!!.use { it.write((header + text).toByteArray()) }
            u
        }.getOrNull()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            if (uri == null) {
                android.widget.Toast.makeText(context, "Could not save the log", android.widget.Toast.LENGTH_LONG).show()
                return@withContext
            }
            android.widget.Toast.makeText(context, "Saved to Download/69-Next Player log.txt", android.widget.Toast.LENGTH_LONG).show()
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.startActivity(
                    android.content.Intent.createChooser(send, "Share log").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
