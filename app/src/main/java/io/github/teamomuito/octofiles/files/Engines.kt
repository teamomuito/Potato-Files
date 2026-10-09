package io.github.teamomuito.octofiles.files

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Overwrites files with random bytes before deleting them. */
object SecureDelete {
    /**
     * One random pass, then delete. Flash storage with wear leveling can keep old copies elsewhere,
     * so this is a strong deterrent rather than a guarantee.
     */
    fun wipe(target: File): Boolean = runCatching {
        val files = if (target.isDirectory) {
            val out = ArrayList<File>()
            Fs.walk(target) { item ->
                if (!item.isDir) out += item.file
                true
            }
            out
        } else {
            listOf(target)
        }
        files.forEach(::overwrite)
        target.deleteRecursively()
    }.getOrDefault(false)

    private fun overwrite(file: File) {
        val length = file.length()
        val random = SecureRandom()
        val buffer = ByteArray(64 * 1024)
        RandomAccessFile(file, "rw").use { raf ->
            var position = 0L
            while (position < length) {
                random.nextBytes(buffer)
                val n = minOf(buffer.size.toLong(), length - position).toInt()
                raf.seek(position)
                raf.write(buffer, 0, n)
                position += n
            }
            raf.fd.sync()
        }
    }
}

/** Storage breakdowns, large and old files, and duplicate detection by content hash. */
object Analyzer {
    data class Folder(val file: File, val size: Long)

    private const val PARTIAL_BYTES = 64 * 1024

    /** Immediate children of [dir], biggest first. */
    fun folderSizes(dir: File): List<Folder> = dir.listFiles().orEmpty()
        .filter { !it.isHidden }
        .map { Folder(it, Fs.sizeOf(it)) }
        .sortedByDescending { it.size }

    fun large(root: File, minBytes: Long = 100L shl 20, limit: Int = 200): List<FileItem> {
        val found = ArrayList<FileItem>()
        Fs.walk(root) { item ->
            if (!item.isDir && item.size >= minBytes) found += item
            true
        }
        return found.sortedByDescending { it.size }.take(limit)
    }

    fun untouched(root: File, days: Long = 90, limit: Int = 200): List<FileItem> {
        val cutoff = System.currentTimeMillis() - days * DAY
        val found = ArrayList<FileItem>()
        Fs.walk(root) { item ->
            if (!item.isDir && item.size > 0 && item.modified < cutoff) found += item
            true
        }
        return found.sortedByDescending { it.size }.take(limit)
    }

    /** Groups of identical files. Sizes narrow it down, then a partial hash, then a full hash. */
    fun duplicates(root: File, minBytes: Long = 1L shl 20): List<List<FileItem>> {
        val bySize = HashMap<Long, MutableList<FileItem>>()
        Fs.walk(root) { item ->
            if (!item.isDir && item.size >= minBytes) bySize.getOrPut(item.size) { mutableListOf() }.add(item)
            true
        }
        val groups = mutableListOf<List<FileItem>>()
        for (sameSize in bySize.values.filter { it.size > 1 }) {
            val byHead = sameSize.groupBy { digest(it.file, PARTIAL_BYTES) }
            for (sameHead in byHead.values.filter { it.size > 1 }) {
                sameHead.groupBy { digest(it.file, null) }
                    .values
                    .filter { it.size > 1 }
                    .forEach { groups += it }
            }
        }
        return groups.sortedByDescending { it.first().size * (it.size - 1) }
    }

