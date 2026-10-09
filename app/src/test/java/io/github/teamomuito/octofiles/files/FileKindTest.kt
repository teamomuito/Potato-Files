package io.github.teamomuito.octofiles.files

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FileKindTest {
    @Test
    fun `kinds come from the extension, case insensitively`() {
        assertEquals(FileKind.IMAGE, kindOf("IMG_1.JPG", isDir = false))
        assertEquals(FileKind.VIDEO, kindOf("clip.mp4", isDir = false))
        assertEquals(FileKind.ARCHIVE, kindOf("backup.zip", isDir = false))
        assertEquals(FileKind.OTHER, kindOf("README", isDir = false))
    }

    @Test
    fun `folders are folders whatever they are called`() {
        assertEquals(FileKind.FOLDER, kindOf("photos.jpg", isDir = true))
    }

    @Test
    fun `uniqueIn adds a counter before the extension`() {
        val dir = kotlin.io.path.createTempDirectory("kind").toFile()
        File(dir, "notes.txt").writeText("a")
        assertEquals("notes (2).txt", Fs.uniqueIn(dir, "notes.txt").name)
        dir.deleteRecursively()
    }
}
