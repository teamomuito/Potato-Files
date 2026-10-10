package io.github.teamomuito.octofiles.priv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorpsesTest {
    private val root = "/storage/emulated/0"

    private fun line(kind: CorpseKind, name: String, kb: Long = 1) =
        DuLine(kb * 1024, "$root/Android/${kind.folder}/$name")

    @Test fun `a folder for an app that is not installed is a corpse`() {
        val sizes = mapOf(CorpseKind.DATA to listOf(line(CorpseKind.DATA, "com.gone", kb = 20)))
        val found = Corpses.find(root, sizes, installed = setOf("com.kept"))
        assertEquals(listOf(Corpse(CorpseKind.DATA, "com.gone", "$root/Android/data/com.gone", 20 * 1024)), found)
    }

    @Test fun `installed apps and kept-data apps are never corpses`() {
        val sizes = mapOf(CorpseKind.DATA to listOf(line(CorpseKind.DATA, "com.kept")))
        assertTrue(Corpses.find(root, sizes, installed = setOf("com.kept")).isEmpty())
    }

    @Test fun `folders that are not package names are left alone`() {
        val sizes = mapOf(
            CorpseKind.DATA to listOf(line(CorpseKind.DATA, "Photos"), line(CorpseKind.DATA, "com.gone/nested")),
        )
        assertTrue(Corpses.find(root, sizes, installed = emptySet()).isEmpty())
    }

    @Test fun `each kind keeps its own folder and the biggest come first`() {
        val sizes = mapOf(
            CorpseKind.DATA to listOf(line(CorpseKind.DATA, "com.small", kb = 5)),
            CorpseKind.OBB to listOf(line(CorpseKind.OBB, "com.game.big", kb = 900)),
            CorpseKind.MEDIA to listOf(line(CorpseKind.MEDIA, "org.chat", kb = 50)),
        )
        val found = Corpses.find(root, sizes, installed = emptySet())
        assertEquals(listOf(CorpseKind.OBB, CorpseKind.MEDIA, CorpseKind.DATA), found.map { it.kind })
        assertEquals("$root/Android/obb/com.game.big", found.first().path)
    }

    @Test fun `media folders are never ticked by default`() {
        assertTrue(CorpseKind.DATA.ticked)
        assertFalse(CorpseKind.OBB.ticked)
        assertFalse(CorpseKind.MEDIA.ticked)
    }

    @Test fun `paths are only built for real package names`() {
        assertEquals("$root/Android/data/com.a.b", Corpses.pathOf(root, CorpseKind.DATA, "com.a.b"))
        assertNull(Corpses.pathOf(root, CorpseKind.DATA, "../com.a"))
        assertNull(Corpses.pathOf(root, CorpseKind.OBB, "Documents"))
    }
}
