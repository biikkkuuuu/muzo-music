package com.example.muzo.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.muzo.data.download.SongDownloadManager
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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

/**
 * Echo-Music Signature Player Screen (Matches the exact style from user's screenshots).
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
    onClearUpcomingQueue: () -> Unit = {}
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
                // ==========================================
                // 1. TOP BAR: Centered "Now Playing" + Title (Only in Cover Mode)
                // ==========================================
                AnimatedVisibility(
                    visible = !showLyricsInAlbum,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 40.dp) // Offset the left chevron so title is truly centered
                        ) {
                            Text(
                                text = "Now Playing",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 0.3.sp
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
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // ==========================================
                // 2. ALBUM ARTWORK OR OPEN SYNCED LYRICS (Matches Image 2)
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
                        // Image 2: Open Edge-to-Edge Synchronized Lyrics on Ambient Canvas
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
                        // Big Album Artwork View
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth(0.92f)
                                    .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.75f))
                                    .clip(RoundedCornerShape(24.dp))
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

                                // Floating Lyrics Quick Pill on Cover
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Black.copy(alpha = 0.55f),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { showLyricsInAlbum = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Notes,
                                            contentDescription = "Lyrics",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Lyrics",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

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

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 3. TRACK TITLE, ARTIST, & ACTION SQUIRCLES (Image 2 exact match)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (showLyricsInAlbum) {
                        // Image 2: Mini Album Artwork Thumbnail + Song Title & Artist
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = getHighResThumbnail(song.thumbnail),
                                contentDescription = song.title,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.basicMarquee()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" },
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Image 2: THREE Pure White Squircles on Right: [Download], [CropFree / Cover] and [... / MoreHoriz / Resync]
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Pure White Squircle Download Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSongDownloading) {
                                            Toast.makeText(context, "Download in progress...", Toast.LENGTH_SHORT).show()
                                        } else if (isSongDownloaded) {
                                            downloadManager.removeDownload(song.id, song.title)
                                        } else {
                                            downloadManager.downloadSong(song)
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSongDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Black
                                        )
                                    } else if (isSongDownloaded) {
                                        Icon(
                                            imageVector = Icons.Default.DownloadDone,
                                            contentDescription = "Downloaded (Tap to remove)",
                                            tint = Color(0xFF00A844),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Song",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // 2. CropFree (Corner brackets) -> Return to Big Cover View
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showLyricsInAlbum = false }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CropFree,
                                        contentDescription = "Show Album Cover",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // 3. MoreHoriz (...) -> Open Lyrics Resync & Options Bottom Sheet
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showLyricsResyncSheet = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "Lyrics Sync & Options",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Normal Cover View: Large Title & Artist + Download & Heart
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
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pure White Squircle Download Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSongDownloading) {
                                            Toast.makeText(context, "Download in progress...", Toast.LENGTH_SHORT).show()
                                        } else if (isSongDownloaded) {
                                            downloadManager.removeDownload(song.id, song.title)
                                        } else {
                                            downloadManager.downloadSong(song)
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSongDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Black
                                        )
                                    } else if (isSongDownloaded) {
                                        Icon(
                                            imageVector = Icons.Default.DownloadDone,
                                            contentDescription = "Downloaded (Tap to remove)",
                                            tint = Color(0xFF00A844),
                                            modifier = Modifier.size(23.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download",
                                            tint = Color.Black,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            // Pure White Squircle Heart Like Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .size(46.dp)
                                    .graphicsLayer {
                                        scaleX = likeScale
                                        scaleY = likeScale
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onLikeToggle() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (isLiked) "Liked" else "Like",
                                        tint = if (isLiked) Color(0xFFFF3B30) else Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 4. SEEKBAR & TIMESTAMPS (Echo Thick White Seekbar)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    EchoThickSeekSlider(
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        val posMs = if (isDraggingSeek) (dragSeekProgress * duration).toLong() else currentPosition
                        val totalSec = (posMs / 1000).coerceAtLeast(0)
                        val mins = totalSec / 60
                        val secs = totalSec % 60
                        Text(
                            text = String.format("%d:%02d", mins, secs),
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ==========================================
                // 5. MAIN CONTROLS: Translucent Prev, Hero White Circle Play/Pause, Translucent Next
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable(enabled = hasPrev || repeatMode > 0) { onPrev() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(32.dp),
                                tint = if (hasPrev || repeatMode > 0) Color.White else Color.White.copy(alpha = 0.35f)
                            )
                        }
                    }

                    // Hero Play/Pause Button (Large Pure White Circle with black icon)
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        contentColor = Color.Black,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .clickable { onPlayPause() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(38.dp),
                                tint = Color.Black
                            )
                        }
                    }

                    // Next Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable(enabled = hasNext || repeatMode > 0 || isShuffleActive) { onNext() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(32.dp),
                                tint = if (hasNext || repeatMode > 0 || isShuffleActive) Color.White else Color.White.copy(alpha = 0.35f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 6. BOTTOM TOOLBAR: 5 Connected Segmented Buttons + 1 White Circle More Button
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonHeight = 44.dp
                    val itemShape = RoundedCornerShape(8.dp)

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Queue / Up Next
                        SegmentedToolbarButton(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            shape = itemShape,
                            isActive = showQueueSheet,
                            onClick = { showQueueSheet = true },
                            modifier = Modifier.weight(1f).height(buttonHeight)
                        )

                        // 2. Sleep Timer
                        SegmentedToolbarButton(
                            icon = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            shape = itemShape,
                            isActive = isSleepTimerActive,
                            onClick = { showSleepTimerDialog = true },
                            modifier = Modifier.weight(1f).height(buttonHeight)
                        )

                        // 3. Lyrics (In Image 2, button 3 is LYRICS with 3 horizontal lines, ACTIVE in pure white!)
                        SegmentedToolbarButton(
                            icon = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Lyrics",
                            shape = itemShape,
                            isActive = showLyricsInAlbum,
                            onClick = { showLyricsInAlbum = !showLyricsInAlbum },
                            modifier = Modifier.weight(1f).height(buttonHeight)
                        )

                        // 4. Equalizer / Sound Tuning
                        SegmentedToolbarButton(
                            icon = Icons.Default.Tune,
                            contentDescription = "Equalizer",
                            shape = itemShape,
                            isActive = isEqEnabled,
                            onClick = { showEqualizerSheet = true },
                            modifier = Modifier.weight(1f).height(buttonHeight)
                        )

                        // 5. Repeat
                        SegmentedToolbarButton(
                            icon = when (repeatMode) {
                                2 -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            shape = itemShape,
                            isActive = repeatMode > 0,
                            onClick = onRepeatToggle,
                            modifier = Modifier.weight(1f).height(buttonHeight)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // 6. White Circular More Menu Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable { showMoreMenu = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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
                    isShuffleActive = isShuffleActive,
                    isDownloaded = isSongDownloaded,
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
 * Connected segmented button for the bottom toolbar (Matches Echo's Queue peek bar).
 */
