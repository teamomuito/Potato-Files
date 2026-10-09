package io.github.teamomuito.octofiles.ui.files

import android.content.Context
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.teamomuito.octofiles.files.Analyzer
import io.github.teamomuito.octofiles.files.AutoSort
import io.github.teamomuito.octofiles.files.BatchRename
import io.github.teamomuito.octofiles.files.DAY
import io.github.teamomuito.octofiles.files.FileItem
import io.github.teamomuito.octofiles.files.FilesDb
import io.github.teamomuito.octofiles.files.Fs
import io.github.teamomuito.octofiles.files.LanServer
import io.github.teamomuito.octofiles.files.Privacy
import io.github.teamomuito.octofiles.files.Trash
import io.github.teamomuito.octofiles.files.Vault
import io.github.teamomuito.octofiles.files.VaultRow
import io.github.teamomuito.octofiles.files.Rule
import io.github.teamomuito.octofiles.files.TrashRow
import io.github.teamomuito.octofiles.files.SecureDelete
import io.github.teamomuito.octofiles.ui.formatBytes
import io.github.teamomuito.octofiles.ui.whenTaken
import io.github.teamomuito.octofiles.ui.theme.bottomSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class Tool(val title: String, val blurb: String) {
    ANALYZER("storage analyzer", "what's taking the space, and what you haven't opened in ages"),
    DUPLICATES("duplicates", "identical files, found by content, not name"),
    RENAME("batch rename", "find and replace across a folder, with a preview"),
    VAULT("vault", "encrypted files, unlocked with your fingerprint or screen lock"),
    TIMELINE("timeline", "everything changed in the last two weeks, by day"),
    PRIVACY("privacy audit", "apps holding storage, location, camera, mic or contacts"),
    LAN("wi-fi server", "browse and download from a computer on the same network"),
    AUTOSORT("auto-sort", "rules that move new files into folders"),
    TRASH("trash", "what you've binned, kept for 30 days"),
}

