package io.github.teamomuito.octofiles.priv

import android.content.Context

/** An app's cache folder on shared storage, Android/data/<pkg>/cache. [label] is what the screen shows. */
data class ExternalCache(val pkg: String, val path: String, val bytes: Long, val label: String = pkg)

object DeepCache {

    fun pathOf(root: String, pkg: String): String? =
        if (ShellText.isPackageName(pkg)) "$root/Android/data/$pkg/cache" else null

    /** Only lines that are exactly some app's cache folder get through. Nested folders like files/cache are dropped. */
    fun parse(root: String, lines: List<DuLine>): List<ExternalCache> = lines
        .mapNotNull { line ->
            val pkg = line.path.substringBeforeLast('/').substringAfterLast('/')
            if (pathOf(root, pkg) != line.path || line.bytes <= 0) null else ExternalCache(pkg, line.path, line.bytes)
        }
        .sortedByDescending { it.bytes }

    suspend fun scan(context: Context, root: String): List<ExternalCache> {
        val out = PrivShell.exec(context, "du -sk ${ShellText.quote("$root/Android/data")}/*/cache 2>/dev/null")
        return parse(root, ShellText.parseDu(out.text))
    }

    /** Removes the picked cache folders. Returns the bytes they held when they were scanned. */
    suspend fun clear(context: Context, picked: Collection<ExternalCache>, root: String): Long {
        var freed = 0L
        for (cache in picked) {
            if (pathOf(root, cache.pkg) != cache.path) continue
            if (PrivShell.exec(context, "rm -rf ${ShellText.quote(cache.path)}").ok) freed += cache.bytes
        }
        return freed
    }
}
