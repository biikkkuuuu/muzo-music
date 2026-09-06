package com.example.muzo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.core.prefetchSongStreams
import com.example.muzo.data.local.SearchHistoryManager
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.theme.MuziThemeTokens
import com.example.muzo.ui.components.AnimatedChipsRow
import com.example.muzo.ui.components.MuziSongRow
import com.example.muzo.ui.components.NavigationTitle
import com.example.muzo.ui.components.ShelfCard
import com.example.muzo.ui.components.ShimmerBrush
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // ViVi Filter Options
    val filterOptions = remember { listOf("All", "Songs", "Videos", "Albums", "Artists", "Playlists") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    // Search Results State
    var searchResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var searchArtists by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var searchAlbums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var searchPlaylists by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf("") }
    var hasSubmittedSearch by rememberSaveable { mutableStateOf(false) }

    // Search History & Live Autocomplete Suggestions
    var recentSearches by remember { mutableStateOf(SearchHistoryManager.getHistory(context)) }
    var liveSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var suggestionsJob by remember { mutableStateOf<Job?>(null) }

    // Perform Search for given query and filter
    fun executeSearch(q: String, filter: String = selectedFilter) {
        val cleanQuery = q.trim()
        if (cleanQuery.isBlank()) return

        keyboardController?.hide()
        hasSubmittedSearch = true
        isSearching = true
        searchError = ""

        // Save to persistent history
        SearchHistoryManager.addQuery(context, cleanQuery)
        recentSearches = SearchHistoryManager.getHistory(context)

        scope.launch(Dispatchers.IO) {
            try {
                when (filter) {
                    "All" -> {
                        val songsDef = async {
                            YouTube.search(cleanQuery, YouTube.SearchFilter.FILTER_SONG)
                                .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                        }
                        val artistsDef = async {
                            YouTube.search(cleanQuery, YouTube.SearchFilter.FILTER_ARTIST)
                                .getOrNull()?.items?.filterIsInstance<ArtistItem>().orEmpty()
                        }
                        val albumsDef = async {
                            YouTube.search(cleanQuery, YouTube.SearchFilter.FILTER_ALBUM)
                                .getOrNull()?.items?.filterIsInstance<AlbumItem>().orEmpty()
                        }
                        val playlistsDef = async {
                            YouTube.search("$cleanQuery playlist", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                                .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                        }

                        val songs = songsDef.await()
                        val artists = artistsDef.await()
                        val albums = albumsDef.await()
                        val playlists = playlistsDef.await()

                        withContext(Dispatchers.Main) {
                            searchResults = songs
                            searchArtists = artists.take(15)
                            searchAlbums = albums.take(15)
                            searchPlaylists = playlists.take(15)
                            isSearching = false
                            if (songs.isEmpty() && artists.isEmpty() && albums.isEmpty() && playlists.isEmpty()) {
                                searchError = "No results found for \"$cleanQuery\""
                            }
                        }

                        if (songs.isNotEmpty()) {
                            prefetchSongStreams(songs, limit = 3)
                        }
                    }
                    "Songs" -> {
                        val songs = YouTube.search(cleanQuery, YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                        withContext(Dispatchers.Main) {
                            searchResults = songs
                            isSearching = false
                            if (songs.isEmpty()) searchError = "No songs found for \"$cleanQuery\""
                        }
                        if (songs.isNotEmpty()) prefetchSongStreams(songs, limit = 3)
                    }
                    "Videos" -> {
                        val videos = YouTube.search(cleanQuery, YouTube.SearchFilter.FILTER_VIDEO)
                            .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                        withContext(Dispatchers.Main) {
                            searchResults = videos
                            isSearching = false
                            if (videos.isEmpty()) searchError = "No videos found for \"$cleanQuery\""
                        }
                    }
                    "Albums" -> {
                        val albums = YouTube.search(cleanQuery, YouTube.SearchFilter.FILTER_ALBUM)
                            .getOrNull()?.items?.filterIsInstance<AlbumItem>().orEmpty()
                        withContext(Dispatchers.Main) {
                            searchAlbums = albums
                            isSearching = false
                            if (albums.isEmpty()) searchError = "No albums found for \"$cleanQuery\""
                        }
                    }
                    "Artists" -> {
                        val artists = YouTube.search(cleanQuery, YouTube.SearchFilter.FILTER_ARTIST)
                            .getOrNull()?.items?.filterIsInstance<ArtistItem>().orEmpty()
                        withContext(Dispatchers.Main) {
                            searchArtists = artists
                            isSearching = false
                            if (artists.isEmpty()) searchError = "No artists found for \"$cleanQuery\""
                        }
                    }
                    "Playlists" -> {
                        val playlists = YouTube.search("$cleanQuery playlist", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                            .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                        withContext(Dispatchers.Main) {
                            searchPlaylists = playlists
                            isSearching = false
                            if (playlists.isEmpty()) searchError = "No playlists found for \"$cleanQuery\""
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSearching = false
                    searchError = "Search error: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    // Debounced Live Suggestions
    LaunchedEffect(query) {
        if (!hasSubmittedSearch && query.isNotBlank()) {
            suggestionsJob?.cancel()
            suggestionsJob = scope.launch(Dispatchers.IO) {
                delay(220)
                val suggestions = YouTube.searchSuggestions(query).getOrNull()?.queries.orEmpty()
                withContext(Dispatchers.Main) {
                    liveSuggestions = suggestions
                }
            }
        } else if (query.isBlank()) {
            liveSuggestions = emptyList()
        }
    }

    // External Trigger (from Category or Voice click)
    LaunchedEffect(triggerSearch) {
        if (triggerSearch && query.isNotBlank()) {
            executeSearch(query)
            onSearchHandled()
        }
    }

    // Back Handler: Return from search results to idle view
    BackHandler(enabled = hasSubmittedSearch) {
        hasSubmittedSearch = false
        isSearching = false
    }

    // Moods & Moments list for idle exploration
    val moods = remember {
        listOf(
            "Chill", "Commute", "Energize", "Feel good",
            "Focus", "Gaming", "Party", "Romance", "Sad", "Sleep", "Workout"
        )
    }
    val genres = remember {
        listOf(
            "Hindi", "Punjabi", "Bollywood", "Indian pop",
            "Desi hip-hop", "Indian indie", "Ghazal/sufi", "Rock",
            "Pop", "Dance & electronic", "Classical", "Folk & acoustic"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // 1. ViVi-Style Top SearchBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasSubmittedSearch) {
                IconButton(
                    onClick = {
                        hasSubmittedSearch = false
                        isSearching = false
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty() && !isSearchFocused) {
                    Text(
                        text = "Search songs, albums, artists...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        hasSubmittedSearch = false
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (query.isNotBlank()) executeSearch(query)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isSearchFocused = it.isFocused }
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                        hasSubmittedSearch = false
                        liveSuggestions = emptyList()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. ViVi Animated Filter Chips Row
        AnimatedChipsRow(
            chips = filterOptions,
            selectedChip = selectedFilter,
            onChipSelect = { chip ->
                selectedFilter = chip
                if (query.isNotBlank() && hasSubmittedSearch) {
                    executeSearch(query, chip)
                }
            },
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // 3. Screen Body State Switcher
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // A. Search Running
                isSearching -> {
                    SearchScreenSkeleton()
                }

                // B. Submitted Search Results
                hasSubmittedSearch && query.isNotBlank() -> {
                    SearchResultsContent(
                        selectedFilter = selectedFilter,
                        searchResults = searchResults,
                        searchArtists = searchArtists,
                        searchAlbums = searchAlbums,
                        searchPlaylists = searchPlaylists,
                        searchError = searchError,
                        onSongSelect = onSongSelect,
                        onPlaylistSelect = onPlaylistSelect,
                        onSongActionClick = onSongActionClick
                    )
                }

                // C. Active Live Autocomplete Suggestions (while typing)
                query.isNotBlank() && liveSuggestions.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(liveSuggestions, key = { it }) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onQueryChange(suggestion)
                                        executeSearch(suggestion)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        onQueryChange(suggestion)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "Insert",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // D. Idle State: Recent Searches + Discover Moods & Genres
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 160.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Recent Searches Section
                        if (recentSearches.isNotEmpty()) {
                            item(key = "history_header") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent searches",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = {
                                            SearchHistoryManager.clearHistory(context)
                                            recentSearches = emptyList()
                                        }
                                    ) {
                                        Text(
                                            text = "Clear all",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            items(recentSearches.take(8), key = { "hist_$it" }) { histItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onQueryChange(histItem)
                                            executeSearch(histItem)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = histItem,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { onQueryChange(histItem) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = "Fill",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            SearchHistoryManager.removeQuery(context, histItem)
                                            recentSearches = SearchHistoryManager.getHistory(context)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Explore Moods & Moments
                        item(key = "moods_header") {
                            NavigationTitle(
                                title = "Moods & Moments",
                                label = "Explore vibe"
                            )
                        }
                        item(key = "moods_grid") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(moods) { mood ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.clickable {
                                            onCategoryClick(mood)
                                        }
                                    ) {
                                        Text(
                                            text = mood,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Explore Genres
                        item(key = "genres_header") {
                            NavigationTitle(
                                title = "Genres",
                                label = "Discover by style"
                            )
                        }
                        item(key = "genres_grid") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                genres.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onCategoryClick(pair[0]) }
                                        ) {
                                            Text(
                                                text = pair[0],
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                            )
                                        }
                                        if (pair.size > 1) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onCategoryClick(pair[1]) }
                                            ) {
                                                Text(
                                                    text = pair[1],
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
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
private fun SearchResultsContent(
    selectedFilter: String,
    searchResults: List<SongItem>,
    searchArtists: List<ArtistItem>,
    searchAlbums: List<AlbumItem>,
    searchPlaylists: List<PlaylistItem>,
    searchError: String,
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    onPlaylistSelect: (ShelfItem) -> Unit,
    onSongActionClick: ((SongItem, List<SongItem>) -> Unit)?
) {
    if (searchError.isNotBlank() && searchResults.isEmpty() && searchArtists.isEmpty() && searchAlbums.isEmpty() && searchPlaylists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = searchError,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    when (selectedFilter) {
        "All" -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Result Highlight (ViVi Signature)
                val topArtist = searchArtists.firstOrNull()
                if (topArtist != null) {
                    item(key = "top_result_artist") {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "TOP RESULT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val shelfItem = ShelfItem(
                                            id = topArtist.id,
                                            title = topArtist.title,
                                            subtitle = "Artist",
                                            imageUrls = listOf(topArtist.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                            type = ItemType.ARTIST
                                        )
                                        onPlaylistSelect(shelfItem)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = topArtist.thumbnail?.let { getHighResThumbnail(it) },
                                        contentDescription = topArtist.title,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainer),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = topArtist.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Artist",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Songs Section
                if (searchResults.isNotEmpty()) {
                    item(key = "songs_header") {
                        NavigationTitle(
                            title = "Songs",
                            label = "${searchResults.size} results",
                            onPlayAllClick = {
                                onSongSelect(searchResults.first(), searchResults)
                            }
                        )
                    }

                    items(searchResults.take(5), key = { "song_${it.id}" }) { song ->
                        MuziSongRow(
                            song = song,
                            onClick = { onSongSelect(song, searchResults) },
                            onLongClick = { onSongActionClick?.invoke(song, searchResults) },
                            onActionClick = { onSongActionClick?.invoke(song, searchResults) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Artists Section (Circular Avatars)
                if (searchArtists.isNotEmpty()) {
                    item(key = "artists_header") {
                        NavigationTitle(
                            title = "Artists",
                            label = "${searchArtists.size} results"
                        )
                    }
                    item(key = "artists_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(searchArtists, key = { it.id }) { artist ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(96.dp)
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
                                    AsyncImage(
                                        model = artist.thumbnail?.let { getHighResThumbnail(it) },
                                        contentDescription = artist.title,
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainer),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = artist.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Artist",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Albums Section (14dp Cards)
                if (searchAlbums.isNotEmpty()) {
                    item(key = "albums_header") {
                        NavigationTitle(
                            title = "Albums",
                            label = "${searchAlbums.size} results"
                        )
                    }
                    item(key = "albums_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(searchAlbums, key = { it.id }) { album ->
                                val shelfItem = ShelfItem(
                                    id = album.id,
                                    title = album.title,
                                    subtitle = album.year?.toString() ?: "Album",
                                    imageUrls = listOf(album.thumbnail.let { getHighResThumbnail(it) }),
                                    type = ItemType.ALBUM
                                )
                                ShelfCard(
                                    item = shelfItem,
                                    onClick = { onPlaylistSelect(shelfItem) }
                                )
                            }
                        }
                    }
                }

                // Playlists Section (14dp Cards)
                if (searchPlaylists.isNotEmpty()) {
                    item(key = "playlists_header") {
                        NavigationTitle(
                            title = "Playlists",
                            label = "${searchPlaylists.size} results"
                        )
                    }
                    item(key = "playlists_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(searchPlaylists, key = { it.id }) { playlist ->
                                val shelfItem = ShelfItem(
                                    id = playlist.id,
                                    title = playlist.title,
                                    subtitle = playlist.author?.name ?: "Playlist",
                                    imageUrls = listOf(playlist.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                                    type = ItemType.PLAYLIST
                                )
                                ShelfCard(
                                    item = shelfItem,
                                    onClick = { onPlaylistSelect(shelfItem) }
                                )
                            }
                        }
                    }
                }
            }
        }

        "Songs", "Videos" -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    NavigationTitle(
                        title = if (selectedFilter == "Songs") "Songs" else "Videos",
                        label = "${searchResults.size} results",
                        onPlayAllClick = {
                            if (searchResults.isNotEmpty()) onSongSelect(searchResults.first(), searchResults)
                        }
                    )
                }
                items(searchResults, key = { it.id }) { song ->
                    MuziSongRow(
                        song = song,
                        onClick = { onSongSelect(song, searchResults) },
                        onLongClick = { onSongActionClick?.invoke(song, searchResults) },
                        onActionClick = { onSongActionClick?.invoke(song, searchResults) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        "Artists" -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(searchArtists, key = { it.id }) { artist ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
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
                        AsyncImage(
                            model = artist.thumbnail?.let { getHighResThumbnail(it) },
                            contentDescription = artist.title,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = artist.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        "Albums" -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(searchAlbums, key = { it.id }) { album ->
                    val shelfItem = ShelfItem(
                        id = album.id,
                        title = album.title,
                        subtitle = album.year?.toString() ?: "Album",
                        imageUrls = listOf(album.thumbnail.let { getHighResThumbnail(it) }),
                        type = ItemType.ALBUM
                    )
                    ShelfCard(
                        item = shelfItem,
                        onClick = { onPlaylistSelect(shelfItem) }
                    )
                }
            }
        }

        "Playlists" -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(searchPlaylists, key = { it.id }) { playlist ->
                    val shelfItem = ShelfItem(
                        id = playlist.id,
                        title = playlist.title,
                        subtitle = playlist.author?.name ?: "Playlist",
                        imageUrls = listOf(playlist.thumbnail?.let { getHighResThumbnail(it) } ?: ""),
                        type = ItemType.PLAYLIST
                    )
                    ShelfCard(
                        item = shelfItem,
                        onClick = { onPlaylistSelect(shelfItem) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreenSkeleton() {
    val brush = ShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section 1: Songs List Skeleton
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            repeat(4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
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
                    .height(20.dp)
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
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .width(65.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }

        // Section 3: 14dp Cards (Albums / Playlists)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(3) {
                    Column(
                        modifier = Modifier.width(136.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .clip(RoundedCornerShape(14.dp))
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