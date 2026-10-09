package io.github.teamomuito.octofiles.ui.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.teamomuito.octofiles.files.Preview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val HEX_PAGE = 4096L

/** In-app viewer for text, with a hex mode for anything else. Read only. */
@Composable
fun ViewerScreen(file: File, onBack: () -> Unit) {
    var hexMode by remember(file) { mutableStateOf(false) }
    var offset by remember(file) { mutableLongStateOf(0L) }
    val content by produceState(initialValue = "", file, hexMode, offset) {
        value = withContext(Dispatchers.IO) {
            if (hexMode) Preview.hex(file, offset, HEX_PAGE.toInt()) else Preview.text(file)
        }
    }
    val tooBig = !hexMode && file.length() > Preview.TEXT_LIMIT

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("back") }
            Text(
                file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = {
                hexMode = !hexMode
                offset = 0L
            }) { Text(if (hexMode) "show as text" else "show as hex") }
            if (hexMode) {
                TextButton(
                    onClick = { offset = (offset - HEX_PAGE).coerceAtLeast(0L) },
                    enabled = offset > 0,
                ) { Text("previous") }
                TextButton(
                    onClick = { offset += HEX_PAGE },
                    enabled = offset + HEX_PAGE < file.length(),
                ) { Text("next") }
            }
        }
        if (tooBig) {
            Text(
                "showing the first 256 KB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SelectionContainer {
            Text(
                content.ifEmpty { "nothing to show" },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}
