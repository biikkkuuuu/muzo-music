package com.example.muzo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.music.innertube.YouTube
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class HomeSection(val id: String, val baseWeight: Int) {
    data object SpeedDialHero : HomeSection("speed_dial_hero", 110)
    data object QuickPicks : HomeSection("quick_picks", 100)
    data object FeaturedArtists : HomeSection("featured_artists", 95)
    data object KeepListening : HomeSection("keep_listening", 90)
    data object HotHits : HomeSection("hot_hits", 80)
    data object MoodAndGenres : HomeSection("mood_and_genres", 70)
    data object FeaturedPlaylists : HomeSection("featured_playlists", 65)
    data object DailyDiscover : HomeSection("daily_discover", 60)
    data object SimilarRecommendation : HomeSection("similar_recommendations", 50)
    data object FromTheCommunity : HomeSection("from_the_community", 40)
    data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(14.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    recentHistory: List<SongItem>,
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    onCategoryClick: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedMood by remember { mutableStateOf<String?>("Romance") }
    var speedDialSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var quickPicks by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var hotHits by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var dailyDiscover by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var similarRecs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var communityPlaylists by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var forgottenFavorites by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    
    var artistsList by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var playlistsList by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }

    var isRefreshing by remember { mutableStateOf(false) }

    var activeGenreView by remember { mutableStateOf<String?>(null) }
    var genreSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isGenreLoading by remember { mutableStateOf(false) }

    var selectedPlaylist by remember { mutableStateOf<PlaylistItem?>(null) }
    var playlistSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isPlaylistLoading by remember { mutableStateOf(false) }

    var selectedArtist by remember { mutableStateOf<ArtistItem?>(null) }
    var artistSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isArtistLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val queryKeywords = remember {
        mapOf(
            "Romance" to listOf("Love Ballads Bollywood", "Romantic Hits Hindi", "Soulful Love Melodies"),
            "Relax" to listOf("Chill Vibes Hindi", "Acoustic Chill", "Peaceful Hindi Songs"),
            "Feel good" to listOf("Feel good Bollywood", "Feel good Indie Pop", "Happy Vibes Hindi"),
            "Party" to listOf("Punjabi Party Hits", "Bollywood Club Bangers", "Desi Dance Party"),
            "Energize" to listOf("Workout Hindi Bass", "Gym Motivational Hits", "High Energy Pop"),
            "Chill" to listOf("Late Night Lo-Fi", "Calm Hindi Acoustic", "Slowed Reverb Hits")
        )
    }

    val refreshContent: (String) -> Unit = { category ->
        isRefreshing = true
        scope.launch(Dispatchers.IO) {
            try {
                val pool = queryKeywords[category] ?: listOf(category)
                val q1Key = pool.random()

                val res1 = async { YouTube.search(category, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty() }
                val res2 = async { YouTube.search(q1Key, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty() }
                val res3 = async { YouTube.search("$category Artists", YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items.orEmpty() }
                val res4 = async { YouTube.search("$category Playlists", YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items.orEmpty() }

                val items1 = res1.await().filterIsInstance<SongItem>()
                val items2 = res2.await().filterIsInstance<SongItem>()
                val artists = res3.await().filterIsInstance<ArtistItem>()
                val playlists = res4.await().map { 
                    PlaylistItem(
                        id = it.id, 
                        title = it.title, 
                        thumbnail = it.thumbnail,
                        author = null,
                        songCountText = null,
                        playEndpoint = null,
                        shuffleEndpoint = null,
                        radioEndpoint = null
                    )
                }

                withContext(Dispatchers.Main) {
                    speedDialSongs = items1.shuffled().take(6)
                    quickPicks = items1.shuffled().take(8)
                    hotHits = items2.shuffled().take(10)
                    dailyDiscover = items1.shuffled().take(10)
                    similarRecs = items2.shuffled().take(10)
                    communityPlaylists = items2.shuffled().take(10)
                    forgottenFavorites = items1.shuffled().take(8)
                    
                    artistsList = artists.distinctBy { it.id }.take(10)
                    playlistsList = playlists.distinctBy { it.id }.take(10)

                    isRefreshing = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { isRefreshing = false }
            }
        }
    }

    val openGenreSubPage: (String) -> Unit = { genre ->
        activeGenreView = genre
        isGenreLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val res = YouTube.search("$genre Bollywood Playlist Hits", YouTube.SearchFilter.FILTER_SONG)
                val items = res.getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                withContext(Dispatchers.Main) {
                    genreSongs = items.take(24)
                    isGenreLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { isGenreLoading = false }
            }
        }
    }

    val openPlaylistDetail: (PlaylistItem) -> Unit = { playlist ->
        selectedPlaylist = playlist
        isPlaylistLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val res = YouTube.search(playlist.title, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                val items = res?.items?.filterIsInstance<SongItem>().orEmpty()
                withContext(Dispatchers.Main) {
                    playlistSongs = items
                    isPlaylistLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { isPlaylistLoading = false }
            }
        }
    }

    val openArtistDetail: (ArtistItem) -> Unit = { artist ->
        selectedArtist = artist
        isArtistLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val res = YouTube.search(artist.title, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                val items = res?.items?.filterIsInstance<SongItem>().orEmpty()
                withContext(Dispatchers.Main) {
                    artistSongs = items
                    isArtistLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { isArtistLoading = false }
            }
        }
    }

    LaunchedEffect(selectedMood) {
        refreshContent(selectedMood ?: "Romance")
    }

    val homeSections = remember(
        speedDialSongs,
        quickPicks,
        hotHits,
        dailyDiscover,
        similarRecs,
        communityPlaylists,
        forgottenFavorites,
        recentHistory,
        artistsList,
        playlistsList
    ) {
        val list = mutableListOf<HomeSection>()
        if (speedDialSongs.isNotEmpty()) list.add(HomeSection.SpeedDialHero)
        if (artistsList.isNotEmpty()) list.add(HomeSection.FeaturedArtists)
        if (recentHistory.isNotEmpty()) list.add(HomeSection.KeepListening)
        if (quickPicks.isNotEmpty()) list.add(HomeSection.QuickPicks)
        if (playlistsList.isNotEmpty()) list.add(HomeSection.FeaturedPlaylists)
        if (hotHits.isNotEmpty()) list.add(HomeSection.HotHits)
        list.add(HomeSection.MoodAndGenres)
        if (dailyDiscover.isNotEmpty()) list.add(HomeSection.DailyDiscover)
        if (similarRecs.isNotEmpty()) list.add(HomeSection.SimilarRecommendation)
        if (communityPlaylists.isNotEmpty()) list.add(HomeSection.FromTheCommunity)
        if (forgottenFavorites.isNotEmpty()) list.add(HomeSection.ForgottenFavorites)
        list.sortedByDescending { it.baseWeight }
    }

    when {
        activeGenreView != null -> {
            BackHandler { activeGenreView = null }
            GenreDetailScreen(
                genreTitle = activeGenreView!!,
                songs = genreSongs,
                isLoading = isGenreLoading,
                onBack = { activeGenreView = null },
                onSongSelect = { song -> onSongSelect(song, genreSongs) }
            )
        }
        selectedPlaylist != null -> {
            BackHandler { selectedPlaylist = null }
            PlaylistDetailScreen(
                playlist = selectedPlaylist!!,
                songs = playlistSongs,
                isLoading = isPlaylistLoading,
                onBack = { selectedPlaylist = null },
                onSongSelect = { song -> onSongSelect(song, playlistSongs) }
            )
        }
        selectedArtist != null -> {
            BackHandler { selectedArtist = null }
            ArtistDetailScreen(
                artist = selectedArtist!!,
                songs = artistSongs,
                isLoading = isArtistLoading,
                onBack = { selectedArtist = null },
                onSongSelect = { song -> onSongSelect(song, artistSongs) }
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { refreshContent(selectedMood ?: "Romance") },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    item(key = "home_top_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Muzo",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {}) {
                                    Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {}) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = "Stats", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = onOpenSettings) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    stickyHeader(key = "sticky_mood_chips") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                            tonalElevation = 2.dp
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val moods = listOf("Romance", "Relax", "Feel good", "Party", "Energize", "Chill")
                                items(moods, key = { it }) { mood ->
                                    val isSelected = selectedMood == mood
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedMood = if (isSelected) null else mood },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        label = { Text(mood, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp) },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    )
                                }
                            }
                        }
                    }

                    homeSections.forEach { section ->
                        when (section) {
                            HomeSection.SpeedDialHero -> {
                                item(key = "speed_dial_hero") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Text(
                                                text = "SPOTLIGHT SELECTION",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.8.sp
                                            )
                                            Text(
                                                text = "Top Featured",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            items(speedDialSongs, key = { it.id }) { song ->
                                                HeroCard(song = song, onClick = { onSongSelect(song, speedDialSongs) })
                                            }
                                        }
                                    }
                                }
                            }

                            HomeSection.FeaturedArtists -> {
                                item(key = "featured_artists") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Text(
                                                text = "POPULAR CREATORS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.8.sp
                                            )
                                            Text(
                                                text = "Featured Artists",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(artistsList, key = { it.id }) { artist ->
                                                ArtistCard(artist = artist, onClick = { openArtistDetail(artist) })
                                            }
                                        }
                                    }
                                }
                            }

                            HomeSection.FeaturedPlaylists -> {
                                item(key = "featured_playlists") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Text(
                                                text = "HANDPICKED MIXES",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.8.sp
                                            )
                                            Text(
                                                text = "Featured Playlists",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(playlistsList, key = { it.id }) { playlist ->
                                                PlaylistCard(playlist = playlist, onClick = { openPlaylistDetail(playlist) })
                                            }
                                        }
                                    }
                                }
                            }

                            HomeSection.KeepListening -> {
                                if (recentHistory.isNotEmpty()) {
                                    item(key = "keep_listening") {
                                        ShelfSection(
                                            subtitle = "CONTINUE YOUR SESSION",
                                            title = "Keep Listening",
                                            songs = recentHistory,
                                            isLoading = false,
                                            onSongClick = { song -> onSongSelect(song, recentHistory) }
                                        )
                                    }
                                }
                            }

                            HomeSection.QuickPicks -> {
                                item(key = "quick_picks") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Text(
                                                text = "START RADIO FROM A SONG",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.8.sp
                                            )
                                            Text(
                                                text = "Quick Picks",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))

                                        if (isRefreshing && quickPicks.isEmpty()) {
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                items(4) {
                                                    ShimmerPlaceholder(modifier = Modifier.width(250.dp).height(68.dp))
                                                }
                                            }
                                        } else {
                                            LazyHorizontalGrid(
                                                rows = GridCells.Fixed(2),
                                                modifier = Modifier.height(154.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(quickPicks, key = { it.id }) { song ->
                                                    CompactSongTile(song = song, onClick = { onSongSelect(song, quickPicks) })
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HomeSection.HotHits -> {
                                item(key = "hot_hits") {
                                    ShelfSection(
                                        subtitle = "MUSIC THAT'S HOT AND HAPPENING!",
                                        title = "${selectedMood ?: "India"}'s biggest hits",
                                        songs = hotHits,
                                        isLoading = isRefreshing && hotHits.isEmpty(),
                                        onSongClick = { song -> onSongSelect(song, hotHits) }
                                    )
                                }
                            }

                            HomeSection.MoodAndGenres -> {
                                item(key = "mood_and_genres") {
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
                                        Spacer(modifier = Modifier.height(6.dp))

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
                                                    MoodTile(title = m1, modifier = Modifier.weight(1f), onClick = { openGenreSubPage(m1) })
                                                    MoodTile(title = m2, modifier = Modifier.weight(1f), onClick = { openGenreSubPage(m2) })
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HomeSection.DailyDiscover -> {
                                item(key = "daily_discover") {
                                    ShelfSection(
                                        subtitle = "CURATED JUST FOR TODAY",
                                        title = "Daily Discover",
                                        songs = dailyDiscover,
                                        isLoading = isRefreshing && dailyDiscover.isEmpty(),
                                        onSongClick = { song -> onSongSelect(song, dailyDiscover) }
                                    )
                                }
                            }

                            HomeSection.SimilarRecommendation -> {
                                item(key = "similar_recommendations") {
                                    ShelfSection(
                                        subtitle = "SIMILAR RECOMMENDATIONS",
                                        title = "Because You Love ${selectedMood ?: "Hits"}",
                                        songs = similarRecs,
                                        isLoading = isRefreshing && similarRecs.isEmpty(),
                                        onSongClick = { song -> onSongSelect(song, similarRecs) }
                                    )
                                }
                            }

                            HomeSection.FromTheCommunity -> {
                                item(key = "from_the_community") {
                                    ShelfSection(
                                        subtitle = "FROM THE WEIRD TO THE WONDERFUL",
                                        title = "Trending community playlists",
                                        songs = communityPlaylists,
                                        isLoading = isRefreshing && communityPlaylists.isEmpty(),
                                        onSongClick = { song -> onSongSelect(song, communityPlaylists) }
                                    )
                                }
                            }

                            HomeSection.ForgottenFavorites -> {
                                item(key = "forgotten_favorites") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Text(
                                                text = "RELIVE THE MAGIC OF THE 90S",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.8.sp
                                            )
                                            Text(
                                                text = "Forgotten Favorites",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))

                                        LazyHorizontalGrid(
                                            rows = GridCells.Fixed(2),
                                            modifier = Modifier.height(154.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(forgottenFavorites, key = { it.id }) { song ->
                                                CompactSongTile(song = song, onClick = { onSongSelect(song, forgottenFavorites) })
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
    }
}

@Composable
fun ArtistCard(artist: ArtistItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = getHighResThumbnail(artist.thumbnail),
            contentDescription = artist.title,
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun PlaylistCard(playlist: PlaylistItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = getHighResThumbnail(playlist.thumbnail),
            contentDescription = playlist.title,
            modifier = Modifier
                .size(142.dp)
                .clip(RoundedCornerShape(16.dp))
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.author?.name ?: "YouTube Music",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HeroCard(song: SongItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = getHighResThumbnail(song.thumbnail),
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ShelfSection(
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
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) {
                    Column(modifier = Modifier.width(142.dp)) {
                        ShimmerPlaceholder(
                            modifier = Modifier
                                .size(142.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerPlaceholder(modifier = Modifier.width(100.dp).height(14.dp))
                    }
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SquareSongCard(song = song, onClick = { onSongClick(song) })
                }
            }
        }
    }
}

@Composable
fun SquareSongCard(song: SongItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = getHighResThumbnail(song.thumbnail),
            contentDescription = song.title,
            modifier = Modifier
                .size(142.dp)
                .clip(RoundedCornerShape(16.dp))
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
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
fun CompactSongTile(song: SongItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(250.dp)
            .height(68.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = getHighResThumbnail(song.thumbnail),
                contentDescription = song.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
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
    }
}

@Composable
fun MoodTile(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(50.dp)
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
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
        }
    }
}

@Composable
fun GenreDetailScreen(
    genreTitle: String,
    songs: List<SongItem>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSongSelect: (SongItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = genreTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongSelect(song) }
                    ) {
                        AsyncImage(
                            model = getHighResThumbnail(song.thumbnail),
                            contentDescription = song.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .shadow(4.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
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
            }
        }
    }
}