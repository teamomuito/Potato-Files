package io.github.teamomuito.octofiles.files

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class TrashRow(val id: Long, val original: String, val stored: String, val deletedAt: Long)
data class Rule(val id: Long, val match: String, val dest: String)
data class VaultRow(val id: String, val name: String, val size: Long, val addedAt: Long)

/**
 * Tags, notes, trash index, swipe decisions, auto-sort rules and vault index.
 * Tags and notes live here, so the files themselves are never modified.
 */
class FilesDb private constructor(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE tags (path TEXT NOT NULL, tag TEXT NOT NULL, PRIMARY KEY (path, tag))")
        db.execSQL("CREATE TABLE notes (path TEXT PRIMARY KEY, body TEXT NOT NULL)")
        db.execSQL("CREATE TABLE trash (id INTEGER PRIMARY KEY AUTOINCREMENT, original TEXT NOT NULL, stored TEXT NOT NULL, deletedAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE swipes (path TEXT PRIMARY KEY, decision TEXT NOT NULL, at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE rules (id INTEGER PRIMARY KEY AUTOINCREMENT, match TEXT NOT NULL, dest TEXT NOT NULL)")
        db.execSQL("CREATE TABLE vault (id TEXT PRIMARY KEY, name TEXT NOT NULL, size INTEGER NOT NULL, addedAt INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // tags and notes

    fun tagsOf(path: String): List<String> =
        strings("SELECT tag FROM tags WHERE path = ? ORDER BY tag", listOf(path))

    fun addTag(path: String, tag: String) {
        val clean = tag.trim().lowercase()
        if (clean.isEmpty()) return
        val values = ContentValues().apply {
            put("path", path)
            put("tag", clean)
        }
        writableDatabase.insertWithOnConflict("tags", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun removeTag(path: String, tag: String) {
        writableDatabase.delete("tags", "path = ? AND tag = ?", arrayOf(path, tag))
    }

    fun noteOf(path: String): String =
        strings("SELECT body FROM notes WHERE path = ?", listOf(path)).firstOrNull().orEmpty()

    fun setNote(path: String, body: String) {
        if (body.isBlank()) {
            writableDatabase.delete("notes", "path = ?", arrayOf(path))
        } else {
            val values = ContentValues().apply {
                put("path", path)
                put("body", body.trim())
            }
            writableDatabase.insertWithOnConflict("notes", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    /** Paths that have a tag, for the tag filter. */
    fun pathsWithTag(tag: String): List<String> =
        strings("SELECT path FROM tags WHERE tag = ?", listOf(tag.lowercase()))

    // trash index

    fun trashAdd(original: String, stored: String, at: Long) {
        val values = ContentValues().apply {
            put("original", original)
            put("stored", stored)
            put("deletedAt", at)
        }
        writableDatabase.insert("trash", null, values)
    }

    fun trashAll(): List<TrashRow> = readableDatabase.rawQuery(
        "SELECT id, original, stored, deletedAt FROM trash ORDER BY deletedAt DESC", null,
    ).use { c ->
        buildList {
            while (c.moveToNext()) add(TrashRow(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)))
        }
    }

    fun trashRemove(id: Long) {
        writableDatabase.delete("trash", "id = ?", arrayOf(id.toString()))
    }

    // swipe decisions

    fun decide(path: String, decision: String) {
        val values = ContentValues().apply {
            put("path", path)
            put("decision", decision)
            put("at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("swipes", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun undecide(path: String) {
        writableDatabase.delete("swipes", "path = ?", arrayOf(path))
    }

    fun decidedPaths(): Set<String> = strings("SELECT path FROM swipes", emptyList()).toSet()

    // auto-sort rules

    fun rules(): List<Rule> = readableDatabase.rawQuery("SELECT id, match, dest FROM rules ORDER BY id", null).use { c ->
        buildList {
            while (c.moveToNext()) add(Rule(c.getLong(0), c.getString(1), c.getString(2)))
        }
    }

    fun addRule(match: String, dest: String) {
        val values = ContentValues().apply {
            put("match", match.trim().lowercase())
            put("dest", dest.trim().trim('/'))
        }
        writableDatabase.insert("rules", null, values)
    }

    fun removeRule(id: Long) {
        writableDatabase.delete("rules", "id = ?", arrayOf(id.toString()))
    }

    // vault index

    fun vaultAll(): List<VaultRow> = readableDatabase.rawQuery(
        "SELECT id, name, size, addedAt FROM vault ORDER BY addedAt DESC", null,
    ).use { c ->
        buildList {
            while (c.moveToNext()) add(VaultRow(c.getString(0), c.getString(1), c.getLong(2), c.getLong(3)))
        }
    }

    fun vaultAdd(row: VaultRow) {
        val values = ContentValues().apply {
            put("id", row.id)
            put("name", row.name)
            put("size", row.size)
            put("addedAt", row.addedAt)
        }
        writableDatabase.insert("vault", null, values)
    }

    fun vaultRemove(id: String) {
        writableDatabase.delete("vault", "id = ?", arrayOf(id))
    }

    private fun strings(sql: String, args: List<String>): List<String> =
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            buildList {
                while (c.moveToNext()) add(c.getString(0))
            }
        }

    companion object {
        private const val NAME = "files.db"
        private const val VERSION = 1

        @Volatile
        private var instance: FilesDb? = null

        fun get(context: Context): FilesDb = instance ?: synchronized(this) {
            instance ?: FilesDb(context.applicationContext).also { instance = it }
        }
    }
}
