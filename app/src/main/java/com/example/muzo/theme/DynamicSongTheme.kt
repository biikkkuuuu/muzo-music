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
 * Echo Music 1:1 Default Theme Color (Warm Dusty Rose / Coral Tone)
 */
val EchoDefaultThemeColor = Color(0xFFED5564)

/**
 * DynamicSongTheme: Echo Music 1:1 Material You Monet Architecture
 * Produces minimal, elegant, pastel-toned colors (TonalSpot style) that smoothly
 * respond to the currently playing song without harsh or loud oversaturation.
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

    val fallbackColor = EchoDefaultThemeColor.toArgb()

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

    val systemScheme = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        androidx.compose.material3.dynamicDarkColorScheme(context)
    } else null

    val targetTokens = remember(extractedSeedColor, pureBlack, systemScheme) {
        if (extractedSeedColor != null) {
            computeMonetTokens(extractedSeedColor!!, pureBlack)
        } else if (systemScheme != null) {
            DynamicThemeTokens(
                primary = systemScheme.primary,
                onPrimary = systemScheme.onPrimary,
                primaryContainer = systemScheme.primaryContainer,
                onPrimaryContainer = systemScheme.onPrimaryContainer,
                surface = if (pureBlack) Color.Black else systemScheme.surface,
                surfaceContainer = if (pureBlack) Color(0xFF0C0C0E) else systemScheme.surfaceContainer,
                surfaceContainerHigh = if (pureBlack) Color(0xFF141418) else systemScheme.surfaceContainerHigh,
                surfaceContainerHighest = if (pureBlack) Color(0xFF1C1C22) else systemScheme.surfaceContainerHighest,
                surfaceContainerLow = if (pureBlack) Color(0xFF08080A) else systemScheme.surfaceContainerLow,
                background = if (pureBlack) Color.Black else systemScheme.background,
                outlineVariant = systemScheme.outlineVariant,
                outline = systemScheme.outline,
                onSurface = systemScheme.onSurface,
                onSurfaceVariant = systemScheme.onSurfaceVariant
            )
        } else {
            computeMonetTokens(EchoDefaultThemeColor, pureBlack)
        }
    }

    val morphSpec = remember {
        tween<Color>(durationMillis = 650, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))
    }

    val animatedPrimary by animateColorAsState(targetTokens.primary, morphSpec, label = "dynPrimary")
    val animatedOnPrimary by animateColorAsState(targetTokens.onPrimary, morphSpec, label = "dynOnPrimary")
    val animatedPrimaryContainer by animateColorAsState(targetTokens.primaryContainer, morphSpec, label = "dynPrimaryContainer")
    val animatedOnPrimaryContainer by animateColorAsState(targetTokens.onPrimaryContainer, morphSpec, label = "dynOnPrimaryContainer")
    val animatedSurface by animateColorAsState(targetTokens.surface, morphSpec, label = "dynSurface")
    val animatedSurfaceContainer by animateColorAsState(targetTokens.surfaceContainer, morphSpec, label = "dynSurfaceContainer")
    val animatedSurfaceContainerHigh by animateColorAsState(targetTokens.surfaceContainerHigh, morphSpec, label = "dynSurfaceContainerHigh")
    val animatedSurfaceContainerHighest by animateColorAsState(targetTokens.surfaceContainerHighest, morphSpec, label = "dynSurfaceContainerHighest")
    val animatedSurfaceContainerLow by animateColorAsState(targetTokens.surfaceContainerLow, morphSpec, label = "dynSurfaceContainerLow")
    val animatedBackground by animateColorAsState(targetTokens.background, morphSpec, label = "dynBackground")
    val animatedOutlineVariant by animateColorAsState(targetTokens.outlineVariant, morphSpec, label = "dynOutlineVariant")
    val animatedOutline by animateColorAsState(targetTokens.outline, morphSpec, label = "dynOutline")
    val animatedOnSurface by animateColorAsState(targetTokens.onSurface, morphSpec, label = "dynOnSurface")
    val animatedOnSurfaceVariant by animateColorAsState(targetTokens.onSurfaceVariant, morphSpec, label = "dynOnSurfaceVariant")

    val dynamicColorScheme = darkColorScheme(
        primary = animatedPrimary,
        onPrimary = animatedOnPrimary,
        primaryContainer = animatedPrimaryContainer,
        onPrimaryContainer = animatedOnPrimaryContainer,
        surface = animatedSurface,
        surfaceContainer = animatedSurfaceContainer,
        surfaceContainerHigh = animatedSurfaceContainerHigh,
        surfaceContainerHighest = animatedSurfaceContainerHighest,
        surfaceContainerLow = animatedSurfaceContainerLow,
        background = animatedBackground,
        onBackground = animatedOnSurface,
        onSurface = animatedOnSurface,
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
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val background: Color,
    val outlineVariant: Color,
    val outline: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color
)

/**
 * Generates soft, pastel Monet tones (TonalSpot style) matching Android OS & Echo Music.
 * High saturation is clamped to an organic 0.24f-0.30f, avoiding screaming neon highlights.
 */
