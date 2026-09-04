package com.example.muzo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.muzo.core.formatTime
import com.example.muzo.core.getHighResThumbnail
import com.music.innertube.models.SongItem

import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Echo-Music style Full Player Sheet.
 * Features:
 * - Centered "Now Playing" top bar with collapse button
 * - Large 300dp rounded album art (24dp corners)
 * - Track Title, Artist, with squircle Download & Animated Heart Like button
 * - Animated Squiggly waveform seekbar
 * - Signature Wide Pill Play/Pause button flanked by Skip controls
 * - Bottom utility toolbar (Sleep Timer, Equalizer, Shuffle, Repeat, Queue)
 */
@Composable
fun FullPlayerSheet(
    song: SongItem,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    hasPrev: Boolean,
    hasNext: Boolean,
    queueCount: Int,
    isLiked: Boolean = false,
    sleepTimer: com.example.muzo.playback.SleepTimer? = null,
    equalizerController: com.example.muzo.playback.EqualizerController? = null,
    audioSessionId: Int = 0,
    queue: List<SongItem> = emptyList(),
    currentIndex: Int = -1,
    onLikeToggle: () -> Unit = {},
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onQueueSongSelect: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onRemoveQueueItem: (Int) -> Unit = {},
    onClearUpcomingQueue: () -> Unit = {}
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }

    val isSleepTimerActive = sleepTimer?.isActive?.collectAsStateWithLifecycle()?.value ?: false
    val sleepRemainingMs = sleepTimer?.remainingTimeMs?.collectAsStateWithLifecycle()?.value ?: 0L
    val pauseWhenSongEnd = sleepTimer?.pauseWhenSongEnd?.collectAsStateWithLifecycle()?.value ?: false
    val isEqEnabled = equalizerController?.isEnabled?.collectAsStateWithLifecycle()?.value ?: false

    val currentProgress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale"
    )

    var isShuffleOn by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) } // 0 = off, 1 = all, 2 = one

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar: Down Chevron, Centered "Now Playing", Cast/Menu icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = {}, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Default.Cast,
                        contentDescription = "Cast",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Large Album Artwork with 24dp rounded corners and soft shadow
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
                    .fillMaxWidth(0.92f)
                    .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.6f))
                    .clip(RoundedCornerShape(24.dp))
            ) {
                AsyncImage(
                    model = getHighResThumbnail(song.thumbnail),
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Track Info Row (Title + Artist on left; Download + Squircle Like on right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = song.artists.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Download icon button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {}
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Squircle Like Button with spring bounce
                    Surface(
                        shape = CircleShape,
                        color = if (isLiked) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer {
                                scaleX = likeScale
                                scaleY = likeScale
                            }
                            .clip(CircleShape)
                            .clickable { onLikeToggle() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isLiked) "Liked" else "Like",
                                tint = if (isLiked) Color(0xFFFF3B30) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Squiggly Waveform Seekbar
            Column(modifier = Modifier.fillMaxWidth()) {
                SquigglySlider(
                    value = if (isDragging) dragProgress else currentProgress,
                    onValueChange = {
                        isDragging = true
                        dragProgress = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onSeek((dragProgress * duration).toLong())
                    },
                    isPlaying = isPlaying,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(if (isDragging) (dragProgress * duration).toLong() else currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Main Playback Controls: Circular Prev, Signature Wide Pill Play/Pause, Circular Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    enabled = hasPrev,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(30.dp),
                        tint = if (hasPrev) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // Echo-Music Signature Wide Pill Play/Pause button
                Surface(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .width(130.dp)
                        .height(64.dp)
                        .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    enabled = hasNext,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(30.dp),
                        tint = if (hasNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Bottom Utility Toolbar (Lyrics/Queue, Sleep Timer, Equalizer, Shuffle, Repeat, More)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = "Lyrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { showSleepTimerDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (isSleepTimerActive) Color(0xFF6B9DFE) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isSleepTimerActive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6B9DFE))
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showEqualizerSheet = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = if (isEqEnabled) Color(0xFF6B9DFE) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isEqEnabled) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6B9DFE))
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { isShuffleOn = !isShuffleOn },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { repeatMode = (repeatMode + 1) % 3 },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            2 -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (showQueueSheet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog && sleepTimer != null) {
            SleepTimerDialog(
                sleepTimer = sleepTimer,
                onDismissRequest = { showSleepTimerDialog = false }
            )
        }

        // Queue Management Bottom Sheet
        if (showQueueSheet) {
            QueueBottomSheet(
                queue = queue,
                currentIndex = currentIndex,
                onDismiss = { showQueueSheet = false },
                onSongSelect = { idx ->
                    onQueueSongSelect(idx)
                    showQueueSheet = false
                },
                onMoveItem = onMoveQueueItem,
                onRemoveItem = onRemoveQueueItem,
                onClearUpcoming = onClearUpcomingQueue
            )
        }

        // Built-in Equalizer Bottom Sheet
        if (showEqualizerSheet && equalizerController != null) {
            EqualizerBottomSheet(
                equalizerController = equalizerController,
                audioSessionId = audioSessionId,
                onDismiss = { showEqualizerSheet = false }
            )
        }
    }
}