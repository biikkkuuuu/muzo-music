package com.example.muzo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muzo.mock.MockHomeViewModel
import com.example.muzo.mock.MockPlayerViewModel
import com.example.muzo.theme.GoogleSansFlex
import com.example.muzo.ui.component.EchoLoadingIndicator
import com.example.muzo.ui.component.MetrolistChipsRow
import com.example.muzo.ui.component.MetrolistSongCard
import com.example.muzo.ui.component.MoodTile
import com.example.muzo.ui.component.NavigationTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: MockHomeViewModel,
    playerViewModel: MockPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val shelves by homeViewModel.shelves.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()
    val selectedChip by homeViewModel.selectedFilterChip.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()

    val chips = listOf("Workout", "Feel good", "Energize", "Relax", "Romance", "Focus")

    val moodPairs = listOf(
        "Chill" to "Focus",
        "Commute" to "Gaming",
        "Energize" to "Party",
        "Feel good" to "Romance"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { homeViewModel.refresh() },
            indicator = {
                if (pullRefreshState.distanceFraction > 0f || isRefreshing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EchoLoadingIndicator(
                            isRefreshing = isRefreshing,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 76.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Filter Chips Row
                item(key = "chips_row") {
                    MetrolistChipsRow(
                        chips = chips,
                        selectedChip = selectedChip,
                        onChipSelect = { homeViewModel.selectFilterChip(it) }
                    )
                }

                // 2. Metrolist "Mood and Genres" 2-Column Grid (Screenshot 2 Parity)
                item(key = "mood_and_genres_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Open Mood and Genres */ }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mood and Genres",
                                fontFamily = GoogleSansFlex,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 22.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Mood and Genres",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            moodPairs.forEach { (first, second) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MoodTile(
                                        title = first,
                                        modifier = Modifier.weight(1f),
                                        onClick = { homeViewModel.selectFilterChip(first) }
                                    )
                                    MoodTile(
                                        title = second,
                                        modifier = Modifier.weight(1f),
                                        onClick = { homeViewModel.selectFilterChip(second) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Dynamic Shelves (Rain Therapy, Feel Good, Keep Listening, etc.)
                items(shelves, key = { it.id }) { shelf ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        NavigationTitle(
                            title = shelf.title,
                            label = shelf.subtitle,
                            onClick = { /* Navigate to playlist */ }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(shelf.songs, key = { it.id }) { song ->
                                MetrolistSongCard(
                                    song = song,
                                    onClick = {
                                        playerViewModel.engine.playSong(song, shelf.songs)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top App Bar Header with Metrolist Branding & Actions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to MaterialTheme.colorScheme.background,
                        0.85f to MaterialTheme.colorScheme.background,
                        1.0f to Color.Transparent
                    )
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Metrolist",
                    fontFamily = GoogleSansFlex,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { /* History */ }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { /* Charts */ }) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Charts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { playerViewModel.openThemeSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Themes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { playerViewModel.openSettings() }) {
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
