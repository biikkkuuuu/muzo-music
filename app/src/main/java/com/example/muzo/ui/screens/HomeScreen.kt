package com.example.muzo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.muzo.data.model.HomeShelf
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloatAsState
import com.example.muzo.ui.components.HomeScreenSkeleton
import com.example.muzo.ui.components.PlaylistShelfRow
import com.example.muzo.ui.components.ShelfRowSkeleton
import com.example.muzo.ui.components.ShimmerBrush
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeShelves: List<HomeShelf>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    onPlaylistSelect: (ShelfItem) -> Unit,
    onSeeAllClick: (HomeShelf) -> Unit,
    onCategoryClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onItemLongClick: ((ShelfItem, List<ShelfItem>) -> Unit)? = null
) {
    var selectedMoodChip by remember { mutableStateOf<String?>(null) }
    val moodChips = listOf("Workout", "Commute", "Feel good", "Romance", "Party", "Chill", "Focus", "Gaming")

    val lazyListState = rememberLazyListState()

    // Echo-Music pattern: Update random seed on refresh to smoothly exchange section positions
    var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            randomSeed = System.currentTimeMillis()
        }
    }

    // Dynamic section reordering matching Echo-Music
    val orderedShelves = remember(homeShelves, randomSeed) {
        if (homeShelves.isEmpty()) return@remember emptyList()
        homeShelves.sortedByDescending { shelf ->
            val sectionRandom = kotlin.random.Random(randomSeed + shelf.id.hashCode())
            val base = when {
                shelf.id == "keep_listening" -> 1000
                shelf.id == "shelf_0" -> 900 // New releases
                shelf.id == "shelf_6" -> 800 // Top Artists
                shelf.id == "shelf_4" -> 700 // Featured playlists
                shelf.id == "shelf_1" -> 600 // Rain therapy
                shelf.id == "shelf_2" -> 500 // Dancing on your own
                shelf.id == "shelf_3" -> 400 // Trending community
                shelf.id == "shelf_5" -> 350 // Retro nostalgia
                shelf.id == "mood_and_genres" -> 150
                else -> 300
            }
            val modifier = when (shelf.id) {
                "keep_listening" -> sectionRandom.nextInt(-80, 120)
                else -> sectionRandom.nextInt(-350, 350)
            }
            base + modifier
        }
    }

    var isHeaderVisible by rememberSaveable { mutableStateOf(true) }
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0) {
                    if (scrollAccumulator > 0) scrollAccumulator = 0f
                    scrollAccumulator += delta
                    if (scrollAccumulator < -25f && isHeaderVisible) {
                        isHeaderVisible = false
                        scrollAccumulator = 0f
                    }
                } else if (delta > 0) {
                    if (scrollAccumulator < 0) scrollAccumulator = 0f
                    scrollAccumulator += delta
                    if (scrollAccumulator > 25f && !isHeaderVisible) {
                        isHeaderVisible = true
                        scrollAccumulator = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index == 0 && offset < 30) {
                    isHeaderVisible = true
                    scrollAccumulator = 0f
                }
            }
    }

    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerBarHeight = 56.dp
    val totalHeaderHeight = statusBarHeight + headerBarHeight
    val totalHeaderHeightPx = with(density) { totalHeaderHeight.toPx() }

    val headerOffsetProgress by animateFloatAsState(
        targetValue = if (isHeaderVisible) 0f else -1f,
        animationSpec = tween(
            durationMillis = 350,
            easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        ),
        label = "headerOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080A))
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                randomSeed = System.currentTimeMillis()
                onRefresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            val hasRemote = homeShelves.any { it.id != "keep_listening" }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = totalHeaderHeight + 8.dp,
                    bottom = 150.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Mood & Genre Filter Chips inside LazyColumn (Echo-Music pattern - glides smoothly with feed)
                item(key = "mood_chips_row") {
                    com.example.muzo.ui.components.AnimatedChipsRow(
                        chips = moodChips,
                        selectedChip = selectedMoodChip,
                        onChipSelect = { chip ->
                            val isSelected = selectedMoodChip == chip
                            selectedMoodChip = if (isSelected) null else chip
                            onCategoryClick(chip)
                        }
                    )
                }

                // If remote shelves are still loading:
                if (!hasRemote || homeShelves.isEmpty()) {
                    // Show Keep Listening if user already has local history
                    val keepListeningShelf = homeShelves.firstOrNull { it.id == "keep_listening" }
                    if (keepListeningShelf != null && keepListeningShelf.items.isNotEmpty()) {
                        item(key = "keep_listening") {
                            PlaylistShelfRow(
                                shelf = keepListeningShelf,
                                onItemClick = { item ->
                                    when (item.type) {
                                        ItemType.SONG -> {
                                            val songItem = SongItem(
                                                id = item.id,
                                                title = item.title,
                                                artists = listOf(Artist(name = item.subtitle, id = null)),
                                                album = null,
                                                duration = 0,
                                                thumbnail = item.imageUrls.firstOrNull() ?: ""
                                            )
                                            val allSongsInShelf = keepListeningShelf.items.filter { it.type == ItemType.SONG }.map {
                                                SongItem(
                                                    id = it.id,
                                                    title = it.title,
                                                    artists = listOf(Artist(name = it.subtitle, id = null)),
                                                    album = null,
                                                    duration = 0,
                                                    thumbnail = it.imageUrls.firstOrNull() ?: ""
                                                )
                                            }
                                            onSongSelect(songItem, allSongsInShelf)
                                        }
                                        else -> Unit
                                    }
                                },
                                onSeeAllClick = {
                                    onSeeAllClick(keepListeningShelf)
                                },
                                onItemLongClick = { item ->
                                    onItemLongClick?.invoke(item, keepListeningShelf.items)
                                }
                            )
                        }
                    }

                    // Shimmer skeleton shelves loading in real-time beneath it
                    item(key = "skeleton_shelves") {
                        val brush = ShimmerBrush()
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ShelfRowSkeleton(brush = brush, hasSubtitle = true)
                            ShelfRowSkeleton(brush = brush, hasSubtitle = false)
                            ShelfRowSkeleton(brush = brush, hasSubtitle = true)
                        }
                    }
                } else {
                    // 2. Dynamic Shelves (Smooth liquid scrolling, zero jitter)
                    items(
                        items = orderedShelves,
                        key = { it.id }
                    ) { shelf ->
                        PlaylistShelfRow(
                            shelf = shelf,
                            onItemClick = { item ->
                                when (item.type) {
                                    ItemType.SONG -> {
                                        val songItem = SongItem(
                                            id = item.id,
                                            title = item.title,
                                            artists = listOf(Artist(name = item.subtitle, id = null)),
                                            album = null,
                                            duration = 0,
                                            thumbnail = item.imageUrls.firstOrNull() ?: ""
                                        )
                                        val allSongsInShelf = shelf.items.filter { it.type == ItemType.SONG }.map {
                                            SongItem(
                                                id = it.id,
                                                title = it.title,
                                                artists = listOf(Artist(name = it.subtitle, id = null)),
                                                album = null,
                                                duration = 0,
                                                thumbnail = it.imageUrls.firstOrNull() ?: ""
                                            )
                                        }
                                        onSongSelect(songItem, allSongsInShelf)
                                    }
                                    ItemType.PLAYLIST, ItemType.ALBUM, ItemType.ARTIST -> {
                                        onPlaylistSelect(item)
                                    }
                                    ItemType.CHART -> {
                                        onCategoryClick(item.title)
                                    }
                                }
                            },
                            onSeeAllClick = {
                                onSeeAllClick(shelf)
                            },
                            onItemLongClick = { item ->
                                onItemLongClick?.invoke(item, shelf.items)
                            }
                        )
                    }
                }
            }
        }

        // Floating Top Header (GPU hardware translated - zero layout invalidations on LazyColumn!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = headerOffsetProgress * totalHeaderHeightPx
                    alpha = (1f + headerOffsetProgress).coerceIn(0f, 1f)
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF08080A),
                            Color(0xFF08080A).copy(alpha = 0.96f),
                            Color(0xFF08080A).copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerBarHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Muzi Music",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 24.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onCategoryClick("History") }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onCategoryClick("Charts") }) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Charts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Restored MoodTile for SearchScreen compatibility
@Composable
fun MoodTile(title: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
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
