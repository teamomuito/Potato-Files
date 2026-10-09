package io.github.teamomuito.octofiles.ui.files

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.teamomuito.octofiles.files.Analyzer
import io.github.teamomuito.octofiles.files.Archive
import io.github.teamomuito.octofiles.files.FileItem
import io.github.teamomuito.octofiles.files.FileKind
import io.github.teamomuito.octofiles.files.FilesDb
import io.github.teamomuito.octofiles.files.Fs
import io.github.teamomuito.octofiles.files.Opener
import io.github.teamomuito.octofiles.files.SecureDelete
import io.github.teamomuito.octofiles.files.SortBy
import io.github.teamomuito.octofiles.files.Trash
import io.github.teamomuito.octofiles.files.Vault
import io.github.teamomuito.octofiles.ui.SettingsButton
import io.github.teamomuito.octofiles.ui.formatBytes
import io.github.teamomuito.octofiles.ui.whenTaken
import io.github.teamomuito.octofiles.ui.theme.bottomSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.RectangleShape

/** Files copied or cut from the browser, waiting for a paste. */
object FileClip {
    var paths by mutableStateOf<List<String>>(emptyList())
    var cut by mutableStateOf(false)
}

private sealed interface Pending {
    data class Rename(val item: FileItem) : Pending
    data class NewFolder(val parent: File) : Pending
    data class Tags(val item: FileItem) : Pending
    data class Confirm(val title: String, val body: String, val confirm: String, val action: () -> Unit) : Pending
}