/** The tools tab. Everything here runs on the phone. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolsScreen(onSwipeAll: () -> Unit) {
    var tool by rememberSaveable { mutableStateOf<Tool?>(null) }
    BackHandler(enabled = tool != null) { tool = null }

    val open = tool
    if (open == null) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomSpace() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("tools", style = MaterialTheme.typography.headlineMedium)
                    Button(onClick = onSwipeAll, modifier = Modifier.fillMaxWidth()) { Text("swipe through everything") }
                }
            }
            items(Tool.entries) { entry ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    TextButton(onClick = { tool = entry }) { Text(entry.title) }
                    Text(entry.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("not built yet", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "cloud accounts (Drive, Dropbox, OneDrive), Wi-Fi Direct and Bluetooth transfer, 7z and RAR, " +
                            "password-protected zips, a map view for geotagged photos, scripting, kids and student modes, " +
                            "and OCR beyond the Latin alphabet. Each of these needs its own accounts, hardware or libraries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { tool = null }) { Text("tools") }
            Text(open.title, style = MaterialTheme.typography.titleMedium)
        }
        when (open) {
            Tool.ANALYZER -> AnalyzerTool()
            Tool.DUPLICATES -> DuplicatesTool()
            Tool.RENAME -> RenameTool()
            Tool.VAULT -> VaultTool()
            Tool.TIMELINE -> TimelineTool()
            Tool.PRIVACY -> PrivacyTool()
            Tool.LAN -> LanTool()
            Tool.AUTOSORT -> AutoSortTool()
            Tool.TRASH -> TrashTool()
        }
    }
}

@Composable
private fun ToolList(content: LazyListScope.() -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomSpace() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun AnalyzerTool() {
    val context = LocalContext.current
    val db = remember { FilesDb.get(context) }
    val scope = rememberCoroutineScope()
    var reload by remember { mutableStateOf(0) }
    val data by produceState<Triple<List<Analyzer.Folder>, List<FileItem>, List<FileItem>>?>(null, reload) {
        value = withContext(Dispatchers.IO) {
            Triple(Analyzer.folderSizes(Fs.storage), Analyzer.large(Fs.storage), Analyzer.untouched(Fs.storage))
        }
    }
    val space by produceState(0L to 0L, reload) { value = withContext(Dispatchers.IO) { Fs.space() } }
    val loaded = data
    ToolList {
        item {
            Text(
                "${formatBytes(context, space.first)} free of ${formatBytes(context, space.second)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (loaded == null) {
            item { Text("measuring…") }
            return@ToolList
        }
        item { Text("biggest folders", style = MaterialTheme.typography.titleSmall) }
        val biggest = loaded.first.firstOrNull()?.size?.coerceAtLeast(1L) ?: 1L
        items(loaded.first.take(20)) { folder ->
            Column {
                Text("${folder.file.name}  ·  ${formatBytes(context, folder.size)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = { folder.size.toFloat() / biggest },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
        item { Text("large files, 100 MB and up", style = MaterialTheme.typography.titleSmall) }
        items(loaded.second) { item ->
            FileLine(item) {
                scope.launch(Dispatchers.IO) { Trash.move(db, item.file) }
                reload++
            }
        }
        item { Text("untouched for 90 days or more", style = MaterialTheme.typography.titleSmall) }
        items(loaded.third) { item ->
            FileLine(item) {
                scope.launch(Dispatchers.IO) { Trash.move(db, item.file) }
                reload++
            }
        }
    }
}

@Composable
private fun FileLine(item: FileItem, onTrash: () -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatBytes(context, item.size)} · ${whenTaken(item.modified)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onTrash) { Text("trash") }
    }
}

@Composable
private fun DuplicatesTool() {
    val context = LocalContext.current
    val db = remember { FilesDb.get(context) }
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<List<FileItem>>?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    ToolList {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("finds files over 1 MB with identical contents. Nothing is removed until you tap it.")
                Button(onClick = {
                    scanning = true
                    scope.launch {
                        groups = withContext(Dispatchers.IO) { Analyzer.duplicates(Fs.storage) }
                        scanning = false
                    }
                }, enabled = !scanning) { Text(if (scanning) "scanning…" else "scan storage") }
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
        val found = groups.orEmpty()
        if (groups != null && found.isEmpty()) item { Text("no duplicates found") }
        items(found) { group ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("${group.size} copies · ${formatBytes(context, group.first().size)} each", style = MaterialTheme.typography.titleSmall)
                group.forEachIndexed { i, item ->
                    Text(
                        (if (i == 0) "keep  " else "") + item.path,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(onClick = {
                    val extras = group.drop(1)
                    groups = found.map { g -> if (g == group) listOf(g.first()) else g }.filter { it.size > 1 }
                    scope.launch(Dispatchers.IO) { extras.forEach { Trash.move(db, it.file) } }
                    status = "moved ${extras.size} extra copies to the trash"
                }) { Text("trash the other ${group.size - 1}") }
            }
        }
    }
}

@Composable
private fun RenameTool() {
    var folder by rememberSaveable { mutableStateOf(Fs.storage.absolutePath + "/" + Environment.DIRECTORY_DOWNLOADS) }
    var find by rememberSaveable { mutableStateOf("") }
    var replace by rememberSaveable { mutableStateOf("Photo {n}") }
    var regex by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    val files = remember(folder, reload) {
        File(folder).listFiles().orEmpty().filter { it.isFile && !it.isHidden }.sortedBy { it.name.lowercase() }
    }
    val previews = remember(files, find, replace, regex) { BatchRename.plan(files, find, replace, regex) }
    val changed = previews.count { it.changed }

    ToolList {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(folder, { folder = it }, label = { Text("folder") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(find, { find = it }, label = { Text("find (empty means replace the whole name)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(replace, { replace = it }, label = { Text("replace with ({n} is the number)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { regex = !regex }) { Text(if (regex) "regex: on" else "regex: off") }
                    Button(
                        onClick = {
                            val done = BatchRename.apply(previews)
                            status = "renamed $done files"
                            reload++
                        },
                        enabled = changed > 0,
                    ) { Text("rename $changed") }
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
        items(previews.take(300)) { p ->
            Text(
                if (p.changed) "${p.file.name}  →  ${p.newName}" else p.file.name,
                style = MaterialTheme.typography.bodySmall,
                color = if (p.changed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VaultTool() {
    val context = LocalContext.current
    val db = remember { FilesDb.get(context) }
    val scope = rememberCoroutineScope()
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }
    val rows = remember(reload, unlocked) { db.vaultAll() }

    ToolList {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Lock files from the browser with \"lock in vault\". They're encrypted in app storage, so other apps and the gallery can't see them.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!unlocked) {
                    Button(onClick = { Biometric.require(context, "unlock vault") { unlocked = true } }) { Text("unlock") }
                } else {
                    OutlinedButton(onClick = { unlocked = false }) { Text("lock again") }
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
        if (unlocked && rows.isEmpty()) item { Text("the vault is empty") }
        if (unlocked) {
            items(rows) { row ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatBytes(context, row.size), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            val out = withContext(Dispatchers.IO) {
                                Vault(context, db).export(row, File(Fs.storage, Environment.DIRECTORY_DOWNLOADS + "/restored"))
                            }
                            status = if (out != null) "restored to ${out.path}" else "couldn't restore ${row.name}"
                        }
                    }) { Text("restore") }
                    TextButton(onClick = {
                        Vault(context, db).remove(row)
                        reload++
                    }) { Text("delete") }
                }
            }
        }
    }
}

@Composable
private fun TimelineTool() {
    val context = LocalContext.current
    val files by produceState<List<FileItem>?>(null) {
        value = withContext(Dispatchers.IO) { Fs.recent(Fs.storage, 14) }
    }
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE d MMM") }
    val zone = remember { ZoneId.systemDefault() }
    ToolList {
        val list = files ?: run {
            item { Text("looking…") }
            return@ToolList
        }
        if (list.isEmpty()) item { Text("nothing changed in the last two weeks") }
        val byDay = list.groupBy { Instant.ofEpochMilli(it.modified).atZone(zone).toLocalDate() }
        byDay.forEach { (day, dayFiles) ->
            item {
                Text(
                    "${day.format(formatter)} · ${dayFiles.size} files",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(dayFiles.take(60)) { item ->
                Text(
                    "${item.name}  ·  ${formatBytes(context, item.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PrivacyTool() {
    val context = LocalContext.current
    val apps by produceState<List<Privacy.AppAccess>?>(null) {
        value = withContext(Dispatchers.IO) { Privacy.audit(context) }
    }
    ToolList {
        item {
            Text(
                "Apps you installed that hold sensitive permissions right now. To take one back, open the app's permissions in Android settings.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val list = apps ?: run {
            item { Text("checking…") }
            return@ToolList
        }
        if (list.isEmpty()) item { Text("no installed apps hold sensitive permissions") }
        items(list) { app ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(app.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.sensitive.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LanTool() {
    val token = rememberSaveable { LanServer.newToken() }
    var server by remember { mutableStateOf<LanServer?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val running = server?.isRunning == true
    val address = remember(running) { localAddress() }

    ToolList {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Starts a read-only server on this phone's Wi-Fi for shared storage. Anyone on the network with the code can download files, so only use it at home or somewhere you trust. Traffic isn't encrypted.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (running && address != null) {
                    Text("open on a computer: http://$address:${LanServer.PORT}/?token=$token", style = MaterialTheme.typography.bodyMedium)
                    Text("code: $token", style = MaterialTheme.typography.titleLarge)
                }
                Button(onClick = {
                    if (running) {
                        server?.stop()
                        server = null
                        status = "server stopped"
                    } else {
                        val s = LanServer(Fs.storage, token)
                        runCatching { s.start() }
                            .onSuccess { server = s; status = null }
                            .onFailure { status = "couldn't start, is port ${LanServer.PORT} already in use?" }
                    }
                }) { Text(if (running) "stop server" else "start server") }
                status?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

/** This phone's IPv4 address on a local network, for showing the server URL. */
private fun localAddress(): String? = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
        ?.hostAddress
}.getOrNull()

