package com.example.muzo.core

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
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
private var cachedSigTimestamp: Int? = null

fun warmUpStreamEngine() {
    try {
        NewPipeExtractor.init()
        if (cachedSigTimestamp == null) {
            cachedSigTimestamp = NewPipeExtractor.getSignatureTimestamp("dQw4w9WgXcQ").getOrNull()
        }
    } catch (_: Exception) {}
}

suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
    streamUrlCache[videoId]?.let { return@withContext it }

    // Strategy 1 (Ultra Fast ~250ms): Direct InnerTube JSON API with cached timestamp + JS deobfuscation
    try {
        val sigTimestamp = cachedSigTimestamp ?: NewPipeExtractor.getSignatureTimestamp(videoId).getOrNull()?.also {
            cachedSigTimestamp = it
        }

        val pRes = YouTube.player(
            videoId = videoId,
            client = YouTubeClient.WEB_REMIX,
            signatureTimestamp = sigTimestamp
        ).getOrNull()

        val formats = (pRes?.streamingData?.adaptiveFormats.orEmpty() + pRes?.streamingData?.formats.orEmpty())
            .filter { it.isAudio }

        for (format in formats.sortedByDescending { it.bitrate ?: 0 }) {
            val url = format.url ?: NewPipeExtractor.getStreamUrl(format, videoId)
            if (!url.isNullOrBlank()) {
                streamUrlCache[videoId] = url
                return@withContext url
            }
        }
    } catch (_: Exception) {}

    // Strategy 2: Fallback to full NewPipe player
    try {
        val streamPairs = NewPipeExtractor.newPipePlayer(videoId)
        if (streamPairs.isNotEmpty()) {
            val audioItags = listOf(140, 251, 250, 249)
            val audioMatch = streamPairs.firstOrNull { it.first in audioItags }
            val direct = audioMatch?.second ?: streamPairs.first().second
            if (direct.isNotBlank()) {
                streamUrlCache[videoId] = direct
                return@withContext direct
            }
        }
    } catch (_: Exception) {}

    null
}