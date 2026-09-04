package com.example.muzo.ui.components

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }

    val isBluetoothActive = remember {
        @Suppress("DEPRECATION")
        audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
    }
    val isWiredHeadphones = remember {
        @Suppress("DEPRECATION")
        audioManager.isWiredHeadsetOn
    }

    val outputDeviceName = remember(isBluetoothActive, isWiredHeadphones) {
        when {
            isBluetoothActive -> "Bluetooth Audio Device"
            isWiredHeadphones -> "Wired Headphones"
            else -> "Phone Speaker"
        }
    }

    val outputIcon = remember(isBluetoothActive, isWiredHeadphones) {
        when {
            isBluetoothActive -> Icons.Default.Bluetooth
            isWiredHeadphones -> Icons.Default.Headphones
            else -> Icons.Default.Speaker
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16151E),
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF263352)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = outputIcon,
                        contentDescription = null,
                        tint = Color(0xFF6B9DFE),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Audio Output",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = outputDeviceName,
                        fontSize = 13.sp,
                        color = Color(0xFF6B9DFE)
                    )
                }
            }

            // Volume Control Slider
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF201F2B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Media Volume", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = "${((currentVolume / maxVolume) * 100).roundToInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B9DFE)
                        )
                    }

                    Slider(
                        value = currentVolume,
                        onValueChange = {
                            currentVolume = it
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0)
                        },
                        valueRange = 0f..maxVolume.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6B9DFE),
                            activeTrackColor = Color(0xFF6B9DFE),
                            inactiveTrackColor = Color(0xFF333240)
                        )
                    )
                }
            }

            // Bluetooth & System Sound Launcher Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF201F2B),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = Color(0xFF6B9DFE),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Connect Bluetooth Headphones / Speaker",
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
        }
    }
}