@Composable
private fun SegmentedToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    shape: RoundedCornerShape,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appliedModifier = if (isActive) {
        modifier
            .clip(shape)
            .background(Color.White)
            .clickable(onClick = onClick)
    } else {
        modifier
            .clip(shape)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.35f), shape = shape)
            .clickable(onClick = onClick)
    }

    Box(
        modifier = appliedModifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.Black else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Echo thick seekbar with white track and circle thumb (Matches user's screenshot).
 */
@Composable
private fun EchoThickSeekSlider(
    progress: Float,
    onSeekProgress: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0f) }

    val currentRatio = if (isDragging) dragRatio else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
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
            val trackHeight = 6.dp.toPx()

            // Inactive track: translucent white rounded pill
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, centerY),
                end = Offset(totalWidth, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )

            // Active track: pure white rounded pill
            if (progressX > 0f) {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
            }

            // Thumb: pure white circle
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(progressX.coerceIn(0f, totalWidth), centerY)
            )
        }
    }
}

/**
 * Echo More Menu Modal Bottom Sheet (Opened by the white circular More button).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EchoPlayerMenuSheet(
    song: SongItem,
    queueCount: Int,
    playbackSpeed: Float,
    isShuffleActive: Boolean,
    isDownloaded: Boolean = false,
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