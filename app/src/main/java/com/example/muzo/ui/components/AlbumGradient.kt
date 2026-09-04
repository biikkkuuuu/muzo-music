package com.example.muzo.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dynamic ambient gradient background for the player sheet that extracts vibrant colors
 * from the album art and smoothly animates color shifts between songs.
 */
@Composable
fun AlbumGradient(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val fallbackBaseColor = Color(0xFF14131A)
    val fallbackMidColor = Color(0xFF1B1A24)

    var extractedColors by remember { mutableStateOf<Pair<Color, Color>?>(null) }

    LaunchedEffect(thumbnailUrl) {
        if (!thumbnailUrl.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .allowHardware(false)
                        .size(120, 120)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(12)
                                    .generate()
                            }
                            val topColor = extractVibrantColor(palette)
                            val midColor = topColor.copy(
                                red = (topColor.red * 0.45f).coerceAtLeast(0f),
                                green = (topColor.green * 0.45f).coerceAtLeast(0f),
                                blue = (topColor.blue * 0.45f).coerceAtLeast(0f)
                            )
                            extractedColors = Pair(topColor, midColor)
                        }
                    }
                } catch (_: Exception) {}
            }
        } else {
            extractedColors = null
        }
    }

    val animatedTopColor by animateColorAsState(
        targetValue = extractedColors?.first?.copy(alpha = 0.55f) ?: fallbackMidColor,
        animationSpec = tween(durationMillis = 700),
        label = "ambientTopColor"
    )

    val animatedMidColor by animateColorAsState(
        targetValue = extractedColors?.second?.copy(alpha = 0.28f) ?: fallbackBaseColor,
        animationSpec = tween(durationMillis = 700),
        label = "ambientMidColor"
    )

    val amoledBottom = Color(0xFF0E0D14)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(amoledBottom)
            .background(
                Brush.verticalGradient(
                    0.0f to animatedTopColor,
                    0.45f to animatedMidColor,
                    0.95f to amoledBottom,
                    1.0f to amoledBottom
                )
            )
    ) {
        content()
    }
}

private fun extractVibrantColor(palette: Palette): Color {
    val swatch = palette.vibrantSwatch
        ?: palette.darkVibrantSwatch
        ?: palette.dominantSwatch
        ?: palette.mutedSwatch
        ?: palette.lightVibrantSwatch

    if (swatch != null) {
        val argb = swatch.rgb
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        // Enhance saturation slightly and clamp brightness for elegant AMOLED dark look
        hsv[1] = (hsv[1] * 1.3f).coerceIn(0.35f, 0.95f)
        hsv[2] = (hsv[2] * 0.85f).coerceIn(0.45f, 0.85f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
    return Color(0xFF4C7DE8)
}
