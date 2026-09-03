package com.example.muzo

import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import com.example.muzo.data.HomeFeedViewModel
import com.example.muzo.data.local.MuziDatabase
import com.example.muzo.data.model.HomeShelf
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.playback.PlayerViewModel
import com.example.muzo.ui.components.FullPlayerSheet
import com.example.muzo.ui.components.PlayerWithBottomNav
import com.example.muzo.ui.screens.*
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Optimize for 120Hz+ displays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.let { win ->
                val display = win.decorView.display
                val peakMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                peakMode?.let {
                    val attrs = win.attributes
                    attrs.preferredDisplayModeId = it.modeId
                    win.attributes = attrs
                }
            }
        }

        player = ExoPlayer.Builder(this).build()

        setContent {
            val muzoDarkTheme = darkColorScheme(
                primary = Color(0xFF2F60FF),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF1E294B),
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
                    MuziMainScreen(player = player)
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
fun MuziMainScreen(player: ExoPlayer) {
    val context = LocalContext.current
    val database = remember { MuziDatabase.getInstance(context) }
    val historyDao = database.historyDao()

    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(historyDao, player)
    )
    val feedViewModel: HomeFeedViewModel = viewModel(
        factory = HomeFeedViewModel.Factory(historyDao)
    )

    val homeShelves by feedViewModel.homeShelves.collectAsStateWithLifecycle()
    val isFeedRefreshing by feedViewModel.isRefreshing.collectAsStateWithLifecycle()

    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val playbackQueue by playerViewModel.playbackQueue.collectAsStateWithLifecycle()
    val currentIndex by playerViewModel.currentIndex.collectAsStateWithLifecycle()
    val currentPosition by playerViewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by playerViewModel.duration.collectAsStateWithLifecycle()
    val statusText by playerViewModel.statusText.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // Navigation Sub-Screens
    var selectedPlaylist by remember { mutableStateOf<ShelfItem?>(null) }
    var playlistSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isPlaylistLoading by remember { mutableStateOf(false) }
    var selectedSeeAllShelf by remember { mutableStateOf<HomeShelf?>(null) }
    var isMoodAndGenresOpen by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("Top Trending Hindi") }
    var triggerSearch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                NewPipeExtractor.init()
            } catch (_: Exception) {}
        }
    }

    // Helper to open playlist and fetch songs
    fun openPlaylist(item: ShelfItem) {
        selectedPlaylist = item
        isPlaylistLoading = true
        scope.launch {
            val songs = withContext(Dispatchers.IO) {
                try {
                    val albumRes = YouTube.album(item.id).getOrNull()
                    if (albumRes != null && albumRes.songs.isNotEmpty()) {
                        albumRes.songs
                    } else {
                        val playlistRes = YouTube.playlist(item.id).getOrNull()
                        if (playlistRes != null && playlistRes.songs.isNotEmpty()) {
                            playlistRes.songs
                        } else {
                            YouTube.search("${item.title} songs", YouTube.SearchFilter.FILTER_SONG)
                                .getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                        }
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            playlistSongs = songs
            isPlaylistLoading = false
        }
    }

    // Back handlers hierarchy
    when {
        isPlayerExpanded -> {
            BackHandler { isPlayerExpanded = false }
        }
        selectedPlaylist != null -> {
            BackHandler { selectedPlaylist = null }
        }
        selectedSeeAllShelf != null -> {
            BackHandler { selectedSeeAllShelf = null }
        }
        isMoodAndGenresOpen -> {
            BackHandler { isMoodAndGenresOpen = false }
        }
        isSettingsOpen -> {
            BackHandler { isSettingsOpen = false }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentSong != null) 128.dp else 70.dp)
        ) {
            when {
                isSettingsOpen -> {
                    SettingsScreen(onBack = { isSettingsOpen = false })
                }
                isMoodAndGenresOpen -> {
                    MoodAndGenresScreen(
                        onBack = { isMoodAndGenresOpen = false },
                        onCategoryClick = { tag ->
                            isMoodAndGenresOpen = false
                            searchQuery = tag
                            triggerSearch = true
                            selectedTab = 1
                        }
                    )
                }
                selectedSeeAllShelf != null -> {
                    SeeAllGridScreen(
                        title = selectedSeeAllShelf?.title ?: "Albums & singles",
                        items = selectedSeeAllShelf?.items ?: emptyList(),
                        onBack = { selectedSeeAllShelf = null },
                        onItemClick = { item ->
                            if (item.type == ItemType.SONG) {
                                val song = SongItem(
                                    id = item.id,
                                    title = item.title,
                                    artists = listOf(com.music.innertube.models.Artist(name = item.subtitle, id = null)),
                                    album = null,
                                    duration = 0,
                                    thumbnail = item.imageUrls.firstOrNull() ?: ""
                                )
                                playerViewModel.playTrack(0, listOf(song))
                            } else {
                                openPlaylist(item)
                            }
                        }
                    )
                }
                selectedPlaylist != null -> {
                    val pItem = PlaylistItem(
                        id = selectedPlaylist!!.id,
                        title = selectedPlaylist!!.title,
                        author = com.music.innertube.models.Artist(name = selectedPlaylist!!.subtitle, id = null),
                        songCountText = "${playlistSongs.size} songs",
                        thumbnail = selectedPlaylist!!.imageUrls.firstOrNull() ?: "",
                        playEndpoint = null,
                        shuffleEndpoint = null,
                        radioEndpoint = null
                    )
                    // Related playlists from home feed
                    val related = homeShelves.firstOrNull { it.id == "trending_playlists" }?.items ?: emptyList()

                    PlaylistDetailScreen(
                        playlist = pItem,
                        songs = playlistSongs,
                        relatedPlaylists = related,
                        isLoading = isPlaylistLoading,
                        onBack = { selectedPlaylist = null },
                        onSearchClick = {
                            selectedPlaylist = null
                            selectedTab = 1
                        },
                        onSongSelect = { song, list ->
                            val idx = list.indexOf(song).coerceAtLeast(0)
                            playerViewModel.playTrack(idx, list)
                        },
                        onRelatedPlaylistClick = { relItem ->
                            openPlaylist(relItem)
                        }
                    )
                }
                else -> {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            homeShelves = homeShelves,
                            isRefreshing = isFeedRefreshing,
                            onRefresh = { feedViewModel.refreshFeed() },
                            onSongSelect = { song, list ->
                                val idx = list.indexOf(song).coerceAtLeast(0)
                                playerViewModel.playTrack(idx, list)
                            },
                            onPlaylistSelect = { item ->
                                openPlaylist(item)
                            },
                            onSeeAllClick = { shelf ->
                                if (shelf.id == "mood_and_genres") {
                                    isMoodAndGenresOpen = true
                                } else {
                                    selectedSeeAllShelf = shelf
                                }
                            },
                            onCategoryClick = { tag ->
                                if (tag == "Mood and Genres") {
                                    isMoodAndGenresOpen = true
                                } else {
                                    searchQuery = tag
                                    triggerSearch = true
                                    selectedTab = 1
                                }
                            },
                            onOpenSettings = { isSettingsOpen = true },
                            onOpenSearch = { selectedTab = 1 }
                        )
                        1 -> SearchScreen(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            triggerSearch = triggerSearch,
                            onSearchHandled = { triggerSearch = false },
                            onSongSelect = { song, list ->
                                val idx = list.indexOf(song).coerceAtLeast(0)
                                playerViewModel.playTrack(idx, list)
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
            }
        }

        // Floating Mini Player & Bottom Nav Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            PlayerWithBottomNav(
                currentSong = currentSong,
                isPlaying = isPlaying,
                onPlayPause = { playerViewModel.togglePlayPause() },
                onNext = { playerViewModel.playNext() },
                onPrevious = { playerViewModel.playPrevious() },
                onSongClick = { isPlayerExpanded = true },
                currentTab = selectedTab,
                onTabSelected = { tab ->
                    // Reset sub-screens when switching tab
                    selectedPlaylist = null
                    selectedSeeAllShelf = null
                    isMoodAndGenresOpen = false
                    selectedTab = tab
                }
            )
        }

        // Full Player Sheet (Expandable)
        AnimatedVisibility(
            visible = isPlayerExpanded && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullPlayerSheet(
                song = currentSong!!,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                hasPrev = currentIndex > 0,
                hasNext = currentIndex + 1 < playbackQueue.size,
                queueCount = playbackQueue.size,
                onClose = { isPlayerExpanded = false },
                onPlayPause = { playerViewModel.togglePlayPause() },
                onPrev = { playerViewModel.playPrevious() },
                onNext = { playerViewModel.playNext() },
                onSeek = { targetMs -> playerViewModel.seekTo(targetMs) }
            )
        }
    }
}
