package com.example.muzo.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
 * Echo-Music Player Screen (Pixel-Perfect implementation of echo_ui_player.png).
 *
 * Visual Layout & Features:
 * - Top Bar: Down chevron (collapse), centered "Now Playing" + song title, Cast/Audio Output icon.
 * - Artwork: Large 28dp rounded square with subtle elevation shadow, Prev/Next horizontal swipe,
 *            double-tap +/- 10s seek bubble feedback, tap play/pause.
 * - Title & Action Row:
 *     - Left: Bold Title (24sp) + Artist (16sp)
 *     - Right: Circular Download button (48dp) + Circular Heart Like button (48dp) with spring animation.
 * - Seekbar: Sleek slim slider with vertical blue pill/capsule thumb and left/right timestamps (00:01 / 02:43).
 * - Main Controls: Circular Prev (58dp), Signature Wide Pill Play/Pause button (112x62dp, #3872FF), Circular Next (58dp).
 * - Bottom Utility Row: 6 evenly spaced icons matching Echo:
 *     1. Quotes (Lyrics)
 *     2. Crescent Moon (Sleep Timer with live active indicator dot)
 *     3. Sound Waves / Equalizer (with live active indicator dot)
 *     4. Shuffle (with active #3872FF highlight)
 *     5. Repeat / RepeatOne (with active #3872FF highlight)
 *     6. More Options (Three vertical dots -> Echo More Menu with Queue, Speed, Codec, Radio, Share).
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
    isShuffleActive: Boolean = false,
    repeatMode: Int = 0, // 0 = off, 1 = all, 2 = one
    onShuffleToggle: () -> Unit = {},
    onRepeatToggle: () -> Unit = {},
    onStartRadio: (SongItem) -> Unit = {},
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
    val context = LocalContext.current
    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragSeekProgress by remember { mutableFloatStateOf(0f) }

    // Dialog & sheet visibility states
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showMediaInfoSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioOutputSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Double-tap seek feedback overlay
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(700)
            seekFeedbackText = null
        }
    }

    // Artwork horizontal swipe animation
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

    // Dynamic ambient background wrapping AMOLED dark canvas
    AlbumGradient(thumbnailUrl = getHighResThumbnail(song.thumbnail)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==========================================
                // 1. TOP BAR: Chevron Down, "Now Playing" + Title, Cast Icon
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Now Playing",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFA0A0A8),
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = song.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showAudioOutputSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Cast & Audio Output",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ==========================================
                // 2. ALBUM ARTWORK: 28dp rounded square, swipe gesture & double-tap
                // ==========================================
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .aspectRatio(1f)
                        .fillMaxWidth(0.92f)
                        .offset { IntOffset(animatedArtworkOffset.roundToInt(), 0) }
                        .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.75f))
                        .clip(RoundedCornerShape(28.dp))
                        .pointerInput(song.id) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    swipeOffsetAccumulator = (swipeOffsetAccumulator + dragAmount).coerceIn(-180f, 180f)
                                },
                                onDragEnd = {
                                    if (swipeOffsetAccumulator < -75f && (hasNext || repeatMode > 0 || isShuffleActive)) {
                                        onNext()
                                    } else if (swipeOffsetAccumulator > 75f && (hasPrev || repeatMode > 0)) {
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
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(horizontal = 22.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = seekFeedbackText ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 3. TRACK TITLE, ARTIST, & CIRCULAR ACTION BUTTONS ROW
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = song.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFA0A0A5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Download Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1E24),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { showMediaInfoSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download & Codec",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Circular Heart Like Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1E24),
                            modifier = Modifier
                                .size(48.dp)
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
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 4. SEEKBAR & TIMESTAMPS (Echo Minimalist Capsule Slider)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    EchoSeekSlider(
                        progress = if (isDraggingSeek) dragSeekProgress else currentProgress,
                        onSeekProgress = { ratio ->
                            isDraggingSeek = true
                            dragSeekProgress = ratio
                        },
                        onSeekFinished = { ratio ->
                            isDraggingSeek = false
                            onSeek((ratio * duration).toLong())
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(if (isDraggingSeek) (dragSeekProgress * duration).toLong() else currentPosition),
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatTime(duration),
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ==========================================
                // 5. MAIN PLAYBACK CONTROLS: Circular Prev, Signature Blue Pill Play/Pause, Circular Next
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E1E24),
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .clickable(enabled = hasPrev || repeatMode > 0) { onPrev() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(28.dp),
                                tint = if (hasPrev || repeatMode > 0) Color.White else Color.White.copy(alpha = 0.35f)
                            )
                        }
                    }

                    // Echo Hero Play/Pause Pill Button
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color(0xFF3872FF),
                        contentColor = Color.White,
                        modifier = Modifier
                            .width(112.dp)
                            .height(62.dp)
                            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF3872FF).copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(32.dp))
                            .clickable { onPlayPause() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(34.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // Next Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E1E24),
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .clickable(enabled = hasNext || repeatMode > 0 || isShuffleActive) { onNext() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(28.dp),
                                tint = if (hasNext || repeatMode > 0 || isShuffleActive) Color.White else Color.White.copy(alpha = 0.35f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 6. BOTTOM UTILITY TOOLBAR (6 Icons Matching Echo-Music)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Quotes / Lyrics
                    IconButton(
                        onClick = { showLyricsSheet = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (showLyricsSheet) Color(0xFF3872FF) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 2. Crescent Moon / Sleep Timer
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (isSleepTimerActive) Color(0xFF3872FF) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                            if (isSleepTimerActive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3872FF))
                                )
                            }
                        }
                    }

                    // 3. Waveform / Equalizer
                    IconButton(
                        onClick = { showEqualizerSheet = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Equalizer",
                                tint = if (isEqEnabled) Color(0xFF3872FF) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                            if (isEqEnabled) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3872FF))
                                )
                            }
                        }
                    }

                    // 4. Shuffle
                    IconButton(
                        onClick = onShuffleToggle,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleActive) Color(0xFF3872FF) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 5. Repeat
                    IconButton(
                        onClick = onRepeatToggle,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                2 -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode > 0) Color(0xFF3872FF) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 6. More Options (Three Vertical Dots)
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 7. MODALS & BOTTOM SHEETS
            // ==========================================
            // Echo More Options Bottom Sheet
            if (showMoreMenu) {
                EchoPlayerMenuSheet(
                    song = song,
                    queueCount = queue.size,
                    playbackSpeed = playbackSpeed,
                    onDismiss = { showMoreMenu = false },
                    onOpenQueue = {
                        showMoreMenu = false
                        showQueueSheet = true
                    },
                    onOpenSpeed = {
                        showMoreMenu = false
                        showSpeedDialog = true
                    },
                    onOpenCodecInfo = {
                        showMoreMenu = false
                        showMediaInfoSheet = true
                    },
                    onStartRadio = {
                        showMoreMenu = false
                        onStartRadio(song)
                    },
                    onOpenAudioOutput = {
                        showMoreMenu = false
                        showAudioOutputSheet = true
                    },
                    onShare = {
                        showMoreMenu = false
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Listen to \"${song.title}\" by ${song.artists.joinToString(", ") { it.name }} on Muzi: https://music.youtube.com/watch?v=${song.id}"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
                    }
                )
            }

            // Synchronized / Plain Lyrics Bottom Sheet
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

