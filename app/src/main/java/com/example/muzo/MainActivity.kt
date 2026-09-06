package com.example.muzo

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.muzo.theme.DynamicSongTheme
import com.example.muzo.ui.components.ActionMenuTarget
import com.example.muzo.ui.components.FullPlayerSheet
import com.example.muzo.ui.components.PlayerWithBottomNav
import com.example.muzo.ui.components.SongActionBottomSheet
import com.example.muzo.ui.screens.*
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.ArtistItem
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

        // Enable edge-to-edge transparent system bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Disable artificial system bar scrims/contrast enforcement for true transparent edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        // Make window extend into display cutout area (no letterbox / black bars on display notches)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

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

        // Request notification permission for Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Shared background ExoPlayer managed by MuziMediaSessionService
        player = com.example.muzo.playback.MuziMediaSessionService.getPlayer(this)
        com.example.muzo.playback.MuziMediaSessionService.start(this)

        // Initialize stream engine immediately with persistent timestamp cache
        com.example.muzo.core.initStreamEngine(this)

        setContent {
            val muzoDarkTheme = darkColorScheme(
                primary = Color(0xFFE2E4EB),
                onPrimary = Color(0xFF121216),
                primaryContainer = Color(0xFF262530),
                onPrimaryContainer = Color(0xFFEAEAF0),
                surface = Color(0xFF0F0E13),
                surfaceContainer = Color(0xFF14131A),
                surfaceContainerHigh = Color(0xFF1B1A22),
                surfaceContainerHighest = Color(0xFF24232E),
                background = Color(0xFF08080A),
                onBackground = Color(0xFFEEEEF2),
                onSurface = Color(0xFFEEEEF2),
                onSurfaceVariant = Color(0xFF9EA3B0),
                outlineVariant = Color(0x2AFFFFFF)
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
        // Player release is managed by MuziMediaSessionService so background audio continues
    }
}

data class ArtistBundle(
    val songs: List<SongItem>,
    val playlists: List<ShelfItem>,
    val similarArtists: List<ShelfItem>,
    val subscribers: String?,
    val monthlyListeners: String?,
    val description: String? = null,
    val heroThumbnail: String? = null
)

