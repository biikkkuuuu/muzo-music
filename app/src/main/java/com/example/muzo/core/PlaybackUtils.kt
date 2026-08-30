package com.example.muzo.core

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
    try {
        val streamPairs = NewPipeExtractor.newPipePlayer(videoId)
        if (streamPairs.isNotEmpty()) {
            val audioItags = listOf(140, 251, 250, 249)
            val audioMatch = streamPairs.firstOrNull { it.first in audioItags }
            val direct = audioMatch?.second ?: streamPairs.first().second
            if (direct.isNotBlank()) return@withContext direct
        }
    } catch (_: Exception) {}

    try {
        val sigTimestamp = NewPipeExtractor.getSignatureTimestamp(videoId).getOrNull()
        val clients = listOf(
            YouTubeClient.ANDROID_VR_NO_AUTH,
            YouTubeClient.TVHTML5_SIMPLY,
            YouTubeClient.WEB_REMIX,
            YouTubeClient.WEB
        )
        for (client in clients) {
            val pRes = YouTube.player(
                videoId = videoId,
                client = client,
                signatureTimestamp = sigTimestamp
            ).getOrNull()

            val formats = (pRes?.streamingData?.adaptiveFormats.orEmpty() + pRes?.streamingData?.formats.orEmpty())
                .filter { it.isAudio }

            for (format in formats.sortedByDescending { it.bitrate ?: 0 }) {
                val url = format.url ?: NewPipeExtractor.getStreamUrl(format, videoId)
                if (!url.isNullOrBlank()) return@withContext url
            }
        }
    } catch (_: Exception) {}

    null
}