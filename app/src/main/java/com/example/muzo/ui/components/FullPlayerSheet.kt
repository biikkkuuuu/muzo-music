package com.example.muzo.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.muzo.data.download.SongDownloadManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlin.math.sin

/**
 * Muzi Flow Signature Player Screen.
 *
 * Visual Features:
 * - Top Bar: Centered "Now Playing" + song/album title, subtle down chevron on left.
 * - Artwork: Rounded square (24dp) with deep dynamic ambient background gradient.
 * - Title & Action Row:
 *     - Left: Bold Title + Artist
 *     - Right: Two Pure White Squircles:
 *         1. White Squircle Download button with black icon
 *         2. White Squircle Heart Like button with black/red icon
 * - Seekbar: 6dp rounded track, translucent white inactive, pure white active, white circle thumb, "0:00" left timestamp.
 * - Main Controls:
 *     - Previous: Translucent circular button (68dp)
 *     - Play/Pause: Hero Pure White Circle (84dp) with black icon
 *     - Next: Translucent circular button (68dp)
 * - Bottom Toolbar:
 *     - 5 Connected segmented bordered buttons:
 *         1. Queue (rounded left)
 *         2. Sleep Timer (middle square)
 *         3. Equalizer / Tune (middle square)
 *         4. Fullscreen / Lyrics (middle square)
 *         5. Repeat (rounded right)
 *     - Separator Spacer
 *     - 6. Pure White Circle button with black 3 vertical dots (More Menu).
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
    onClearUpcomingQueue: () -> Unit = {},
    crossfadeSeconds: Int = 4,
    isGaplessEnabled: Boolean = true,
    onCrossfadeChange: (Int) -> Unit = {},
    onGaplessToggle: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val downloadManager = remember { SongDownloadManager.getInstance(context.applicationContext) }
    val downloadedIds by downloadManager.downloadedVideoIds.collectAsStateWithLifecycle()
    val activeDownloads by downloadManager.activeDownloads.collectAsStateWithLifecycle()
    val isSongDownloaded = downloadedIds.contains(song.id)
    val isSongDownloading = activeDownloads.contains(song.id)

    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragSeekProgress by remember { mutableFloatStateOf(0f) }

    // Dialog & sheet visibility states
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showLyricsInAlbum by rememberSaveable { mutableStateOf(false) }
    var showLyricsResyncSheet by remember { mutableStateOf(false) }
    var lyricsManualOffsetMs by remember(song.id) { mutableLongStateOf(songOffsetMap[song.id] ?: 0L) }
    var showMediaInfoSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioOutputSheet by remember { mutableStateOf(false) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }
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
                // 1. TOP BAR (vivi_player1.png)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            modifier = Modifier.size(30.dp),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Now Playing",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 0.2.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = song.album?.name?.ifBlank { null } ?: (song.title + " Mix"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showAudioOutputSheet = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Cast / Output",
                            modifier = Modifier.size(22.dp),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // ==========================================
                // 2. ALBUM ARTWORK OR OPEN SYNCED LYRICS
                // ==========================================
                AnimatedContent(
                    targetState = showLyricsInAlbum,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(280))
                    },
                    label = "AlbumOrLyricsTransition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { isLyricsActive ->
                    if (isLyricsActive) {
                        AlbumSyncedLyricsView(
                            song = song,
                            currentPosition = currentPosition,
                            manualOffsetMs = lyricsManualOffsetMs,
                            onOffsetChange = { lyricsManualOffsetMs = it },
                            onSeek = onSeek,
                            onCloseLyrics = { showLyricsInAlbum = false },
                            onClose = onClose,
                            onNext = onNext,
                            onPrev = onPrev,
                            hasNext = hasNext || repeatMode > 0 || isShuffleActive,
                            hasPrev = hasPrev || repeatMode > 0,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth(0.92f)
                                    .shadow(24.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.7f))
                                    .clip(RoundedCornerShape(18.dp))
                                    .offset { IntOffset(animatedArtworkOffset.roundToInt(), 0) }
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
                                            onDragCancel = { swipeOffsetAccumulator = 0f }
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
                        }
                    }
                }

                // ==========================================
                // 3. TRACK TITLE, ARTIST, & CIRCULAR ACTION BUTTONS (vivi_player1.png)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
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
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Translucent Circular 3-Dots More Button (vivi_player1.png)
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .clickable { showMoreMenu = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Translucent Circular Heart Like Button (vivi_player1.png)
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .size(46.dp)
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
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 4. CAPSULE SEEKBAR & TIMESTAMPS (vivi_player1.png)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    ViViCapsuleSlider(
                        progress = if (isDraggingSeek) dragSeekProgress else currentProgress,
                        onProgressChange = { ratio ->
                            isDraggingSeek = true
                            dragSeekProgress = ratio
                        },
                        onProgressFinished = { ratio ->
                            isDraggingSeek = false
                            onSeek((ratio * duration).toLong())
                        },
                        height = 8.dp,
                        activeColor = Color.White,
                        inactiveColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val posMs = if (isDraggingSeek) (dragSeekProgress * duration).toLong() else currentPosition
                        Text(
                            text = formatTime(posMs),
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatTime(duration),
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ==========================================
                // 5. MAIN PLAYBACK CONTROLS (vivi_player1.png)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip Previous
                    IconButton(
                        onClick = onPrev,
                        enabled = hasPrev || repeatMode > 0,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(42.dp),
                            tint = if (hasPrev || repeatMode > 0) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }

                    // Huge Clean White Play/Pause
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(56.dp),
                            tint = Color.White
                        )
                    }

                    // Skip Next
                    IconButton(
                        onClick = onNext,
                        enabled = hasNext || repeatMode > 0 || isShuffleActive,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(42.dp),
                            tint = if (hasNext || repeatMode > 0 || isShuffleActive) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                // ==========================================
                // 6. VOLUME SLIDER ROW (vivi_player1.png)
                // ==========================================
                val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
                var volumeProgress by remember {
                    val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                    val cur = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                    mutableFloatStateOf(if (max > 0) cur / max else 0.5f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeMute,
                        contentDescription = "Volume Down",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )

                    ViViCapsuleSlider(
                        progress = volumeProgress,
                        onProgressChange = { ratio ->
                            volumeProgress = ratio
                            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val target = (ratio * maxVol).roundToInt()
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
                        },
                        height = 5.dp,
                        activeColor = Color.White,
                        inactiveColor = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume Up",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // ==========================================
                // 7. BOTTOM TOOLBAR (vivi_player1.png)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Queue Icon
                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Center: Segmented Pill (Equalizer + Sleep Timer)
                    Surface(
                        shape = RoundedCornerShape(21.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .height(42.dp)
                            .width(112.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Equalizer Segment
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { showEqualizerSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Equalizer",
                                    tint = if (isEqEnabled) Color(0xFF4ADE80) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Hairline vertical divider
                            Box(
                                modifier = Modifier
                                    .width(0.7.dp)
                                    .height(20.dp)
                                    .background(Color.White.copy(alpha = 0.25f))
                            )

                            // Sleep Timer Segment
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { showSleepTimerDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (isSleepTimerActive) Color(0xFF4ADE80) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Right: Lyrics Quote Bubble
                    IconButton(
                        onClick = { showLyricsInAlbum = !showLyricsInAlbum },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (showLyricsInAlbum) Color.White else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 7. MODALS & BOTTOM SHEETS
            // ==========================================
            // Muzi Flow More Options Bottom Sheet
            if (showMoreMenu) {
                MuziPlayerMenuSheet(
                    song = song,
                    queueCount = queue.size,
                    playbackSpeed = playbackSpeed,
                    isShuffleActive = isShuffleActive,
                    isDownloaded = isSongDownloaded,
                    crossfadeSeconds = crossfadeSeconds,
                    isGaplessEnabled = isGaplessEnabled,
                    onOpenCrossfade = {
                        showMoreMenu = false
                        showCrossfadeDialog = true
                    },
                    onToggleDownload = {
                        downloadManager.toggleDownload(song)
                    },
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
                    onToggleShuffle = {
                        onShuffleToggle()
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

            // Crossfade & Gapless Controller Dialog
            if (showCrossfadeDialog) {
                CrossfadeGaplessDialog(
                    crossfadeSeconds = crossfadeSeconds,
                    isGaplessEnabled = isGaplessEnabled,
                    onCrossfadeChange = onCrossfadeChange,
                    onGaplessToggle = onGaplessToggle,
                    onDismissRequest = { showCrossfadeDialog = false }
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

            // Lyrics Resync & Options Bottom Sheet
            if (showLyricsResyncSheet) {
                LyricsResyncBottomSheet(
                    song = song,
                    manualOffsetMs = lyricsManualOffsetMs,
                    onOffsetChange = { lyricsManualOffsetMs = it },
                    onDismiss = { showLyricsResyncSheet = false }
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
 * ViVi Music Material 3 Expressive Capsule Slider.
 * Used for both the main track Seekbar (8dp height) and Volume bar (5dp height).
 */
