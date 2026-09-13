package com.biikkkuuuu.muzi.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WaveformSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
    barWidth: Dp = 3.dp,
    barGap: Dp = 2.5.dp,
    seedKey: String? = null,
    height: Dp = 36.dp
) {
    val density = LocalDensity.current
    val barWidthPx = with(density) { barWidth.toPx() }
    val barGapPx = with(density) { barGap.toPx() }
    val minBarHeightPx = with(density) { 4.dp.toPx() }

    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val normalizedValue = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }

    val displayProgress = if (isDragging) dragValue else normalizedValue

    // Generate static normalized waveform amplitudes for the song
    val staticAmplitudes = remember(seedKey) {
        val seed = seedKey?.hashCode() ?: 42
        val random = Random(seed)
        val count = 120
        FloatArray(count) { i ->
            val harmonic1 = sin(i.toDouble() * 0.15) * 0.35
            val harmonic2 = sin(i.toDouble() * 0.35 + 1.2) * 0.25
            val noise = (random.nextFloat() - 0.5f) * 0.4f
            val base = 0.55f + harmonic1.toFloat() + harmonic2.toFloat() + noise
            base.coerceIn(0.15f, 0.95f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveformPulse")
    val livePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livePulse"
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor.copy(alpha = 0.4f)
    val thumbColor = colors.thumbColor

    val baseModifier = modifier
        .fillMaxWidth()
        .height(height)

    val interactiveModifier = if (enabled) {
        baseModifier
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val progress = (offset.x / size.width).coerceIn(0f, 1f)
                    val targetVal = valueRange.start + progress * rangeSpan
                    onValueChange(targetVal)
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                        val targetVal = valueRange.start + dragValue * rangeSpan
                        onValueChange(targetVal)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                        val targetVal = valueRange.start + dragValue * rangeSpan
                        onValueChange(targetVal)
                    }
                )
            }
    } else {
        baseModifier
    }

    Box(
        modifier = interactiveModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            if (totalWidth <= 0 || totalHeight <= 0) return@Canvas

            val stepPx = barWidthPx + barGapPx
            val totalBars = (totalWidth / stepPx).toInt().coerceAtLeast(1)
            val currentPosBarIndex = (displayProgress * totalBars).toInt().coerceIn(0, totalBars - 1)

            val startX = (totalWidth - (totalBars * stepPx - barGapPx)) / 2f
            val cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)

            for (i in 0 until totalBars) {
                val ampIndex = (i * staticAmplitudes.size / totalBars).coerceIn(0, staticAmplitudes.size - 1)
                var barAmp = staticAmplitudes[ampIndex]

                // Live pulsing for current playing position if music is playing
                if (isPlaying && abs(i - currentPosBarIndex) <= 2) {
                    val factor = if (i == currentPosBarIndex) livePulse else (1f + (livePulse - 1f) * 0.5f)
                    barAmp = (barAmp * factor).coerceIn(0.15f, 1f)
                }

                val calculatedBarHeight = (totalHeight * barAmp).coerceAtLeast(minBarHeightPx)
                val x = startX + i * stepPx
                val y = (totalHeight - calculatedBarHeight) / 2f

                val isPlayed = i <= currentPosBarIndex
                val barColor = if (isPlayed) activeColor else inactiveColor

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidthPx, calculatedBarHeight),
                    cornerRadius = cornerRadius
                )
            }

            // Sleek cursor indicator at the thumb position
            val thumbX = (startX + currentPosBarIndex * stepPx + barWidthPx / 2f).coerceIn(0f, totalWidth)
            drawCircle(
                color = thumbColor,
                radius = with(density) { 3.5.dp.toPx() },
                center = Offset(thumbX, totalHeight / 2f)
            )
        }
    }
}
