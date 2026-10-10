package io.github.teamomuito.octofiles.priv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellTextTest {

    @Test fun `quote keeps a single quote inside one shell word`() {
        assertEquals("'a'\\''b'", ShellText.quote("a'b"))
        assertEquals("'/storage/emulated/0/Android/data'", ShellText.quote("/storage/emulated/0/Android/data"))
    }

    @Test fun `exec output splits exit code from the text`() {
        val out = ShellText.parseExec("0\nhello\nworld\n")
        assertEquals(0, out.code)
        assertTrue(out.ok)
        assertEquals("hello\nworld\n", out.text)
    }

    @Test fun `a failing exit code is not ok`() {
        val out = ShellText.parseExec("1\nError: boom")
        assertEquals(1, out.code)
        assertFalse(out.ok)
    }

    @Test fun `garbage without an exit code counts as a failure`() {
        assertEquals(-1, ShellText.parseExec("no code here").code)
    }

    @Test fun `package list takes only the package names`() {
        val text = "package:com.example.app\npackage:com.other uid:10123\n\npackage:org.kept\n"
        assertEquals(setOf("com.example.app", "com.other", "org.kept"), ShellText.parsePackages(text))
    }

    @Test fun `du lines turn kilobytes into bytes and keep the path`() {
        val lines = ShellText.parseDu("12\t/storage/emulated/0/Android/data/com.a\nnot a line\n3\t/x y/z\n")
        assertEquals(listOf(DuLine(12 * 1024, "/storage/emulated/0/Android/data/com.a"), DuLine(3 * 1024, "/x y/z")), lines)
    }

    @Test fun `only dotted names count as package names`() {
        assertTrue(ShellText.isPackageName("com.example.app"))
        assertTrue(ShellText.isPackageName("org.example_1.app2"))
        assertFalse(ShellText.isPackageName("Downloads"))
        assertFalse(ShellText.isPackageName("com"))
        assertFalse(ShellText.isPackageName("../com.example"))
        assertFalse(ShellText.isPackageName("com.example/cache"))
        assertFalse(ShellText.isPackageName("a b.c"))
        assertFalse(ShellText.isPackageName("com..example"))
    }
}
