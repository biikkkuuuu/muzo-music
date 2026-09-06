package com.example.muzo.core

import android.content.Context
import android.util.Log
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

fun getHighResThumbnail(url: String?): String {
    if (url.isNullOrBlank()) return ""
    return if (url.contains("=w") || url.contains("=s")) {
        url.replace(Regex("=w\\d+-h\\d+.*"), "=w600-h600-l90-rj")
           .replace(Regex("=s\\d+.*"), "=s600")
    } else {
        url
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

val streamUrlCache = ConcurrentHashMap<String, String>()

// Persistent signature timestamp so cold launch never blocks on network extraction
@Volatile
private var cachedSigTimestamp: Int = 20150

fun initStreamEngine(context: Context) {
    try {
        val prefs = context.getSharedPreferences("muzi_stream_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getInt("sig_timestamp", -1)
        if (saved > 0) {
            cachedSigTimestamp = saved
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                NewPipeExtractor.init()
                val fresh = NewPipeExtractor.getSignatureTimestamp("dQw4w9WgXcQ").getOrNull()
                if (fresh != null && fresh > 0) {
                    cachedSigTimestamp = fresh
                    prefs.edit().putInt("sig_timestamp", fresh).apply()
                }
            } catch (_: Exception) {}
        }
    } catch (_: Exception) {}
}

fun warmUpStreamEngine() {
    try {
        NewPipeExtractor.init()
    } catch (_: Exception) {}
}

/**
 * Extract the best audio URL from a player response.
 */
private fun extractAudioUrl(pRes: com.music.innertube.models.response.PlayerResponse?): String? {
    val formats = (pRes?.streamingData?.adaptiveFormats.orEmpty() + pRes?.streamingData?.formats.orEmpty())
        .filter { it.isAudio && !it.url.isNullOrBlank() }
    val bestAudio = formats.sortedWith(
        compareByDescending<com.music.innertube.models.response.PlayerResponse.StreamingData.Format> { it.itag == 140 }
            .thenByDescending { it.itag == 251 }
            .thenByDescending { it.bitrate ?: 0 }
    ).firstOrNull()
    return bestAudio?.url
}

suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
    streamUrlCache[videoId]?.let { return@withContext it }

    val startTime = System.currentTimeMillis()

    // Strategy 1: Race IPADOS and ANDROID_VR in parallel with a tight per-client timeout.
    // Whichever client returns a valid audio URL first wins. This eliminates the sequential
    // wait when one client is slow/failing — previously, a 10s IPADOS timeout would block
    // ANDROID_VR from even being attempted.
    val clientTimeout = 6000L  // 6 seconds max per client

    val ipadJob = async {
        withTimeoutOrNull(clientTimeout) {
            try {
                val pRes = YouTube.player(videoId = videoId, client = YouTubeClient.IPADOS).getOrNull()
                extractAudioUrl(pRes)
            } catch (e: Exception) {
                Log.w("StreamEngine", "IPADOS error: ${e.message}")
                null
            }
        }
    }

    val vrJob = async {
        withTimeoutOrNull(clientTimeout) {
            try {
                val pRes = YouTube.player(videoId = videoId, client = YouTubeClient.ANDROID_VR_1_65_10).getOrNull()
                extractAudioUrl(pRes)
            } catch (e: Exception) {
                Log.w("StreamEngine", "ANDROID_VR error: ${e.message}")
                null
            }
        }
    }

    // Use select to pick whichever completes first with a valid URL
    val fastResult = select<String?> {
        ipadJob.onAwait { url ->
            if (!url.isNullOrBlank()) {
                vrJob.cancel()
                Log.d("StreamEngine", "Resolved via iPadOS in ${System.currentTimeMillis() - startTime}ms")
                url
            } else {
                // iPadOS failed, wait for VR result
                vrJob.await()?.also { vrUrl ->
                    if (vrUrl.isNotBlank()) {
                        Log.d("StreamEngine", "Resolved via ANDROID_VR in ${System.currentTimeMillis() - startTime}ms")
                    }
                }
            }
        }
        vrJob.onAwait { url ->
            if (!url.isNullOrBlank()) {
                ipadJob.cancel()
                Log.d("StreamEngine", "Resolved via ANDROID_VR in ${System.currentTimeMillis() - startTime}ms")
                url
            } else {
                // VR failed, wait for iPadOS result
                ipadJob.await()?.also { ipadUrl ->
                    if (ipadUrl.isNotBlank()) {
                        Log.d("StreamEngine", "Resolved via iPadOS in ${System.currentTimeMillis() - startTime}ms")
                    }
                }
            }
        }
    }

    if (!fastResult.isNullOrBlank()) {
        streamUrlCache[videoId] = fastResult
        return@withContext fastResult
    }

    // Strategy 2 (Guaranteed Fallback): Full NewPipe player extraction with timeout
    val newPipeResult = withTimeoutOrNull(8000L) {
        try {
            val streamPairs = NewPipeExtractor.newPipePlayer(videoId)
            if (streamPairs.isNotEmpty()) {
                val audioItags = listOf(140, 251, 250, 249)
                val audioMatch = streamPairs.firstOrNull { it.first in audioItags }
                val direct = audioMatch?.second ?: streamPairs.first().second
                if (direct.isNotBlank()) direct else null
            } else null
        } catch (e: Exception) {
            Log.e("StreamEngine", "NewPipe fallback failed: ${e.message}")
            null
        }
    }

    if (!newPipeResult.isNullOrBlank()) {
        streamUrlCache[videoId] = newPipeResult
        Log.d("StreamEngine", "Resolved via NewPipe fallback in ${System.currentTimeMillis() - startTime}ms")
        return@withContext newPipeResult
    }

    null
}

fun prefetchSongStreams(songs: List<com.music.innertube.models.SongItem>, limit: Int = 3) {
    CoroutineScope(Dispatchers.IO).launch {
        songs.take(limit).forEach { song ->
            try {
                if (!streamUrlCache.containsKey(song.id)) {
                    resolveStreamUrl(song.id)
                }
            } catch (_: Exception) {}
        }
    }
}

val artworkBytesCache = android.util.LruCache<String, ByteArray>(25)

suspend fun loadArtworkBitmapBytes(url: String?): ByteArray? {
    if (url.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        val cached = artworkBytesCache.get(url)
        if (cached != null) return@withContext cached

        try {
            val highRes = getHighResThumbnail(url)
            val conn = java.net.URL(highRes).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            conn.inputStream.use { input ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                if (bitmap != null) {
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, stream)
                    val bytes = stream.toByteArray()
                    artworkBytesCache.put(url, bytes)
                    bytes
                } else null
            }
        } catch (e: Exception) {
            Log.w("PlaybackUtils", "Failed to load artwork bitmap bytes: ${e.message}")
            null
        }
    }
}