package io.github.teamomuito.octofiles.files

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

/** Which installed apps hold sensitive permissions right now. Read-only, nothing is revoked from here. */
object Privacy {
    data class AppAccess(val label: String, val packageName: String, val sensitive: List<String>)

    private val groups = linkedMapOf(
        "storage" to setOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
        ),
        "location" to setOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
        ),
        "camera" to setOf("android.permission.CAMERA"),
        "microphone" to setOf("android.permission.RECORD_AUDIO"),
        "contacts" to setOf("android.permission.READ_CONTACTS"),
    )

    /** Only user-installed apps with at least one sensitive grant, most grants first. */
    @Suppress("DEPRECATION")
    fun audit(context: Context): List<AppAccess> {
        val pm = context.packageManager
        return pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            .filter { isUserApp(it) }
            .map { pkg ->
                val granted = grantedPermissions(pkg)
                val sensitive = groups.filter { (_, perms) -> granted.any { it in perms } }.keys.toList()
                AppAccess(
                    label = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName,
                    packageName = pkg.packageName,
                    sensitive = sensitive,
                )
            }
            .filter { it.sensitive.isNotEmpty() }
            .sortedByDescending { it.sensitive.size }
    }

    private fun isUserApp(pkg: PackageInfo): Boolean {
        val flags = pkg.applicationInfo?.flags ?: return false
        return flags and ApplicationInfo.FLAG_SYSTEM == 0
    }

    private fun grantedPermissions(pkg: PackageInfo): Set<String> {
        val names = pkg.requestedPermissions ?: return emptySet()
        val flags = pkg.requestedPermissionsFlags ?: IntArray(0)
        return names.indices
            .filter { i -> i < flags.size && flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }
            .map { names[it] }
            .toSet()
    }
}
