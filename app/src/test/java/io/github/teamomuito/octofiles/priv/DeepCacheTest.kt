package io.github.teamomuito.octofiles.priv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepCacheTest {
    private val root = "/storage/emulated/0"

    @Test fun `only an app's own cache folder counts`() {
        val lines = listOf(
            DuLine(100, "$root/Android/data/com.a/cache"),
            DuLine(900, "$root/Android/data/com.b/files/cache"),
            DuLine(300, "$root/Android/data/not-a-package/cache"),
        )
        assertEquals(listOf(ExternalCache("com.a", "$root/Android/data/com.a/cache", 100)), DeepCache.parse(root, lines))
    }

    @Test fun `empty cache folders are dropped and the biggest come first`() {
        val lines = listOf(
            DuLine(0, "$root/Android/data/com.empty/cache"),
            DuLine(10, "$root/Android/data/com.small/cache"),
            DuLine(500, "$root/Android/data/com.big/cache"),
        )
        assertEquals(listOf("com.big", "com.small"), DeepCache.parse(root, lines).map { it.pkg })
    }

    @Test fun `paths are only built for real package names`() {
        assertEquals("$root/Android/data/com.a.b/cache", DeepCache.pathOf(root, "com.a.b"))
        assertNull(DeepCache.pathOf(root, "../com.a"))
    }
}
