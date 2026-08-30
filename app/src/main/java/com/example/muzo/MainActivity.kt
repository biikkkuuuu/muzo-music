package com.example.muzo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.muzo.core.resolveStreamUrl
import com.example.muzo.ui.components.FullPlayerSheet
import com.example.muzo.ui.components.PlayerWithBottomNav
import com.example.muzo.ui.screens.HomeScreen
import com.example.muzo.ui.screens.LibraryScreen
import com.example.muzo.ui.screens.SearchScreen
import com.example.muzo.ui.screens.SettingsScreen
import com.music.innertube.NewPipeExtractor
import com.music.innertube.models.SongItem
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
            val muzoDarkTheme = darkColorScheme(
                primary = Color(0xFF6B8AFD),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF283560),
                onPrimaryContainer = Color(0xFFD6E2FF),
                surface = Color(0xFF0F0E13),
                surfaceContainer = Color(0xFF16151C),
                surfaceContainerHigh = Color(0xFF1E1D26),
                surfaceContainerHighest = Color(0xFF282732),
                background = Color(0xFF08080A),
                onBackground = Color(0xFFEEEEF2),
                onSurface = Color(0xFFEEEEF2),
                onSurfaceVariant = Color(0xFF9292A2)
            )

            MaterialTheme(colorScheme = muzoDarkTheme) {
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

@Composable
fun MuzoApp(player: ExoPlayer) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var currentSong by remember { mutableStateOf<SongItem?>(null) }
    var playbackQueue by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var recentHistory by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("Top Trending Hindi") }
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

        if (recentHistory.none { it.id == song.id }) {
            recentHistory = (listOf(song) + recentHistory).take(12)
        }

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

    if (isSettingsOpen) {
        BackHandler { isSettingsOpen = false }
        SettingsScreen(onBack = { isSettingsOpen = false })
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (currentSong != null) 128.dp else 70.dp)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        recentHistory = recentHistory,
                        onSongSelect = { song, list ->
                            val idx = list.indexOf(song).coerceAtLeast(0)
                            playTrack(idx, list)
                        },
                        onCategoryClick = { tag ->
                            searchQuery = tag
                            triggerSearch = true
                            selectedTab = 1
                        },
                        onOpenSettings = { isSettingsOpen = true }
                    )
                    1 -> SearchScreen(
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
                    2 -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Voice Search / Feature Coming Soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    3 -> LibraryScreen()
                    4 -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("More Options", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                PlayerWithBottomNav(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onPlayPause = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    onNext = {
                        if (currentIndex + 1 < playbackQueue.size) playTrack(currentIndex + 1, playbackQueue)
                    },
                    onPrevious = {
                        if (currentIndex > 0) playTrack(currentIndex - 1, playbackQueue)
                    },
                    onSongClick = { isPlayerExpanded = true },
                    currentTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            AnimatedVisibility(
                visible = isPlayerExpanded && currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BackHandler { isPlayerExpanded = false }
                FullPlayerSheet(
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