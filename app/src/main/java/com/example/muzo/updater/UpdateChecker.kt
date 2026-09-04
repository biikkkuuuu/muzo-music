package com.example.muzo.updater

import android.util.Log
import com.example.muzo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val updateUrl: String,
    val description: String? = null
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"

    // Default update redirect website. Can be updated when user provides custom domain.
    const val DEFAULT_UPDATE_WEBSITE_URL = "https://github.com/biikkkuuuu/muzo-music/releases/latest"

    // GitHub Latest Release API for version checking
    private const val GITHUB_RELEASE_API = "https://api.github.com/repos/biikkkuuuu/muzo-music/releases/latest"

    /**
     * Checks if a newer version is available.
     * Returns UpdateInfo if available, null otherwise.
     */
    suspend fun checkUpdate(websiteUrl: String = DEFAULT_UPDATE_WEBSITE_URL): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASE_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MuziMusic-App")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").trim()
                val htmlUrl = json.optString("html_url", websiteUrl)
                val body = if (json.has("body") && !json.isNull("body")) json.getString("body") else null

                // Clean tag name, e.g. "v1.0.1" -> "1.0.1"
                val remoteVersion = tagName.removePrefix("v").trim()
                val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

                if (isNewerVersion(remoteVersion, currentVersion)) {
                    Log.d(TAG, "Newer version available: $remoteVersion (Current: $currentVersion)")
                    return@withContext UpdateInfo(
                        versionName = remoteVersion,
                        updateUrl = websiteUrl.ifEmpty { htmlUrl },
                        description = body
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check update: ${e.message}")
        }
        null
    }

    /**
     * Compares semver versions like "1.0.1" vs "1.0.0".
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isEmpty()) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
