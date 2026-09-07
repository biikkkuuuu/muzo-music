package com.example.muzo.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muzo.ui.components.GlassEffectConfig
import com.example.muzo.ui.components.liquidGlass
import kotlin.math.roundToInt

/**
 * GlassEffectSettings: Live tuner for the AGSL Liquid Glass shader engine.
 * Echo-Music style with real-time preview card, blur/lens sliders, and per-surface toggles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassEffectSettings(
    config: GlassEffectConfig,
    onConfigChange: (GlassEffectConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("muzi_glass_prefs", Context.MODE_PRIVATE) }
    val scrollState = rememberScrollState()

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Liquid Glass Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val default = GlassEffectConfig()
                            onConfigChange(default)
                            saveGlassConfig(prefs, default)
                        }
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ==========================================
            // 1. LIVE PREVIEW CARD
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF416C),
                                Color(0xFFFF4B2B),
                                Color(0xFF8A2387),
                                Color(0xFFE94057),
                                Color(0xFFF27121)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Floating simulated glass pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (config.globalEnabled) {
                        Color.White.copy(alpha = config.surfaceOpacity.coerceIn(0.1f, 0.7f))
                    } else {
                        Color.Black.copy(alpha = 0.5f)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "Liquid Glass Live Preview",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (config.globalEnabled) "Blur: ${config.blurRadius.roundToInt()}dp • Lens: ${(config.lensHeight * 100).roundToInt()}%"
                                else "Shader Disabled",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. MASTER TOGGLE
            // ==========================================
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Liquid Glass Effect",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Apple/AGSL-inspired refraction and depth blur",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = config.globalEnabled,
                        onCheckedChange = {
                            val updated = config.copy(globalEnabled = it)
                            onConfigChange(updated)
                            saveGlassConfig(prefs, updated)
                        }
                    )
                }
            }

            AnimatedVisibility(visible = config.globalEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    // ==========================================
                    // 3. SHADER PARAMETERS (SLIDERS)
                    // ==========================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Shader Tuning",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Blur Radius Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Blur Radius", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("${config.blurRadius.roundToInt()} dp", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = config.blurRadius,
                                    onValueChange = {
                                        val updated = config.copy(blurRadius = it)
                                        onConfigChange(updated)
                                        saveGlassConfig(prefs, updated)
                                    },
                                    valueRange = 2f..32f
                                )
                            }

                            // Lens Refraction Height Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Lens Refraction Height", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("${(config.lensHeight * 100).roundToInt()}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = config.lensHeight,
                                    onValueChange = {
                                        val updated = config.copy(lensHeight = it)
                                        onConfigChange(updated)
                                        saveGlassConfig(prefs, updated)
                                    },
                                    valueRange = 0.1f..1f
                                )
                            }

                            // Lens Refraction Amount Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Lens Refraction Amount", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("${(config.lensAmount * 100).roundToInt()}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = config.lensAmount,
                                    onValueChange = {
                                        val updated = config.copy(lensAmount = it)
                                        onConfigChange(updated)
                                        saveGlassConfig(prefs, updated)
                                    },
                                    valueRange = 0.1f..1f
                                )
                            }

                            // Surface Opacity Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Surface Tint Opacity", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("${(config.surfaceOpacity * 100).roundToInt()}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = config.surfaceOpacity,
                                    onValueChange = {
                                        val updated = config.copy(surfaceOpacity = it)
                                        onConfigChange(updated)
                                        saveGlassConfig(prefs, updated)
                                    },
                                    valueRange = 0.15f..0.7f
                                )
                            }

                            // Chromatic Aberration Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Chromatic Aberration", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("Subtle RGB lens color separation at curved borders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Switch(
                                    checked = config.chromaticAberration,
                                    onCheckedChange = {
                                        val updated = config.copy(chromaticAberration = it)
                                        onConfigChange(updated)
                                        saveGlassConfig(prefs, updated)
                                    }
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 4. PER-SURFACE TOGGLES
                    // ==========================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Enabled Surfaces",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Floating Nav Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Floating Navigation Dock", fontSize = 14.sp)
                                Switch(
                                    checked = config.navBarEnabled,
                                    onCheckedChange = {
                                        val updated = config.copy(navBarEnabled = it)
                                        onConfigChange(updated)
                                        saveGlassConfig(prefs, updated)
                                    }
                                )
                            }

                            // Floating Mini-Player
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Floating Mini-Player", fontSize = 14.sp)
                                Switch(
                                    checked = config.miniPlayerEnabled,
                                    onCheckedChange = {
                                        val updated = config.copy(miniPlayerEnabled = it)
                                        onConfigChange(updated)
                                        saveGlassConfig(prefs, updated)
                                    }
                                )
                            }

                            // Full Player Sheet
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Full Player Sheets & Dialogs", fontSize = 14.sp)
                                Switch(
                                    checked = config.playerEnabled,
                                    onCheckedChange = {
                                        val updated = config.copy(playerEnabled = it)
                                        onConfigChange(updated)
                                        saveGlassConfig(prefs, updated)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun saveGlassConfig(prefs: android.content.SharedPreferences, config: GlassEffectConfig) {
    prefs.edit()
        .putBoolean("glass_global_enabled", config.globalEnabled)
        .putFloat("glass_blur_radius", config.blurRadius)
        .putFloat("glass_lens_height", config.lensHeight)
        .putFloat("glass_lens_amount", config.lensAmount)
        .putBoolean("glass_chromatic_aberration", config.chromaticAberration)
        .putFloat("glass_surface_opacity", config.surfaceOpacity)
        .putBoolean("glass_navbar_enabled", config.navBarEnabled)
        .putBoolean("glass_miniplayer_enabled", config.miniPlayerEnabled)
        .putBoolean("glass_player_enabled", config.playerEnabled)
        .apply()
}

fun loadGlassConfig(context: Context): GlassEffectConfig {
    val prefs = context.getSharedPreferences("muzi_glass_prefs", Context.MODE_PRIVATE)
    return GlassEffectConfig(
        globalEnabled = prefs.getBoolean("glass_global_enabled", true),
        blurRadius = prefs.getFloat("glass_blur_radius", 8f),
        lensHeight = prefs.getFloat("glass_lens_height", 0.5f),
        lensAmount = prefs.getFloat("glass_lens_amount", 0.5f),
        chromaticAberration = prefs.getBoolean("glass_chromatic_aberration", true),
        surfaceOpacity = prefs.getFloat("glass_surface_opacity", 0.4f),
        navBarEnabled = prefs.getBoolean("glass_navbar_enabled", true),
        miniPlayerEnabled = prefs.getBoolean("glass_miniplayer_enabled", true),
        playerEnabled = prefs.getBoolean("glass_player_enabled", true)
    )
}
