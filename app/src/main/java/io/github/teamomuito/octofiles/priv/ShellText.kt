package io.github.teamomuito.octofiles.priv

/** One line of `du -sk` output: the size in bytes and the path it measured. */
data class DuLine(val bytes: Long, val path: String)

/** What a shell command said back: its exit code and everything it printed. */
data class ShellOutput(val code: Int, val text: String) {
    val ok: Boolean get() = code == 0
}

/** Pure parsing and quoting for the shell. No Android in here, so it can be unit tested. */
object ShellText {
    private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")

    /** Looks like an Android package name: dot-separated parts, nothing that could climb out of a folder. */
    fun isPackageName(name: String): Boolean = PACKAGE.matches(name)

    /** Wraps [text] in single quotes so the shell reads it as one word. */
    fun quote(text: String): String = "'" + text.replace("'", "'\\''") + "'"

    fun parseExec(raw: String): ShellOutput {
        val newline = raw.indexOf('\n')
        val head = if (newline < 0) raw else raw.substring(0, newline)
        val code = head.trim().toIntOrNull() ?: return ShellOutput(-1, raw)
        return ShellOutput(code, if (newline < 0) "" else raw.substring(newline + 1))
    }

    /** `pm list packages` prints lines like `package:com.example.app`. */
    fun parsePackages(text: String): Set<String> = text.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("package:") }
        .map { it.removePrefix("package:").substringBefore(' ').trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    /** `du -sk` prints lines like `1234<TAB>/storage/emulated/0/Android/data/com.example`. */
    fun parseDu(text: String): List<DuLine> = text.lineSequence().mapNotNull { line ->
        val tab = line.indexOf('\t')
        if (tab <= 0) return@mapNotNull null
        val kilobytes = line.substring(0, tab).trim().toLongOrNull() ?: return@mapNotNull null
        DuLine(kilobytes * 1024, line.substring(tab + 1).trimEnd())
    }.toList()
}
