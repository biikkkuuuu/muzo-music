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
    var chipsVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -10f) {
                    // Scrolling down into feed -> collapse/hide mood chips
                    chipsVisible = false
                } else if (delta > 10f) {
                    // Scrolling up towards top -> expand/show mood chips
                    chipsVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val isAtTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset <= 15
        }
    }
    val showChips = isAtTop || chipsVisible

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF08080A))
                    .statusBarsPadding()
            ) {
                // 1. App Header (Echo Music / MUZI, History, Settings, Search)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MUZI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onCategoryClick("History") }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = Color.LightGray
                            )
                        }
                        IconButton(onClick = onOpenSearch) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.LightGray
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.LightGray
                            )
                        }
                    }
                }

                // 2. Horizontally scrollable Mood Chips (Collapses smoothly on scroll)
                AnimatedVisibility(
                    visible = showChips,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 200)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 180)
                    )
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(moodChips) { chip ->
                            val isSelected = selectedMoodChip == chip
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFF2F60FF) else Color(0xFF1E1E24),
                                modifier = Modifier.clickable {
                                    selectedMoodChip = if (isSelected) null else chip
                                    onCategoryClick(chip)
                                }
                            ) {
                                Text(
                                    text = chip,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color.LightGray
                                )
                            }
                        }
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(homeShelves, key = { it.id }) { shelf ->
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
                                    ItemType.PLAYLIST, ItemType.ALBUM -> {
                                        onPlaylistSelect(item)
                                    }
                                    ItemType.CHART, ItemType.ARTIST -> {
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
