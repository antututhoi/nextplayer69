package dev.anilbeesetti.nextplayer.settings.screens.medialibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.SelectablePreference
import dev.anilbeesetti.nextplayer.core.ui.components.rememberTvListFocusRequester
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusDown
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.extensions.plus
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme

@Composable
fun FolderPreferencesScreen(viewModel: FolderPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle(
        minActiveState = Lifecycle.State.RESUMED,
    )

    FolderPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun FolderPreferencesScreenContent(
    state: FolderPreferencesUiState,
    onAction: (FolderPreferencesUiEvent) -> Unit,
) {
    val listFocusRequester = rememberTvListFocusRequester()
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.manage_folders),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = { onAction(FolderPreferencesUiEvent.NavigateUp) },
                        modifier = Modifier.tvFocusDown(target = listFocusRequester),
                    ) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        when (state.foldersDataState) {
            is DataState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = innerPadding),
                ) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }
            }

            is DataState.Success -> {
                // 69 mod: the excluded folders first, "Add folder" opens the folder picker,
                // and the old full list is behind "Choose from found folders"
                val context = androidx.compose.ui.platform.LocalContext.current
                var showAll by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                val picker = androidx.activity.compose.rememberLauncherForActivityResult(
                    dev.anilbeesetti.nextplayer.core.common.mod69.Mod69PickerContracts.Tree(),
                ) { uri ->
                    val path = uri?.let { mod69TreeToPath(context, it) }
                    if (path != null && path !in state.preferences.excludeFolders) {
                        onAction(FolderPreferencesUiEvent.UpdateExcludeList(path))
                    } else if (uri != null && path == null) {
                        android.widget.Toast.makeText(context, "This folder can't be used here", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                val excluded = state.preferences.excludeFolders.sorted()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .tvListFocus(focusRequester = listFocusRequester),
                    contentPadding = innerPadding + PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    item {
                        androidx.compose.material3.Text(
                            "Excluded folders (" + excluded.size + ")",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    if (excluded.isEmpty()) {
                        item {
                            androidx.compose.material3.Text(
                                "No folder is excluded. Excluded folders are not shown in the app.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                    itemsIndexed(excluded) { _, path ->
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            ) {
                                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                    androidx.compose.material3.Text(path.substringAfterLast('/').ifEmpty { path }, style = MaterialTheme.typography.titleSmall)
                                    androidx.compose.material3.Text(
                                        path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                androidx.compose.material3.IconButton(onClick = { onAction(FolderPreferencesUiEvent.UpdateExcludeList(path)) }) {
                                    Icon(imageVector = NextIcons.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                    item {
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp),
                        ) {
                            androidx.compose.material3.Button(onClick = { runCatching { picker.launch(null) } }) {
                                androidx.compose.material3.Text("Add folder")
                            }
                            androidx.compose.material3.OutlinedButton(onClick = { showAll = true }) {
                                androidx.compose.material3.Text("Choose from found folders")
                            }
                        }
                    }
                }
                if (showAll) {
                    val folders = state.foldersDataState.value.sortedBy { it.path.lowercase() }
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showAll = false },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { showAll = false }) {
                                androidx.compose.material3.Text("Done")
                            }
                        },
                        title = { androidx.compose.material3.Text("Folders with videos") },
                        text = {
                            LazyColumn {
                                itemsIndexed(folders) { index, folder ->
                                    SelectablePreference(
                                        title = folder.name,
                                        description = folder.path,
                                        selected = folder.path in state.preferences.excludeFolders,
                                        onClick = {
                                            onAction(FolderPreferencesUiEvent.UpdateExcludeList(folder.path))
                                        },
                                        isFirstItem = index == 0,
                                        isLastItem = index == folders.lastIndex,
                                    )
                                }
                            }
                        },
                    )
                }
            }

            is DataState.Error -> Unit
        }
    }
}

@PreviewLightDark
@Composable
private fun FolderPreferencesScreenPreview() {
    NextPlayerTheme {
        FolderPreferencesScreenContent(
            state = FolderPreferencesUiState(),
            onAction = {},
        )
    }
}

/** 69 mod: a folder picked with a picker as a file path ("/storage/emulated/0/Movies"), or null. */
private fun mod69TreeToPath(context: android.content.Context, uri: android.net.Uri): String? {
    val id = runCatching { android.provider.DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return null
    // 69-Material Files: the id already is the path
    if (id.startsWith("/")) return id.trimEnd('/')
    if (uri.authority != "com.android.externalstorage.documents") return null
    val volume = id.substringBefore(':')
    val rel = id.substringAfter(':', "").trim('/')
    val base = if (volume.equals("primary", ignoreCase = true)) {
        android.os.Environment.getExternalStorageDirectory().absolutePath
    } else {
        "/storage/$volume"
    }
    return if (rel.isEmpty()) base else "$base/$rel"
}
