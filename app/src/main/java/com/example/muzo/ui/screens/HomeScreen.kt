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
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.muzo.data.model.HomeShelf
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.data.model.ShelfType
import com.example.muzo.ui.components.HomeScreenSkeleton
import com.example.muzo.ui.components.PlaylistShelfRow
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
    onOpenSearch: () -> Unit = {}
) {
    var selectedMoodChip by remember { mutableStateOf<String?>(null) }
    val moodChips = listOf("Workout", "Commute", "Feel good", "Romance", "Party", "Chill", "Focus", "Gaming")

    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            // App Header (Fixed height, static background - 0 layout re-measurement during vertical scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF08080A))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
        },
        containerColor = Color(0xFF08080A)
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val hasRemote = homeShelves.any { it.id != "keep_listening" }

            if (isRefreshing || !hasRemote) {
                HomeScreenSkeleton()
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
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

                    // Top Hero Carousel (First shelf or quick picks featured prominently)
                    val featuredShelf = homeShelves.firstOrNull()
                    if (featuredShelf != null && featuredShelf.items.isNotEmpty()) {
                        item(key = "hero_carousel") {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                                Text(
                                    text = "Featured for you",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                                com.example.muzo.ui.components.HeroCarousel(
                                    items = featuredShelf.items,
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
                                                val allSongs = featuredShelf.items.filter { it.type == ItemType.SONG }.map {
                                                    SongItem(
                                                        id = it.id,
                                                        title = it.title,
                                                        artists = listOf(Artist(name = it.subtitle, id = null)),
                                                        album = null,
                                                        duration = 0,
                                                        thumbnail = it.imageUrls.firstOrNull() ?: ""
                                                    )
                                                }
                                                onSongSelect(songItem, allSongs)
                                            }
                                            ItemType.PLAYLIST, ItemType.ALBUM, ItemType.ARTIST -> {
                                                onPlaylistSelect(item)
                                            }
                                            ItemType.CHART -> {
                                                onCategoryClick(item.title)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Remaining shelves
                    val remainingShelves = if (homeShelves.size > 1) homeShelves.drop(1) else homeShelves
                    items(remainingShelves, key = { it.id }) { shelf ->
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
                            }
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
