package com.example.muzo.ui.screens.equalizer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.muzo.playback.EqualizerController

/**
 * Echo-Music style Axion Equalizer Screen.
 * Features:
 * - Master HW AudioEffect toggle
 * - Mode switch: Simple (Circular Arc Dials) vs Advanced (Multi-Band Graphic Sliders)
 * - Circular interactive Bass, Mid, Treble dial
 * - Bass Boost & 3D Virtualizer hardware DSP sliders
 * - Instant EQ Presets (Flat, Bass Heavy, Vocal Boost, Rock, Pop, Electronic)
 * - Echo iOS-inspired translucent card aesthetic (`surfaceVariant.copy(alpha = 0.35f)`)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxionEqScreen(
    equalizerController: EqualizerController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled by equalizerController.isEnabled.collectAsStateWithLifecycle()
    val currentPreset by equalizerController.currentPreset.collectAsStateWithLifecycle()
    val bandLevels by equalizerController.bandLevels.collectAsStateWithLifecycle()
    val bassBoost by equalizerController.bassBoostStrength.collectAsStateWithLifecycle()
    val virtualizer by equalizerController.virtualizerStrength.collectAsStateWithLifecycle()

    var selectedMode by remember { mutableIntStateOf(0) } // 0 = Simple, 1 = Advanced

    // Derived 3-axis values for the circular dial:
    // Bass = (band0 + band1) / 2
    // Mid = band2
    // Treble = (band3 + band4) / 2
    val bassValue = remember(bandLevels) {
        if (bandLevels.size >= 5) {
            (((bandLevels[0] + bandLevels[1]) / 2f) / 120f).coerceIn(-10f, 10f)
        } else 0f
    }
    val midValue = remember(bandLevels) {
        if (bandLevels.size >= 5) {
            (bandLevels[2].toFloat() / 120f).coerceIn(-10f, 10f)
        } else 0f
    }
    val trebleValue = remember(bandLevels) {
        if (bandLevels.size >= 5) {
            (((bandLevels[3] + bandLevels[4]) / 2f) / 120f).coerceIn(-10f, 10f)
        } else 0f
    }

    val bandFrequencies = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    val presets = listOf("Flat", "Bass Heavy", "Vocal Boost", "Rock", "Pop", "Electronic")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Axion Equalizer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                    IconButton(onClick = { equalizerController.resetToFlat() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Switch Translucent Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Equalizer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEnabled) "Hardware DSP active ($currentPreset)" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { equalizerController.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // Mode Selector Pill (Simple vs Advanced)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Simple Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (selectedMode == 0) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { selectedMode = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Simple",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Advanced Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (selectedMode == 1) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { selectedMode = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Advanced",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // EQ Mode Content
            AnimatedContent(
                targetState = selectedMode,
                label = "eqModeTransition"
            ) { mode ->
                if (mode == 0) {
                    // Simple Mode: Circular Dial
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularEqControl(
                                    bass = bassValue,
                                    mid = midValue,
                                    treble = trebleValue,
                                    enabled = isEnabled,
                                    onBassChange = { newBass ->
                                        val mB = (newBass * 120).toInt()
                                        equalizerController.setBandLevel(0, mB)
                                        equalizerController.setBandLevel(1, mB)
                                    },
                                    onMidChange = { newMid ->
                                        val mB = (newMid * 120).toInt()
                                        equalizerController.setBandLevel(2, mB)
                                    },
                                    onTrebleChange = { newTreble ->
                                        val mB = (newTreble * 120).toInt()
                                        equalizerController.setBandLevel(3, mB)
                                        equalizerController.setBandLevel(4, mB)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                } else {
                    // Advanced Mode: 5-Band Vertical Graphic Sliders
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            bandLevels.forEachIndexed { index, level ->
                                val freq = bandFrequencies.getOrElse(index) { "Band $index" }
                                val dbVal = level / 100f
                                val sign = if (dbVal > 0) "+" else ""

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = freq,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${sign}%.1f dB".format(dbVal),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Slider(
                                        value = level.toFloat(),
                                        onValueChange = { newLevel ->
                                            equalizerController.setBandLevel(index, newLevel.toInt())
                                        },
                                        valueRange = -1200f..1200f,
                                        enabled = isEnabled,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DSP Enhancements: Bass Boost & 3D Virtualizer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Hardware DSP Enhancements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Bass Boost Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bass Boost",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(bassBoost / 10)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { equalizerController.setBassBoost(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = isEnabled
                        )
                    }

                    // 3D Virtualizer Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "3D Virtualizer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(virtualizer / 10)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = virtualizer.toFloat(),
                            onValueChange = { equalizerController.setVirtualizer(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = isEnabled
                        )
                    }
                }
            }

            // Presets Flow
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Equalizer Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = currentPreset.equals(preset, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { equalizerController.setPreset(preset) },
                            label = { Text(preset, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(100.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