/**
 * Echo-style Minimalist Seekbar with vertical blue pill/capsule thumb.
 */
@Composable
private fun EchoSeekSlider(
    progress: Float,
    onSeekProgress: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = Color(0xFF3872FF),
    inactiveTrackColor: Color = Color(0xFF1E2640),
    thumbColor: Color = Color(0xFF3872FF)
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0f) }

    val currentRatio = if (isDragging) dragRatio else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeekProgress(ratio)
                    onSeekFinished(ratio)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragRatio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekProgress(dragRatio)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeekProgress(dragRatio)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekFinished(dragRatio)
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val centerY = size.height / 2f
            val totalWidth = size.width
            val progressX = currentRatio * totalWidth
            val trackHeight = 3.5.dp.toPx()

            // Inactive track
            drawLine(
                color = inactiveTrackColor,
                start = Offset(0f, centerY),
                end = Offset(totalWidth, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )

            // Active track
            if (progressX > 0f) {
                drawLine(
                    color = activeTrackColor,
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
            }

            // Echo vertical capsule/pill thumb
            val thumbHalfHeight = 7.dp.toPx()
            val thumbWidth = 4.5.dp.toPx()
            drawLine(
                color = thumbColor,
                start = Offset(progressX, centerY - thumbHalfHeight),
                end = Offset(progressX, centerY + thumbHalfHeight),
                strokeWidth = thumbWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Echo More Menu Modal Bottom Sheet (Opened by 6th More Options icon).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EchoPlayerMenuSheet(
    song: SongItem,
    queueCount: Int,
    playbackSpeed: Float,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenCodecInfo: () -> Unit,
    onStartRadio: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141A),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Track Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = getHighResThumbnail(song.thumbnail),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" },
                        fontSize = 13.sp,
                        color = Color(0xFFA0A0A5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Option Items
            MenuActionRow(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = "Up Next / Queue",
                badge = "$queueCount songs",
                onClick = onOpenQueue
            )

            MenuActionRow(
                icon = Icons.Default.Speed,
                title = "Playback Speed",
                badge = "${playbackSpeed}x",
                onClick = onOpenSpeed
            )

            MenuActionRow(
                icon = Icons.Default.HighQuality,
                title = "Audio Quality & Stream Info",
                badge = "320 KBPS / Opus",
                onClick = onOpenCodecInfo
            )

            MenuActionRow(
                icon = Icons.Default.Radio,
                title = "Start Radio",
                subtitle = "Generate endless mix from this song",
                onClick = onStartRadio
            )

            MenuActionRow(
                icon = Icons.Default.VolumeUp,
                title = "Audio Output & Volume",
                subtitle = "Speaker, Bluetooth, or Cast",
                onClick = onOpenAudioOutput
            )

            MenuActionRow(
                icon = Icons.Default.Share,
                title = "Share Song",
                onClick = onShare
            )
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22222C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }

            if (!badge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF3872FF).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3872FF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}