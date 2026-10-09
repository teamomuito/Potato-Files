package io.github.teamomuito.octofiles.data

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Looks for a newer release on GitHub. It's one plain GET of the public releases API, with no
 * account and no token. Nothing about this phone is sent.
 */
object Updates {
    private const val LATEST = "https://api.github.com/repos/teamomuito/Potato-Files/releases/latest"

    data class Release(val version: String, val apkUrl: String?, val page: String)

    /** The newest published release, or null if GitHub can't be reached or nothing is published yet. */
    fun latest(): Release? {
        val conn = URL(LATEST).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0))
                .map { assets!!.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith("-release.apk") }
                ?.getString("browser_download_url")
            Release(json.getString("tag_name").removePrefix("v"), apk, json.getString("html_url"))
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /** True when [remote] is a higher version than [local]. Compares dot-separated numbers, so 0.10.0 beats 0.9.0. */
    fun isNewer(remote: String, local: String): Boolean {
        val a = numbers(remote)
        val b = numbers(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun numbers(version: String): List<Int> =
        version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
}
