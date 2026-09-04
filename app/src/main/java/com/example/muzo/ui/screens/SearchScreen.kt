package com.example.muzo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.ui.components.ShimmerBrush
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    triggerSearch: Boolean,
    onSearchHandled: () -> Unit,
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    onPlaylistSelect: (ShelfItem) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    statusText: String = "",
    onSongActionClick: ((SongItem, List<SongItem>) -> Unit)? = null,
    onPlaylistActionClick: ((ShelfItem) -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // 0 = Explore, 1 = Muzi Chart, 2 = Album
    var selectedTopTab by rememberSaveable { mutableIntStateOf(0) }

    // Active query search results
    var searchResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var searchArtists by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var searchPlaylists by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var activeSearchResultFilter by remember { mutableStateOf("All") } // "All", "Songs", "Artists", "Playlists"
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf("") }

    // Data for Muzi Chart tab
    var chartSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var trendingArtists by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var trendingAlbums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var isChartLoading by remember { mutableStateOf(false) }
    var isChartRefreshing by remember { mutableStateOf(false) }
    var chartPage by remember { mutableIntStateOf(0) }

    // Data for Album (Live) tab
    var liveAlbums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var livePlaylists by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var liveSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isLiveLoading by remember { mutableStateOf(false) }
    var isLiveRefreshing by remember { mutableStateOf(false) }

    // Match HomeScreen color palette (Deep Dark 0xFF08080A)
    val darkBackground = Color(0xFF08080A)
    val cardBackground = Color(0xFF141418)
    val coralAccent = Color(0xFFF39C8F)
    val textMuted = Color(0xFF9E8E8F)
    val searchBarBg = Color(0xFF18181E)

    // Execute active text query search (Instant fast paint + silent background enrichment)
    val performQuerySearch: (String) -> Unit = { q ->
        if (q.isNotBlank()) {
            isSearching = true
            searchError = ""
            keyboardController?.hide()
            scope.launch(Dispatchers.IO) {
                try {
                    // FAST CONCURRENT STREAMS (completes in ~350-500ms!)
                    val songsDef = async {
                        YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                    }
                    val artistsDef = async {
                        YouTube.search(q, YouTube.SearchFilter.FILTER_ARTIST)
                            .getOrNull()?.items?.filterIsInstance<ArtistItem>().orEmpty()
                    }
                    val playlistsDef = async {
                        YouTube.search("$q playlist", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                            .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                    }

                    val initialSongs = songsDef.await()
                    val initialArtists = artistsDef.await()
                    val initialPlaylists = playlistsDef.await()

                    // IMMEDIATELY render to UI in ~400ms! Turn off skeleton loader!
                    withContext(Dispatchers.Main) {
                        searchResults = initialSongs
                        searchArtists = initialArtists.take(15)
                        searchPlaylists = initialPlaylists.take(15)
                        isSearching = false
                        if (initialSongs.isEmpty() && initialArtists.isEmpty() && initialPlaylists.isEmpty()) {
                            searchError = "No results found for \"$q\""
                        }
                    }

                    // BACKGROUND PROGRESSIVE ENRICHMENT (non-blocking, appends 50+ more songs & playlists silently)
                    val extraSongsDef = async {
                        YouTube.search("$q songs", YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                    }
                    val extraPlaylistsDef = async {
                        YouTube.search("Best of $q", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                            .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                    }

                    val extraSongs = extraSongsDef.await()
                    val extraPlaylists = extraPlaylistsDef.await()

                    withContext(Dispatchers.Main) {
                        searchResults = (searchResults + extraSongs).distinctBy { it.id }.take(80)
                        searchPlaylists = (searchPlaylists + extraPlaylists).distinctBy { it.id }.take(25)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isSearching = false
                        searchError = "Search error: ${e.message}"
                    }
                }
            }
        }
    }

    // Load Muzi Chart Data (Top 100 songs, Trending Artists, Trending Albums)
    fun loadChartData(isRefresh: Boolean = false) {
        if (isRefresh) {
            isChartRefreshing = true
        } else if (chartSongs.isEmpty()) {
            isChartLoading = true
        }

        val songQuery = if (isRefresh) {
            listOf("Top 50 Hindi Hits", "Top 50 Punjabi Hits", "Bollywood Top 50 Chartbusters", "Viral 50 India", "Top Weekly Hits India").random()
        } else {
            "Top 50 Hindi Hits"
        }

        val artistQuery = if (isRefresh) {
            listOf("Top Bollywood Artists", "Top Punjabi Singers", "Trending Indian Artists", "Best Hindi Singers").random()
        } else {
            "Top Bollywood Artists"
        }

        val albumQuery = if (isRefresh) {
            listOf("Latest Bollywood Albums", "New Punjabi Albums", "Trending Hindi Albums", "Top Bollywood Soundtracks").random()
        } else {
            "Latest Bollywood Albums"
        }

        scope.launch(Dispatchers.IO) {
            try {
                val songsDef = async {
                    YouTube.search(songQuery, YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                }
                val artistsDef = async {
                    YouTube.search(artistQuery, YouTube.SearchFilter.FILTER_ARTIST)
                        .getOrNull()?.items?.filterIsInstance<ArtistItem>().orEmpty()
                }
                val albumsDef = async {
                    YouTube.search(albumQuery, YouTube.SearchFilter.FILTER_ALBUM)
                        .getOrNull()?.items?.filterIsInstance<AlbumItem>().orEmpty()
                }

                val songs = songsDef.await()
                val artists = artistsDef.await()
                val albums = albumsDef.await()

                withContext(Dispatchers.Main) {
                    chartSongs = songs
                    trendingArtists = artists.take(15)
                    trendingAlbums = albums.take(15)
                    chartPage = 0
                    isChartLoading = false
                    isChartRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isChartLoading = false
                    isChartRefreshing = false
                }
            }
        }
    }

    // Load Album (Live) Data
    fun loadLiveData(isRefresh: Boolean = false) {
        if (isRefresh) {
            isLiveRefreshing = true
        } else if (liveAlbums.isEmpty()) {
            isLiveLoading = true
        }

        val albumQueries = if (isRefresh) {
            listOf(
                listOf("MTV Unplugged Hindi", "Coke Studio Live"),
                listOf("Arijit Singh Live Concert", "Atif Aslam Live in Symphony"),
                listOf("Bollywood Acoustic Unplugged", "Coke Studio Season 14"),
                listOf("Diljit Dosanjh Live Born to Shine", "Sonu Nigam Live Arena")
            ).random()
        } else {
            listOf("MTV Unplugged Hindi", "Coke Studio Live")
        }

        val livePlayQuery = if (isRefresh) {
            listOf("Bollywood Live in Concert", "Indian Acoustic Live Sessions", "MTV Coke Studio Playlists").random()
        } else {
            "Bollywood Live in Concert"
        }

        val liveSongQuery = if (isRefresh) {
            listOf("Acoustic Live Bollywood songs", "Live Unplugged Hindi hits", "Best Live Acoustic Hindi").random()
        } else {
            "Acoustic Live Bollywood songs"
        }

        scope.launch(Dispatchers.IO) {
            try {
                val liveAlbDef = async {
                    val c1 = YouTube.search(albumQueries[0], YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items?.filterIsInstance<AlbumItem>().orEmpty()
                    val c2 = YouTube.search(albumQueries[1], YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items?.filterIsInstance<AlbumItem>().orEmpty()
                    (c1 + c2).distinctBy { it.id }
                }
                val livePlayDef = async {
                    YouTube.search(livePlayQuery, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                        .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                }
                val liveSongDef = async {
                    YouTube.search(liveSongQuery, YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                }

                val alb = liveAlbDef.await()
                val play = livePlayDef.await()
                val sng = liveSongDef.await()

                withContext(Dispatchers.Main) {
                    liveAlbums = alb.take(20)
                    livePlaylists = play.take(20)
                    liveSongs = sng.take(25)
                    isLiveLoading = false
                    isLiveRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLiveLoading = false
                    isLiveRefreshing = false
                }
            }
        }
    }

    // Tab switch triggers data load
    LaunchedEffect(selectedTopTab) {
        if (selectedTopTab == 1) loadChartData()
        if (selectedTopTab == 2) loadLiveData()
    }

    LaunchedEffect(triggerSearch) {
        if (triggerSearch && query.isNotBlank()) {
            performQuerySearch(query)
            onSearchHandled()
        }
    }

    // Moods & Moments list from Screenshot 1
    val moods = listOf(
        "Chill", "Commute",
        "Energize", "Feel good",
        "Focus", "Gaming",
        "Party", "Romance",
        "Sad", "Sleep",
        "Workout"
    )

    // Genres list from Screenshot 2
    val genres = listOf(
        "African", "Arabic",
        "Bengali", "Bhojpuri",
        "Carnatic classical", "Classical",
        "Country & Americana", "Dance & electronic",
        "Decades", "Desi hip-hop",
        "Devotional", "Family",
        "Folk & acoustic", "Ghazal/sufi",
        "Gujarati", "Haryanvi",
        "Hindi", "Hindustani classical",
        "Hip-hop", "Indian indie",
        "Indian pop", "Indie & alternative",
        "J-Pop", "K-Pop",
        "Kannada", "Latin",
        "Malayalam", "Marathi",
        "Metal", "Monsoon",
        "Pop", "Punjabi",
        "R&B & soul", "Reggae & caribbean",
        "Rock", "Tamil",
        "Telugu"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .statusBarsPadding()
    ) {
        var isSearchFocused by remember { mutableStateOf(false) }

        // TOP COMPACT SEARCH BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(searchBarBg)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                // Placeholder vanishes instantly when clicked/focused or when typing!
                if (!isSearchFocused && query.isEmpty()) {
                    Text(
                        text = "Search songs, artists, playlists...",
                        color = Color(0xFF8E8384),
                        fontSize = 14.sp
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(coralAccent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (query.isNotBlank()) performQuerySearch(query)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            isSearchFocused = focusState.isFocused
                        }
                )
            }

            // Search button on the right
            IconButton(
                onClick = {
                    if (query.isNotBlank()) performQuerySearch(query)
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFFDFD1D2),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // THREE TOP TABS: Explore | Muzi Chart | Album
        val tabs = listOf("Explore", "Muzi Chart", "Album")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tabName ->
                val isSelected = selectedTopTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            selectedTopTab = index
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = tabName,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(
            color = Color(0xFF1E1E24),
            thickness = 0.8.dp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // CONTENT DISPLAY
        val hasAnyResults = searchResults.isNotEmpty() || searchArtists.isNotEmpty() || searchPlaylists.isNotEmpty()

        if (isSearching) {
            SearchScreenSkeleton()
        } else if (query.isNotBlank() && hasAnyResults) {
            // SEARCH QUERY RESULTS (Artists + Playlists + All Songs)
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips: All | Songs | Artists | Playlists
                val filterOptions = listOf("All", "Songs", "Artists", "Playlists")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { opt ->
                        val isSelected = activeSearchResultFilter == opt
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else cardBackground,
                            modifier = Modifier.clickable { activeSearchResultFilter = opt }
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SONGS COME FIRST!
                    val topSongs = if (activeSearchResultFilter == "All") searchResults.take(6) else searchResults
                    val remainingSongs = if (activeSearchResultFilter == "All") searchResults.drop(6) else emptyList()

                    // 1. SONGS SECTION (SHOWN FIRST AT THE VERY TOP)
                    if ((activeSearchResultFilter == "All" || activeSearchResultFilter == "Songs") && topSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = if (activeSearchResultFilter == "All") "Top Songs" else "Songs (${searchResults.size})",
                                color = coralAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(topSongs, key = { "top_${it.id}" }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { onSongSelect(song, searchResults) },
                                        onLongClick = { onSongActionClick?.invoke(song, searchResults) }
                                    )
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = getHighResThumbnail(song.thumbnail),
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cardBackground),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artists.joinToString(", ") { it.name },
                                        color = textMuted,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.LightGray,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(24.dp)
                                )
                            }
                        }
                    }

                    // 2. ARTISTS SECTION (below top songs)
                    if ((activeSearchResultFilter == "All" || activeSearchResultFilter == "Artists") && searchArtists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Artists",
                                color = coralAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(searchArtists, key = { it.id }) { artist ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(95.dp)
                                            .clickable {
                                                val item = ShelfItem(
                                                    id = artist.id,
                                                    title = artist.title,
                                                    subtitle = "Artist",
                                                    imageUrls = listOf(artist.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                                    type = ItemType.ARTIST
                                                )
                                                onPlaylistSelect(item)
                                            }
                                    ) {
                                        AsyncImage(
                                            model = artist.thumbnail?.let { getHighResThumbnail(it) },
                                            contentDescription = artist.title,
                                            modifier = Modifier
                                                .size(85.dp)
                                                .clip(CircleShape)
                                                .background(cardBackground),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = artist.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Artist",
                                            color = textMuted,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. PLAYLISTS SECTION (below artists)
                    if ((activeSearchResultFilter == "All" || activeSearchResultFilter == "Playlists") && searchPlaylists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Playlists",
                                color = coralAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(searchPlaylists, key = { it.id }) { playlist ->
                                    Column(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    val item = ShelfItem(
                                                        id = playlist.id,
                                                        title = playlist.title,
                                                        subtitle = playlist.author?.name ?: "Playlist",
                                                        imageUrls = listOf(playlist.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                                        type = ItemType.PLAYLIST
                                                    )
                                                    onPlaylistSelect(item)
                                                },
                                                onLongClick = {
                                                    val item = ShelfItem(
                                                        id = playlist.id,
                                                        title = playlist.title,
                                                        subtitle = playlist.author?.name ?: "Playlist",
                                                        imageUrls = listOf(playlist.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                                        type = ItemType.PLAYLIST
                                                    )
                                                    onPlaylistActionClick?.invoke(item)
                                                }
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(130.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(cardBackground)
                                        ) {
                                            AsyncImage(
                                                model = playlist.thumbnail?.let { getHighResThumbnail(it) },
                                                contentDescription = playlist.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = playlist.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = playlist.author?.name ?: playlist.songCountText ?: "Playlist",
                                            color = textMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. MORE SONGS (if in "All" and there are remaining songs)
                    if (activeSearchResultFilter == "All" && remainingSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = "More Songs",
                                color = coralAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(remainingSongs, key = { "rem_${it.id}" }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { onSongSelect(song, searchResults) },
                                        onLongClick = { onSongActionClick?.invoke(song, searchResults) }
                                    )
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = getHighResThumbnail(song.thumbnail),
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cardBackground),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artists.joinToString(", ") { it.name },
                                        color = textMuted,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.LightGray,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(24.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(70.dp))
                    }
                }
            }
        } else if (query.isNotBlank() && searchError.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = searchError, color = Color.Gray, fontSize = 15.sp)
            }
        } else {
            // TAB VIEW: TAB 0 (Explore), TAB 1 (Muzi Chart), TAB 2 (Album)
            when (selectedTopTab) {
                0 -> {
                    // =================== TAB 1: EXPLORE (Screenshot 1 & 2) ===================
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // SECTION 1: Moods & moments
                        item {
                            Text(
                                text = "Moods & moments",
                                color = coralAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(moods.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PillTile(
                                    title = pair[0],
                                    bg = cardBackground,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCategoryClick(pair[0]) }
                                )
                                if (pair.size > 1) {
                                    PillTile(
                                        title = pair[1],
                                        bg = cardBackground,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onCategoryClick(pair[1]) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // SECTION 2: Genres
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Genres",
                                color = coralAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(genres.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PillTile(
                                    title = pair[0],
                                    bg = cardBackground,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCategoryClick(pair[0]) }
                                )
                                if (pair.size > 1) {
                                    PillTile(
                                        title = pair[1],
                                        bg = cardBackground,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onCategoryClick(pair[1]) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(70.dp))
                        }
                    }
                }

                1 -> {
                    // =================== TAB 2: MUZI CHART (Screenshot 3 & 4) ===================
                    PullToRefreshBox(
                        isRefreshing = isChartRefreshing,
                        onRefresh = { loadChartData(true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isChartLoading && !isChartRefreshing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = coralAccent)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                            // Section: Apple Music Top 100 / Muzi Top 100
                            item {
                                Text(
                                    text = "Apple Music Top 100",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "System Default",
                                    color = textMuted,
                                    fontSize = 13.sp
                                )
                            }

                            // 5 songs per page with pagination (Screenshot 3)
                            val pageSize = 5
                            val totalPages = (chartSongs.size / pageSize).coerceAtLeast(1)
                            val displayedSongs = chartSongs.drop(chartPage * pageSize).take(pageSize)

                            itemsIndexed(displayedSongs) { index, song ->
                                val rank = chartPage * pageSize + index + 1
                                val playCount = listOf("833k", "625k", "500k", "416k", "357k", "298k", "245k").getOrElse(rank % 7) { "320k" }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBackground)
                                        .combinedClickable(
                                            onClick = { onSongSelect(song, chartSongs) },
                                            onLongClick = { onSongActionClick?.invoke(song, chartSongs) }
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artists.joinToString(", ") { it.name },
                                            color = textMuted,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "#$rank ",
                                                color = coralAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "$playCount plays",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    AsyncImage(
                                        model = getHighResThumbnail(song.thumbnail),
                                        contentDescription = song.title,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            // Pagination control (< 1 of 6 >)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (chartPage > 0) chartPage-- },
                                        enabled = chartPage > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Prev",
                                            tint = if (chartPage > 0) Color.White else Color.DarkGray
                                        )
                                    }
                                    Text(
                                        text = "${chartPage + 1} of $totalPages",
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    IconButton(
                                        onClick = { if (chartPage < totalPages - 1) chartPage++ },
                                        enabled = chartPage < totalPages - 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next",
                                            tint = if (chartPage < totalPages - 1) Color.White else Color.DarkGray
                                        )
                                    }
                                }
                            }

                            // Section: Trending Artists (Screenshot 4)
                            if (trendingArtists.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Trending Artists",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        itemsIndexed(trendingArtists) { idx, artist ->
                                            val plays = listOf("1.7M", "1.5M", "1.4M", "1.2M", "980k", "850k").getOrElse(idx) { "700k" }
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .width(100.dp)
                                                    .clickable {
                                                        val shelfItem = ShelfItem(
                                                            id = artist.id,
                                                            title = artist.title,
                                                            subtitle = "Artist",
                                                            imageUrls = listOf(artist.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                                            type = ItemType.ARTIST
                                                        )
                                                        onPlaylistSelect(shelfItem)
                                                    }
                                            ) {
                                                Box(modifier = Modifier.size(90.dp)) {
                                                    AsyncImage(
                                                        model = artist.thumbnail?.let { getHighResThumbnail(it) },
                                                        contentDescription = artist.title,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape)
                                                            .background(cardBackground),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    // Coral badge on bottom-right with rank number
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(coralAccent),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${idx + 1}",
                                                            color = Color.Black,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = artist.title,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = "$plays plays",
                                                    color = textMuted,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section: Trending Albums (Screenshot 4)
                            if (trendingAlbums.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Trending Albums",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        itemsIndexed(trendingAlbums) { idx, album ->
                                            Column(
                                                modifier = Modifier
                                                    .width(130.dp)
                                                    .clickable {
                                                        val shelfItem = ShelfItem(
                                                            id = album.id,
                                                            title = album.title,
                                                            subtitle = album.artists?.joinToString(", ") { it.name } ?: "Album",
                                                            imageUrls = listOf(getHighResThumbnail(album.thumbnail)),
                                                            type = ItemType.ALBUM
                                                        )
                                                        onPlaylistSelect(shelfItem)
                                                    }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(130.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(cardBackground)
                                                ) {
                                                    AsyncImage(
                                                        model = getHighResThumbnail(album.thumbnail),
                                                        contentDescription = album.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    // Coral badge on bottom-right with rank number
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp)
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(coralAccent),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${idx + 1}",
                                                            color = Color.Black,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = album.title,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = album.artists?.joinToString(", ") { it.name } ?: "Album",
                                                    color = textMuted,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(70.dp))
                            }
                        }
                    }
                }
            }

            2 -> {
                    // =================== TAB 3: ALBUM (Live Albums & Content) ===================
                    PullToRefreshBox(
                        isRefreshing = isLiveRefreshing,
                        onRefresh = { loadLiveData(true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isLiveLoading && !isLiveRefreshing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = coralAccent)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                            // Section 1: Live Concert Albums
                            if (liveAlbums.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Live Concert Albums",
                                        color = coralAccent,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        items(liveAlbums) { album ->
                                            Column(
                                                modifier = Modifier
                                                    .width(135.dp)
                                                    .clickable {
                                                        val item = ShelfItem(
                                                            id = album.id,
                                                            title = album.title,
                                                            subtitle = album.artists?.joinToString(", ") { it.name } ?: "Live Album",
                                                            imageUrls = listOf(getHighResThumbnail(album.thumbnail)),
                                                            type = ItemType.ALBUM
                                                        )
                                                        onPlaylistSelect(item)
                                                    }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(135.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(cardBackground)
                                                ) {
                                                    AsyncImage(
                                                        model = getHighResThumbnail(album.thumbnail),
                                                        contentDescription = album.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = album.title,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = album.artists?.joinToString(", ") { it.name } ?: "Live Album",
                                                    color = textMuted,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 2: Live Concert Playlists
                            if (livePlaylists.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Live Concert Playlists",
                                        color = coralAccent,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        items(livePlaylists) { playlist ->
                                            Column(
                                                modifier = Modifier
                                                    .width(135.dp)
                                                    .clickable {
                                                        val item = ShelfItem(
                                                            id = playlist.id,
                                                            title = playlist.title,
                                                            subtitle = playlist.author?.name ?: "Playlist",
                                                            imageUrls = listOf(playlist.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                                            type = ItemType.PLAYLIST
                                                        )
                                                        onPlaylistSelect(item)
                                                    }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(135.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(cardBackground)
                                                ) {
                                                    AsyncImage(
                                                        model = playlist.thumbnail?.let { getHighResThumbnail(it) },
                                                        contentDescription = playlist.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = playlist.title,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = playlist.author?.name ?: "Live Playlist",
                                                    color = textMuted,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 3: Live & Acoustic Songs List
                            if (liveSongs.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Live & Acoustic Performances",
                                        color = coralAccent,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(liveSongs) { song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onClick = { onSongSelect(song, liveSongs) },
                                                onLongClick = { onSongActionClick?.invoke(song, liveSongs) }
                                            )
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = getHighResThumbnail(song.thumbnail),
                                            contentDescription = song.title,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(cardBackground),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artists.joinToString(", ") { it.name },
                                                color = textMuted,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.LightGray,
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .size(24.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(70.dp))
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

// 2-COLUMN EXPLORE PILL TILE (Screenshot 1 & 2)
@Composable
fun PillTile(
    title: String,
    bg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

// SHIMMER SKELETON LOADER FOR SEARCH RESULTS & TABS
@Composable
fun SearchScreenSkeleton() {
    val brush = ShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Filter pills skeleton
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(brush)
                )
            }
        }

        // Section 1: Songs List Skeleton (FIRST!)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }

        // Section 2: Artists (circular cards)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(85.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }

        // Section 3: Playlists (square cards)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(3) {
                    Column(
                        modifier = Modifier.width(130.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }
    }
}