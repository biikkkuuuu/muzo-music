package com.example.muzo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.music.innertube.models.SongItem
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Echo-Music style Floating Mini Player capsule with both Expanded and Inline modes,
 * horizontal swipe gestures to change tracks, spring physics, and translucent surface styling.
 */
@Composable
fun FloatingMiniPlayer(
    song: SongItem,
    isPlaying: Boolean,
    currentPosition: Long = 0L,
    duration: Long = 0L,
    hasPrev: Boolean = true,
    hasNext: Boolean = true,
    isInline: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    }

    val pressInteractionSource = remember { MutableInteractionSource() }
    val isPressed by pressInteractionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "miniPlayerPressScale"
    )

    val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val artSize = if (isInline) 32.dp else 44.dp
    val artCornerRadius = if (isInline) 8.dp else 12.dp
    val controlSize = if (isInline) 32.dp else 40.dp

    Surface(
        modifier = modifier
            .then(
                if (!isInline) {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = if (isInline) 8.dp else 16.dp,
                shape = RoundedCornerShape(if (isInline) 100.dp else 26.dp),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clipToBounds()
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragStartTime = System.currentTimeMillis()
                        totalDragDistance = 0f
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(0f, animationSpec)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragDistance += abs(dragAmount)
                        coroutineScope.launch {
                            offsetXAnimatable.snapTo(offsetXAnimatable.value + dragAmount)
                        }
                    },
                    onDragEnd = {
                        val currentOffset = offsetXAnimatable.value
                        val threshold = if (isInline) 80f else 120f
                        if (currentOffset > threshold && hasPrev) {
                            onPrev()
                        } else if (currentOffset < -threshold && hasNext) {
                            onNext()
                        }
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(0f, animationSpec)
                        }
                    }
                )
            },
        shape = RoundedCornerShape(if (isInline) 100.dp else 26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = pressInteractionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                    .padding(
                        horizontal = if (isInline) 8.dp else 12.dp,
                        vertical = if (isInline) 4.dp else 7.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular artwork with progress ring matching Echo Screenshot Image 2
                Box(
                    modifier = Modifier.size(artSize),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        strokeWidth = 2.5.dp
                    )
                    AsyncImage(
                        model = getHighResThumbnail(song.thumbnail),
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(artSize - 6.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(if (isInline) 8.dp else 12.dp))

                // Title & Artists
                if (isInline) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = com.example.muzo.theme.GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = com.example.muzo.theme.GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = song.artists.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = com.example.muzo.theme.GoogleSansFlex,
                            fontSize = 12.5.sp,
                            color = contentColor.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Controls
                if (!isInline && hasPrev) {
                    IconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Solid primary circular play button matching Echo Screenshot Image 2
                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (isInline) 34.dp else 40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(if (isInline) 20.dp else 24.dp)
                        )
                    }
                }

                if (!isInline && hasNext) {
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}