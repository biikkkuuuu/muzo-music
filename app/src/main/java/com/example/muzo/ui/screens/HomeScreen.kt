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
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onOpenRecognition: () -> Unit = {},
    onItemLongClick: ((ShelfItem, List<ShelfItem>) -> Unit)? = null
) {
    var selectedMoodChip by remember { mutableStateOf<String?>(null) }
    val moodChips = listOf("Workout", "Commute", "Feel good", "Romance", "Party", "Chill", "Focus", "Gaming")

    val lazyListState = rememberLazyListState()

    // ViVi dynamic seed: Update random seed on refresh to smoothly exchange section positions
    var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            randomSeed = System.currentTimeMillis()
        }
    }

    // Dynamic section reordering matching ViVi algorithm
    val orderedShelves = remember(homeShelves, randomSeed) {
        if (homeShelves.isEmpty()) return@remember emptyList()
        homeShelves.sortedByDescending { shelf ->
            val sectionRandom = kotlin.random.Random(randomSeed + shelf.id.hashCode())
            val base = when (shelf.id) {
                "keep_listening" -> 1000
                "shelf_new_releases" -> 850
                "shelf_top_artists" -> 700
                "shelf_rain_therapy" -> 600
                "shelf_dancing" -> 550
                "shelf_featured" -> 450
                "shelf_community" -> 400
                "shelf_nostalgic" -> 350
                "mood_and_genres" -> 150
                else -> 300
            }
            val modifier = when (shelf.id) {
                "keep_listening" -> sectionRandom.nextInt(-50, 100)
                "shelf_new_releases" -> sectionRandom.nextInt(-150, 200)
                else -> sectionRandom.nextInt(-250, 300)
            }
            base + modifier
        }
    }

    val featuredHeroItems = remember(homeShelves) {
        val candidate = homeShelves.firstOrNull { it.id == "shelf_new_releases" || it.id == "shelf_featured" }?.items
            ?: homeShelves.firstOrNull { it.id != "keep_listening" && it.items.isNotEmpty() }?.items
            ?: homeShelves.firstOrNull { it.items.isNotEmpty() }?.items
            ?: emptyList()
        candidate.take(6)
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerBarHeight = 56.dp
    val totalHeaderHeight = statusBarHeight + headerBarHeight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = {
                randomSeed = System.currentTimeMillis()
                onRefresh()
            },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = totalHeaderHeight + 6.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            val hasRemote = homeShelves.any { it.id != "keep_listening" }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = totalHeaderHeight + 6.dp,
                    bottom = 160.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. ViVi Mood & Genre Filter ChipsRow
                item(key = "vivi_chips_row") {
                    if (homeShelves.isEmpty()) {
                        com.example.muzo.ui.components.ChipsRowSkeleton()
                    } else {
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
                }

                // 2. Material 3 Expressive Hero Carousel (Echo-Style Hero Banner)
                if (featuredHeroItems.isNotEmpty()) {
                    item(key = "hero_expressive_carousel") {
                        com.example.muzo.ui.components.HeroExpressiveCarousel(
                            featuredItems = featuredHeroItems,
                            onSongSelect = onSongSelect,
                            onPlaylistSelect = onPlaylistSelect
                        )
                    }
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
                                onPlayAllClick = {
                                    val allSongs = keepListeningShelf.items.filter { it.type == ItemType.SONG }.map {
                                        SongItem(
                                            id = it.id,
                                            title = it.title,
                                            artists = listOf(Artist(name = it.subtitle, id = null)),
                                            album = null,
                                            duration = 0,
                                            thumbnail = it.imageUrls.firstOrNull() ?: ""
                                        )
                                    }
                                    if (allSongs.isNotEmpty()) {
                                        onSongSelect(allSongs.first(), allSongs)
                                    }
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
                    // 2. Dynamic Shelves (ViVi NavigationTitle + 14dp Cards + Community Cards)
                    items(
                        items = orderedShelves,
                        key = { it.id }
                    ) { shelf ->
                        // If community playlist shelf, render ViVi CommunityPlaylistCard
                        if (shelf.id == "shelf_community" && shelf.items.size >= 3) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                com.example.muzo.ui.components.NavigationTitle(
                                    title = shelf.title,
                                    label = shelf.subtitle,
                                    onClick = if (shelf.seeAllRoute != null) { { onSeeAllClick(shelf) } } else null
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item(key = "community_card") {
                                        com.example.muzo.ui.components.CommunityPlaylistCard(
                                            shelf = shelf,
                                            onClick = { onSeeAllClick(shelf) },
                                            onSongClick = { songItem ->
                                                val allSongs = shelf.items.filter { it.type == ItemType.SONG }.map {
                                                    SongItem(
                                                        id = it.id,
                                                        title = it.title,
                                                        artists = listOf(Artist(name = it.subtitle, id = null)),
                                                        album = null,
                                                        duration = 0,
                                                        thumbnail = it.imageUrls.firstOrNull() ?: ""
                                                    )
                                                }
                                                val selected = SongItem(
                                                    id = songItem.id,
                                                    title = songItem.title,
                                                    artists = listOf(Artist(name = songItem.subtitle, id = null)),
                                                    album = null,
                                                    duration = 0,
                                                    thumbnail = songItem.imageUrls.firstOrNull() ?: ""
                                                )
                                                onSongSelect(selected, if (allSongs.isNotEmpty()) allSongs else listOf(selected))
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
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
                                onPlayAllClick = {
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
                                    if (allSongsInShelf.isNotEmpty()) {
                                        onSongSelect(allSongsInShelf.first(), allSongsInShelf)
                                    }
                                },
                                onItemLongClick = { item ->
                                    onItemLongClick?.invoke(item, shelf.items)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ViVi 1:1 Top Bar: Circular App Icon + Bold "Music" Title + Action Icons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.82f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerBarHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ViVi App Icon + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = "Music",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp
                    )
                }

                // Actions: Refresh (Echo Setting format), History, Equalizer/Charts, Settings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val refreshRotation by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isRefreshing) 360f else 0f,
                        animationSpec = if (isRefreshing) {
                            androidx.compose.animation.core.infiniteRepeatable(
                                animation = androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing)
                            )
                        } else {
                            androidx.compose.animation.core.tween(300)
                        },
                        label = "homeRefreshRotation"
                    )

                    // Refresh Button (Echo Setting Icon Format: Squircle with primary tint)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .clickable {
                                randomSeed = System.currentTimeMillis()
                                onRefresh()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Feed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = refreshRotation }
                        )
                    }

                    IconButton(onClick = onOpenRecognition) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Identify Music",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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

        // Status Bar Protection Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight + 12.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to MaterialTheme.colorScheme.background,
                        0.7f to MaterialTheme.colorScheme.background,
                        1.0f to Color.Transparent
                    )
                )
        )
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
