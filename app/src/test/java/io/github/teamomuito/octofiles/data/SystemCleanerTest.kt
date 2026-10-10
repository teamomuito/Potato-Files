package io.github.teamomuito.octofiles.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemCleanerTest {
    private val day = Expiry.DAY_MS
    private val now = 1000 * day

    private fun file(rel: String, size: Long = 10) = FileNode("/sd/$rel", rel.substringAfterLast('/'), size, now - day)

    private fun dir(rel: String, vararg children: Any) = DirNode(
        path = if (rel.isEmpty()) "/sd" else "/sd/$rel",
        name = rel.substringAfterLast('/'),
        relative = rel,
        dirs = children.filterIsInstance<DirNode>(),
        files = children.filterIsInstance<FileNode>(),
    )

    private val all = SystemFilters.active(off = emptySet(), custom = emptyList())

    private fun find(root: DirNode, filters: List<SystemFilter> = all) = SystemCleaner.find(root, filters)

    private fun stock(id: String) = SystemFilters.STOCK.single { it.id == id }

    @Test fun `mac and windows leftovers are found as files`() {
        val root = dir("", dir("Pictures", file("Pictures/.DS_Store", 4), file("Pictures/._photo.jpg", 2), file("Pictures/Thumbs.db", 8)))
        val items = find(root).map { it.name }.sorted()
        assertEquals(listOf(".DS_Store", "._photo.jpg", "Thumbs.db"), items)
    }

    @Test fun `a matched folder is reported once, whole, with its size`() {
        val root = dir("", dir("Stuff", dir("Stuff/__MACOSX", file("Stuff/__MACOSX/a", 100), file("Stuff/__MACOSX/b", 50))))
        val item = find(root).single()
        assertEquals(JunkKind.SYSTEM, item.kind)
        assertTrue(item.isDir)
        assertEquals("/sd/Stuff/__MACOSX", item.path)
        assertEquals(150L, item.bytes)
        assertEquals(2, item.files)
        assertEquals("mac leftovers", item.filter)
    }

    @Test fun `lost folders match folders only`() {
        val root = dir("", dir("LOST.DIR", file("LOST.DIR/x", 5)), file("lost.dir", 9))
        val items = find(root)
        assertEquals(listOf(true), items.map { it.isDir }.distinct())
        assertEquals("LOST.DIR", items.single().name)
    }

    @Test fun `office lock files are matched by their ~ prefix`() {
        val root = dir("", dir("Docs2", file("Docs2/~\$report.docx", 3)))
        assertEquals("~\$report.docx", find(root).single().name)
    }

    @Test fun `documents is never walked`() {
        val root = dir("", dir("Documents", dir("Documents/.Trash", file("Documents/.Trash/keep.pdf", 900))))
        assertTrue(find(root).isEmpty())
    }

    @Test fun `switched off stock filters are left out`() {
        val root = dir("", file("Thumbs.db", 1), file(".DS_Store", 1))
        val filters = SystemFilters.active(off = setOf("windows"), custom = emptyList())
        assertEquals(listOf(".DS_Store"), find(root, filters).map { it.name })
    }

    @Test fun `a custom glob matches file names case-insensitively and only files`() {
        val root = dir("", file("notes.BAK", 1), file("draft.txt", 1), dir("old.bak", file("old.bak/keep.jpg", 7)))
        val filters = SystemFilters.active(off = emptySet(), custom = listOf(CustomFilter("*.bak")))
        val items = find(root, filters)
        assertEquals(listOf("notes.BAK"), items.map { it.name })
        assertEquals("custom: *.bak", items.single().filter)
    }

    @Test fun `question mark matches exactly one character`() {
        val match = SystemFilters.glob("a?c")
        assertTrue(match("abc"))
        assertFalse(match("ac"))
        assertFalse(match("abbc"))
    }

    @Test fun `glob treats regex characters as plain text`() {
        assertTrue(SystemFilters.glob("file(1).txt")("FILE(1).TXT"))
        assertFalse(SystemFilters.glob("file(1).txt")("file1.txt"))
    }

    @Test fun `patterns that would match everything or can't be names are refused`() {
        assertNotNull(CustomFilter.problem("", emptyList()))
        assertNotNull(CustomFilter.problem("*", emptyList()))
        assertNotNull(CustomFilter.problem("??", emptyList()))
        assertNotNull(CustomFilter.problem("a/b", emptyList()))
        assertNotNull(CustomFilter.problem("a\tb", emptyList()))
        assertNotNull(CustomFilter.problem("*.bak", listOf(CustomFilter("*.bak"))))
        assertNull(CustomFilter.problem("*.bak", emptyList()))
    }

    @Test fun `saved patterns read back and bad lines are dropped`() {
        val saved = CustomFilter.encodeAll(listOf(CustomFilter("*.bak"), CustomFilter("tmp?")))
        assertEquals(listOf(CustomFilter("*.bak"), CustomFilter("tmp?")), CustomFilter.decodeAll(saved))
        assertEquals(listOf(CustomFilter("*.old")), CustomFilter.decodeAll("*\n  *.old  \na/b\n\n"))
    }

    @Test fun `stock filter ids are unique`() {
        assertEquals(SystemFilters.STOCK.size, SystemFilters.STOCK.map { it.id }.toSet().size)
        assertTrue(stock("trash").matches(".trashed-123"))
        assertTrue(stock("trash").matches("\$RECYCLE.BIN"))
    }
}