private fun computeMonetTokens(seed: Color, pureBlack: Boolean): DynamicThemeTokens {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    val hue = hsv[0]

    // 1. Primary: Soft, luminous pastel tone (Tonal level 80)
    // Minimal saturation (0.24f - 0.32f), high brightness (0.88f - 0.94f)
    val pastelSaturation = (hsv[1] * 0.45f).coerceIn(0.24f, 0.32f)
    val primaryHsv = floatArrayOf(hue, pastelSaturation, 0.92f)
    val primary = Color(android.graphics.Color.HSVToColor(primaryHsv))

    // 2. OnPrimary: Deep contrast tone for play button & primary pills
    val onPrimary = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.60f, 0.14f)))

    // 3. PrimaryContainer: Soft translucent capsule for active tabs & badges
    val primaryContainer = primary.copy(alpha = 0.28f)
    val onPrimaryContainer = Color.White

    // 4. Surfaces: Deep AMOLED with subtle warm tonal hue (Tonal levels 6 to 18)
    val surfaceHueSat = (hsv[1] * 0.20f).coerceIn(0.10f, 0.18f)

    val bgHsv = floatArrayOf(hue, surfaceHueSat, if (pureBlack) 0.0f else 0.06f)
    val background = if (pureBlack) Color.Black else Color(android.graphics.Color.HSVToColor(bgHsv))

    val surfHsv = floatArrayOf(hue, surfaceHueSat, if (pureBlack) 0.0f else 0.09f)
    val surface = if (pureBlack) Color.Black else Color(android.graphics.Color.HSVToColor(surfHsv))

    val surfLowHsv = floatArrayOf(hue, surfaceHueSat, if (pureBlack) 0.02f else 0.11f)
    val surfaceContainerLow = Color(android.graphics.Color.HSVToColor(surfLowHsv))

    val surfContHsv = floatArrayOf(hue, surfaceHueSat, if (pureBlack) 0.06f else 0.14f)
    val surfaceContainer = Color(android.graphics.Color.HSVToColor(surfContHsv))

    val surfHighHsv = floatArrayOf(hue, surfaceHueSat, if (pureBlack) 0.10f else 0.18f)
    val surfaceContainerHigh = Color(android.graphics.Color.HSVToColor(surfHighHsv))

    val surfHighestHsv = floatArrayOf(hue, surfaceHueSat, if (pureBlack) 0.14f else 0.22f)
    val surfaceContainerHighest = Color(android.graphics.Color.HSVToColor(surfHighestHsv))

    // 5. Outlines: Subtle border lines (Tonal Level 30)
    val outlineVariant = primary.copy(alpha = 0.18f)
    val outline = primary.copy(alpha = 0.35f)

    // 6. OnSurface & OnSurfaceVariant: Crisp readable off-white and soft warm gray
    val onSurfaceHsv = floatArrayOf(hue, 0.08f, 0.95f)
    val onSurface = Color(android.graphics.Color.HSVToColor(onSurfaceHsv))

    val onSurfaceVarHsv = floatArrayOf(hue, 0.14f, 0.65f)
    val onSurfaceVariant = Color(android.graphics.Color.HSVToColor(onSurfaceVarHsv))

    return DynamicThemeTokens(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        surface = surface,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        background = background,
        outlineVariant = outlineVariant,
        outline = outline,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant
    )
}
