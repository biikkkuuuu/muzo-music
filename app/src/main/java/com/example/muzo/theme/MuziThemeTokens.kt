package com.example.muzo.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Muzi Flow Design System Tokens
 * Pure AMOLED Material 3 aesthetic with fluid snapping and micro-interactions.
 */
object MuziThemeTokens {
    // Colors
    val AmoledCanvas = Color(0xFF000000)
    val SurfaceCard = Color(0xFF141418)
    val SurfaceCardElevated = Color(0xFF1E1E24)
    val SurfacePill = Color(0xFF1E1E24)
    val SurfaceBorder = Color(0x1AFFFFFF)
    val SurfaceBorderSubtle = Color(0x0DFFFFFF)
    
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA1A1AA)
    val TextMuted = Color(0xFF71717A)
    
    val AccentRose = Color(0xFFF39C8F)
    val AccentRed = Color(0xFFFF4B6E)
    
    // Shapes
    val ShapePill = RoundedCornerShape(24.dp)
    val ShapeCard = RoundedCornerShape(16.dp)
    val ShapeCover = RoundedCornerShape(12.dp)
    val ShapeThumbnail = RoundedCornerShape(8.dp)
    val ShapeCircle = CircleShape
    
    // Dimensions
    val ButtonHeight = 44.dp
    val ThumbnailSize = 52.dp
    val HorizontalPadding = 16.dp
}
