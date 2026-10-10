package io.github.teamomuito.octofiles.data

/** What a filter looks at: files, folders, or both. */
enum class Target { FILES, FOLDERS, BOTH }

/** One junk pattern. [matches] takes an entry's name and says whether it's junk. */
class SystemFilter(
    val id: String,
    val label: String,
    val body: String,
    val target: Target,
    val matches: (String) -> Boolean,
)

/** A name pattern the person wrote, like `*.bak`. It only ever matches files, never folders. */
data class CustomFilter(val pattern: String) {
    fun toFilter(): SystemFilter = SystemFilter(
        id = "custom:$pattern",
        label = "custom: $pattern",
        body = "a pattern you wrote",
        target = Target.FILES,
        matches = SystemFilters.glob(pattern),
    )

    companion object {
        /** Says why [pattern] can't be saved, or returns null when it can. */
        fun problem(pattern: String, existing: List<CustomFilter>): String? = when {
            pattern.isBlank() -> "type a pattern, like *.bak"
            pattern.any { it.isISOControl() } -> "no tabs or line breaks"
            '/' in pattern -> "patterns match file names, so no slashes"
            pattern.none { it != '*' && it != '?' } -> "add a letter or a dot, a pattern of only wildcards would match everything"
            existing.any { it.pattern == pattern } -> "that one's already there"
            else -> null
        }

        fun encodeAll(list: List<CustomFilter>): String = list.joinToString("\n") { it.pattern }

        /** Reads back what [encodeAll] wrote. Anything that wouldn't be accepted today is dropped. */
        fun decodeAll(text: String): List<CustomFilter> = text.lines()
            .map { it.trim() }
            .filter { problem(it, emptyList()) == null }
            .map(::CustomFilter)
    }
}

object SystemFilters {
    /** The built-in filters. Only ones that work without root, on shared storage. */
    val STOCK: List<SystemFilter> = listOf(
        SystemFilter(
            id = "mac",
            label = "mac leftovers",
            body = "files macs leave on storage: .DS_Store, ._ copies and __MACOSX folders.",
            target = Target.BOTH,
            matches = { it == ".DS_Store" || it.startsWith("._") || it == "__MACOSX" },
        ),
        SystemFilter(
            id = "windows",
            label = "windows leftovers",
            body = "thumbnail caches and desktop settings windows copies onto usb drives.",
            target = Target.FILES,
            matches = { it.equals("Thumbs.db", true) || it.equals("ehthumbs.db", true) || it.equals("desktop.ini", true) },
        ),
        SystemFilter(
            id = "lost",
            label = "lost folders",
            body = "folders a repair tool drops when it rescues files from a broken card or drive.",
            target = Target.FOLDERS,
            matches = { it.equals("LOST.DIR", true) || it.equals("FOUND.000", true) },
        ),
        SystemFilter(
            id = "trash",
            label = "trash folders",
            body = "trash some apps and file managers keep in hidden folders.",
            target = Target.BOTH,
            matches = {
                val lower = it.lowercase()
                lower in setOf(".trash", ".trashes", ".recycle", "\$recycle.bin") || lower.startsWith(".trashed-")
            },
        ),
        SystemFilter(
            id = "office",
            label = "office lock files",
            body = "~\$ files word and excel leave behind while a document is open, or after a crash.",
            target = Target.FILES,
            matches = { it.startsWith("~\$") },
        ),
    )

    /** The filters that apply right now: the built-in ones that are still on, plus the person's own patterns. */
    fun active(off: Set<String>, custom: List<CustomFilter>): List<SystemFilter> =
        STOCK.filter { it.id !in off } + custom.map { it.toFilter() }

    /** Turns a file name pattern into a matcher. `*` is any run of characters, `?` is one character. Case doesn't matter. */
    fun glob(pattern: String): (String) -> Boolean {
        val regex = Regex(
            pattern.map { c -> when (c) { '*' -> ".*"; '?' -> "."; else -> Regex.escape(c.toString()) } }.joinToString(""),
            RegexOption.IGNORE_CASE,
        )
        return { name -> regex.matches(name) }
    }
}

/** Walks a scanned tree and reports what the filters match. Documents is the person's own, so it's never walked. */
object SystemCleaner {

    fun find(root: DirNode, filters: List<SystemFilter>): List<JunkItem> {
        if (filters.isEmpty()) return emptyList()
        val out = ArrayList<JunkItem>()
        visit(root, filters, out)
        return out
    }

    private fun visit(dir: DirNode, filters: List<SystemFilter>, out: MutableList<JunkItem>) {
        for (d in dir.dirs) {
            if (d.relative.equals("Documents", ignoreCase = true)) continue
            val hit = filters.firstOrNull { it.target != Target.FILES && it.matches(d.name) }
            // a matched folder goes whole, so nothing inside it is reported on its own
            if (hit != null) {
                out += JunkItem(JunkKind.SYSTEM, d.path, d.name, dir.relative, d.bytes, d.fileCount, isDir = true, modified = 0, filter = hit.label)
            } else {
                visit(d, filters, out)
            }
        }
        for (f in dir.files) {
            val hit = filters.firstOrNull { it.target != Target.FOLDERS && it.matches(f.name) } ?: continue
            out += JunkItem(JunkKind.SYSTEM, f.path, f.name, dir.relative, f.size, 1, isDir = false, modified = f.modified, filter = hit.label)
        }
    }
}
