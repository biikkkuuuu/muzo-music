package com.example.muzo.theme

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.muzo.core.getHighResThumbnail
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DynamicSongTheme: Echo-Music 1:1 Dynamic Color Architecture
 * Extracts colors from current playing song artwork and adapts theme colors
 * across all surfaces, cards, borders, and controls with exact opacities.
 */
@Composable
fun DynamicSongTheme(
    currentSong: SongItem?,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var extractedSeedColor by remember { mutableStateOf<Color?>(null) }
    var extractedGradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    val fallbackColor = Color(0xFFED5564).toArgb()

    LaunchedEffect(currentSong?.id) {
        val rawThumb = currentSong?.thumbnail
        if (!rawThumb.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = context.imageLoader
                    val req = ImageRequest.Builder(context)
                        .data(rawThumb)
                        .allowHardware(false)
                        .size(100, 100)
                        .build()
                    val res = loader.execute(req)
                    var bitmap = (res as? SuccessResult)?.drawable?.let { (it as? BitmapDrawable)?.bitmap }

                    if (bitmap == null) {
                        val hiResUrl = getHighResThumbnail(rawThumb)
                        val hiResReq = ImageRequest.Builder(context)
                            .data(hiResUrl)
                            .allowHardware(false)
                            .size(100, 100)
                            .build()
                        val hiResRes = loader.execute(hiResReq)
                        bitmap = (hiResRes as? SuccessResult)?.drawable?.let { (it as? BitmapDrawable)?.bitmap }
                    }

                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(32)
                                .resizeBitmapArea(100 * 100)
                                .generate()
                        }

                        val primaryThemeColor = PlayerColorExtractor.extractPrimaryThemeColor(palette, fallbackColor)
                        val gradient = PlayerColorExtractor.extractGradientColors(palette, fallbackColor)

                        withContext(Dispatchers.Main) {
                            extractedSeedColor = primaryThemeColor
                            extractedGradientColors = gradient
                        }
                    }
                } catch (_: Exception) {
                    // Retain existing color
                }
            }
        } else {
            extractedSeedColor = null
            extractedGradientColors = emptyList()
        }
    }

    // Default Neutral / AMOLED Stealth Palette when no song is playing
    val defaultPrimary = Color(0xFFED5564) // Echo Default Theme Color
    val defaultSurfaceContainerHigh = Color(0xFF1B1A22)
    val defaultSurfaceContainer = Color(0xFF14131A)
    val defaultPrimaryContainer = Color(0xFF262530)
    val defaultOutlineVariant = Color(0x2AFFFFFF)
    val defaultOnSurfaceVariant = Color(0xFF9EA3B0)

    val targetTokens = remember(extractedSeedColor) {
        if (extractedSeedColor != null) {
            computeDynamicTokens(extractedSeedColor!!, pureBlack)
        } else {
            DynamicThemeTokens(
                primary = defaultPrimary,
                onPrimary = Color(0xFF121216),
                primaryContainer = defaultPrimaryContainer,
                onPrimaryContainer = Color(0xFFEAEAF0),
                surfaceContainer = if (pureBlack) Color(0xFF0A0A0E) else defaultSurfaceContainer,
                surfaceContainerHigh = if (pureBlack) Color(0xFF121216) else defaultSurfaceContainerHigh,
                surfaceContainerHighest = if (pureBlack) Color(0xFF18181E) else Color(0xFF24232E),
                surfaceContainerLow = if (pureBlack) Color.Black else Color(0xFF0F0E13),
                outlineVariant = defaultOutlineVariant,
                onSurfaceVariant = defaultOnSurfaceVariant,
                outline = defaultPrimary.copy(alpha = 0.50f)
            )
        }
    }

    val morphSpec = remember {
        tween<Color>(durationMillis = 650, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))
    }

    val animatedPrimary by animateColorAsState(targetTokens.primary, morphSpec, label = "dynPrimary")
    val animatedOnPrimary by animateColorAsState(targetTokens.onPrimary, morphSpec, label = "dynOnPrimary")
    val animatedPrimaryContainer by animateColorAsState(targetTokens.primaryContainer, morphSpec, label = "dynPrimaryContainer")
    val animatedOnPrimaryContainer by animateColorAsState(targetTokens.onPrimaryContainer, morphSpec, label = "dynOnPrimaryContainer")
    val animatedSurfaceContainer by animateColorAsState(targetTokens.surfaceContainer, morphSpec, label = "dynSurfaceContainer")
    val animatedSurfaceContainerHigh by animateColorAsState(targetTokens.surfaceContainerHigh, morphSpec, label = "dynSurfaceContainerHigh")
    val animatedSurfaceContainerHighest by animateColorAsState(targetTokens.surfaceContainerHighest, morphSpec, label = "dynSurfaceContainerHighest")
    val animatedSurfaceContainerLow by animateColorAsState(targetTokens.surfaceContainerLow, morphSpec, label = "dynSurfaceContainerLow")
    val animatedOutlineVariant by animateColorAsState(targetTokens.outlineVariant, morphSpec, label = "dynOutlineVariant")
    val animatedOutline by animateColorAsState(targetTokens.outline, morphSpec, label = "dynOutline")
    val animatedOnSurfaceVariant by animateColorAsState(targetTokens.onSurfaceVariant, morphSpec, label = "dynOnSurfaceVariant")

    val dynamicColorScheme = darkColorScheme(
        primary = animatedPrimary,
        onPrimary = animatedOnPrimary,
        primaryContainer = animatedPrimaryContainer,
        onPrimaryContainer = animatedOnPrimaryContainer,
        surface = if (pureBlack) Color.Black else Color(0xFF0F0E13),
        surfaceContainer = animatedSurfaceContainer,
        surfaceContainerHigh = animatedSurfaceContainerHigh,
        surfaceContainerHighest = animatedSurfaceContainerHighest,
        surfaceContainerLow = animatedSurfaceContainerLow,
        background = if (pureBlack) Color.Black else Color(0xFF08080A),
        onBackground = Color(0xFFEEEEF2),
        onSurface = Color(0xFFEEEEF2),
        onSurfaceVariant = animatedOnSurfaceVariant,
        outline = animatedOutline,
        outlineVariant = animatedOutlineVariant
    )

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = AppTypography,
        shapes = MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}

