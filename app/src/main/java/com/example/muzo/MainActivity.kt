package com.example.muzo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MuzoSearchScreen(player = player)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

@Composable
fun MuzoSearchScreen(player: ExoPlayer) {
    var query by remember { mutableStateOf("Arijit Singh") }
    var songResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var currentlyPlaying by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                NewPipeExtractor.init()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Songs / Artists") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (query.isNotBlank()) {
                    isLoading = true
                    statusText = "Searching YouTube Music..."
                    scope.launch(Dispatchers.IO) {
                        try {
                            val response = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                            val items = response.getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                            withContext(Dispatchers.Main) {
                                songResults = items.take(4)
                                isLoading = false
                                statusText = if (songResults.isEmpty()) "No songs found." else "Found ${songResults.size} songs."
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                statusText = "Search error: ${e.localizedMessage ?: e.javaClass.simpleName}"
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Searching..." else "Search")
        }

        if (statusText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusText, style = MaterialTheme.typography.bodySmall)
        }

        if (currentlyPlaying != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Playing: $currentlyPlaying",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(songResults) { song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            statusText = "Resolving audio for: ${song.title}..."
                            scope.launch(Dispatchers.IO) {
                                try {
                                    var streamUrl: String? = null

                                    // Primary Strategy: NewPipe Extractor player (fully decrypts cipher & throttled params)
                                    val streamPairs = NewPipeExtractor.newPipePlayer(song.id)
                                    if (streamPairs.isNotEmpty()) {
                                        // Common audio itags: 140 (128k m4a), 251 (160k opus), 250 (70k opus), 249 (50k opus)
                                        val audioItags = listOf(140, 251, 250, 249)
                                        val audioMatch = streamPairs.firstOrNull { it.first in audioItags }
                                        streamUrl = audioMatch?.second ?: streamPairs.first().second
                                    }

                                    // Fallback Strategy: InnerTube Player API
                                    if (streamUrl.isNullOrBlank()) {
                                        val sigTimestamp = NewPipeExtractor.getSignatureTimestamp(song.id).getOrNull()
                                        val clients = listOf(
                                            YouTubeClient.ANDROID_VR_NO_AUTH,
                                            YouTubeClient.TVHTML5_SIMPLY,
                                            YouTubeClient.WEB_REMIX,
                                            YouTubeClient.WEB
                                        )

                                        for (client in clients) {
                                            val pRes = YouTube.player(
                                                videoId = song.id,
                                                client = client,
                                                signatureTimestamp = sigTimestamp
                                            ).getOrNull()

                                            val formats = (pRes?.streamingData?.adaptiveFormats.orEmpty() + pRes?.streamingData?.formats.orEmpty())
                                                .filter { it.isAudio }

                                            for (format in formats.sortedByDescending { it.bitrate ?: 0 }) {
                                                val url = format.url ?: NewPipeExtractor.getStreamUrl(format, song.id)
                                                if (!url.isNullOrBlank()) {
                                                    streamUrl = url
                                                    break
                                                }
                                            }
                                            if (!streamUrl.isNullOrBlank()) break
                                        }
                                    }

                                    withContext(Dispatchers.Main) {
                                        if (!streamUrl.isNullOrBlank()) {
                                            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                                            player.setMediaItem(mediaItem)
                                            player.prepare()
                                            player.play()
                                            currentlyPlaying = song.title
                                            statusText = ""
                                        } else {
                                            statusText = "Could not resolve audio stream. Cipher blocked."
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        statusText = "Stream error: ${e.localizedMessage ?: e.javaClass.simpleName}"
                                    }
                                }
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium)
                        val artistNames = song.artists.joinToString(", ") { it.name }
                        if (artistNames.isNotBlank()) {
                            Text(artistNames, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}