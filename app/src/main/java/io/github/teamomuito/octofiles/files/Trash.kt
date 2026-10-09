package io.github.teamomuito.octofiles.files

import java.io.File

/**
 * Our own trash. Files move into a hidden folder on the same volume, so the move is a rename
 * and restoring is a rename back. Everything older than [KEEP_DAYS] is purged.
 */
object Trash {
    const val DIR_NAME = ".OctoFilesTrash"
    const val KEEP_DAYS = 30L

    fun dir(): File = File(Fs.storage, DIR_NAME).apply { mkdirs() }

    /** Returns false when the file can't be renamed into the trash (for example, it's on an SD card). */
    fun move(db: FilesDb, file: File): Boolean {
        val now = System.currentTimeMillis()
        val stored = Fs.uniqueIn(dir(), "${now}_${file.name}")
        if (!file.renameTo(stored)) return false
        db.trashAdd(file.absolutePath, stored.absolutePath, now)
        return true
    }

    fun restore(db: FilesDb, row: TrashRow): Boolean {
        val original = File(row.original)
        val parent = original.parentFile ?: return false
        parent.mkdirs()
        val target = Fs.uniqueIn(parent, original.name)
        if (!File(row.stored).renameTo(target)) return false
        db.trashRemove(row.id)
        return true
    }

    fun purge(db: FilesDb, row: TrashRow) {
        File(row.stored).deleteRecursively()
        db.trashRemove(row.id)
    }

    fun purgeExpired(db: FilesDb, now: Long = System.currentTimeMillis()): Int {
        val cutoff = now - KEEP_DAYS * DAY
        val expired = db.trashAll().filter { it.deletedAt < cutoff }
        expired.forEach { purge(db, it) }
        return expired.size
    }

    fun emptyAll(db: FilesDb) {
        db.trashAll().forEach { purge(db, it) }
    }
}
