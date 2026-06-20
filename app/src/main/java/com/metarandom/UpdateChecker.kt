package com.metarandom

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val releaseUrl: String,
    val apkUrl: String?          // direct APK download URL, null if no asset attached
)

object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/travelermarco/metadata-randomizer/releases/latest"
    private const val FALLBACK_URL =
        "https://github.com/travelermarco/metadata-randomizer/releases/latest"

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout    = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            if (conn.responseCode == 404) return@withContext null
            if (conn.responseCode != 200) return@withContext null

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val obj    = JSONObject(body)
            val tag    = obj.getString("tag_name").trimStart('v')
            val html   = obj.optString("html_url", FALLBACK_URL)

            if (!isNewer(tag, currentVersion)) return@withContext null

            // Find the APK asset (first .apk in the assets array)
            val assets = obj.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name").endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url").ifEmpty { null }
                        break
                    }
                }
            }

            UpdateInfo(tag, html, apkUrl)
        } catch (_: Exception) {
            null
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }
}
