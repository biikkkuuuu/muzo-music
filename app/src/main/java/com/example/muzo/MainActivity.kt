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
import com.example.muzo.core.getHighResThumbnail
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
import kotlinx.coroutines.async
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

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.muzo.core.warmUpStreamEngine()
        }

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
    var selectedCategoryTitle by remember { mutableStateOf<String?>(null) }
    var categoryPlaylists by remember { mutableStateOf<List<ShelfItem>>(emptyList()) }
    var isCategoryLoading by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var triggerSearch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                NewPipeExtractor.init()
            } catch (_: Exception) {}
        }
    }

    // Helper to open playlist/artist and fetch songs
    fun openPlaylist(item: ShelfItem) {
        selectedPlaylist = item
        isPlaylistLoading = true
        scope.launch {
            val songs = withContext(Dispatchers.IO) {
                try {
                    if (item.type == ItemType.ARTIST) {
                        // Parallel multi-query search to get 50-80+ comprehensive songs for the artist
                        val officialDeferred = async {
                            YouTube.artist(item.id).getOrNull()?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>().orEmpty()
                        }
                        val hitsDeferred = async {
                            YouTube.search("${item.title} best songs", YouTube.SearchFilter.FILTER_SONG)
                                .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                        }
                        val allHitsDeferred = async {
                            YouTube.search("${item.title} all hit songs", YouTube.SearchFilter.FILTER_SONG)
                                .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                        }
                        val latestDeferred = async {
                            YouTube.search("${item.title} latest songs", YouTube.SearchFilter.FILTER_SONG)
                                .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                        }

                        val combined = (officialDeferred.await() + hitsDeferred.await() + allHitsDeferred.await() + latestDeferred.await())
                            .distinctBy { it.id }

                        if (combined.isNotEmpty()) {
                            return@withContext combined
                        }
                    }

                    val albumRes = YouTube.album(item.id).getOrNull()
                    if (albumRes != null && albumRes.songs.isNotEmpty()) {
                        albumRes.songs
                    } else {
                        val playlistRes = YouTube.playlist(item.id).getOrNull()
                        if (playlistRes != null && playlistRes.songs.isNotEmpty()) {
                            playlistRes.songs
                        } else {
                            val artistRes = YouTube.artist(item.id).getOrNull()
                            val artistSongs = artistRes?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>()
                            if (!artistSongs.isNullOrEmpty()) {
                                artistSongs
                            } else {
                                YouTube.search("${item.title} songs", YouTube.SearchFilter.FILTER_SONG)
                                    .getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                            }
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

    // Helper to open mood/genre category and fetch its rich playlists
    fun openCategory(categoryName: String) {
        selectedCategoryTitle = categoryName
        isCategoryLoading = true
        scope.launch {
            val playlists = withContext(Dispatchers.IO) {
                try {
                    val q1 = async {
                        YouTube.search("$categoryName Hindi", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                            .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                    }
                    val q2 = async {
                        YouTube.search("$categoryName Bollywood", YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                            .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                    }
                    val q3 = async {
                        YouTube.search("$categoryName songs", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                            .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                    }
                    val q4 = async {
                        YouTube.search(categoryName, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                            .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                    }

                    val all = (q1.await() + q2.await() + q3.await() + q4.await()).distinctBy { it.id }
                    all.map { p ->
                        val thumb = p.thumbnail?.let { getHighResThumbnail(it) } ?: ""
                        ShelfItem(
                            id = p.id,
                            title = p.title,
                            subtitle = p.author?.name ?: "Playlist",
                            imageUrls = listOf(thumb),
                            type = ItemType.PLAYLIST
                        )
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            categoryPlaylists = playlists
            isCategoryLoading = false
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
        selectedCategoryTitle != null -> {
            BackHandler {
                selectedCategoryTitle = null
                categoryPlaylists = emptyList()
            }
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
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isSettingsOpen -> {
                    SettingsScreen(onBack = { isSettingsOpen = false })
                }
                selectedPlaylist != null -> {
                    val isArtist = selectedPlaylist!!.type == ItemType.ARTIST
                    val pItem = PlaylistItem(
                        id = selectedPlaylist!!.id,
                        title = selectedPlaylist!!.title,
                        author = com.music.innertube.models.Artist(name = if (isArtist) "Artist" else selectedPlaylist!!.subtitle, id = null),
                        songCountText = if (isArtist) "Artist • ${playlistSongs.size} top songs" else "${playlistSongs.size} songs",
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
                        onPlayAll = {
                            if (playlistSongs.isNotEmpty()) {
                                playerViewModel.playTrack(0, playlistSongs)
                            }
                        },
                        onRelatedPlaylistClick = { relItem ->
                            openPlaylist(relItem)
                        }
                    )
                }
                selectedCategoryTitle != null -> {
                    SeeAllGridScreen(
                        title = "$selectedCategoryTitle Playlists",
                        items = categoryPlaylists,
                        isLoading = isCategoryLoading,
                        onBack = {
                            selectedCategoryTitle = null
                            categoryPlaylists = emptyList()
                        },
                        onItemClick = { item ->
                            openPlaylist(item)
                        }
                    )
                }
                selectedSeeAllShelf != null -> {
                    SeeAllGridScreen(
                        title = selectedSeeAllShelf?.title ?: "Albums & singles",
                        items = selectedSeeAllShelf?.items ?: emptyList(),
                        isLoading = false,
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
                isMoodAndGenresOpen -> {
                    MoodAndGenresScreen(
                        onBack = { isMoodAndGenresOpen = false },
                        onCategoryClick = { tag ->
                            openCategory(tag)
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
                                    openCategory(tag)
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
                            onPlaylistSelect = { item ->
                                openPlaylist(item)
                            },
                            onCategoryClick = { category ->
                                openCategory(category)
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
