package com.example.muzo.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.muzo.core.getHighResThumbnail
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dynamic Song-Driven Theming Engine (1:1 ViVi Music Match).
 * Extracts seed colors from the playing song's thumbnail, generating accessible,
 * luminous Material 3 tokens that smoothly morph across the entire app with 650ms transitions.
 */
@Composable
fun DynamicSongTheme(
    currentSong: SongItem?,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var extractedSeedColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(currentSong?.id, currentSong?.thumbnail) {
        val rawThumb = currentSong?.thumbnail
        if (!rawThumb.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    // First try raw thumbnail for instant cache hit
                    val request = ImageRequest.Builder(context)
                        .data(rawThumb)
                        .allowHardware(false)
                        .size(100, 100)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(16)
                                    .generate()
                            }
                            extractedSeedColor = extractDynamicSeedColor(palette)
                        }
                    } else {
                        // Fallback to high res url
                        val hiResUrl = getHighResThumbnail(rawThumb)
                        val hiResReq = ImageRequest.Builder(context)
                            .data(hiResUrl)
                            .allowHardware(false)
                            .size(100, 100)
                            .build()
                        val hiResRes = loader.execute(hiResReq)
                        if (hiResRes is SuccessResult) {
                            val bmp = (hiResRes.drawable as? BitmapDrawable)?.bitmap
                            if (bmp != null) {
                                val pal = withContext(Dispatchers.Default) {
                                    Palette.from(bmp).maximumColorCount(16).generate()
                                }
                                extractedSeedColor = extractDynamicSeedColor(pal)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Retain existing color or let fallback handle it
                }
            }
        } else {
            extractedSeedColor = null
        }
    }

    // Default Neutral / AMOLED Stealth Palette when no song is playing
    val defaultPrimary = Color(0xFFE2E4EB)
    val defaultSurfaceContainerHigh = Color(0xFF1B1A22)
    val defaultSurfaceContainer = Color(0xFF14131A)
    val defaultPrimaryContainer = Color(0xFF262530)
    val defaultOutlineVariant = Color(0x2AFFFFFF)
    val defaultOnSurfaceVariant = Color(0xFF9EA3B0)

    val targetTokens = remember(extractedSeedColor) {
        if (extractedSeedColor != null) {
            computeDynamicTokens(extractedSeedColor!!)
        } else {
            DynamicThemeTokens(
                primary = defaultPrimary,
                onPrimary = Color(0xFF121216),
                primaryContainer = defaultPrimaryContainer,
                onPrimaryContainer = Color(0xFFEAEAF0),
                surfaceContainer = defaultSurfaceContainer,
                surfaceContainerHigh = defaultSurfaceContainerHigh,
                surfaceContainerHighest = Color(0xFF24232E),
                outlineVariant = defaultOutlineVariant,
                onSurfaceVariant = defaultOnSurfaceVariant
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
    val animatedOutlineVariant by animateColorAsState(targetTokens.outlineVariant, morphSpec, label = "dynOutlineVariant")
    val animatedOnSurfaceVariant by animateColorAsState(targetTokens.onSurfaceVariant, morphSpec, label = "dynOnSurfaceVariant")

    val dynamicColorScheme = darkColorScheme(
        primary = animatedPrimary,
        onPrimary = animatedOnPrimary,
        primaryContainer = animatedPrimaryContainer,
        onPrimaryContainer = animatedOnPrimaryContainer,
        surface = Color(0xFF0F0E13),
        surfaceContainer = animatedSurfaceContainer,
        surfaceContainerHigh = animatedSurfaceContainerHigh,
        surfaceContainerHighest = animatedSurfaceContainerHighest,
        background = Color(0xFF08080A),
        onBackground = Color(0xFFEEEEF2),
        onSurface = Color(0xFFEEEEF2),
        onSurfaceVariant = animatedOnSurfaceVariant,
        outlineVariant = animatedOutlineVariant
    )

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
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
    val outlineVariant: Color,
    val onSurfaceVariant: Color
)

private fun extractDynamicSeedColor(palette: Palette): Color {
    val swatch = palette.vibrantSwatch
        ?: palette.dominantSwatch
        ?: palette.lightVibrantSwatch
        ?: palette.darkVibrantSwatch
        ?: palette.mutedSwatch

    if (swatch != null) {
        return Color(swatch.rgb)
    }
    return Color(0xFF7E9FD9)
}

private fun computeDynamicTokens(seed: Color): DynamicThemeTokens {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    val hue = hsv[0]

    // 1. Primary: Luminous vibrant accent tone (ViVi signature accent for titles, pills, icons)
    val primaryHsv = floatArrayOf(
        hue,
        (hsv[1] * 1.15f).coerceIn(0.42f, 0.82f),
        (hsv[2] * 1.15f).coerceIn(0.78f, 0.96f)
    )
    val primary = Color(android.graphics.Color.HSVToColor(primaryHsv))

    // 2. OnPrimary: High-contrast text on primary
    val onPrimary = if (primaryHsv[2] > 0.70f && primaryHsv[1] < 0.65f) Color(0xFF0D0E12) else Color.White

    // 3. PrimaryContainer (Active Tab Capsule Pill & Badges):
    // Visibly glowing tint matching the song
    val primaryContainer = primary.copy(alpha = 0.28f)
    val onPrimaryContainer = primary

    // 4. SurfaceContainerHigh (MiniPlayer & BottomDock background):
    // RICH, VISIBLE dark tone reflecting the song's hue (exact ViVi #1A2436 / #241A1C)
    val surfaceHighHsv = floatArrayOf(
        hue,
        (hsv[1] * 0.45f).coerceIn(0.22f, 0.42f),
        0.18f
    )
    val surfaceContainerHigh = Color(android.graphics.Color.HSVToColor(surfaceHighHsv))

    // 5. SurfaceContainer (nested background layers)
    val surfaceHsv = floatArrayOf(
        hue,
        (hsv[1] * 0.35f).coerceIn(0.18f, 0.35f),
        0.14f
    )
    val surfaceContainer = Color(android.graphics.Color.HSVToColor(surfaceHsv))

    // 6. SurfaceContainerHighest
    val surfaceHighestHsv = floatArrayOf(
        hue,
        (hsv[1] * 0.45f).coerceIn(0.25f, 0.45f),
        0.22f
    )
    val surfaceContainerHighest = Color(android.graphics.Color.HSVToColor(surfaceHighestHsv))

    // 7. OutlineVariant: Luminous subtle halo border for MiniPlayer & Bottom Bar
    val outlineVariant = primary.copy(alpha = 0.32f)

    // 8. OnSurfaceVariant: Soft tinted tone for subtitles and icons
    val onSurfaceVariantHsv = floatArrayOf(
        hue,
        0.30f,
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
        outlineVariant = outlineVariant,
        onSurfaceVariant = onSurfaceVariant
    )
}