private data class DynamicThemeTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val outlineVariant: Color,
    val outline: Color,
    val onSurfaceVariant: Color
)

private fun computeDynamicTokens(seed: Color, pureBlack: Boolean): DynamicThemeTokens {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    val hue = hsv[0]

    // 1. Primary: Luminous vibrant accent tone (Echo signature accent for titles, active pills, icons)
    val primaryHsv = floatArrayOf(
        hue,
        (hsv[1] * 1.3f).coerceIn(0.40f, 0.85f),
        (hsv[2] * 0.95f).coerceIn(0.75f, 0.95f)
    )
    val primary = Color(android.graphics.Color.HSVToColor(primaryHsv))

    // 2. OnPrimary: High-contrast text on primary
    val onPrimary = if (primaryHsv[2] > 0.70f && primaryHsv[1] < 0.65f) Color(0xFF0D0E12) else Color.White

    // 3. PrimaryContainer (Active Tab Capsule Pill & Badges):
    // 28% opacity matching Echo container tokens
    val primaryContainer = primary.copy(alpha = 0.28f)
    val onPrimaryContainer = Color.White

    // 4. SurfaceContainerHigh (Cards, Settings Groups, BottomDock):
    // 35% opacity on surface variant or subtle hue tint
    val surfaceHighHsv = floatArrayOf(
        hue,
        (hsv[1] * 0.40f).coerceIn(0.20f, 0.40f),
        if (pureBlack) 0.10f else 0.18f
    )
    val surfaceContainerHigh = Color(android.graphics.Color.HSVToColor(surfaceHighHsv))

    // 5. SurfaceContainer (Nested background layers)
    val surfaceHsv = floatArrayOf(
        hue,
        (hsv[1] * 0.30f).coerceIn(0.15f, 0.32f),
        if (pureBlack) 0.06f else 0.14f
    )
    val surfaceContainer = Color(android.graphics.Color.HSVToColor(surfaceHsv))

    // 6. SurfaceContainerHighest
    val surfaceHighestHsv = floatArrayOf(
        hue,
        (hsv[1] * 0.40f).coerceIn(0.22f, 0.42f),
        if (pureBlack) 0.12f else 0.22f
    )
    val surfaceContainerHighest = Color(android.graphics.Color.HSVToColor(surfaceHighestHsv))

    val surfaceContainerLow = if (pureBlack) Color.Black else Color(0xFF0F0E13)

    // 7. Outline & OutlineVariant: 32% halo border matching Echo
    val outlineVariant = primary.copy(alpha = 0.32f)
    val outline = primary.copy(alpha = 0.50f)

    // 8. OnSurfaceVariant: Soft tinted tone for subtitles and icons
    val onSurfaceVariantHsv = floatArrayOf(
        hue,
        0.28f,
        0.82f
    )
    val onSurfaceVariant = Color(android.graphics.Color.HSVToColor(onSurfaceVariantHsv))

    return DynamicThemeTokens(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        outlineVariant = outlineVariant,
        outline = outline,
        onSurfaceVariant = onSurfaceVariant
    )
}
