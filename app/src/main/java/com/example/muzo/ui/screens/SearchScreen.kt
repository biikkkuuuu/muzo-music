package com.example.muzo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    triggerSearch: Boolean,
    onSearchHandled: () -> Unit,
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    statusText: String
) {
    var searchResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var selectedSearchTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var searchMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val performSearch: (String) -> Unit = { q ->
        if (q.isNotBlank()) {
            isLoading = true
            searchMsg = "Searching YouTube Music..."
            scope.launch(Dispatchers.IO) {
                try {
                    val response = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                    val items = response.getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        searchResults = items.take(30)
                        isLoading = false
                        searchMsg = if (searchResults.isEmpty()) "No songs found" else ""
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        searchMsg = "Search error: ${e.localizedMessage ?: e.javaClass.simpleName}"
                    }
                }
            }
        }
    }

    LaunchedEffect(triggerSearch) {
        if (triggerSearch && query.isNotBlank()) {
            performSearch(query)
            onSearchHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search YouTube Music...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Submit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { performSearch(query) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedSearchTab,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(selected = selectedSearchTab == 0, onClick = { selectedSearchTab = 0 }, text = { Text("Explore", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedSearchTab == 1, onClick = { selectedSearchTab = 1 }, text = { Text("Chart", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedSearchTab == 2, onClick = { selectedSearchTab = 2 }, text = { Text("Album", fontWeight = FontWeight.Bold) })
        }

        if (statusText.isNotBlank() || searchMsg.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (statusText.isNotBlank()) statusText else searchMsg,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.id }) { song ->
                    SearchSongTile(song = song, onClick = { onSongSelect(song, searchResults) })
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Moods & moments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val moments = listOf(
                    "Chill" to "Commute",
                    "Energize" to "Feel good",
                    "Focus" to "Gaming",
                    "Party" to "Romance",
                    "Sad" to "Sleep",
                    "Workout" to "Romance"
                )
                items(moments) { (m1, m2) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MoodTile(title = m1, modifier = Modifier.weight(1f), onClick = {
                            onQueryChange(m1)
                            performSearch(m1)
                        })
                        MoodTile(title = m2, modifier = Modifier.weight(1f), onClick = {
                            onQueryChange(m2)
                            performSearch(m2)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSongTile(song: SongItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = getHighResThumbnail(song.thumbnail),
                contentDescription = song.title,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}