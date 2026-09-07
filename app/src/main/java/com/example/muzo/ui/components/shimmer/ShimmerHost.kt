package com.example.muzo.ui.components.shimmer

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier for smooth skeleton loading screens.
 * Uses infinite transition with linear gradient highlight.
 */
fun Modifier.shimmer(
    shape: Shape = RoundedCornerShape(8.dp),
    durationMillis: Int = 1100
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val baseColor = surfaceVariant.copy(alpha = 0.25f)
    val highlightColor = surfaceVariant.copy(alpha = 0.55f)

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim * 1000f, y = 0f),
        end = Offset(x = (translateAnim + 0.5f) * 1000f, y = 1000f)
    )

    this
        .clip(shape)
        .background(brush)
}

/**
 * ShimmerHost container for grouping placeholder items.
 */
@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        content = content
    )
}

/**
 * Skeleton placeholder for standard song row list items.
 */
@Composable
fun ListItemPlaceholder(
    modifier: Modifier = Modifier,
    thumbnailShape: Shape = RoundedCornerShape(10.dp)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(54.dp)
                .shimmer(shape = thumbnailShape)
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Title and subtitle lines
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .shimmer(RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(11.dp)
                    .shimmer(RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Trailing placeholder (action / duration)
        Box(
            modifier = Modifier
                .size(24.dp)
                .shimmer(CircleShape)
        )
    }
}

/**
 * Skeleton placeholder for album / playlist grid cards.
 */
@Composable
fun GridItemPlaceholder(
    modifier: Modifier = Modifier,
    cardSize: Dp = 140.dp,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    Column(
        modifier = modifier.width(cardSize),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Card image skeleton
        Box(
            modifier = Modifier
                .size(cardSize)
                .shimmer(shape = shape)
        )

        // Title line
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(13.dp)
                .shimmer(RoundedCornerShape(4.dp))
        )

        // Subtitle line
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(11.dp)
                .shimmer(RoundedCornerShape(4.dp))
        )
    }
}
