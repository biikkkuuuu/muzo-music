package com.example.muzo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Android 14 Signature Squiggly Waveform Slider (Google Pixel Style).
 *
 * Features:
 * - Active track (left of thumb): Smooth animated sine wave in pure white during playback.
 * - Inactive track (right of thumb): Smooth straight horizontal line in translucent white.
 * - Junction: Prominent crisp white circular thumb dot.
 * - Pause / Drag: Wave smoothly flattens down to a straight bar.
 * - High touch target (42dp) with tap-to-seek and drag-to-scrub.
 */
@Composable
fun SquigglySlider(
    progress: Float, // 0f..1f
    onSeekProgress: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.32f),
    thumbColor: Color = Color.White
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0f) }

    val currentRatio = if (isDragging) dragRatio else progress.coerceIn(0f, 1f)

    var phaseOffset by remember { mutableFloatStateOf(0f) }
    val heightFraction = remember { Animatable(if (isPlaying) 1f else 0f) }

    val waveLengthPx = 54f
    val waveAmplitudePx = 9f
    val waveSpeedPxPerSec = 38f

    // Smoothly flatten wave on pause or drag
    LaunchedEffect(isPlaying, isDragging) {
        val shouldFlatten = !isPlaying || isDragging
        val target = if (shouldFlatten) 0f else 1f
        heightFraction.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 220, easing = LinearEasing)
        )
    }

    // Continuous wave phase animation while playing
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastTime = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { now ->
                val dt = (now - lastTime) / 1000f
                phaseOffset = (phaseOffset + dt * waveSpeedPxPerSec) % waveLengthPx
                lastTime = now
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
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
                .height(30.dp)
        ) {
            val centerY = size.height / 2f
            val totalWidth = size.width
            val progressX = (currentRatio * totalWidth).coerceIn(0f, totalWidth)

            val trackThickness = 4.dp.toPx()
            val thumbRadius = 5.5.dp.toPx()
            val currentAmp = waveAmplitudePx * heightFraction.value

            // 1. Draw Active Wavy Path (0 to progressX)
            if (progressX > 0f) {
                val wavePath = Path()
                val step = 3f
                var x = 0f

                // Starting point
                val startY = centerY + currentAmp * sin((0f - phaseOffset) / waveLengthPx * 2f * PI.toFloat())
                wavePath.moveTo(0f, startY)

                while (x <= progressX) {
                    val y = centerY + currentAmp * sin((x - phaseOffset) / waveLengthPx * 2f * PI.toFloat())
                    wavePath.lineTo(x, y)
                    x += step
                }

                // Connect to exact progress point
                val endY = centerY + currentAmp * sin((progressX - phaseOffset) / waveLengthPx * 2f * PI.toFloat())
                wavePath.lineTo(progressX, endY)

                clipRect(left = 0f, top = 0f, right = progressX, bottom = size.height) {
                    drawPath(
                        path = wavePath,
                        color = activeColor,
                        style = Stroke(width = trackThickness, cap = StrokeCap.Round)
                    )
                }
            }

            // 2. Draw Inactive Track (progressX to totalWidth) - Straight sleek horizontal line
            val inactiveStartX = (progressX + thumbRadius).coerceAtMost(totalWidth)
            if (inactiveStartX < totalWidth) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(inactiveStartX, centerY),
                    end = Offset(totalWidth, centerY),
                    strokeWidth = trackThickness,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw Thumb Dot (Android 14 crisp circular scrubber)
            val thumbY = centerY + currentAmp * sin((progressX - phaseOffset) / waveLengthPx * 2f * PI.toFloat())
            // Subtle outer glow
            drawCircle(
                color = activeColor.copy(alpha = 0.25f),
                radius = thumbRadius + 2.dp.toPx(),
                center = Offset(progressX, thumbY)
            )
            // Solid white thumb
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(progressX, thumbY)
            )
        }
    }
}
