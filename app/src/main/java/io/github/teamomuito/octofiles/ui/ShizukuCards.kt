package io.github.teamomuito.octofiles.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.teamomuito.octofiles.priv.Corpse
import io.github.teamomuito.octofiles.priv.CorpseKind
import io.github.teamomuito.octofiles.priv.ExternalCache
import io.github.teamomuito.octofiles.priv.PrivShell
import io.github.teamomuito.octofiles.priv.ShizukuState
import io.github.teamomuito.octofiles.ui.theme.LocalPanel
import io.github.teamomuito.octofiles.ui.theme.PanelCard
import kotlinx.coroutines.launch

/** The deeper clean that needs Shizuku: how to get it running until it is, then its two sections. */
@Composable
fun ShizukuSection(vm: CleanViewModel, open: Set<String>, onToggle: (String) -> Unit) {
    val state by vm.shizuku.collectAsStateWithLifecycle()
    if (state != ShizukuState.READY) {
        ShizukuAsk(state, onAllow = vm::askShizuku, onRecheck = vm::refreshShizuku)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CorpseCard(vm, open = "corpses" in open, onOpen = { onToggle("corpses") })
        CacheCard(vm, open = "shizuku-caches" in open, onOpen = { onToggle("shizuku-caches") })
    }
}

@Composable
private fun ShizukuAsk(state: ShizukuState, onAllow: () -> Unit, onRecheck: () -> Unit) {
    val context = LocalContext.current
    val (title, body) = when (state) {
        ShizukuState.MISSING -> "go deeper with shizuku" to
            "shizuku lets potato look inside other apps' storage folders and clear app caches, without root. " +
            "install shizuku, start it (on android 11 and up you can start it from its own app with wireless debugging), " +
            "then come back here."
        ShizukuState.NEEDS_PERMISSION -> "let potato use shizuku" to
            "shizuku is running. one tap lets potato use it. only the leftovers and deep cache sections use it, " +
            "and only when you open them."
        ShizukuState.OUTDATED -> "update shizuku" to
            "this shizuku is too old for potato. update it from shizuku's download page, then come back here."
        ShizukuState.READY -> return
    }
    PanelCard(
        tint = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (LocalPanel.current.dark) 0.45f else 0.7f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state == ShizukuState.NEEDS_PERMISSION) {
                    Button(onClick = onAllow) { Text("allow") }
                } else {
                    Button(onClick = { openShizuku(context) }) {
                        Text(if (state == ShizukuState.MISSING) "get shizuku" else "open shizuku")
                    }
                }
                TextButton(onClick = onRecheck) { Text("i've done it") }
            }
        }
    }
}

