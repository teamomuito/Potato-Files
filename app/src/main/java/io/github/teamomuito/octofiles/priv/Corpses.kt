package io.github.teamomuito.octofiles.priv

import android.content.Context
import kotlinx.coroutines.CancellationException

/** The folders apps leave under Android/. [ticked] says whether a fresh scan starts with it selected. */
enum class CorpseKind(val folder: String, val label: String, val body: String, val ticked: Boolean) {
    DATA(
        "data", "app data",
        "what a removed app kept in its own folder. it can go, unless you plan to reinstall it and want its saves back.",
        ticked = true,
    ),
    OBB(
        "obb", "game files",
        "big expansion files from games you removed. you can download them again if you ever want the game back.",
        ticked = false,
    ),
    MEDIA(
        "media", "app media",
        "photos, voice notes and other media some apps keep here. it may be yours, so nothing is ticked for you.",
        ticked = false,
    ),
}

/** A folder left behind by an app that isn't installed any more. [path] is the folder itself. */
data class Corpse(val kind: CorpseKind, val pkg: String, val path: String, val bytes: Long)

object Corpses {

    /** The folder for [pkg] under [kind], or null when [pkg] isn't a package name we'd trust with a delete. */
    fun pathOf(root: String, kind: CorpseKind, pkg: String): String? =
        if (ShellText.isPackageName(pkg)) "$root/Android/${kind.folder}/$pkg" else null

    /**
     * Folders named after a package that isn't installed. Any other name, like a folder a person made,
     * is never a corpse. [sizes] holds the du lines under each kind's folder.
     */
    fun find(root: String, sizes: Map<CorpseKind, List<DuLine>>, installed: Set<String>): List<Corpse> =
        sizes.flatMap { (kind, lines) ->
            lines.mapNotNull { line ->
                val pkg = line.path.substringAfterLast('/')
                if (pathOf(root, kind, pkg) != line.path || pkg in installed) return@mapNotNull null
                Corpse(kind, pkg, line.path, line.bytes)
            }
        }.sortedByDescending { it.bytes }

    /** Looks in Android/data, obb and media. The installed list is read first, so an empty answer can't make everything a corpse. */
    suspend fun scan(context: Context, root: String): List<Corpse> {
        val installed = PrivShell.installed(context)
        val sizes = CorpseKind.entries.associateWith { kind ->
            val folder = ShellText.quote("$root/Android/${kind.folder}")
            ShellText.parseDu(PrivShell.exec(context, "du -sk $folder/* 2>/dev/null").text)
        }
        return find(root, sizes, installed)
    }

    /**
     * Deletes the picked folders. The installed list is read again first, so an app installed since the
     * scan is skipped, and only the folders the scan showed are touched. Returns the bytes the scan reported for what went.
     */
    suspend fun remove(context: Context, root: String, picked: Collection<Corpse>): Long {
        val installed = PrivShell.installed(context)
        var freed = 0L
        for (corpse in picked) {
            if (corpse.pkg in installed || pathOf(root, corpse.kind, corpse.pkg) != corpse.path) continue
            // a dropped connection ends the run, but what already went still counts
            val ok = try {
                PrivShell.exec(context, "rm -rf ${ShellText.quote(corpse.path)}").ok
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                break
            }
            if (ok) freed += corpse.bytes
        }
        return freed
    }
}
