package io.github.teamomuito.octofiles.ui.files

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.github.teamomuito.octofiles.files.FileItem
import io.github.teamomuito.octofiles.files.FilesDb
import io.github.teamomuito.octofiles.files.FileKind

enum class FileAction(val label: String) {
    OPEN("open"),
    RENAME("rename"),
    TAGS("tags and note"),
    COPY("copy"),
    CUT("move"),
    ZIP("zip"),
    UNZIP("unzip"),
    LOCK("lock in vault"),
    SHARE("share"),
    TRASH("move to trash"),
    SECURE_DELETE("secure delete"),
}

/** Only the actions that make sense for this item. */
fun actionsFor(item: FileItem): List<FileAction> = buildList {
    add(FileAction.OPEN)
    add(FileAction.RENAME)
    add(FileAction.TAGS)
    add(FileAction.COPY)
    add(FileAction.CUT)
    add(FileAction.ZIP)
    if (item.file.extension.equals("zip", ignoreCase = true)) add(FileAction.UNZIP)
    if (!item.isDir) {
        add(FileAction.LOCK)
        add(FileAction.SHARE)
    }
    add(FileAction.TRASH)
    add(FileAction.SECURE_DELETE)
}

@Composable
fun FileActionsDialog(item: FileItem, onAction: (FileAction) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, maxLines = 2) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                actionsFor(item).forEach { action ->
                    TextButton(onClick = { onAction(action) }, modifier = Modifier.fillMaxWidth()) {
                        Text(action.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("close") } },
    )
}

@Composable
fun TextPromptDialog(
    title: String,
    initial: String,
    label: String,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("cancel") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagNoteDialog(path: String, name: String, db: FilesDb, onDismiss: () -> Unit) {
    var tags by remember(path) { mutableStateOf(db.tagsOf(path)) }
    var newTag by remember(path) { mutableStateOf("") }
    var note by remember(path) { mutableStateOf(db.noteOf(path)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("tags and note") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(name, modifier = Modifier.padding(bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = {
                                db.removeTag(path, tag)
                                tags = db.tagsOf(path)
                            },
                            label = { Text("$tag  x") },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text("add tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        db.addTag(path, newTag)
                        newTag = ""
                        tags = db.tagsOf(path)
                    }) { Text("add") }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                db.setNote(path, note)
                onDismiss()
            }) { Text("save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("cancel") } },
    )
}

/**
 * Biometric or device-PIN gate for the vault. Calls [onOk] only after a successful check.
 * Needs a FragmentActivity, which MainActivity is.
 */
object Biometric {
    fun require(context: android.content.Context, title: String, onOk: () -> Unit) {
        val activity = context as? FragmentActivity ?: return
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onOk()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
        prompt.authenticate(info)
    }
}

/** Kind labels that should open in the in-app text viewer instead of another app. */
fun opensInViewer(item: FileItem): Boolean =
    item.kind == FileKind.TEXT || (item.kind == FileKind.OTHER && item.size in 1..(256L * 1024))
