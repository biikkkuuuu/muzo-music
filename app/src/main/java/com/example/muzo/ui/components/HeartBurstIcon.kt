package com.example.muzo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Echo-Music style exploding particle like button.
 * Triggers a radial particle burst and scale bounce when the track is liked.
 */
@Composable
fun HeartBurstIcon(
    isLiked: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    likedColor: Color = Color(0xFFFF3366),
    unlikedColor: Color = LocalContentColor.current
) {
    val burstAnim = remember { Animatable(0f) }
    var wasLiked by remember { mutableStateOf(isLiked) }

    LaunchedEffect(isLiked) {
        if (isLiked && !wasLiked) {
            burstAnim.snapTo(0f)
            burstAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            )
        }
        wasLiked = isLiked
    }

    val actualScale = if (burstAnim.isRunning) {
        if (burstAnim.value < 0.35f) 1f + burstAnim.value * 0.8f
        else 1.28f - (burstAnim.value - 0.35f) * 0.43f
    } else {
        1f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Particle burst animation
        if (burstAnim.value > 0f && burstAnim.value < 1f) {
            val progress = burstAnim.value
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val particleCount = 7

            Canvas(modifier = Modifier.size(iconSize * 2.5f)) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2.2f

                for (i in 0 until particleCount) {
                    val angle = (i * (360f / particleCount)) * (Math.PI / 180f)
                    val currentRadius = maxRadius * progress
                    val x = center.x + cos(angle).toFloat() * currentRadius
                    val y = center.y + sin(angle).toFloat() * currentRadius
                    val particleSize = (3.5f * (1f - progress * 0.5f)).dp.toPx()

                    drawCircle(
                        color = likedColor.copy(alpha = alpha),
                        radius = particleSize,
                        center = Offset(x, y)
                    )
                }
            }
        }

        Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isLiked) "Liked" else "Not liked",
            tint = if (isLiked) likedColor else unlikedColor,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = actualScale
                    scaleY = actualScale
                }
        )
    }
}
