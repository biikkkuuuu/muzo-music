package com.example.muzo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
    statusText: String = ""
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // 0 = Explore, 1 = Muzi Chart, 2 = Album
    var selectedTopTab by rememberSaveable { mutableIntStateOf(0) }

    // Active query search results
    var searchResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
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

    // Theme palette from Echo Music screenshot
    val darkBackground = Color(0xFF0E0B0C)
    val cardBackground = Color(0xFF1E1718)
    val coralAccent = Color(0xFFF39C8F)
    val textMuted = Color(0xFF9E8E8F)
    val searchBarBg = Color(0xFF231A1B)

    // Execute active text query search
    val performQuerySearch: (String) -> Unit = { q ->
        if (q.isNotBlank()) {
            isSearching = true
            searchError = ""
            keyboardController?.hide()
            scope.launch(Dispatchers.IO) {
                try {
                    val response = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                    val items = response.getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                    withContext(Dispatchers.Main) {
                        searchResults = items.take(35)
                        isSearching = false
                        if (searchResults.isEmpty()) {
                            searchError = "No songs found for \"$q\""
                        }
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
        // TOP SEARCH BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(searchBarBg)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFFDFD1D2),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Search YouTube Music...",
                        color = Color(0xFF8E8384),
                        fontSize = 16.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) performQuerySearch(query)
                })
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = {
                    onQueryChange("")
                    searchResults = emptyList()
                    searchError = ""
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color(0xFFDFD1D2),
                        modifier = Modifier.size(20.dp)
                    )
                }
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
                        color = if (isSelected) coralAccent else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) coralAccent else Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(
            color = Color(0xFF221A1B),
            thickness = 0.8.dp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // CONTENT DISPLAY
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = coralAccent)
            }
        } else if (query.isNotBlank() && searchResults.isNotEmpty()) {
            // SEARCH QUERY RESULTS
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(searchResults, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSongSelect(song, searchResults) }
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
                item {
                    Spacer(modifier = Modifier.height(70.dp))
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
                                        .clickable { onSongSelect(song, chartSongs) }
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
                                            .clickable { onSongSelect(song, liveSongs) }
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