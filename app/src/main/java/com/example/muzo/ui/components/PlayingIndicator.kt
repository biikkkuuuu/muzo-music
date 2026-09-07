package com.example.muzo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Echo-Music style jumping audio equalizer bars indicator
 * displayed next to the currently playing song in rows.
 */
@Composable
fun PlayingIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    bars: Int = 3,
    barWidth: Dp = 3.dp,
    barHeight: Dp = 16.dp,
    cornerRadius: Dp = 2.dp,
) {
    val animatables = remember {
        List(bars) { Animatable(0.15f) }
    }

    LaunchedEffect(Unit) {
        animatables.forEach { animatable ->
            launch {
                while (true) {
                    animatable.animateTo(Random.nextFloat() * 0.85f + 0.15f)
                    delay(80)
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.height(barHeight),
    ) {
        animatables.forEach { animatable ->
            Canvas(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight()
            ) {
                val heightPx = size.height * animatable.value
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, size.height - heightPx),
                    size = Size(size.width, heightPx),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
        }
    }
}
