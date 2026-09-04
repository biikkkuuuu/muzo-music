package com.example.muzo.ui.components

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.muzo.playback.EqualizerController
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    equalizerController: EqualizerController,
    audioSessionId: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEnabled by equalizerController.isEnabled.collectAsStateWithLifecycle()
    val currentPreset by equalizerController.currentPreset.collectAsStateWithLifecycle()
    val bandLevels by equalizerController.bandLevels.collectAsStateWithLifecycle()
    val bassBoostStrength by equalizerController.bassBoostStrength.collectAsStateWithLifecycle()
    val virtualizerStrength by equalizerController.virtualizerStrength.collectAsStateWithLifecycle()

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val bandLabels = listOf("Sub-Bass", "Bass", "Mids", "High-Mids", "Treble")

    val openSystemEqualizer: () -> Unit = {
        try {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No system equalizer found on device", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open system equalizer", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14131A),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: GraphicEq, Title, Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) Color(0xFF2B3A60) else Color(0xFF252430)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isEnabled) Color(0xFF6B9DFE) else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Equalizer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isEnabled) currentPreset else "Turned Off",
                            fontSize = 13.sp,
                            color = if (isEnabled) Color(0xFF6B9DFE) else Color.Gray
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { equalizerController.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4C7DE8),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF2B2A36)
                    )
                )
            }

            // System Equalizer Launcher Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E1D27),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openSystemEqualizer() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF9E9EA8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "System Equalizer (Dolby / Device FX)",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Open",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B9DFE)
                    )
                }
            }

            AnimatedVisibility(visible = isEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    // Presets Horizontal Row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PRESETS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            equalizerController.presetMap.keys.forEach { presetName ->
                                val isSelected = currentPreset == presetName
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF4C7DE8) else Color(0xFF22212D),
                                    modifier = Modifier.clickable {
                                        equalizerController.setPreset(presetName)
                                    }
                                ) {
                                    Text(
                                        text = presetName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFB0B0BE),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Sound Enhancement Sliders: Bass Boost & Virtualizer
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "SOUND ENHANCEMENTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Bass Boost Card
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF1E1D27),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Headphones,
                                                contentDescription = null,
                                                tint = Color(0xFFFF7A8A),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Bass Boost",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }

                                        Text(
                                            text = "${(bassBoostStrength / 10)}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF7A8A)
                                        )
                                    }

                                    Slider(
                                        value = bassBoostStrength.toFloat(),
                                        onValueChange = { equalizerController.setBassBoost(it.roundToInt()) },
                                        valueRange = 0f..1000f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFF7A8A),
                                            activeTrackColor = Color(0xFFFF7A8A),
                                            inactiveTrackColor = Color(0xFF333240)
                                        )
                                    )
                                }
                            }

                            // 3D Virtualizer Card
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF1E1D27),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SurroundSound,
                                                contentDescription = null,
                                                tint = Color(0xFF6B9DFE),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Virtualizer",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }

                                        Text(
                                            text = "${(virtualizerStrength / 10)}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6B9DFE)
                                        )
                                    }

                                    Slider(
                                        value = virtualizerStrength.toFloat(),
                                        onValueChange = { equalizerController.setVirtualizer(it.roundToInt()) },
                                        valueRange = 0f..1000f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF6B9DFE),
                                            activeTrackColor = Color(0xFF6B9DFE),
                                            inactiveTrackColor = Color(0xFF333240)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 5-Band Equalizer Section
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "FREQUENCY BANDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF1E1D27),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                bandFrequencies.forEachIndexed { index, freq ->
                                    val levelMb = bandLevels.getOrElse(index) { 0 }
                                    val levelDb = levelMb / 100

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = freq,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "(${bandLabels.getOrElse(index) { "" }})",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            Text(
                                                text = if (levelDb > 0) "+${levelDb} dB" else "${levelDb} dB",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (levelDb != 0) Color(0xFF6B9DFE) else Color.Gray
                                            )
                                        }

                                        Slider(
                                            value = levelMb.toFloat(),
                                            onValueChange = { equalizerController.setBandLevel(index, it.roundToInt()) },
                                            valueRange = -1200f..1200f,
                                            steps = 23,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF6B9DFE),
                                                activeTrackColor = Color(0xFF6B9DFE),
                                                inactiveTrackColor = Color(0xFF333240)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
