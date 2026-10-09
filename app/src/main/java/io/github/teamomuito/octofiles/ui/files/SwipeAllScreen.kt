package io.github.teamomuito.octofiles.ui.files

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.offset
import io.github.teamomuito.octofiles.files.FileItem
import io.github.teamomuito.octofiles.files.FileKind
import io.github.teamomuito.octofiles.files.FilesDb
import io.github.teamomuito.octofiles.files.Fs
import io.github.teamomuito.octofiles.files.Trash
import io.github.teamomuito.octofiles.ui.formatBytes
import io.github.teamomuito.octofiles.ui.whenTaken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val SWIPE_THRESHOLD = 180f

/**
 * Tinder-style cleanup for every file under [root], not just photos. Oldest first.
 * Swipe right to keep, left to bin. Binned files wait in a pile until you confirm,
 * then they go to our trash. Buttons do the same for anyone who doesn't want to drag.
 */
@Composable
fun SwipeAllScreen(root: File, onClose: () -> Unit) {
    val context = LocalContext.current
    val db = remember { FilesDb.get(context) }
    val scope = rememberCoroutineScope()

    var queue by remember { mutableStateOf<List<FileItem>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var kept by remember { mutableIntStateOf(0) }
    val bin = remember { mutableStateListOf<FileItem>() }
    val history = remember { mutableStateListOf<Pair<Int, String>>() }
    var dragX by remember { mutableFloatStateOf(0f) }
    var reviewing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(root) {
        queue = withContext(Dispatchers.IO) {
            val decided = db.decidedPaths()
            val found = ArrayList<FileItem>()
            Fs.walk(root) { item ->
                if (!item.isDir && item.path !in decided) found += item
                true
            }
            found.sortedBy { it.modified }
        }
    }

    fun decide(keep: Boolean) {
        val list = queue ?: return
        val item = list.getOrNull(index) ?: return
        if (keep) {
            kept++
            scope.launch(Dispatchers.IO) { db.decide(item.path, "keep") }
        } else {
            bin += item
        }
        history += (index to (if (keep) "keep" else "bin"))
        index++
        dragX = 0f
    }

    fun undo() {
        val list = queue ?: return
        if (history.isEmpty()) return
        val (i, decision) = history[history.lastIndex]
        history.removeAt(history.lastIndex)
        val item = list[i]
        if (decision == "keep") {
            kept--
            scope.launch(Dispatchers.IO) { db.undecide(item.path) }
        } else {
            bin.remove(item)
        }
        index = i
        dragX = 0f
    }

    fun emptyBin() {
        val list = bin.toList()
        bin.clear()
        scope.launch {
            val failed = withContext(Dispatchers.IO) {
                list.count { item ->
                    val moved = Trash.move(db, item.file)
                    if (moved) db.decide(item.path, "bin")
                    !moved
                }
            }
            result = "binned ${list.size - failed}" + if (failed > 0) ", $failed couldn't be moved" else ""
            reviewing = false
            index = Int.MAX_VALUE / 2
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("back") }
            Text(
                "swipe ${root.name.ifEmpty { "storage" }}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        val list = queue
        when {
            list == null -> Text("looking through your files…", modifier = Modifier.padding(top = 24.dp))
            reviewing -> BinReview(
                bin = bin.toList(),
                onRescue = { item -> bin.remove(item) },
                onEmpty = { emptyBin() },
                onBack = { reviewing = false },
            )
            index >= list.size -> Finished(
                kept = kept,
                binned = bin.size,
                result = result,
                onReview = { reviewing = true },
                onEmpty = { emptyBin() },
                onClose = onClose,
            )
            else -> {
                val item = list[index]
                Text(
                    "${index + 1} of ${list.size} · kept $kept · bin pile ${bin.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SwipeCard(
                        item = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize()
                            .offset { IntOffset(dragX.roundToInt(), 0) }
                            .graphicsLayer { rotationZ = dragX / 40f }
                            .pointerInput(index) {
                                detectDragGestures(
                                    onDragEnd = {
                                        when {
                                            dragX > SWIPE_THRESHOLD -> decide(true)
                                            dragX < -SWIPE_THRESHOLD -> decide(false)
                                            else -> { dragX = 0f }
                                        }
                                    },
                                    onDragCancel = { dragX = 0f },
                                ) { change, drag ->
                                    change.consume()
                                    dragX += drag.x
                                }
                            },
                    )
                }
                Text(
                    "swipe right to keep, left to bin. Buttons work too.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { decide(false) }, modifier = Modifier.weight(1f)) { Text("bin") }
                    OutlinedButton(onClick = { undo() }, enabled = history.isNotEmpty()) { Text("undo") }
                    Button(onClick = { decide(true) }, modifier = Modifier.weight(1f)) { Text("keep") }
                }
            }
        }
    }
}

@Composable
private fun SwipeCard(item: FileItem, modifier: Modifier) {
    val context = LocalContext.current
    val preview by produceState<ImageBitmap?>(initialValue = null, item.path) {
        value = if (item.kind == FileKind.IMAGE) {
            withContext(Dispatchers.IO) { decodeSample(item.file, 900) }
        } else {
            null
        }
    }
    Surface(
        modifier = modifier.clip(RoundedCornerShape(28.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            preview?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } ?: Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(item.kind.label, style = MaterialTheme.typography.displaySmall)
            }
            Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(
                "${formatBytes(context, item.size)} · ${whenTaken(item.modified)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(item.file.parent.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

@Composable
private fun BinReview(bin: List<FileItem>, onRescue: (FileItem) -> Unit, onEmpty: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("bin pile · ${bin.size} files", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap a file to keep it after all. The rest go to the trash, where they stay for 30 days.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEmpty, enabled = bin.isNotEmpty()) { Text("move ${bin.size} to trash") }
            OutlinedButton(onClick = onBack) { Text("back") }
        }
        bin.forEach { item ->
            TextButton(onClick = { onRescue(item) }, modifier = Modifier.fillMaxWidth()) {
                Text("${item.name} · ${formatBytes(LocalContext.current, item.size)}", maxLines = 1)
            }
        }
    }
}

@Composable
private fun Finished(
    kept: Int,
    binned: Int,
    result: String?,
    onReview: () -> Unit,
    onEmpty: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("all caught up", style = MaterialTheme.typography.headlineSmall)
        Text("kept $kept · bin pile $binned", style = MaterialTheme.typography.bodyLarge)
        result?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (binned > 0) {
            Button(onClick = onReview) { Text("review bin pile") }
            OutlinedButton(onClick = onEmpty) { Text("move $binned to trash") }
        }
        TextButton(onClick = onClose) { Text("done") }
    }
}

/** Decodes a downscaled bitmap, so big photos don't blow memory. Null for anything that won't decode. */
fun decodeSample(file: File, maxSide: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
}.getOrNull()
