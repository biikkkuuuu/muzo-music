package com.example.muzo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MuzoApp(player = player)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

// -------------------------------------------------------------
// Playback Stream Resolver (Verified Working Logic)
// -------------------------------------------------------------
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

// -------------------------------------------------------------
// Main App Scaffold & Navigation
// -------------------------------------------------------------
@Composable
fun MuzoApp(player: ExoPlayer) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSong by remember { mutableStateOf<SongItem?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                NewPipeExtractor.init()
            } catch (_: Exception) {}
        }
    }

    val playSong: (SongItem) -> Unit = { song ->
        currentSong = song
        statusText = "Resolving audio for ${song.title}..."
        scope.launch {
            val streamUrl = resolveStreamUrl(song.id)
            if (!streamUrl.isNullOrBlank()) {
                val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                isPlaying = true
                statusText = ""
            } else {
                statusText = "Unable to load stream for ${song.title}"
            }
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                if (currentSong != null) {
                    ArchiveTuneMiniPlayer(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        onPlayPause = {
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.play()
                                isPlaying = true
                            }
                        }
                    )
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    onSongSelect = playSong,
                    onQuickPickQuery = { query ->
                        selectedTab = 1
                    }
                )
                1 -> SearchScreen(onSongSelect = playSong, statusText = statusText)
                2 -> LibraryScreen()
            }
        }
    }
}

// -------------------------------------------------------------
// Home Screen (ArchiveTune Style with Real Recommendations)
// -------------------------------------------------------------
@Composable
fun HomeScreen(
    onSongSelect: (SongItem) -> Unit,
    onQuickPickQuery: (String) -> Unit
) {
    var trendingSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isHomeLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            isHomeLoading = true
            try {
                val res = YouTube.search("Trending Hindi Songs", YouTube.SearchFilter.FILTER_SONG)
                val items = res.getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                withContext(Dispatchers.Main) {
                    trendingSongs = items.take(6)
                    isHomeLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { isHomeLoading = false }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Discover",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Arijit Singh", "Bollywood", "Chill", "Workout", "Focus")) { tag ->
                    SuggestionChip(
                        onClick = { onQuickPickQuery(tag) },
                        label = { Text(tag) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "Quick Picks & Trending",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isHomeLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    trendingSongs.forEach { song ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSongSelect(song) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("♫", style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val artistNames = song.artists.joinToString(", ") { it.name }
                                    if (artistNames.isNotBlank()) {
                                        Text(
                                            text = artistNames,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Search Screen (Connected to YouTube Music)
// -------------------------------------------------------------
@Composable
fun SearchScreen(onSongSelect: (SongItem) -> Unit, statusText: String) {
    var query by remember { mutableStateOf("Arijit Singh") }
    var searchResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Songs, Artists, Albums") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (query.isNotBlank()) {
                    isLoading = true
                    searchMsg = "Searching YouTube Music..."
                    scope.launch(Dispatchers.IO) {
                        try {
                            val response = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                            val items = response.getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                            withContext(Dispatchers.Main) {
                                searchResults = items.take(15)
                                isLoading = false
                                searchMsg = if (searchResults.isEmpty()) "No songs found." else ""
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                searchMsg = "Search error: ${e.localizedMessage ?: e.javaClass.simpleName}"
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Searching..." else "Search")
        }

        if (statusText.isNotBlank() || searchMsg.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (statusText.isNotBlank()) statusText else searchMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchResults) { song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongSelect(song) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♪", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val artists = song.artists.joinToString(", ") { it.name }
                            if (artists.isNotBlank()) {
                                Text(
                                    text = artists,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Library Screen Placeholder
// -------------------------------------------------------------
@Composable
fun LibraryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LibraryMusic,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Playlists, liked songs, and downloaded tracks will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// ArchiveTune-Inspired Mini-Player
// -------------------------------------------------------------
@Composable
fun ArchiveTuneMiniPlayer(
    song: SongItem,
    isPlaying: Boolean,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("♫")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                val artistNames = song.artists.joinToString(", ") { it.name }
                if (artistNames.isNotBlank()) {
                    Text(
                        text = artistNames,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}