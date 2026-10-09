package io.github.teamomuito.octofiles.files

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class BatchRenameTest {
    private val files = listOf(File("/x/IMG_001.jpg"), File("/x/IMG_002.jpg"), File("/x/notes"))

    @Test
    fun `empty find replaces the base name and keeps the extension`() {
        val names = BatchRename.plan(files, find = "", replace = "Holiday {n}", regex = false).map { it.newName }
        assertEquals(listOf("Holiday 1.jpg", "Holiday 2.jpg", "Holiday 3"), names)
    }

    @Test
    fun `literal find replaces every occurrence`() {
        val names = BatchRename.plan(files, find = "IMG", replace = "PHOTO", regex = false).map { it.newName }
        assertEquals(listOf("PHOTO_001.jpg", "PHOTO_002.jpg", "notes"), names)
    }

    @Test
    fun `regex groups and bad patterns`() {
        val names = BatchRename.plan(files.take(1), find = "IMG_(\\d+)", replace = "shot-$1", regex = true).map { it.newName }
        assertEquals(listOf("shot-001.jpg"), names)
        val broken = BatchRename.plan(files.take(1), find = "([", replace = "x", regex = true).map { it.newName }
        assertEquals(listOf("IMG_001.jpg"), broken)
    }
}
