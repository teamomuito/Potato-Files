package io.github.teamomuito.octofiles.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.teamomuito.octofiles.data.CustomFilter
import io.github.teamomuito.octofiles.data.JunkKind
import io.github.teamomuito.octofiles.data.Prefs
import io.github.teamomuito.octofiles.data.SystemFilters

/** The system cleaner: built-in filters to switch on or off, and the person's own name patterns. */
@Composable
fun SystemCleanerCard(vm: CleanViewModel, open: Boolean, onOpen: () -> Unit) {
    val off by Prefs.systemOff.collectAsStateWithLifecycle()
    val custom by Prefs.systemCustom.collectAsStateWithLifecycle()
    val scan by vm.scan.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }

    val found = (scan as? ScanState.Done)?.report?.items.orEmpty().filter { it.kind == JunkKind.SYSTEM }
    val size = if (scan is ScanState.Done) "${found.size} found" else ""

    CardSection {
        SectionHeader(
            title = "system cleaner",
            body = "junk the system and other apps leave behind. switch filters on or off, or add your own name pattern.",
            size = size,
            open = open,
            onOpen = onOpen,
        )
        if (open) {
            Spacer(Modifier.height(10.dp))
            for (filter in SystemFilters.STOCK) {
                val on = filter.id !in off
                val count = found.count { it.filter == filter.label }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setStockFilter(filter.id, !on) },
                ) {
                    Checkbox(checked = on, onCheckedChange = { vm.setStockFilter(filter.id, it) })
                    Column(Modifier.weight(1f)) {
                        Text(filter.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            filter.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (count > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("$count", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (custom.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("your patterns", style = MaterialTheme.typography.titleSmall)
                for (c in custom) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            c.pattern,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.removeCustomFilter(c.pattern) }) { Text("remove") }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { adding = true }) { Text("add pattern") }
                Text(
                    "files only. *.bak matches any file ending in .bak.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (adding) {
        AddPatternDialog(vm, onDone = { adding = false })
    }
}

@Composable
private fun AddPatternDialog(vm: CleanViewModel, onDone: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val problem = if (text.isBlank()) null else CustomFilter.problem(text.trim(), Prefs.systemCustom.value)

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("add a pattern") },
        text = {
            Column {
                Text(
                    "matches file names, not folders. * is any run of characters, ? is one.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("pattern") },
                    isError = problem != null,
                    supportingText = { problem?.let { Text(it) } },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (vm.addCustomFilter(text) == null) onDone() },
                enabled = text.isNotBlank() && problem == null,
            ) { Text("add") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("cancel") } },
    )
}
