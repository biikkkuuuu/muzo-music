package com.example.muzo.ui.components

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.muzo.ui.components.backdrop.Backdrop
import com.example.muzo.ui.components.backdrop.drawBackdrop
import com.example.muzo.ui.components.backdrop.effects.blur
import com.example.muzo.ui.components.backdrop.effects.colorControls
import com.example.muzo.ui.components.backdrop.effects.lens
import com.example.muzo.ui.components.backdrop.highlight.Highlight
import com.example.muzo.ui.components.backdrop.shadow.Shadow

/**
 * User-configurable parameters of the liquid glass effect.
 */
@Stable
data class GlassEffectConfig(
    val globalEnabled: Boolean = true,
    val vibrancy: Float = 1f,
    val blurRadius: Float = 8f,
    val lensHeight: Float = 0.5f,
    val lensAmount: Float = 0.5f,
    val chromaticAberration: Boolean = true,
    val depthEffect: Boolean = true,
    val surfaceTintColor: Color = Color.Unspecified,
    val surfaceOpacity: Float = 0.4f,
    val textColor: Color = Color.Unspecified,
    val playerEnabled: Boolean = true,
    val miniPlayerEnabled: Boolean = true,
    val navBarEnabled: Boolean = true,
) {
    fun isEnabledFor(component: GlassComponent): Boolean =
        globalEnabled && when (component) {
            GlassComponent.PLAYER -> playerEnabled
            GlassComponent.MINI_PLAYER -> miniPlayerEnabled
            GlassComponent.NAV_BAR -> navBarEnabled
        }
}

/** UI surfaces that can individually opt in or out of the liquid glass effect. */
enum class GlassComponent {
    PLAYER,
    MINI_PLAYER,
    NAV_BAR,
}

internal const val LENS_MAX_DP = 48f
internal const val PLAYER_BLUR_MULTIPLIER = 4f
internal const val MIN_GLASS_RESOLUTION_SCALE = 0.33f
internal const val FULL_QUALITY_BLUR_DP = 8f

fun glassResolutionScale(blurRadiusDp: Float): Float {
    val t = (blurRadiusDp / FULL_QUALITY_BLUR_DP).coerceIn(0f, 1f)
    return 1f - t * (1f - MIN_GLASS_RESOLUTION_SCALE)
}

fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.S

fun glassSaturation(vibrancy: Float): Float = 1f + 0.5f * vibrancy.coerceIn(0f, 2f)

val LocalGlassEffectConfig = staticCompositionLocalOf { GlassEffectConfig() }

/** The backdrop content (app UI) that glass surfaces sample from. */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Renders this composable as a liquid glass surface sampling [LocalAppBackdrop].
 */
@Composable
fun Modifier.liquidGlass(
    config: GlassEffectConfig = LocalGlassEffectConfig.current,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    applyEdgeEffects: Boolean = true,
    blurRadiusDp: Float = config.blurRadius,
): Modifier {
    if (!isGlassSupported()) return this
    val backdrop = LocalAppBackdrop.current ?: return this
    val density = LocalDensity.current
    val resolutionScale = glassResolutionScale(blurRadiusDp)
    val blurPx = with(density) { blurRadiusDp.dp.toPx() } * resolutionScale
    val saturation = glassSaturation(config.vibrancy)
    val lensHeightPx = with(density) { (config.lensHeight * LENS_MAX_DP).dp.toPx() } * resolutionScale
    val lensAmountPx = with(density) { (config.lensAmount * LENS_MAX_DP).dp.toPx() } * resolutionScale

    val surfaceTintColor = if (config.surfaceTintColor.isSpecified) {
        config.surfaceTintColor
    } else if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (saturation != 1f) {
                colorControls(saturation = saturation)
            }
            if (blurPx > 0f) {
                blur(blurPx)
            }
            if (applyEdgeEffects &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (lensHeightPx > 0f || lensAmountPx > 0f)
            ) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = config.depthEffect,
                    chromaticAberration = config.chromaticAberration,
                )
            }
        },
        highlight = if (applyEdgeEffects) ({ Highlight.Default }) else null,
        shadow = if (applyEdgeEffects) ({ Shadow.Default }) else null,
        onDrawSurface = {
            if (config.surfaceOpacity > 0f) {
                drawRect(
                    color = surfaceTintColor.copy(alpha = config.surfaceOpacity),
                    size = size,
                )
            }
        },
        backdropScale = resolutionScale,
    )
}
