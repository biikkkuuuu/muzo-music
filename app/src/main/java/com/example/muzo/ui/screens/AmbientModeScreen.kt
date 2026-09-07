package com.example.muzo.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.example.muzo.core.formatTime
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.ui.components.AmbientGlowBackground
import com.music.innertube.models.SongItem
import kotlin.math.abs

/**
 * AmbientModeScreen: Landscape OLED StandBy display (Echo-Music style).
 *
 * Features:
 * - Immersive landscape lock with hidden system bars
 * - Dynamic Ambient Glow background reacting to album art
 * - Vertical drag gesture: Adjust system volume
 * - Horizontal swipe gesture: Skip track forward/backward
 * - Double-tap album artwork: Play / Pause toggle
 * - Synchronized flowing lyrics with tap-to-seek
 */
@Composable
fun AmbientModeScreen(
    song: SongItem?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Lock to landscape and hide system bars during ambient mode
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        var insetsController: WindowInsetsControllerCompat? = null
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        onClose()
    }

    // Gestures for swipe volume and track skip
    var swipeDeltaX by remember { mutableFloatStateOf(0f) }
    var swipeDeltaY by remember { mutableFloatStateOf(0f) }

    // Lyrics state
    val cachedResult = remember(song?.id) { song?.id?.let { com.example.muzo.ui.components.lyricsCache[it] } }
    val syncedLines = remember(cachedResult) {
        cachedResult?.activeCandidate?.syncedLines.orEmpty()
    }
    val plainLyrics = remember(cachedResult) {
        cachedResult?.activeCandidate?.plainLyrics
    }

    val activeIndex = remember(currentPosition, syncedLines) {
        if (syncedLines.isEmpty()) -1
        else {
            val idx = syncedLines.indexOfLast { it.timeMs <= currentPosition }
            if (idx >= 0) idx else 0
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex in syncedLines.indices) {
            listState.animateScrollToItem(
                index = (activeIndex - 1).coerceAtLeast(0),
                scrollOffset = -40
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (abs(swipeDeltaX) > 160f && abs(swipeDeltaX) > abs(swipeDeltaY)) {
                            if (swipeDeltaX > 0) onPrev() else onNext()
                        }
                        swipeDeltaX = 0f
                        swipeDeltaY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        swipeDeltaX += dragAmount.x
                        swipeDeltaY += dragAmount.y

                        // Vertical swipe adjusts volume
                        if (abs(dragAmount.y) > 12f && abs(dragAmount.y) > abs(dragAmount.x)) {
                            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            if (dragAmount.y < 0 && curVolume < maxVolume) {
                                audioManager.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_RAISE,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            } else if (dragAmount.y > 0 && curVolume > 0) {
                                audioManager.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_LOWER,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            }
                            swipeDeltaY = 0f
                        }
                    }
                )
            }
    ) {
        // Deep Ambient Radial Glow
        AmbientGlowBackground(
            thumbnailUrl = song?.let { getHighResThumbnail(it.thumbnail) },
            modifier = Modifier.fillMaxSize()
        )

        // Semi-translucent dark tint for OLED contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        // Content Row: Left (Album + Controls), Right (Lyrics)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT PANEL: Album Artwork + Track Details + Playback Pill
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { onPlayPause() }
                            )
                        }
                ) {
                    AsyncImage(
                        model = song?.let { getHighResThumbnail(it.thumbnail) },
                        contentDescription = "Album Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle Glass Play/Pause Indicator Overlay on Double Tap Hint
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Track Title & Artist
                Text(
                    text = song?.title ?: "Muzi Music",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .basicMarquee(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = song?.artists?.joinToString { it.name } ?: "Unknown Artist",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mini Progress Bar
                val progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                    Text(
                        text = formatTime(duration),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // RIGHT PANEL: Synchronized Flowing Lyrics
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    syncedLines.isNotEmpty() -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = 40.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(syncedLines) { index, line ->
                                val isCurrent = index == activeIndex
                                val dist = kotlin.math.abs(index - activeIndex)

                                com.example.muzo.ui.components.MetroLyricsLine(
                                    text = line.text,
                                    isActive = isCurrent,
                                    distanceFromActive = dist,
                                    enableBlur = true,
                                    fontSize = if (isCurrent) 24f else 20f,
                                    onClick = { onSeek(line.timeMs) }
                                )
                            }
                        }
                    }

                    !plainLyrics.isNullOrBlank() -> {
                        Text(
                            text = plainLyrics,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 17.sp,
                            lineHeight = 26.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Muzi Ambient Display",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Double-tap art to play/pause • Swipe horizontal to skip • Swipe vertical for volume",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Top-Left Back Button to Exit StandBy Mode
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Exit Ambient Mode",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
