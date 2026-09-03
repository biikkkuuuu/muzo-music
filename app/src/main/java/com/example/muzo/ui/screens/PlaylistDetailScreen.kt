package com.example.muzo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.ui.components.CollageCover
import com.example.muzo.ui.components.ShelfCard
import com.example.muzo.ui.components.SingleCover
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistItem,
    songs: List<SongItem>,
    relatedPlaylists: List<ShelfItem> = emptyList(),
    isLoading: Boolean,
    onBack: () -> Unit,
    onSearchClick: () -> Unit = {},
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    onPlayAll: () -> Unit = {},
    onRelatedPlaylistClick: (ShelfItem) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist.title ?: "Playlist",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF08080A)
                )
            )
        },
        containerColor = Color(0xFF08080A)
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF6B8AFD))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // 1. Hero Artwork Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val thumb = playlist.thumbnail?.let { getHighResThumbnail(it) } ?: ""
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1E24))
                        ) {
                            if (playlist.title?.contains("community", ignoreCase = true) == true) {
                                CollageCover(imageUrls = listOf(thumb, thumb, thumb, thumb))
                            } else {
                                SingleCover(imageUrl = thumb)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = playlist.title ?: "Untitled Playlist",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val songCountText = if (songs.isNotEmpty()) "${songs.size} songs" else (playlist.songCountText ?: "Playlist")
                        Text(
                            text = songCountText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Action Buttons Row ([Save], [Play], [Share/Shuffle])
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {},
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = "Save", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save", fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    if (songs.isNotEmpty()) {
                                        onSongSelect(songs[0], songs)
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2F60FF),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            OutlinedButton(
                                onClick = {
                                    if (songs.isNotEmpty()) {
                                        val shuffled = songs.shuffled()
                                        onSongSelect(shuffled[0], shuffled)
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 3. Tracklist Items
                itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongSelect(song, songs) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = getHighResThumbnail(song.thumbnail),
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val artistName = song.artists.joinToString(", ") { it.name }
                            val durationSec = song.duration
                            val durationText = if (durationSec != null && durationSec > 0) {
                                val m = durationSec / 60
                                val s = durationSec % 60
                                String.format(" • %02d:%02d", m, s)
                            } else ""
                            Text(
                                text = "$artistName$durationText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.LightGray
                            )
                        }
                    }
                }

                // 4. "Related Playlist" Shelf at Bottom (from the video!)
                if (relatedPlaylists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Related Playlist",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B8AFD),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(relatedPlaylists) { item ->
                                ShelfCard(item = item, onClick = { onRelatedPlaylistClick(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}