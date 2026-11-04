package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.common.mod69.SafLibrary
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One place in the in-app folder picker: a real folder or a folder inside a cloud (rclone) root. */
private data class PickerDir(
    val name: String,
    val localPath: String? = null,
    val treeUri: String? = null,
    val docId: String? = null,
) {
    val isCloud get() = treeUri != null
    val cloudDocUri: String?
        get() = if (treeUri != null && docId != null) {
            DocumentsContract.buildDocumentUriUsingTree(Uri.parse(treeUri), docId).toString()
        } else {
            null
        }
}

private fun storageRoots(context: Context): List<PickerDir> {
    val out = mutableListOf(PickerDir("Internal storage", localPath = Environment.getExternalStorageDirectory().path))
    context.getExternalFilesDirs(null).drop(1).filterNotNull().forEach { f ->
        val root = f.path.substringBefore("/Android/")
        out += PickerDir("SD card (" + root.substringAfterLast('/') + ")", localPath = root)
    }
    SafLibrary.roots(context).forEach { r ->
        val tree = Uri.parse(r.treeUri)
        val id = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull()
        if (id != null) out += PickerDir("☁ " + r.label, treeUri = r.treeUri, docId = id)
    }
    return out
}

private fun childrenOf(context: Context, dir: PickerDir): List<PickerDir> {
    if (dir.localPath != null) {
        return File(dir.localPath).listFiles { f -> f.isDirectory && !f.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase() }
            ?.map { PickerDir(it.name, localPath = it.path) }
            .orEmpty()
    }
    val tree = Uri.parse(dir.treeUri ?: return emptyList())
    val out = mutableListOf<PickerDir>()
    runCatching {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dir.docId)
        context.contentResolver.query(
            uri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR && !name.startsWith(".")) {
                    out += PickerDir(name, treeUri = dir.treeUri, docId = c.getString(0))
                }
            }
        }
    }
    return out.sortedBy { it.name.lowercase() }
}

private fun hasAllFilesAccess(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

/**
 * 69 mod: MX-style destination picker for Copy / Move. Browse your folders and cloud (rclone)
 * folders inside the app, then tap "Paste here".
 */
@Composable
internal fun Mod69FolderPickerDialog(
    move: Boolean,
    count: Int,
    onPasteLocal: (String) -> Unit,
    onPasteCloud: (String) -> Unit,
    onUseSystemPicker: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val stack = remember { mutableStateListOf<PickerDir>() }
    var items by remember { mutableStateOf<List<PickerDir>>(emptyList()) }
    var newFolder by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var needAccess by remember { mutableStateOf(false) }
    val current = stack.lastOrNull()

    LaunchedEffect(current, refresh) {
        items = withContext(Dispatchers.IO) {
            if (current == null) storageRoots(context) else childrenOf(context, current)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text((if (move) "Move " else "Copy ") + "$count item(s) to…")
                Text(
                    text = current?.let { it.localPath ?: stack.joinToString("/") { d -> d.name } } ?: "Choose a place",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
        },
        text = {
            Column {
                if (needAccess) {
                    Text(
                        "To paste into phone folders, allow \"All files access\" for 69-Next Player, then come back and tap Paste here again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + context.packageName),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }) { Text("Allow all files access") }
                    HorizontalDivider()
                }
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    if (current != null) {
                        item {
                            PickerRow(icon = true, name = "..  (up)") { stack.removeAt(stack.lastIndex) }
                        }
                    }
                    items(items, key = { (it.localPath ?: it.cloudDocUri).orEmpty() + it.name }) { d ->
                        PickerRow(icon = true, name = d.name) { stack.add(d) }
                    }
                    if (items.isEmpty() && current != null) {
                        item { Text("No folders here", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp)) }
                    }
                }
                newFolder?.let { name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newFolder = it },
                            singleLine = true,
                            label = { Text("New folder name") },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            val n = name.trim()
                            val dir = current
                            if (n.isNotEmpty() && dir != null) {
                                if (dir.localPath != null) {
                                    File(dir.localPath, n).mkdirs()
                                } else {
                                    runCatching {
                                        DocumentsContract.createDocument(
                                            context.contentResolver,
                                            Uri.parse(dir.cloudDocUri),
                                            DocumentsContract.Document.MIME_TYPE_DIR,
                                            n,
                                        )
                                    }
                                }
                            }
                            newFolder = null
                            refresh++
                        }) { Text("Create") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = current != null,
                onClick = {
                    val dir = current ?: return@TextButton
                    if (dir.isCloud) {
                        dir.cloudDocUri?.let(onPasteCloud)
                    } else if (!hasAllFilesAccess()) {
                        needAccess = true
                    } else {
                        dir.localPath?.let(onPasteLocal)
                    }
                },
            ) { Text("Paste here") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                if (current != null && newFolder == null) TextButton(onClick = { newFolder = "" }) { Text("New folder") }
                TextButton(onClick = onUseSystemPicker) { Text("System picker") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun PickerRow(icon: Boolean, name: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        if (icon) {
            Icon(
                imageVector = NextIcons.Folder,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(name, modifier = Modifier.padding(start = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 69 mod: videos waiting to be pasted after Copy / Move (survives moving between folders). */
object Mod69Clipboard {
    data class Clip(val uris: List<Uri>, val move: Boolean)

    var clip by androidx.compose.runtime.mutableStateOf<Clip?>(null)
}