@Composable
fun ViViCapsuleSlider(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onProgressFinished: ((Float) -> Unit)? = null,
    height: androidx.compose.ui.unit.Dp = 8.dp,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.25f)
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onProgressChange(ratio)
                    onProgressFinished?.invoke(ratio)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onProgressChange(ratio)
                    },
                    onDragEnd = {
                        onProgressFinished?.invoke(progress)
                    },
                    onDragCancel = {
                        onProgressFinished?.invoke(progress)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth

        // 1. Inactive background capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(CircleShape)
                .background(inactiveColor)
        )

        // 2. Active played progress capsule
        if (progress > 0.002f) {
            Box(
                modifier = Modifier
                    .width(totalWidth * progress.coerceIn(0f, 1f))
                    .height(height)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }
    }
}

/**
 * Muzi Flow More Menu Modal Bottom Sheet (Opened by the white circular More button).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuziPlayerMenuSheet(
    song: SongItem,
    queueCount: Int,
    playbackSpeed: Float,
    isShuffleActive: Boolean,
    isDownloaded: Boolean = false,
    crossfadeSeconds: Int = 4,
    isGaplessEnabled: Boolean = true,
    onOpenCrossfade: () -> Unit = {},
    onToggleDownload: () -> Unit = {},
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenCodecInfo: () -> Unit,
    onToggleShuffle: () -> Unit,
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
                icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                title = if (isDownloaded) "Remove Download" else "Download Song",
                subtitle = if (isDownloaded) "Saved offline • Tap to delete" else "Download track for offline listening",
                badge = if (isDownloaded) "SAVED" else null,
                onClick = {
                    onDismiss()
                    onToggleDownload()
                }
            )

            MenuActionRow(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = "Up Next / Queue",
                badge = "$queueCount songs",
                onClick = onOpenQueue
            )

            val crossfadeBadge = buildString {
                if (crossfadeSeconds > 0) append("${crossfadeSeconds}s") else append("Off")
                if (isGaplessEnabled) append(" • GAPLESS")
            }
            MenuActionRow(
                icon = Icons.Default.GraphicEq,
                title = "Gapless & Crossfade",
                subtitle = "Seamless preloading & smooth volume blend",
                badge = crossfadeBadge,
                onClick = onOpenCrossfade
            )

            MenuActionRow(
                icon = Icons.Default.Shuffle,
                title = "Shuffle Queue",
                badge = if (isShuffleActive) "ON" else "OFF",
                onClick = onToggleShuffle
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
                icon = Icons.AutoMirrored.Filled.VolumeUp,
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
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Crossfade & Gapless Playback configuration modal dialog.
 */
