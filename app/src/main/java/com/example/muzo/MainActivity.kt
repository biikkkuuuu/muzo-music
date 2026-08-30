package com.example.muzo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        setContent {
            val echoDarkTheme = darkColorScheme(
                primary = Color(0xFF6B8AFD),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF283560),
                onPrimaryContainer = Color(0xFFD6E2FF),
                surface = Color(0xFF0E0E11),
                surfaceContainer = Color(0xFF14141A),
                surfaceContainerHigh = Color(0xFF1C1C24),
                surfaceContainerHighest = Color(0xFF262632),
                background = Color(0xFF08080A),
                onBackground = Color(0xFFE6E6EB),
                onSurface = Color(0xFFE6E6EB),
                onSurfaceVariant = Color(0xFF9090A0)
            )

            MaterialTheme(colorScheme = echoDarkTheme) {
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
// Playback Stream Resolver (Strictly Untouched)
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

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// -------------------------------------------------------------
// Main Application Navigation & Queue
// -------------------------------------------------------------
@Composable
fun MuzoApp(player: ExoPlayer) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSong by remember { mutableStateOf<SongItem?>(null) }
    var playbackQueue by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("Arijit Singh") }
    var triggerSearch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    fun playTrack(index: Int, queue: List<SongItem>) {
        if (index !in queue.indices) return
        playbackQueue = queue
        currentIndex = index
        val song = queue[index]
        currentSong = song
        statusText = "Loading ${song.title}..."

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
                statusText = "Unable to load stream"
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    if (currentIndex + 1 < playbackQueue.size) {
                        playTrack(currentIndex + 1, playbackQueue)
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            delay(400)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                NewPipeExtractor.init()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                if (currentSong != null && !isPlayerExpanded) {
                    EchoFloatingMiniPlayer(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        hasNext = currentIndex + 1 < playbackQueue.size,
                        onClick = { isPlayerExpanded = true },
                        onPlayPause = {
                            if (player.isPlaying) player.pause() else player.play()
                        },
                        onNext = {
                            if (currentIndex + 1 < playbackQueue.size) {
                                playTrack(currentIndex + 1, playbackQueue)
                            }
                        }
                    )
                }

                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
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
                0 -> EchoHomeScreen(
                    onSongSelect = { song, list ->
                        val idx = list.indexOf(song).coerceAtLeast(0)
                        playTrack(idx, list)
                    },
                    onCategoryClick = { tag ->
                        searchQuery = tag
                        triggerSearch = true
                        selectedTab = 1
                    }
                )
                1 -> EchoSearchScreen(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    triggerSearch = triggerSearch,
                    onSearchHandled = { triggerSearch = false },
                    onSongSelect = { song, list ->
                        val idx = list.indexOf(song).coerceAtLeast(0)
                        playTrack(idx, list)
                    },
                    statusText = statusText
                )
                2 -> EchoLibraryScreen()
            }

            AnimatedVisibility(
                visible = isPlayerExpanded && currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BackHandler { isPlayerExpanded = false }
                EchoFullPlayerSheet(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    hasPrev = currentIndex > 0,
                    hasNext = currentIndex + 1 < playbackQueue.size,
                    queueCount = playbackQueue.size,
                    onClose = { isPlayerExpanded = false },
                    onPlayPause = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    onPrev = {
                        if (currentIndex > 0) playTrack(currentIndex - 1, playbackQueue)
                    },
                    onNext = {
                        if (currentIndex + 1 < playbackQueue.size) playTrack(currentIndex + 1, playbackQueue)
                    },
                    onSeek = { targetMs ->
                        player.seekTo(targetMs)
                        currentPosition = targetMs
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. ECHO HOME SCREEN (Exact 1:1 Layout)
// -------------------------------------------------------------
@Composable
fun EchoHomeScreen(
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    var selectedTag by remember { mutableStateOf<String?>("Romance") }
    var bigHits by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var nostalgicSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var trendingPlaylists by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun loadCategoryContent(category: String) {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val q1 = YouTube.search("$category Hits", YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                val q2 = YouTube.search("90s Bollywood $category", YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                val q3 = YouTube.search("Top Trending $category Songs", YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()

                withContext(Dispatchers.Main) {
                    bigHits = q1.take(8)
                    nostalgicSongs = q2.take(8)
                    trendingPlaylists = q3.take(8)
                    isLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(selectedTag) {
        loadCategoryContent(selectedTag ?: "Bollywood")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tags = listOf("Romance", "Relax", "Feel good", "Party", "Energize", "Chill")
                items(tags) { tag ->
                    val isSelected = selectedTag == tag
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTag = if (isSelected) null else tag
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = { Text(tag, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            }
        }

        item {
            EchoShelfSection(
                subtitle = "MUSIC THAT'S HOT AND HAPPENING!",
                title = "India's biggest hits",
                songs = bigHits,
                isLoading = isLoading,
                onSongClick = { song -> onSongSelect(song, bigHits) }
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mood and Genres",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val moods = listOf(
                    "Chill" to "Focus",
                    "Commute" to "Gaming",
                    "Energize" to "Party",
                    "Feel good" to "Romance"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    moods.forEach { (m1, m2) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EchoMoodTile(title = m1, modifier = Modifier.weight(1f), onClick = { onCategoryClick(m1) })
                            EchoMoodTile(title = m2, modifier = Modifier.weight(1f), onClick = { onCategoryClick(m2) })
                        }
                    }
                }
            }
        }

        item {
            EchoShelfSection(
                subtitle = "FROM THE WEIRD TO THE WONDERFUL",
                title = "Trending community playlists",
                songs = trendingPlaylists,
                isLoading = isLoading,
                onSongClick = { song -> onSongSelect(song, trendingPlaylists) }
            )
        }

        item {
            EchoShelfSection(
                subtitle = "RELIVE THE MAGIC OF THE 90S",
                title = "90s Throwback Fun",
                songs = nostalgicSongs,
                isLoading = isLoading,
                onSongClick = { song -> onSongSelect(song, nostalgicSongs) }
            )
        }
    }
}

// -------------------------------------------------------------
// 2. ECHO SEARCH SCREEN (Exact 1:1 Layout)
// -------------------------------------------------------------
@Composable
fun EchoSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    triggerSearch: Boolean,
    onSearchHandled: () -> Unit,
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    statusText: String
) {
    var searchResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var selectedSearchTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var searchMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val performSearch: (String) -> Unit = { q ->
        if (q.isNotBlank()) {
            isLoading = true
            searchMsg = "Searching YouTube Music..."
            scope.launch(Dispatchers.IO) {
                try {
                    val response = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                    val items = response.getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        searchResults = items.take(30)
                        isLoading = false
                        searchMsg = if (searchResults.isEmpty()) "No songs found" else ""
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        searchMsg = "Search error: ${e.localizedMessage ?: e.javaClass.simpleName}"
                    }
                }
            }
        }
    }

    LaunchedEffect(triggerSearch) {
        if (triggerSearch && query.isNotBlank()) {
            performSearch(query)
            onSearchHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search YouTube Music...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { performSearch(query) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedSearchTab,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(selected = selectedSearchTab == 0, onClick = { selectedSearchTab = 0 }, text = { Text("Explore", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedSearchTab == 1, onClick = { selectedSearchTab = 1 }, text = { Text("Muzo Chart", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedSearchTab == 2, onClick = { selectedSearchTab = 2 }, text = { Text("Album", fontWeight = FontWeight.Bold) })
        }

        if (statusText.isNotBlank() || searchMsg.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (statusText.isNotBlank()) statusText else searchMsg,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { song ->
                    EchoSongTile(song = song, onClick = { onSongSelect(song, searchResults) })
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Moods & moments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val moments = listOf(
                    "Chill" to "Commute",
                    "Energize" to "Feel good",
                    "Focus" to "Gaming",
                    "Party" to "Romance",
                    "Sad" to "Sleep",
                    "Workout" to "Romance"
                )
                items(moments) { (m1, m2) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EchoMoodTile(title = m1, modifier = Modifier.weight(1f), onClick = {
                            onQueryChange(m1)
                            performSearch(m1)
                        })
                        EchoMoodTile(title = m2, modifier = Modifier.weight(1f), onClick = {
                            onQueryChange(m2)
                            performSearch(m2)
                        })
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. ECHO LIBRARY SCREEN (Exact 1:1 Layout)
// -------------------------------------------------------------
@Composable
fun EchoLibraryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Playlists", "Songs", "Albums", "Artists")) { chip ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(chip, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(18.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date added", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val actions = listOf(
            "❤️" to "Liked",
            "✓" to "Downloaded",
            "↓" to "Exported",
            "🔄" to "Cached",
            "📈" to "My top 50",
            "📁" to "Local"
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in actions.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EchoLibraryTile(iconEmoji = actions[i].first, title = actions[i].second, modifier = Modifier.weight(1f))
                    if (i + 1 < actions.size) {
                        EchoLibraryTile(iconEmoji = actions[i + 1].first, title = actions[i + 1].second, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------
// REUSABLE ECHO COMPONENTS
// -------------------------------------------------------------
@Composable
fun EchoShelfSection(
    subtitle: String,
    title: String,
    songs: List<SongItem>,
    isLoading: Boolean,
    onSongClick: (SongItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(songs) { song ->
                    EchoSquareCard(song = song, onClick = { onSongClick(song) })
                }
            }
        }
    }
}

@Composable
fun EchoSquareCard(song: SongItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.thumbnail,
            contentDescription = song.title,
            modifier = Modifier
                .size(148.dp)
                .clip(RoundedCornerShape(16.dp))
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artists.joinToString(", ") { it.name },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EchoMoodTile(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun EchoLibraryTile(iconEmoji: String, title: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(54.dp)
            .clickable {},
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(iconEmoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun EchoSongTile(song: SongItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = song.title,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Floating Mini Player & Full Player Sheet
// -------------------------------------------------------------
@Composable
fun EchoFloatingMiniPlayer(
    song: SongItem,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    hasNext: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 10.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.thumbnail,
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artists.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (hasNext) {
                    IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next")
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun EchoFullPlayerSheet(
    song: SongItem,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    hasPrev: Boolean,
    hasNext: Boolean,
    queueCount: Int,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val currentProgress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    LaunchedEffect(currentProgress) {
        if (!isDragging) sliderPosition = currentProgress
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = "PLAYING FROM QUEUE ($queueCount)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // High-Res Artwork
            AsyncImage(
                model = song.thumbnail,
                contentDescription = song.title,
                modifier = Modifier
                    .size(310.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(16.dp),
                contentScale = ContentScale.Crop
            )

            // Title & Artists
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Seekbar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (isDragging) sliderPosition else currentProgress,
                    onValueChange = {
                        isDragging = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onSeek((sliderPosition * duration).toLong())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(if (isDragging) (sliderPosition * duration).toLong() else currentPosition),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    enabled = hasPrev,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Prev",
                        modifier = Modifier.size(36.dp),
                        tint = if (hasPrev) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = hasNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp),
                        tint = if (hasNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}