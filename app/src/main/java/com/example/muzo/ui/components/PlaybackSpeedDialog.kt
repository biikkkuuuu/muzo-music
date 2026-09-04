package com.example.muzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    var customSpeed by remember { mutableFloatStateOf(currentSpeed) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1D24),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B3A60)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFF6B9DFE),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Playback Speed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    speedPresets.forEach { speed ->
                        val isSelected = (currentSpeed * 100).roundToInt() == (speed * 100).roundToInt()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF4C7DE8) else Color(0xFF282732),
                            modifier = Modifier.clickable {
                                onSpeedChange(speed)
                                customSpeed = speed
                            }
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFB0B0BE),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Slider for fine tuning
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Custom Speed", color = Color.Gray, fontSize = 12.sp)
                        Text("${"%.2f".format(customSpeed)}x", color = Color(0xFF6B9DFE), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Slider(
                        value = customSpeed,
                        onValueChange = {
                            customSpeed = it
                            onSpeedChange(it)
                        },
                        valueRange = 0.25f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6B9DFE),
                            activeTrackColor = Color(0xFF6B9DFE),
                            inactiveTrackColor = Color(0xFF333240)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Done", color = Color(0xFF6B9DFE), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onSpeedChange(1.0f)
                customSpeed = 1.0f
            }) {
                Text("Reset (1.0x)", color = Color.Gray)
            }
        }
    )
}
