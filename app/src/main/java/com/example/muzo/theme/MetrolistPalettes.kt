package com.example.muzo.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class ThemePalette(
    val id: String,
    val name: String,
    val color: Color
)

val DefaultThemeColor = Color(0xFFED5564)

/**
 * The 19 signature Metrolist color palettes.
 */
val MetrolistThemePalettes = listOf(
    ThemePalette("crimson", "Crimson", Color(0xFFEC5464)),
    ThemePalette("rose", "Rose", Color(0xFFD81B60)),
    ThemePalette("purple", "Purple", Color(0xFF8E24AA)),
    ThemePalette("deep_purple", "Deep Purple", Color(0xFF5E35B1)),
    ThemePalette("indigo", "Indigo", Color(0xFF3949AB)),
    ThemePalette("blue", "Blue", Color(0xFF1E88E5)),
    ThemePalette("sky_blue", "Sky Blue", Color(0xFF039BE5)),
    ThemePalette("cyan", "Cyan", Color(0xFF00ACC1)),
    ThemePalette("teal", "Teal", Color(0xFF00897B)),
    ThemePalette("green", "Green", Color(0xFF43A047)),
    ThemePalette("light_green", "Light Green", Color(0xFF7CB342)),
    ThemePalette("lime", "Lime", Color(0xFFC0CA33)),
    ThemePalette("yellow", "Yellow", Color(0xFFFDD835)),
    ThemePalette("amber", "Amber", Color(0xFFFFB300)),
    ThemePalette("orange", "Orange", Color(0xFFFB8C00)),
    ThemePalette("deep_orange", "Deep Orange", Color(0xFFF4511E)),
    ThemePalette("brown", "Brown", Color(0xFF6D4C41)),
    ThemePalette("grey", "Grey", Color(0xFF757575)),
    ThemePalette("blue_grey", "Blue Grey", Color(0xFF546E7A))
)

fun ColorScheme.pureBlack(apply: Boolean): ColorScheme =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF111111),
        surfaceContainerHigh = Color(0xFF1A1A1A),
        surfaceContainerHighest = Color(0xFF222222),
        primary = Color(0xFFE0E0E0),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF2A2A2A),
        onPrimaryContainer = Color(0xFFE8E8E8),
        onBackground = Color(0xFFE6E1E5),
        onSurface = Color(0xFFE6E1E5),
        onSurfaceVariant = Color(0xFF938F93),
        outline = Color(0xFF3D3D3D),
        outlineVariant = Color(0xFF2A2A2A)
    ) else this
