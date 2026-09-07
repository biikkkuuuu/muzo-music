package com.example.muzo.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PlayerColorExtractor: Echo-Music 1:1 Color Extraction Engine
 * Extracts weighted vibrant and gradient swatches from album art bitmaps
 * and enhances saturation and brightness for AMOLED contrast.
 */
object PlayerColorExtractor {

    suspend fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int
    ): List<Color> = withContext(Dispatchers.Default) {
        val colorCandidates = listOfNotNull(
            palette.dominantSwatch,
            palette.vibrantSwatch,
            palette.darkVibrantSwatch,
            palette.lightVibrantSwatch,
            palette.mutedSwatch,
            palette.darkMutedSwatch,
            palette.lightMutedSwatch
        )

        val bestSwatch = colorCandidates.maxByOrNull { calculateColorWeight(it) }
        val fallbackDominant = palette.dominantSwatch?.rgb?.let { Color(it) }
            ?: Color(palette.getDominantColor(fallbackColor))

        val primaryColor = if (bestSwatch != null) {
            val bestColor = Color(bestSwatch.rgb)
            if (isColorVibrant(bestColor)) {
                enhanceColorEchodness(bestColor, Config.VIBRANT_SATURATION_FACTOR)
            } else {
                enhanceColorEchodness(fallbackDominant, Config.FALLBACK_SATURATION_FACTOR)
            }
        } else {
            enhanceColorEchodness(fallbackDominant, Config.FALLBACK_SATURATION_FACTOR)
        }

        listOf(
            primaryColor,
            primaryColor.copy(
                red = (primaryColor.red * Config.DARKER_VARIANT_FACTOR).coerceAtLeast(0f),
                green = (primaryColor.green * Config.DARKER_VARIANT_FACTOR).coerceAtLeast(0f),
                blue = (primaryColor.blue * Config.DARKER_FACTOR).coerceAtLeast(0f)
            ),
            Color.Black
        )
    }

    fun extractPrimaryThemeColor(palette: Palette, fallbackColor: Int): Color {
        val colorCandidates = listOfNotNull(
            palette.dominantSwatch,
            palette.vibrantSwatch,
            palette.darkVibrantSwatch,
            palette.lightVibrantSwatch,
            palette.mutedSwatch,
            palette.darkMutedSwatch,
            palette.lightMutedSwatch
        )

        val bestSwatch = colorCandidates.maxByOrNull { calculateColorWeight(it) }
        val fallbackDominant = palette.dominantSwatch?.rgb?.let { Color(it) }
            ?: Color(palette.getDominantColor(fallbackColor))

        return if (bestSwatch != null) {
            val bestColor = Color(bestSwatch.rgb)
            if (isColorVibrant(bestColor)) {
                enhanceColorEchodness(bestColor, Config.VIBRANT_SATURATION_FACTOR)
            } else {
                enhanceColorEchodness(fallbackDominant, Config.FALLBACK_SATURATION_FACTOR)
            }
        } else {
            enhanceColorEchodness(fallbackDominant, Config.FALLBACK_SATURATION_FACTOR)
        }
    }

    fun isColorVibrant(color: Color): Boolean {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val saturation = hsv[1]
        val brightness = hsv[2]
        return saturation > Config.VIBRANT_SATURATION_THRESHOLD &&
                brightness > Config.VIBRANT_BRIGHTNESS_MIN &&
                brightness < Config.VIBRANT_BRIGHTNESS_MAX
    }

    fun enhanceColorEchodness(color: Color, saturationFactor: Float = Config.DEFAULT_SATURATION_FACTOR): Color {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)

        hsv[1] = (hsv[1] * saturationFactor).coerceAtMost(1.0f)
        hsv[2] = (hsv[2] * Config.BRIGHTNESS_MULTIPLIER).coerceIn(Config.BRIGHTNESS_MIN, Config.BRIGHTNESS_MAX)

        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    fun calculateColorWeight(swatch: Palette.Swatch?): Float {
        if (swatch == null) return 0f
        val population = swatch.population.toFloat()
        val color = Color(swatch.rgb)
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val saturation = hsv[1]
        val brightness = hsv[2]

        val populationWeight = population * Config.POPULATION_WEIGHT_MULTIPLIER
        val vibrancyBonus = if (saturation > Config.VIBRANCY_THRESHOLD_SATURATION && brightness > Config.VIBRANCY_THRESHOLD_BRIGHTNESS) Config.VIBRANCY_BONUS else 1f

        return populationWeight * vibrancyBonus * (saturation + brightness) / 2f
    }

    object Config {
        const val MAX_COLOR_COUNT = 32
        const val BITMAP_AREA = 8000
        const val IMAGE_SIZE = 200

        const val VIBRANT_SATURATION_THRESHOLD = 0.25f
        const val VIBRANT_BRIGHTNESS_MIN = 0.2f
        const val VIBRANT_BRIGHTNESS_MAX = 0.9f

        const val POPULATION_WEIGHT_MULTIPLIER = 2f
        const val VIBRANCY_THRESHOLD_SATURATION = 0.3f
        const val VIBRANCY_THRESHOLD_BRIGHTNESS = 0.3f
        const val VIBRANCY_BONUS = 1.5f

        const val DEFAULT_SATURATION_FACTOR = 1.4f
        const val VIBRANT_SATURATION_FACTOR = 1.3f
        const val FALLBACK_SATURATION_FACTOR = 1.1f

        const val BRIGHTNESS_MULTIPLIER = 0.9f
        const val BRIGHTNESS_MIN = 0.4f
        const val BRIGHTNESS_MAX = 0.85f

        const val DARKER_VARIANT_FACTOR = 0.6f
        const val DARKER_FACTOR = 0.6f
    }
}