/**
 * File browser. Folders open in place, text files open in the viewer, everything else goes to
 * another app. Long press selects, "more" opens the per-file actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesScreen(onOpenText: (File) -> Unit, onSwipe: (File) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val db = remember { FilesDb.get(context) }
    val scope = rememberCoroutineScope()

    var path by rememberSaveable { mutableStateOf(Fs.storage.absolutePath) }
    var sort by rememberSaveable { mutableStateOf(SortBy.NAME) }
    var query by rememberSaveable { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var allowed by remember { mutableStateOf(Fs.hasAllFilesAccess()) }
    val selected = remember { mutableStateListOf<String>() }
    var menuFor by remember { mutableStateOf<FileItem?>(null) }
    var pending by remember { mutableStateOf<Pending?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        allowed = Fs.hasAllFilesAccess()
        reload++
    }

    val here = File(path)
    val files by produceState(initialValue = emptyList<FileItem>(), path, sort, query, reload, allowed) {
        value = withContext(Dispatchers.IO) {
            when {
                !allowed -> emptyList()
                query.isBlank() -> Fs.list(here, sort)
                else -> Fs.search(here, query)
            }
        }
    }
    val space by produceState(initialValue = 0L to 0L, reload) {
        value = withContext(Dispatchers.IO) { Fs.space() }
    }

    fun refresh() {
        reload++
    }

    fun runIo(message: String?, block: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.IO) { block() }
            status = message
            refresh()
        }
    }

    fun act(action: FileAction, item: FileItem) {
        when (action) {
            FileAction.OPEN -> when {
                item.isDir -> path = item.path
                opensInViewer(item) -> onOpenText(item.file)
                else -> Opener.open(context, item.file)
            }
            FileAction.RENAME -> pending = Pending.Rename(item)
            FileAction.TAGS -> pending = Pending.Tags(item)
            FileAction.COPY -> {
                FileClip.paths = listOf(item.path)
                FileClip.cut = false
                status = "copied ${item.name}, open a folder and paste"
            }
            FileAction.CUT -> {
                FileClip.paths = listOf(item.path)
                FileClip.cut = true
                status = "ready to move ${item.name}, open a folder and paste"
            }
            FileAction.ZIP -> runIo("zipped ${item.name}") {
                val dest = Fs.uniqueIn(item.file.parentFile ?: here, item.name + ".zip")
                Archive.zip(listOf(item.file), dest)
            }
            FileAction.UNZIP -> runIo("unzipped ${item.name}") { Archive.unzip(item.file) }
            FileAction.LOCK -> pending = Pending.Confirm(
                title = "lock ${item.name}?",
                body = "It gets encrypted into the vault, then the original is overwritten and deleted. Unlocking takes your fingerprint or screen lock.",
                confirm = "lock",
            ) {
                Biometric.require(context, "lock ${item.name}") {
                    runIo("locked ${item.name} in the vault") {
                        Vault(context, db).add(item.file) && SecureDelete.wipe(item.file)
                    }
                }
            }
            FileAction.SHARE -> Opener.share(context, listOf(item.file))
            FileAction.TRASH -> runIo("moved ${item.name} to the trash") { Trash.move(db, item.file) }
            FileAction.SECURE_DELETE -> pending = Pending.Confirm(
                title = "secure delete ${item.name}?",
                body = "This overwrites the file with random data and deletes it. It skips the trash and can't be undone.",
                confirm = "delete",
            ) {
                runIo("securely deleted ${item.name}") { SecureDelete.wipe(item.file) }
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomSpace() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("files", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        SettingsButton(onClick = onSettings)
                    }
                    Text(
                        "${formatBytes(context, space.first)} free of ${formatBytes(context, space.second)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!allowed) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer, RectangleShape)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Browsing needs all files access. Nothing leaves the phone.")
                            Button(onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }) { Text("allow all files access") }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (path != Fs.storage.absolutePath) {
                            OutlinedButton(onClick = { path = here.parent ?: path }) { Text("up") }
                        }
                        Text(
                            "/" + here.absolutePath.removePrefix(Fs.storage.absolutePath).trimStart('/'),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("search this folder and below") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SortBy.entries.forEach { option ->
                            FilterChip(
                                selected = sort == option,
                                onClick = { sort = option },
                                label = { Text(option.label) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { pending = Pending.NewFolder(here) }) { Text("new folder") }
                        TextButton(onClick = { onSwipe(here) }) { Text("swipe this folder") }
                        if (FileClip.paths.isNotEmpty()) {
                            Button(onClick = {
                                val sources = FileClip.paths.map { File(it) }
                                val cut = FileClip.cut
                                FileClip.paths = emptyList()
                                runIo("pasted ${sources.size} here") {
                                    sources.forEach { if (cut) Fs.moveInto(it, here) else Fs.copyInto(it, here) }
                                }
                            }) { Text("paste ${FileClip.paths.size}") }
                        }
                    }
                    if (selected.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${selected.size} selected", modifier = Modifier.padding(top = 8.dp))
                            TextButton(onClick = {
                                val chosen = selected.map { File(it) }
                                selected.clear()
                                runIo("moved ${chosen.size} to the trash") { chosen.forEach { Trash.move(db, it) } }
                            }) { Text("trash") }
                            TextButton(onClick = {
                                FileClip.paths = selected.toList()
                                FileClip.cut = false
                                selected.clear()
                            }) { Text("copy") }
                            TextButton(onClick = {
                                FileClip.paths = selected.toList()
                                FileClip.cut = true
                                selected.clear()
                            }) { Text("move") }
                            TextButton(onClick = { selected.clear() }) { Text("clear") }
                        }
                    }
                    status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                }
            }
            if (files.isEmpty() && allowed) {
                item {
                    Text(
                        if (query.isBlank()) "this folder is empty" else "nothing matches \"$query\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            items(files, key = { it.path }) { item ->
                FileRow(
                    item = item,
                    selected = item.path in selected,
                    onOpen = {
                        if (selected.isNotEmpty()) toggle(selected, item.path) else act(FileAction.OPEN, item)
                    },
                    onSelect = { toggle(selected, item.path) },
                    onMore = { menuFor = item },
                )
            }
        }
    }

    menuFor?.let { item ->
        FileActionsDialog(
            item = item,
            onAction = { action ->
                menuFor = null
                act(action, item)
            },
            onDismiss = { menuFor = null },
        )
    }

    when (val p = pending) {
        null -> Unit
        is Pending.Rename -> TextPromptDialog(
            title = "rename",
            initial = p.item.name,
            label = "new name",
            confirm = "rename",
            onConfirm = { name ->
                pending = null
                val renamed = Fs.rename(p.item.file, name)
                status = if (renamed != null) "renamed to ${renamed.name}" else "that name is taken or invalid"
                refresh()
            },
            onDismiss = { pending = null },
        )
        is Pending.NewFolder -> TextPromptDialog(
            title = "new folder",
            initial = "",
            label = "folder name",
            confirm = "create",
            onConfirm = { name ->
                pending = null
                val made = Fs.mkdir(p.parent, name)
                status = if (made != null) "created ${made.name}" else "couldn't create that folder"
                refresh()
            },
            onDismiss = { pending = null },
        )
        is Pending.Tags -> TagNoteDialog(
            path = p.item.path,
            name = p.item.name,
            db = db,
            onDismiss = { pending = null },
        )
        is Pending.Confirm -> ConfirmDialog(
            title = p.title,
            body = p.body,
            confirm = p.confirm,
            onConfirm = {
                pending = null
                p.action()
            },
            onDismiss = { pending = null },
        )
    }
}

private fun toggle(selected: MutableList<String>, path: String) {
    if (path in selected) selected.remove(path) else selected.add(path)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    item: FileItem,
    selected: Boolean,
    onOpen: () -> Unit,
    onSelect: () -> Unit,
    onMore: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(onClick = onOpen, onLongClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (item.isDir) "${item.name}/" else item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                if (item.isDir) {
                    "folder · ${whenTaken(item.modified)}"
                } else {
                    "${item.kind.label} · ${formatBytes(context, item.size)} · ${whenTaken(item.modified)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onMore) { Text("more") }
    }
}