    private fun digest(file: File, limit: Int?): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var left = limit ?: Int.MAX_VALUE
            while (left > 0) {
                val n = input.read(buffer, 0, minOf(buffer.size, left))
                if (n < 0) break
                md.update(buffer, 0, n)
                left -= n
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

/** Batch rename with a preview. `{n}` in the replacement becomes a running number, starting at 1. */
object BatchRename {
    data class Preview(val file: File, val newName: String) {
        val changed: Boolean get() = newName != file.name
    }

    /**
     * With an empty [find], the replacement is the new base name and the extension is kept.
     * With [regex] off, [find] is a literal string.
     */
    fun plan(files: List<File>, find: String, replace: String, regex: Boolean): List<Preview> {
        val pattern = if (regex && find.isNotEmpty()) runCatching { Regex(find) }.getOrNull() else null
        return files.mapIndexed { i, file ->
            val number = (i + 1).toString()
            val replacement = replace.replace("{n}", number)
            val newName = when {
                find.isEmpty() -> {
                    val dot = file.name.lastIndexOf('.').takeIf { it > 0 }
                    replacement + (dot?.let { file.name.substring(it) } ?: "")
                }
                regex -> pattern?.replace(file.name, replacement) ?: file.name
                else -> file.name.replace(find, replacement)
            }
            Preview(file, newName)
        }
    }

    fun apply(previews: List<Preview>): Int = previews
        .filter { it.changed }
        .count { Fs.rename(it.file, it.newName) != null }
}

/** Zip create and extract. Password-protected zips aren't supported. */
object Archive {
    fun zip(sources: List<File>, dest: File): Boolean = runCatching {
        ZipOutputStream(dest.outputStream().buffered()).use { zos ->
            sources.forEach { addEntry(zos, it, it.name) }
        }
        true
    }.getOrDefault(false)

    private fun addEntry(zos: ZipOutputStream, file: File, name: String) {
        if (file.isDirectory) {
            zos.putNextEntry(ZipEntry("$name/"))
            zos.closeEntry()
            file.listFiles().orEmpty().filter { !it.isHidden }.forEach { addEntry(zos, it, "$name/${it.name}") }
        } else {
            zos.putNextEntry(ZipEntry(name))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    /** Extracts into a new folder beside the archive. Entries that would escape that folder are skipped. */
    fun unzip(archive: File): File? {
        val parent = archive.parentFile ?: return null
        return runCatching {
            val root = Fs.uniqueIn(parent, archive.nameWithoutExtension).apply { mkdirs() }.canonicalFile
            ZipInputStream(archive.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val target = File(root, entry.name).canonicalFile
                    if (target.path.startsWith(root.path + File.separator)) {
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zis.copyTo(it) }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            root
        }.getOrNull()
    }
}

/** Reads files for the in-app viewer without loading big files whole. */
object Preview {
    const val TEXT_LIMIT = 256 * 1024

    fun looksLikeText(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val buffer = ByteArray(512)
            val n = input.read(buffer)
            n <= 0 || (0 until n).none { buffer[it].toInt() == 0 }
        }
    }.getOrDefault(false)

    fun text(file: File): String = runCatching {
        file.inputStream().use { input ->
            val buffer = ByteArray(TEXT_LIMIT)
            val n = input.read(buffer)
            if (n <= 0) "" else String(buffer, 0, n, Charsets.UTF_8)
        }
    }.getOrDefault("")

    /** Offset, hex and ASCII columns, 16 bytes per line. */
    fun hex(file: File, offset: Long, length: Int = 4096): String = runCatching {
        java.io.RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            val buffer = ByteArray(length)
            val n = raf.read(buffer).coerceAtLeast(0)
            buildString {
                var i = 0
                while (i < n) {
                    val end = minOf(i + 16, n)
                    val hex = (i until end).joinToString(" ") { "%02x".format(buffer[it].toInt() and 0xff) }
                    val ascii = (i until end).map { idx ->
                        val b = buffer[idx].toInt() and 0xff
                        if (b in 32..126) b.toChar() else '.'
                    }.joinToString("")
                    appendLine("%08x  %-47s  %s".format(offset + i, hex, ascii))
                    i = end
                }
            }
        }
    }.getOrDefault("")
}

/** Moves files whose kind or extension matches a rule into a folder under shared storage. */
object AutoSort {
    fun matches(rule: Rule, item: FileItem): Boolean =
        item.kind.label == rule.match || item.file.extension.equals(rule.match, ignoreCase = true)

    /** Only looks at the top level of [from]. Returns how many files moved. */
    fun run(db: FilesDb, from: java.io.File): Int {
        val rules = db.rules()
        if (rules.isEmpty()) return 0
        var moved = 0
        from.listFiles().orEmpty().filter { !it.isDirectory && !it.isHidden }.forEach { file ->
            val item = file.toItem()
            val rule = rules.firstOrNull { matches(it, item) } ?: return@forEach
            if (Fs.moveInto(file, File(Fs.storage, rule.dest)) != null) moved++
        }
        return moved
    }
}
