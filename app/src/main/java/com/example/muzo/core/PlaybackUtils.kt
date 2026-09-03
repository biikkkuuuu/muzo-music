package com.example.muzo.core

import android.content.Context
import android.util.Log
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
    streamUrlCache[videoId]?.let { return@withContext it }

    val startTime = System.currentTimeMillis()

    // Strategy 1 (Ultra Fast ~250ms): Direct InnerTube JSON API with cached timestamp + JS deobfuscation
    try {
        val pRes = YouTube.player(
            videoId = videoId,
            client = YouTubeClient.WEB_REMIX,
            signatureTimestamp = cachedSigTimestamp
        ).getOrNull()

        val formats = (pRes?.streamingData?.adaptiveFormats.orEmpty() + pRes?.streamingData?.formats.orEmpty())
            .filter { it.isAudio }

        for (format in formats.sortedByDescending { it.bitrate ?: 0 }) {
            val url = format.url ?: NewPipeExtractor.getStreamUrl(format, videoId)
            if (!url.isNullOrBlank()) {
                streamUrlCache[videoId] = url
                Log.d("StreamEngine", "Resolved via WEB_REMIX in ${System.currentTimeMillis() - startTime}ms: $url")
                return@withContext url
            }
        }
    } catch (e: Exception) {
        Log.e("StreamEngine", "WEB_REMIX strategy failed: ${e.message}")
    }

    // Strategy 2: Fallback to full NewPipe player
    try {
        val streamPairs = NewPipeExtractor.newPipePlayer(videoId)
        if (streamPairs.isNotEmpty()) {
            val audioItags = listOf(140, 251, 250, 249)
            val audioMatch = streamPairs.firstOrNull { it.first in audioItags }
            val direct = audioMatch?.second ?: streamPairs.first().second
            if (direct.isNotBlank()) {
                streamUrlCache[videoId] = direct
                Log.d("StreamEngine", "Resolved via NewPipe fallback in ${System.currentTimeMillis() - startTime}ms")
                return@withContext direct
            }
        }
    } catch (e: Exception) {
        Log.e("StreamEngine", "NewPipe fallback failed: ${e.message}")
    }

    null
}