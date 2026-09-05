package com.example.muzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

fun Modifier.fadingEdge(
    bottom: Dp? = null,
    top: Dp? = null
) = graphicsLayer(alpha = 0.99f)
    .drawWithContent {
        drawContent()
        if (top != null) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = top.toPx()
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (bottom != null) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - bottom.toPx(),
                    endY = size.height
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * Renders a heavily blurred ambient background directly from the album/playlist cover image,
 * matching Echo Music's immersive header aesthetic.
 */
@Composable
fun OnlineBlur(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 50.dp,
    bottomFade: Dp = 200.dp
) {
    Box(modifier = modifier) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .fadingEdge(bottom = bottomFade)
            )
        }

        // Dark scrim overlay to ensure contrast and smooth fade into AMOLED black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Transparent,
                            Color(0xFF08080A).copy(alpha = 0.70f),
                            Color(0xFF08080A)
                        )
                    )
                )
        )
    }
}
