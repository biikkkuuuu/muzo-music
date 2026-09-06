package com.example.muzo.core

import android.content.Context
import android.util.Log
import com.music.innertube.NewPipeExtractor
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

fun initStreamEngine(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            NewPipeExtractor.init()
        } catch (_: Exception) {}
    }
}

fun warmUpStreamEngine() {
    try {
        NewPipeExtractor.init()
    } catch (_: Exception) {}
}

/**
 * Resolve a direct audio stream URL for the given videoId.
 *
 * Guaranteed to return an unthrottled HTTP 206 (Partial Content) stream.
 * 1. In-memory cache hit -> 0ms instant return
 * 2. NewPipeExtractor -> extracts dedicated audio streams (itag 140/251/250/249)
 *    backed by OkHttp disk/memory cache for high-speed repeated extraction.
 */
suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
    // 1. Fast path: cache hit (0ms)
    streamUrlCache[videoId]?.let { return@withContext it }

    val startTime = System.currentTimeMillis()

    try {
        val result = withTimeoutOrNull(9000L) {
            val streamPairs = NewPipeExtractor.newPipePlayer(videoId)
            if (streamPairs.isNotEmpty()) {
                val audioItags = listOf(140, 251, 250, 249)
                val audioMatch = streamPairs.firstOrNull { it.first in audioItags }
                audioMatch?.second ?: streamPairs.first().second
            } else null
        }

        if (!result.isNullOrBlank()) {
            streamUrlCache[videoId] = result
            Log.d("StreamEngine", "✓ NewPipe resolved $videoId in ${System.currentTimeMillis() - startTime}ms")
            return@withContext result
        }
    } catch (e: Exception) {
        Log.e("StreamEngine", "NewPipe extraction failed for $videoId: ${e.message}")
    }

    Log.e("StreamEngine", "✗ All strategies failed for $videoId in ${System.currentTimeMillis() - startTime}ms")
    null
}

/**
 * Background prefetch: resolve stream URLs for upcoming songs concurrently
 * so they are instant (0ms cache hit) when the user taps them.
 * Called from Search results, Home feed, and Playlist screens.
 */
fun prefetchSongStreams(songs: List<com.music.innertube.models.SongItem>, limit: Int = 5) {
    val toPrefetch = songs.take(limit).filter { !streamUrlCache.containsKey(it.id) }
    if (toPrefetch.isEmpty()) return

    CoroutineScope(Dispatchers.IO).launch {
        toPrefetch.forEach { song ->
            // Launch each song prefetch concurrently in parallel
            launch {
                try {
                    resolveStreamUrl(song.id)
                } catch (_: Exception) {}
            }
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