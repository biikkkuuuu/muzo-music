package com.example.muzo.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.mock.MockPlayerViewModel
import com.example.muzo.mock.MockSong
import com.example.muzo.theme.GoogleSansFlex
import com.example.muzo.ui.component.PlayingIndicator
import com.example.muzo.ui.component.SquigglySlider
import com.example.muzo.ui.utils.bounceClick
import kotlinx.coroutines.launch

/**
 * Metrolist Floating MiniPlayer dock matching Screenshot 2.
 * Features circular artwork surrounded by real-time progress ring,
 * marquee text, and solid pastel circular play button.
 */
@Composable
fun MetrolistMiniPlayer(
    viewModel: MockPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPos by viewModel.currentPositionMs.collectAsState()
    val duration by viewModel.durationMs.collectAsState()

    val progress = if (duration > 0L) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .bounceClick(scaleDown = 0.98f, onClick = { viewModel.openPlayer() }),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Thumbnail with Progress Ring Stroke
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeWidth = 2.5.dp
                )

                AsyncImage(
                    model = currentSong.thumbnailUrl,
                    contentDescription = currentSong.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentSong.title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong.artist,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Previous Button
            IconButton(
                onClick = { viewModel.engine.playPrevious() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Circular Solid Primary Play/Pause Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .bounceClick(scaleDown = 0.90f, onClick = { viewModel.engine.togglePlayPause() }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Next Button
            IconButton(
                onClick = { viewModel.engine.playNext() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Metrolist Full Expandable Player Bottom Sheet.
 * Complete with album artwork glow, real-time SquigglySlider,
 * live synced lyrics view, and queue sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetrolistFullPlayer(
    viewModel: MockPlayerViewModel
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPos by viewModel.currentPositionMs.collectAsState()
    val duration by viewModel.durationMs.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val isRepeat by viewModel.isRepeat.collectAsState()
    val isLyricsOpen by viewModel.isLyricsOpen.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()

    var isLiked by remember(currentSong.id) { mutableStateOf(false) }

    val sliderFraction = if (duration > 0L) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    ModalBottomSheet(
        onDismissRequest = { viewModel.closePlayer() },
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Down Chevron, Album Title, More Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closePlayer() }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM",
                        fontFamily = GoogleSansFlex,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = currentSong.album,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { /* More options */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Content: Either Album Art or Live Synced Lyrics or Queue
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLyricsOpen -> {
                        MetrolistLiveLyricsView(viewModel = viewModel)
                    }
                    isQueueOpen -> {
                        MetrolistQueueView(viewModel = viewModel)
                    }
                    else -> {
                        // Large Rounded Artwork with Ambient Glow
                        Box(contentAlignment = Alignment.Center) {
                            // Soft ambient glow underneath
                            AsyncImage(
                                model = currentSong.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(290.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .graphicsLayer { alpha = 0.35f }
                            )

                            AsyncImage(
                                model = currentSong.thumbnailUrl,
                                contentDescription = currentSong.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(280.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track Title, Artist & Like Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.artist,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { isLiked = !isLiked }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Squiggly Waveform Seekbar
            SquigglySlider(
                value = sliderFraction,
                onValueChange = { frac ->
                    viewModel.engine.seekTo((frac * duration).toLong())
                },
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth()
            )

            // Timers: Elapsed / Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentPos),
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-" + formatMs((duration - currentPos).coerceAtLeast(0L)),
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls Row: Shuffle, Previous, Big Play/Pause, Next, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.engine.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { viewModel.engine.playPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Big Solid Primary Play/Pause Button with Spring Physics
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .bounceClick(scaleDown = 0.90f, onClick = { viewModel.engine.togglePlayPause() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.engine.playNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(onClick = { viewModel.engine.toggleRepeat() }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Accessories: Lyrics, Audio Devices, Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics Toggle Capsule
                FilterChip(
                    selected = isLyricsOpen,
                    onClick = { viewModel.toggleLyrics() },
                    label = {
                        Text(
                            text = "Lyrics",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Lyrics",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Queue Toggle Capsule
                FilterChip(
                    selected = isQueueOpen,
                    onClick = { viewModel.toggleQueue() },
                    label = {
                        Text(
                            text = "Up Next",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

/**
 * Metrolist Live Synced Lyrics View:
 * Highlights lyrics line-by-line as the coroutine timer advances.
 * Tapping any line seeks immediately to that timestamp!
 */
@Composable
fun MetrolistLiveLyricsView(
    viewModel: MockPlayerViewModel
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val activeIdx by viewModel.activeLyricIndex.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0 && activeIdx < currentSong.lyrics.size) {
            coroutineScope.launch {
                listState.animateScrollToItem(activeIdx)
            }
        }
    }

    if (currentSong.lyrics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No lyrics available for this track",
                fontFamily = GoogleSansFlex,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 40.dp)
        ) {
            itemsIndexed(currentSong.lyrics) { index, lyricLine ->
                val isActive = index == activeIdx
                val isPast = index < activeIdx

                val targetColor = when {
                    isActive -> MaterialTheme.colorScheme.primary
                    isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
                }

                Text(
                    text = lyricLine.text,
                    fontFamily = GoogleSansFlex,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (isActive) 22.sp else 18.sp,
                    color = targetColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(scaleDown = 0.98f, onClick = {
                            viewModel.engine.seekTo(lyricLine.timeMs)
                        })
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}

/**
 * Metrolist Queue Sheet View showing upcoming tracks with playing indicator.
 */
@Composable
fun MetrolistQueueView(
    viewModel: MockPlayerViewModel
) {
    val queue by viewModel.queue.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        itemsIndexed(queue) { index, song ->
            val isCurrent = song.id == currentSong.id

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(onClick = { viewModel.engine.playSong(song) }),
                shape = RoundedCornerShape(14.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontFamily = GoogleSansFlex,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            fontFamily = GoogleSansFlex,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isCurrent) {
                        PlayingIndicator(isPlaying = isPlaying)
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