@Composable
fun MuziMainScreen(player: ExoPlayer) {
    val context = LocalContext.current
    val database = remember { MuziDatabase.getInstance(context) }
    val historyDao = database.historyDao()
    val likedSongDao = database.likedSongDao()
    val userPlaylistDao = database.userPlaylistDao()
    val downloadedSongDao = database.downloadedSongDao()

    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(context.applicationContext, historyDao, likedSongDao, player)
    )
    val feedViewModel: HomeFeedViewModel = viewModel(
        factory = HomeFeedViewModel.Factory(historyDao)
    )
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(likedSongDao, historyDao, userPlaylistDao, downloadedSongDao)
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
    val isCurrentSongLiked by playerViewModel.isCurrentSongLiked.collectAsStateWithLifecycle()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isShuffleActive by playerViewModel.isShuffleActive.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatMode.collectAsStateWithLifecycle()
    val crossfadeSeconds by playerViewModel.crossfadeSeconds.collectAsStateWithLifecycle()
    val isGaplessEnabled by playerViewModel.isGaplessEnabled.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isSettingsSheetOpen by remember { mutableStateOf(false) }
    var isAboutDialogOpen by remember { mutableStateOf(false) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isEqualizerOpen by remember { mutableStateOf(false) }
    var actionMenuTarget by remember { mutableStateOf<ActionMenuTarget?>(null) }

    val prefs = remember { context.getSharedPreferences("muzi_app_prefs", Context.MODE_PRIVATE) }
    var showWelcomeDialog by rememberSaveable {
        mutableStateOf(!prefs.getBoolean("has_shown_welcome_dialog", false))
    }
    var availableUpdate by remember { mutableStateOf<com.example.muzo.updater.UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        val update = com.example.muzo.updater.UpdateChecker.checkUpdate()
        if (update != null) {
            availableUpdate = update
        }
    }

    val librarySubScreen by libraryViewModel.activeSubScreen.collectAsStateWithLifecycle()
    BackHandler(enabled = librarySubScreen != null && selectedTab == 2) {
        libraryViewModel.setSubScreen(null)
    }

    // Navigation Sub-Screens
    var selectedPlaylist by remember { mutableStateOf<ShelfItem?>(null) }
    var playlistSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isPlaylistLoading by remember { mutableStateOf(false) }
    var artistPlaylists by remember { mutableStateOf<List<ShelfItem>>(emptyList()) }
    var similarArtists by remember { mutableStateOf<List<ShelfItem>>(emptyList()) }
    var artistSubscribers by remember { mutableStateOf<String?>(null) }
    var artistMonthlyListeners by remember { mutableStateOf<String?>(null) }
    var artistDescription by remember { mutableStateOf<String?>(null) }
    var artistHeroThumbnail by remember { mutableStateOf<String?>(null) }
    val playlistBackStack = remember { mutableStateListOf<ShelfItem>() }

    // Instant In-Memory Caches for 0ms Reload
    val playlistCache = remember { mutableMapOf<String, List<SongItem>>() }
    val artistBundleCache = remember { mutableMapOf<String, ArtistBundle>() }
    val categoryCache = remember { mutableMapOf<String, List<ShelfItem>>() }

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

    // Helper to open playlist/artist and fetch songs, playlists, and similar artists
    fun openPlaylist(item: ShelfItem, addToBackStack: Boolean = true) {
        if (addToBackStack && selectedPlaylist != null && selectedPlaylist?.id != item.id) {
            playlistBackStack.add(selectedPlaylist!!)
        }
        selectedPlaylist = item

        if (item.type == ItemType.ARTIST) {
            val cachedBundle = artistBundleCache[item.id]
            if (cachedBundle != null && cachedBundle.songs.isNotEmpty()) {
                playlistSongs = cachedBundle.songs
                artistPlaylists = cachedBundle.playlists
                similarArtists = cachedBundle.similarArtists
                artistSubscribers = cachedBundle.subscribers
                artistMonthlyListeners = cachedBundle.monthlyListeners
                artistDescription = cachedBundle.description
                artistHeroThumbnail = cachedBundle.heroThumbnail
                isPlaylistLoading = false
                return
            }

            isPlaylistLoading = true
            artistPlaylists = emptyList()
            similarArtists = emptyList()
            artistSubscribers = null
            artistMonthlyListeners = null
            artistDescription = null
            artistHeroThumbnail = null

            scope.launch {
                val bundle = withContext(Dispatchers.IO) {
                    try {
                        // 1. Direct artist browse: returns top songs, albums, and similar artists in ONE call
                        val artistPageRes = YouTube.artist(item.id).getOrNull()
                        val subsText = artistPageRes?.subscriberCountText ?: "Artist"
                        val monthlyText = artistPageRes?.monthlyListenerCount ?: ""
                        val artistBio = artistPageRes?.description
                        val heroThumb = artistPageRes?.artist?.thumbnail

                        val fromPageSongs = artistPageRes?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>().orEmpty()
                        val fromPageArtists = artistPageRes?.sections?.flatMap { it.items }?.filterIsInstance<com.music.innertube.models.ArtistItem>().orEmpty()
                        val fromPagePlaylists = artistPageRes?.sections?.flatMap { it.items }?.mapNotNull { ytItem ->
                            when (ytItem) {
                                is PlaylistItem -> ShelfItem(
                                    id = ytItem.id,
                                    title = ytItem.title,
                                    subtitle = ytItem.author?.name ?: "Playlist",
                                    imageUrls = listOf(ytItem.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                    type = ItemType.PLAYLIST
                                )
                                is com.music.innertube.models.AlbumItem -> ShelfItem(
                                    id = ytItem.id,
                                    title = ytItem.title,
                                    subtitle = ytItem.year?.toString() ?: "Album",
                                    imageUrls = listOf(ytItem.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                    type = ItemType.ALBUM
                                )
                                else -> null
                            }
                        }.orEmpty()

                        // 2. Parallel lightweight enrichment ONLY for missing sections (not 8 heavy searches)
                        val extraSongsDeferred = async {
                            if (fromPageSongs.size < 8) {
                                YouTube.search("${item.title} songs", YouTube.SearchFilter.FILTER_SONG)
                                    .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                            } else emptyList()
                        }

                        val extraPlaylistsDeferred = async {
                            if (fromPagePlaylists.size < 6) {
                                YouTube.search("${item.title} playlist", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty().map { p ->
                                        ShelfItem(
                                            id = p.id,
                                            title = p.title,
                                            subtitle = "Playlist",
                                            imageUrls = listOf(p.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                            type = ItemType.PLAYLIST
                                        )
                                    }
                            } else emptyList()
                        }

                        val extraSimilarDeferred = async {
                            if (fromPageArtists.isEmpty()) {
                                YouTube.search("${item.title} similar artists", YouTube.SearchFilter.FILTER_ARTIST)
                                    .getOrNull()?.items?.filterIsInstance<com.music.innertube.models.ArtistItem>().orEmpty()
                            } else emptyList()
                        }

                        val allSongs = (fromPageSongs + extraSongsDeferred.await()).distinctBy { it.id }
                        val allPlaylists = (fromPagePlaylists + extraPlaylistsDeferred.await()).distinctBy { it.id }.take(15)
                        val allSimilar = (fromPageArtists + extraSimilarDeferred.await())
                            .filter { it.id != item.id && !it.title.equals(item.title, ignoreCase = true) }
                            .distinctBy { it.id }
                            .take(15)
                            .map { a ->
                                ShelfItem(
                                    id = a.id,
                                    title = a.title,
                                    subtitle = "Artist",
                                    imageUrls = listOf(a.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                    type = ItemType.ARTIST
                                )
                            }

                        ArtistBundle(
                            songs = allSongs,
                            playlists = allPlaylists,
                            similarArtists = allSimilar,
                            subscribers = subsText,
                            monthlyListeners = monthlyText,
                            description = artistBio,
                            heroThumbnail = heroThumb
                        )
                    } catch (e: Exception) {
                        ArtistBundle(emptyList(), emptyList(), emptyList(), null, null, null, null)
                    }
                }

                if (bundle.songs.isNotEmpty()) {
                    artistBundleCache[item.id] = bundle
                    com.example.muzo.core.prefetchSongStreams(bundle.songs, limit = 5)
                }
                playlistSongs = bundle.songs
                artistPlaylists = bundle.playlists
                similarArtists = bundle.similarArtists
                artistSubscribers = bundle.subscribers
                artistMonthlyListeners = bundle.monthlyListeners
                artistDescription = bundle.description
                artistHeroThumbnail = bundle.heroThumbnail
                isPlaylistLoading = false
            }
        } else {
            // PLAYLIST or ALBUM
            val cachedSongs = playlistCache[item.id]
            if (cachedSongs != null && cachedSongs.isNotEmpty()) {
                playlistSongs = cachedSongs
                isPlaylistLoading = false
                return
            }

            isPlaylistLoading = true
            artistPlaylists = emptyList()
            similarArtists = emptyList()
            artistSubscribers = null
            artistMonthlyListeners = null

            scope.launch {
                val songs = withContext(Dispatchers.IO) {
                    try {
                        val isAlbum = item.type == ItemType.ALBUM || item.id.startsWith("MPRE")
                        val isPlaylist = item.type == ItemType.PLAYLIST || item.id.startsWith("PL") || item.id.startsWith("VL") || item.id.startsWith("RD")

                        when {
                            isAlbum -> {
                                YouTube.album(item.id).getOrNull()?.songs.orEmpty()
                            }
                            isPlaylist -> {
                                YouTube.playlist(item.id.removePrefix("VL")).getOrNull()?.songs.orEmpty()
                            }
                            else -> {
                                val pId = item.id.removePrefix("VL")
                                val pSongs = YouTube.playlist(pId).getOrNull()?.songs.orEmpty()
                                if (pSongs.isNotEmpty()) pSongs else {
                                    YouTube.album(item.id).getOrNull()?.songs.orEmpty()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                if (songs.isNotEmpty()) {
                    playlistCache[item.id] = songs
                    com.example.muzo.core.prefetchSongStreams(songs, limit = 5)
                }
                playlistSongs = songs
                isPlaylistLoading = false
            }
        }
    }

    // Helper to open mood/genre category and fetch its rich playlists
    fun openCategory(categoryName: String) {
        selectedCategoryTitle = categoryName
        val cached = categoryCache[categoryName]
        if (cached != null && cached.isNotEmpty()) {
            categoryPlaylists = cached
            isCategoryLoading = false
            return
        }

        isCategoryLoading = true
        scope.launch {
            val playlists = withContext(Dispatchers.IO) {
                try {
                    val q1 = async {
                        YouTube.search("$categoryName Hindi", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                            .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                    }
                    val q2 = async {
                        YouTube.search(categoryName, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                            .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                    }
                    val all = (q1.await() + q2.await()).distinctBy { it.id }
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
            if (playlists.isNotEmpty()) {
                categoryCache[categoryName] = playlists
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
            BackHandler {
                if (playlistBackStack.isNotEmpty()) {
                    val prev = playlistBackStack.removeAt(playlistBackStack.lastIndex)
                    openPlaylist(prev, addToBackStack = false)
                } else {
                    selectedPlaylist = null
                }
            }
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
        isEqualizerOpen -> {
            BackHandler { isEqualizerOpen = false }
        }
        isSettingsOpen -> {
            BackHandler { isSettingsOpen = false }
        }
    }

    DynamicSongTheme(currentSong = currentSong) {
        Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isSettingsOpen -> {
                    SettingsScreen(
                        onBack = { isSettingsOpen = false },
                        onOpenAbout = { isAboutDialogOpen = true },
                        onOpenEqualizer = { isEqualizerOpen = true },
                        onUpdateFound = { updateInfo -> availableUpdate = updateInfo },
                        crossfadeSeconds = crossfadeSeconds,
                        isGaplessEnabled = isGaplessEnabled,
                        onCrossfadeChange = { playerViewModel.setCrossfadeSeconds(it) },
                        onGaplessToggle = { playerViewModel.setGaplessEnabled(it) }
                    )
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
                        isArtist = isArtist,
                        artistSubscribers = artistSubscribers,
                        artistMonthlyListeners = artistMonthlyListeners,
                        artistDescription = artistDescription,
                        artistHeroThumbnail = artistHeroThumbnail,
                        currentPlayingSongId = currentSong?.id,
                        isPlaybackPlaying = isPlaying,
                        artistPlaylists = artistPlaylists,
                        similarArtists = similarArtists,
                        relatedPlaylists = related,
                        isLoading = isPlaylistLoading,
                        onBack = {
                            if (playlistBackStack.isNotEmpty()) {
                                val prev = playlistBackStack.removeAt(playlistBackStack.lastIndex)
                                openPlaylist(prev, addToBackStack = false)
                            } else {
                                selectedPlaylist = null
                            }
                        },
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
                        onRadioClick = {
                            if (playlistSongs.isNotEmpty()) {
                                playerViewModel.playTrack(0, playlistSongs)
                            }
                        },
                        onRelatedPlaylistClick = { relItem ->
                            openPlaylist(relItem)
                        },
                        onSimilarArtistClick = { artistItem ->
                            openPlaylist(artistItem)
                        },
                        onSongActionClick = { song, songs ->
                            actionMenuTarget = ActionMenuTarget.Song(song, songs)
                        },
                        onPlaylistActionClick = {
                            selectedPlaylist?.let { pl ->
                                actionMenuTarget = ActionMenuTarget.Playlist(pl, playlistSongs)
                            }
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
                        },
                        onItemLongClick = { item ->
                            if (item.type == ItemType.SONG) {
                                val song = SongItem(
                                    id = item.id,
                                    title = item.title,
                                    artists = listOf(com.music.innertube.models.Artist(name = item.subtitle, id = null)),
                                    album = null,
                                    duration = 0,
                                    thumbnail = item.imageUrls.firstOrNull() ?: ""
                                )
                                actionMenuTarget = ActionMenuTarget.Song(song, listOf(song))
                            } else {
                                actionMenuTarget = ActionMenuTarget.Playlist(item)
                            }
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
                        },
                        onItemLongClick = { item ->
                            if (item.type == ItemType.SONG) {
                                val song = SongItem(
                                    id = item.id,
                                    title = item.title,
                                    artists = listOf(com.music.innertube.models.Artist(name = item.subtitle, id = null)),
                                    album = null,
                                    duration = 0,
                                    thumbnail = item.imageUrls.firstOrNull() ?: ""
                                )
                                actionMenuTarget = ActionMenuTarget.Song(song, listOf(song))
                            } else {
                                actionMenuTarget = ActionMenuTarget.Playlist(item)
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
                            onOpenSettings = { isSettingsSheetOpen = true },
                            onOpenSearch = { selectedTab = 1 },
                            onItemLongClick = { item, list ->
                                if (item.type == ItemType.SONG) {
                                    val songItem = SongItem(
                                        id = item.id,
                                        title = item.title,
                                        artists = listOf(com.music.innertube.models.Artist(name = item.subtitle, id = null)),
                                        album = null,
                                        duration = 0,
                                        thumbnail = item.imageUrls.firstOrNull() ?: ""
                                    )
                                    val surroundingSongs = list.filter { it.type == ItemType.SONG }.map {
                                        SongItem(
                                            id = it.id,
                                            title = it.title,
                                            artists = listOf(com.music.innertube.models.Artist(name = it.subtitle, id = null)),
                                            album = null,
                                            duration = 0,
                                            thumbnail = it.imageUrls.firstOrNull() ?: ""
                                        )
                                    }
                                    actionMenuTarget = ActionMenuTarget.Song(songItem, surroundingSongs)
                                } else {
                                    actionMenuTarget = ActionMenuTarget.Playlist(item)
                                }
                            }
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
                            statusText = statusText,
                            onSongActionClick = { song, list ->
                                actionMenuTarget = ActionMenuTarget.Song(song, list)
                            },
                            onPlaylistActionClick = { item ->
                                actionMenuTarget = ActionMenuTarget.Playlist(item)
                            }
                        )
                        2 -> LibraryScreen(
                            libraryViewModel = libraryViewModel,
                            onSongPlay = { song, queue ->
                                val songIdx = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                playerViewModel.playTrack(songIdx, queue)
                            },
                            onSettingsClick = { isSettingsSheetOpen = true },
                            onSongActionClick = { song, list ->
                                actionMenuTarget = ActionMenuTarget.Song(song, list)
                            },
                            onPlaylistActionClick = { item ->
                                actionMenuTarget = ActionMenuTarget.Playlist(item)
                            }
                        )
                    }
                }
            }
        }

        // Floating Mini Player & Floating Dock Bottom Nav Bar (Echo-Music Style)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            PlayerWithBottomNav(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                hasPrev = currentIndex > 0,
                hasNext = currentIndex + 1 < playbackQueue.size,
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
                },
                onMoreClick = { isSettingsSheetOpen = true }
            )
        }

        // Full Player Sheet (Expandable)
        AnimatedVisibility(
            visible = isPlayerExpanded && currentSong != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 380,
                    easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
                )
            ) + fadeIn(animationSpec = tween(durationMillis = 260)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 320,
                    easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
                )
            ) + fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            FullPlayerSheet(
                song = currentSong!!,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                hasPrev = currentIndex > 0,
                hasNext = currentIndex + 1 < playbackQueue.size,
                queueCount = playbackQueue.size,
                isLiked = isCurrentSongLiked,
                sleepTimer = playerViewModel.sleepTimer,
                equalizerController = playerViewModel.equalizerController,
                audioSessionId = playerViewModel.player.audioSessionId,
                playbackSpeed = playbackSpeed,
                onSpeedChange = { playerViewModel.setPlaybackSpeed(it) },
                queue = playbackQueue,
                currentIndex = currentIndex,
                isShuffleActive = isShuffleActive,
                repeatMode = repeatMode,
                crossfadeSeconds = crossfadeSeconds,
                isGaplessEnabled = isGaplessEnabled,
                onCrossfadeChange = { playerViewModel.setCrossfadeSeconds(it) },
                onGaplessToggle = { playerViewModel.setGaplessEnabled(it) },
                onShuffleToggle = { playerViewModel.toggleShuffle() },
                onRepeatToggle = { playerViewModel.toggleRepeat() },
                onStartRadio = { songItem -> playerViewModel.startRadio(songItem) },
                onLikeToggle = { playerViewModel.toggleLikeCurrentSong() },
                onClose = { isPlayerExpanded = false },
                onPlayPause = { playerViewModel.togglePlayPause() },
                onPrev = { playerViewModel.playPrevious() },
                onNext = { playerViewModel.playNext() },
                onSeek = { targetMs -> playerViewModel.seekTo(targetMs) },
                onQueueSongSelect = { idx -> playerViewModel.playTrack(idx, playbackQueue) },
                onMoveQueueItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
                onRemoveQueueItem = { idx -> playerViewModel.removeQueueItem(idx) },
                onClearUpcomingQueue = { playerViewModel.clearUpcomingQueue() }
            )
        }

        // Settings Bottom Sheet (Matches Screenshot 1)
        if (isSettingsSheetOpen) {
            com.example.muzo.ui.components.SettingsBottomSheet(
                onDismiss = { isSettingsSheetOpen = false },
                onOpenSettings = {
                    isSettingsSheetOpen = false
                    isSettingsOpen = true
                },
                onOpenAbout = {
                    isSettingsSheetOpen = false
                    isAboutDialogOpen = true
                },
                onOpenEqualizer = {
                    isSettingsSheetOpen = false
                    isEqualizerOpen = true
                }
            )
        }

        // Equalizer Bottom Sheet (when opened from Settings or Quick Menu)
        if (isEqualizerOpen) {
            com.example.muzo.ui.components.EqualizerBottomSheet(
                equalizerController = playerViewModel.equalizerController,
                audioSessionId = playerViewModel.player.audioSessionId,
                onDismiss = { isEqualizerOpen = false }
            )
        }

        // Welcome & Developer Info Dialog (Shown once on first install & when explicitly opened)
        if (showWelcomeDialog || isAboutDialogOpen) {
            com.example.muzo.ui.components.WelcomeDialog(
                onDismissRequest = {
                    showWelcomeDialog = false
                    isAboutDialogOpen = false
                    prefs.edit().putBoolean("has_shown_welcome_dialog", true).apply()
                }
            )
        }

        // Update Available Dialog (Pops up when remote version > current version)
        availableUpdate?.let { updateInfo ->
            com.example.muzo.updater.UpdateAvailableDialog(
                version = updateInfo.versionName,
                updateUrl = updateInfo.updateUrl,
                description = updateInfo.description,
                onDismiss = { availableUpdate = null }
            )
        }

        // Song / Playlist Action Menu Bottom Sheet (Long-press on any track or playlist across all screens)
        actionMenuTarget?.let { target ->
            SongActionBottomSheet(
                target = target,
                playerViewModel = playerViewModel,
                likedSongDao = likedSongDao,
                userPlaylistDao = userPlaylistDao,
                onDismiss = { actionMenuTarget = null },
                onOpenPlaylistDetail = { item ->
                    actionMenuTarget = null
                    openPlaylist(item)
                }
            )
        }
    }
}
}
