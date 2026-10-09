package io.github.teamomuito.octofiles.files

import android.os.Environment
import android.os.StatFs
import java.io.File
import java.nio.file.Files

enum class SortBy(val label: String) { NAME("name"), DATE("date"), SIZE("size"), TYPE("type") }

/** Everything that touches the file system. Plain java.io.File access needs "all files access". */
object Fs {
    val storage: File get() = Environment.getExternalStorageDirectory()

    fun hasAllFilesAccess(): Boolean = Environment.isExternalStorageManager()

    /** Folders first, then files, both in [by] order. */
    fun list(dir: File, by: SortBy): List<FileItem> {
        val items = dir.listFiles().orEmpty().filter { !it.isHidden }.map { it.toItem() }
        return sorted(items.filter { it.isDir }, by) + sorted(items.filterNot { it.isDir }, by)
    }

    fun sorted(items: List<FileItem>, by: SortBy): List<FileItem> = when (by) {
        SortBy.NAME -> items.sortedBy { it.name.lowercase() }
        SortBy.DATE -> items.sortedByDescending { it.modified }
        SortBy.SIZE -> items.sortedByDescending { it.size }
        SortBy.TYPE -> items.sortedWith(compareBy<FileItem> { it.kind.label }.thenBy { it.name.lowercase() })
    }

    /** Visits everything under [root], skipping hidden entries and symlinks. Return false from [visit] to stop. */
    fun walk(root: File, visit: (FileItem) -> Boolean) {
        val pending = ArrayDeque<File>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val dir = pending.removeLast()
            for (child in dir.listFiles().orEmpty()) {
                if (child.isHidden || Files.isSymbolicLink(child.toPath())) continue
                if (child.isDirectory) pending.add(child)
                if (!visit(child.toItem())) return
            }
        }
    }

    fun search(root: File, query: String, limit: Int = 300): List<FileItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val found = ArrayList<FileItem>()
        walk(root) { item ->
            if (item.name.lowercase().contains(q)) found += item
            found.size < limit
        }
        return found
    }

    /** Files changed in the last [days], newest first. */
    fun recent(root: File, days: Int, limit: Int = 800): List<FileItem> {
        val since = System.currentTimeMillis() - days * DAY
        val found = ArrayList<FileItem>()
        walk(root) { item ->
            if (!item.isDir && item.modified >= since) found += item
            true
        }
        return found.sortedByDescending { it.modified }.take(limit)
    }

    /** Free and total bytes on shared storage. */
    fun space(): Pair<Long, Long> {
        val stat = StatFs(storage.path)
        return stat.availableBytes to stat.totalBytes
    }

    fun sizeOf(file: File): Long {
        if (!file.isDirectory) return file.length()
        var total = 0L
        walk(file) { item ->
            if (!item.isDir) total += item.size
            true
        }
        return total
    }

    /** A name in [dir] that doesn't exist yet: "photo.jpg" becomes "photo (2).jpg". */
    fun uniqueIn(dir: File, name: String): File {
        val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
        val base = name.substring(0, dot)
        val ext = name.substring(dot)
        var target = File(dir, name)
        var n = 2
        while (target.exists()) {
            target = File(dir, "$base ($n)$ext")
            n++
        }
        return target
    }

    fun rename(item: File, newName: String): File? {
        val clean = newName.trim()
        if (clean.isEmpty() || clean.contains('/') || clean == "." || clean == "..") return null
        val target = File(item.parentFile ?: return null, clean)
        if (target.exists()) return null
        return target.takeIf { item.renameTo(it) }
    }

    fun mkdir(parent: File, name: String): File? {
        val clean = name.trim()
        if (clean.isEmpty() || clean.contains('/')) return null
        return File(parent, clean).takeIf { !it.exists() && it.mkdirs() }
    }

    /** Moves into [dir] without overwriting. Falls back to copy and delete when renaming can't cross volumes. */
    fun moveInto(src: File, dir: File): File? {
        dir.mkdirs()
        val dst = uniqueIn(dir, src.name)
        if (src.renameTo(dst)) return dst
        val copied = runCatching { src.copyRecursively(dst, overwrite = false) }.getOrDefault(false)
        return if (copied && src.deleteRecursively()) dst else null
    }

    fun copyInto(src: File, dir: File): File? {
        dir.mkdirs()
        val dst = uniqueIn(dir, src.name)
        return dst.takeIf { runCatching { src.copyRecursively(it, overwrite = false) }.getOrDefault(false) }
    }
}
