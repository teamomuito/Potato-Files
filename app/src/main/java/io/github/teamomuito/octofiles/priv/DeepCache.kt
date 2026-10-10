package io.github.teamomuito.octofiles.priv

import android.content.Context
import kotlinx.coroutines.CancellationException

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
        // directories only: a plain file that happens to be called cache is not an app's cache folder
        val folders = ShellText.quote("$root/Android/data")
        val out = PrivShell.exec(
            context,
            "for d in $folders/*/cache; do [ -d \"\$d\" ] && du -sk \"\$d\"; done 2>/dev/null",
        )
        return parse(root, ShellText.parseDu(out.text))
    }

    /** Removes the picked cache folders. Returns the bytes they held when they were scanned. */
    suspend fun clear(context: Context, picked: Collection<ExternalCache>, root: String): Long {
        var freed = 0L
        for (cache in picked) {
            if (pathOf(root, cache.pkg) != cache.path) continue
            // a dropped connection ends the run, but what already went still counts
            val ok = try {
                PrivShell.exec(context, "rm -rf ${ShellText.quote(cache.path)}").ok
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                break
            }
            if (ok) freed += cache.bytes
        }
        return freed
    }
}
