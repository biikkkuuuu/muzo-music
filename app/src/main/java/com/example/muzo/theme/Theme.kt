package com.example.muzo.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    useDynamicColor: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isSystemDynamic = useDynamicColor && (themeColor == DefaultThemeColor) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val baseColorScheme = if (isSystemDynamic) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        remember(themeColor, darkTheme) {
            generateMetrolistColorScheme(themeColor, darkTheme)
        }
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}

/**
 * Generates a full Material 3 ColorScheme based on a seed color using
 * authentic TonalSpot minimal pastel aesthetics (0.24f-0.30f saturation for dark mode).
 */
fun generateMetrolistColorScheme(seed: Color, isDark: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    val hue = hsv[0]

    return if (isDark) {
        val sat = (hsv[1] * 0.40f).coerceIn(0.22f, 0.30f)
        val primary = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 0.92f)))
        val onPrimary = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.60f, 0.15f)))
        val primaryContainer = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.8f, 0.25f)))
        val onPrimaryContainer = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.15f, 0.95f)))

        val surfHueSat = (hsv[1] * 0.12f).coerceIn(0.06f, 0.14f)
        val background = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, surfHueSat, 0.06f)))
        val surface = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, surfHueSat, 0.08f)))
        val surfaceContainerLow = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, surfHueSat, 0.10f)))
        val surfaceContainer = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, surfHueSat, 0.13f)))
        val surfaceContainerHigh = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, surfHueSat, 0.17f)))
        val surfaceContainerHighest = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, surfHueSat, 0.22f)))

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            surface = surface,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            background = background,
            onBackground = Color(0xFFEDE0E2),
            onSurface = Color(0xFFEDE0E2),
            onSurfaceVariant = Color(0xFF9E9294),
            outline = primary.copy(alpha = 0.35f),
            outlineVariant = primary.copy(alpha = 0.18f)
        )
    } else {
        val sat = hsv[1].coerceIn(0.40f, 0.80f)
        val primary = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 0.60f)))
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            background = Color(0xFFFCF8F8),
            surface = Color(0xFFFCF8F8),
            surfaceContainer = Color(0xFFF0EAEB),
            surfaceContainerHigh = Color(0xFFE8E2E3),
            onSurface = Color(0xFF1E1A1B),
            onSurfaceVariant = Color(0xFF514345)
        )
    }
}
