package io.github.teamomuito.octofiles.data

import android.os.Build
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Looks for a newer build on GitHub. It's one plain GET of the public releases API, with no
 * account and no token. Nothing about this phone is sent.
 */
object Updates {
    private const val LATEST = "https://api.github.com/repos/teamomuito/Potato-Files/releases/latest"

    /** [build] is the CI run number, the same number as the version code. [apkSize] is in bytes, 0 when unknown. */
    data class Release(val build: Int, val apkUrl: String?, val apkSize: Long, val notes: String, val page: String)

    /** The newest published build, or null if GitHub can't be reached or nothing is published yet. */
    fun latest(): Release? {
        val conn = URL(LATEST).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val build = buildNumber(json.getString("tag_name")) ?: return null
            val assets = json.optJSONArray("assets")?.let { list -> (0 until list.length()).map { list.getJSONObject(it) } }.orEmpty()
            // one apk per chip: pick the one for this phone, falling back to any apk
            val abi = Build.SUPPORTED_ABIS.firstOrNull { it == "arm64-v8a" || it == "armeabi-v7a" }
            val asset = assets.firstOrNull { abi != null && it.getString("name").endsWith("-$abi.apk") }
                ?: assets.firstOrNull { it.getString("name").endsWith(".apk") }
            Release(
                build = build,
                apkUrl = asset?.getString("browser_download_url"),
                apkSize = asset?.optLong("size", 0L) ?: 0L,
                notes = json.optString("body", "").trim(),
                page = json.getString("html_url"),
            )
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /** The build number from a release tag like "build-42". Null for any other tag. */
    fun buildNumber(tag: String): Int? = tag.removePrefix("build-").takeIf { tag.startsWith("build-") }?.toIntOrNull()
}
