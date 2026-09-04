package com.example.muzo.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.muzo.core.formatTime
import com.example.muzo.core.getHighResThumbnail
import com.music.innertube.models.SongItem
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Echo-Music style Ultra Premium Full Player Sheet.
 * Features:
 * - Dynamic Ambient Blur / Palette gradient background (AlbumGradient)
 * - Top bar with Playback Speed badge and Audio Output device switcher
 * - Large 300dp rounded album art with horizontal swipe gesture (Next/Prev)
 * - Double tap on artwork to seek (+10s / -10s) with floating badge
 * - Track Title, Artist, with High-Quality 320 kbps Codec Badge
 * - Squircle Download & Animated Heart Like button
 * - Animated Squiggly waveform seekbar
 * - Signature Wide Pill Play/Pause button flanked by Skip controls
 * - Bottom utility toolbar:
 *   - Synchronized / Plain Lyrics modal (Quote button)
 *   - Sleep Timer (with live countdown dot)
 *   - 5-Band Equalizer (with active dot)
 *   - Shuffle & Repeat modes
 *   - Queue Management (reorder & swipe delete)
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
    playbackSpeed: Float = 1.0f,
    onSpeedChange: (Float) -> Unit = {},
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

    // Dialog & sheet states
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showMediaInfoSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioOutputSheet by remember { mutableStateOf(false) }

    // Double tap seek feedback overlay
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(700)
            seekFeedbackText = null
        }
    }

    // Swipe gesture on artwork
    var swipeOffsetAccumulator by remember { mutableFloatStateOf(0f) }
    val animatedArtworkOffset by animateFloatAsState(
        targetValue = swipeOffsetAccumulator,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "artworkSwipeOffset"
    )

    val isSleepTimerActive = sleepTimer?.isActive?.collectAsStateWithLifecycle()?.value ?: false
    val isEqEnabled = equalizerController?.isEnabled?.collectAsStateWithLifecycle()?.value ?: false

    val currentProgress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale"
    )

    var isShuffleOn by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) } // 0 = off, 1 = all, 2 = one

    // Wrap with dynamic ambient palette background
    AlbumGradient(thumbnailUrl = getHighResThumbnail(song.thumbnail)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Bar: Down Chevron, Centered "Now Playing", Speed & Audio Output Controls
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
                            tint = Color.White
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
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed pill button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { showSpeedDialog = true }
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        // Audio Output device button
                        IconButton(
                            onClick = { showAudioOutputSheet = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Audio Output",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Large Album Artwork with 24dp rounded corners, Swipe Gestures, & Double-Tap seek
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .aspectRatio(1f)
                        .fillMaxWidth(0.92f)
                        .offset { IntOffset(animatedArtworkOffset.roundToInt(), 0) }
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.7f))
                        .clip(RoundedCornerShape(24.dp))
                        .pointerInput(song.id) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    swipeOffsetAccumulator = (swipeOffsetAccumulator + dragAmount).coerceIn(-180f, 180f)
                                },
                                onDragEnd = {
                                    if (swipeOffsetAccumulator < -80f && hasNext) {
                                        onNext()
                                    } else if (swipeOffsetAccumulator > 80f && hasPrev) {
                                        onPrev()
                                    }
                                    swipeOffsetAccumulator = 0f
                                },
                                onDragCancel = {
                                    swipeOffsetAccumulator = 0f
                                }
                            )
                        }
                        .pointerInput(song.id) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (offset.x > size.width / 2) {
                                        onSeek((currentPosition + 10000).coerceAtMost(duration))
                                        seekFeedbackText = "+10s"
                                    } else {
                                        onSeek((currentPosition - 10000).coerceAtLeast(0L))
                                        seekFeedbackText = "-10s"
                                    }
                                },
                                onTap = { onPlayPause() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = getHighResThumbnail(song.thumbnail),
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Double-tap Seek Feedback Bubble Overlay
                    this@Column.AnimatedVisibility(
                        visible = seekFeedbackText != null,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = seekFeedbackText ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Track Info Row (Title + Artist + Audio Quality Pill on left; Download + Like on right)
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
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = song.artists.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            // Audio Quality / Codec Badge (HQ 320 KBPS)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF263352).copy(alpha = 0.85f),
                                modifier = Modifier.clickable { showMediaInfoSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = Color(0xFF6B9DFE),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "320 KBPS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6B9DFE),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Download icon button
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    showMediaInfoSheet = true
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = Color.White,
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
                                Color.White.copy(alpha = 0.12f)
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
                                    tint = if (isLiked) Color(0xFFFF3B30) else Color.White,
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
                            activeTrackColor = Color(0xFF5B8DEF),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
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
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
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
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(30.dp),
                            tint = if (hasPrev) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }

                    // Echo-Music Signature Wide Pill Play/Pause button
                    Surface(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .width(130.dp)
                            .height(64.dp)
                            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF4C7DE8).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(32.dp),
                        color = Color(0xFF4C7DE8),
                        contentColor = Color.White
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
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(30.dp),
                            tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Bottom Utility Toolbar (Lyrics, Sleep Timer, Equalizer, Shuffle, Repeat, Queue)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Synchronized / Plain Lyrics
                    IconButton(
                        onClick = { showLyricsSheet = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (showLyricsSheet) Color(0xFF6B9DFE) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Sleep Timer
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (isSleepTimerActive) Color(0xFF6B9DFE) else Color.White.copy(alpha = 0.7f),
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

                    // 5-Band Equalizer
                    IconButton(
                        onClick = { showEqualizerSheet = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Equalizer",
                                tint = if (isEqEnabled) Color(0xFF6B9DFE) else Color.White.copy(alpha = 0.7f),
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

                    // Shuffle
                    IconButton(
                        onClick = { isShuffleOn = !isShuffleOn },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleOn) Color(0xFF6B9DFE) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Repeat
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
                            tint = if (repeatMode > 0) Color(0xFF6B9DFE) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Queue
                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (showQueueSheet) Color(0xFF6B9DFE) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }

            // 7. Modals & Bottom Sheets
            // Synchronized Lyrics Bottom Sheet
            if (showLyricsSheet) {
                LyricsBottomSheet(
                    song = song,
                    currentPosition = currentPosition,
                    onSeek = onSeek,
                    onDismiss = { showLyricsSheet = false }
                )
            }

            // Media Info & Stream Quality Sheet
            if (showMediaInfoSheet) {
                MediaInfoBottomSheet(
                    song = song,
                    onDismiss = { showMediaInfoSheet = false }
                )
            }

            // Playback Speed Controller Dialog
            if (showSpeedDialog) {
                PlaybackSpeedDialog(
                    currentSpeed = playbackSpeed,
                    onSpeedChange = onSpeedChange,
                    onDismissRequest = { showSpeedDialog = false }
                )
            }

            // Audio Output Device & Volume Sheet
            if (showAudioOutputSheet) {
                AudioOutputBottomSheet(
                    onDismiss = { showAudioOutputSheet = false }
                )
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
}