@Composable
fun CrossfadeGaplessDialog(
    crossfadeSeconds: Int,
    isGaplessEnabled: Boolean,
    onCrossfadeChange: (Int) -> Unit,
    onGaplessToggle: (Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1D24),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color(0xFF5B8DEF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Gapless & Crossfade",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Gapless Playback Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onGaplessToggle(!isGaplessEnabled) }
                        .background(Color(0xFF282732))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gapless Playback",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pre-buffers next track for 0ms transition",
                            color = Color(0xFF9E9EA8),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isGaplessEnabled,
                        onCheckedChange = onGaplessToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF5B8DEF),
                            uncheckedThumbColor = Color(0xFF8E8E9A),
                            uncheckedTrackColor = Color(0xFF1C1C24)
                        )
                    )
                }

                // Crossfade Duration
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Audio Crossfade Duration",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s",
                            color = Color(0xFF5B8DEF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Smoothly blends outgoing and incoming audio",
                        color = Color(0xFF9E9EA8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val options = listOf(0, 2, 4, 6, 8, 12)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        options.forEach { sec ->
                            val isSelected = crossfadeSeconds == sec
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF5B8DEF) else Color(0xFF282732),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onCrossfadeChange(sec) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = if (sec == 0) "Off" else "${sec}s",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else Color(0xFFCDCDD5)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Done", color = Color(0xFF5B8DEF), fontWeight = FontWeight.Bold)
            }
        }
    )
}