@Composable
private fun CorpseCard(vm: CleanViewModel, open: Boolean, onOpen: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scan by vm.corpses.collectAsStateWithLifecycle()
    val picked by vm.corpsePicked.collectAsStateWithLifecycle()
    val busy by vm.privBusy.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf(false) }

    val found = (scan as? PrivScan.Done<Corpse>)?.items.orEmpty()
    val pickedItems = found.filter { it.path in picked }
    val pickedBytes = pickedItems.sumOf { it.bytes }
    // only the app-data folders are ticked by default, so the header's select-all covers those and nothing else
    val tickable = found.filter { it.kind.ticked }
    val tickedPicked = tickable.count { it.path in picked }
    val size = when (scan) {
        is PrivScan.Done -> if (found.isEmpty()) "none" else formatBytes(context, found.sumOf { it.bytes })
        PrivScan.Working -> "…"
        else -> ""
    }

    CardSection {
        SectionHeader(
            title = if (found.isEmpty()) "leftovers from uninstalled apps" else "leftovers · ${found.size}",
            body = "folders apps left in android/data, obb and media after you removed them. nothing goes until you tap remove.",
            size = size,
            open = open,
            onOpen = onOpen,
            leading = {
                TriStateCheckbox(
                    state = tristate(tickedPicked, tickable.size),
                    enabled = tickable.isNotEmpty() && !busy,
                    onClick = { vm.setCorpses(tickable, tickedPicked != tickable.size) },
                )
            },
        )
        if (open) {
            Spacer(Modifier.height(10.dp))
            when (val s = scan) {
                PrivScan.Idle -> FilledTonalButton(onClick = vm::scanCorpses) { Text("look") }
                PrivScan.Working -> Text("looking around…", style = MaterialTheme.typography.bodySmall)
                is PrivScan.Failed -> {
                    Text(s.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    FilledTonalButton(onClick = vm::scanCorpses) { Text("try again") }
                }
                is PrivScan.Done -> {
                    if (found.isEmpty()) {
                        Text("no leftovers, all tidy.", style = MaterialTheme.typography.bodySmall)
                    }
                    for (corpse in found) {
                        val detail = if (corpse.kind == CorpseKind.MEDIA) "${corpse.kind.label} · might be yours" else corpse.kind.label
                        PickRow(
                            name = corpse.pkg,
                            detail = detail,
                            bytes = corpse.bytes,
                            checked = corpse.path in picked,
                            onToggle = { vm.toggleCorpse(corpse) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = vm::scanCorpses, enabled = !busy) { Text("look again") }
                        if (pickedItems.isNotEmpty()) {
                            Button(onClick = { confirming = true }, enabled = !busy) {
                                Text("remove ${plural(pickedItems.size, "folder")} · ${formatBytes(context, pickedBytes)}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("remove these?") },
            text = {
                Text(
                    "this deletes ${plural(pickedItems.size, "folder")} (${formatBytes(context, pickedBytes)}) for good. " +
                        "media folders can hold photos and voice notes, so have a look first if you're not sure.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    scope.launch {
                        vm.removeCorpses()
                            .onSuccess { freed -> toast(context, "poof! ${formatBytes(context, freed)} of leftovers removed") }
                            .onFailure { toast(context, it.message ?: "shizuku didn't answer") }
                    }
                }) { Text("remove") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("not yet") } },
        )
    }
}

@Composable
private fun CacheCard(vm: CleanViewModel, open: Boolean, onOpen: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scan by vm.externalCaches.collectAsStateWithLifecycle()
    val picked by vm.cachePicked.collectAsStateWithLifecycle()
    val busy by vm.privBusy.collectAsStateWithLifecycle()

    val found = (scan as? PrivScan.Done<ExternalCache>)?.items.orEmpty()
    val pickedItems = found.filter { it.path in picked }
    val size = when (scan) {
        is PrivScan.Done -> formatBytes(context, found.sumOf { it.bytes })
        PrivScan.Working -> "…"
        else -> ""
    }

    CardSection {
        SectionHeader(
            title = "app caches, deep",
            body = "cache folders apps keep on shared storage. the button clears every app's cache at once, the way android's own trim does.",
            size = size,
            open = open,
            onOpen = onOpen,
            leading = {
                TriStateCheckbox(
                    state = tristate(pickedItems.size, found.size),
                    enabled = found.isNotEmpty() && !busy,
                    onClick = { vm.setCaches(found, pickedItems.size != found.size) },
                )
            },
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        vm.trimAllCaches()
                            .onSuccess { freed ->
                                toast(context, if (freed > 0) "poof! ${formatBytes(context, freed)} of cache cleared" else "android trimmed every app's cache")
                            }
                            .onFailure { toast(context, it.message ?: "shizuku didn't answer") }
                    }
                },
                enabled = !busy,
            ) { Text("clear all app caches") }
            TextButton(onClick = vm::scanCaches, enabled = !busy && scan != PrivScan.Working) {
                Text(if (scan is PrivScan.Done) "look again" else "look")
            }
        }
        (scan as? PrivScan.Failed)?.let {
            Text(it.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (open) {
            Spacer(Modifier.height(6.dp))
            Text(
                "the big button clears every app's cache, including the parts potato can't see. " +
                    "tick apps below to clear only their shared-storage cache.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for (cache in found) {
                PickRow(
                    name = cache.label,
                    detail = cache.pkg,
                    bytes = cache.bytes,
                    checked = cache.path in picked,
                    onToggle = { vm.toggleCache(cache) },
                )
            }
            if (pickedItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            vm.clearPickedCaches()
                                .onSuccess { freed -> toast(context, "poof! ${formatBytes(context, freed)} of cache cleared") }
                                .onFailure { toast(context, it.message ?: "shizuku didn't answer") }
                        }
                    },
                    enabled = !busy,
                ) {
                    Text("clear ${plural(pickedItems.size, "app")} · ${formatBytes(context, pickedItems.sumOf { it.bytes })}")
                }
            }
        }
    }
}

/** The card wrapper for the clean tab's newer sections. Named apart from the Section helpers in the other screens, which it would clash with. */
@Composable
internal fun CardSection(content: @Composable () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun PickRow(name: String, detail: String, bytes: Long, checked: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(formatBytes(context, bytes), style = MaterialTheme.typography.labelMedium)
    }
}

private fun tristate(ticked: Int, total: Int): ToggleableState = when {
    total == 0 || ticked == 0 -> ToggleableState.Off
    ticked == total -> ToggleableState.On
    else -> ToggleableState.Indeterminate
}

/** Opens Shizuku's own app if it's installed, otherwise its download page. */
private fun openShizuku(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(PrivShell.MANAGER_PACKAGE)
        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
    runCatching { context.startActivity(intent) }
}

private fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