@Composable
private fun AutoSortTool() {
    val context = LocalContext.current
    val db = remember { FilesDb.get(context) }
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<Rule>>(db.rules()) }
    var adding by remember { mutableStateOf(false) }
    var match by remember { mutableStateOf("pdf") }
    var dest by remember { mutableStateOf("Documents/PDFs") }
    var status by remember { mutableStateOf<String?>(null) }

    ToolList {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "A rule matches a kind (image, video, audio, document, archive, text) or an extension (pdf, apk). Matching files in Downloads move to the folder under storage. Runs when you tap the button.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { adding = true }) { Text("add rule") }
                    Button(onClick = {
                        scope.launch {
                            val moved = withContext(Dispatchers.IO) {
                                AutoSort.run(db, File(Fs.storage, Environment.DIRECTORY_DOWNLOADS))
                            }
                            status = "moved $moved files"
                        }
                    }) { Text("run now") }
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
        if (rules.isEmpty()) item { Text("no rules yet") }
        items(rules) { rule ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${rule.match} → ${rule.dest}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = {
                    db.removeRule(rule.id)
                    rules = db.rules()
                }) { Text("remove") }
            }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("new rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(match, { match = it }, label = { Text("kind or extension") }, singleLine = true)
                    OutlinedTextField(dest, { dest = it }, label = { Text("folder under storage") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (match.isNotBlank() && dest.isNotBlank()) {
                        db.addRule(match, dest)
                        rules = db.rules()
                    }
                    adding = false
                }) { Text("save") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("cancel") } },
        )
    }
}

@Composable
private fun TrashTool() {
    val context = LocalContext.current
    val db = remember { FilesDb.get(context) }
    var rows by remember { mutableStateOf<List<TrashRow>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        withContext(Dispatchers.IO) { Trash.purgeExpired(db) }
        rows = withContext(Dispatchers.IO) { db.trashAll() }
    }

    ToolList {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Files stay here for ${Trash.KEEP_DAYS} days, then are removed for good.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        Trash.emptyAll(db)
                        status = "trash emptied"
                        reload++
                    }, enabled = rows.isNotEmpty()) { Text("empty trash") }
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
        if (rows.isEmpty()) item { Text("the trash is empty") }
        items(rows) { row ->
            Column(Modifier.fillMaxWidth()) {
                Text(File(row.original).name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${File(row.original).parent} · ${whenTaken(row.deletedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row {
                    TextButton(onClick = {
                        val ok = Trash.restore(db, row)
                        status = if (ok) "restored ${File(row.original).name}" else "couldn't restore that one"
                        reload++
                    }) { Text("restore") }
                    TextButton(onClick = {
                        Trash.purge(db, row)
                        reload++
                    }) { Text("delete now") }
                }
            }
        }
    }
}
