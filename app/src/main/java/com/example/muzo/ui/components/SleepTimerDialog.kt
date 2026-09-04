package com.example.muzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
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
import com.example.muzo.core.formatTime
import com.example.muzo.playback.SleepTimer
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    sleepTimer: SleepTimer,
    onDismissRequest: () -> Unit
) {
    val isActive by sleepTimer.isActive.collectAsStateWithLifecycle()
    val remainingTimeMs by sleepTimer.remainingTimeMs.collectAsStateWithLifecycle()
    val pauseWhenSongEnd by sleepTimer.pauseWhenSongEnd.collectAsStateWithLifecycle()

    var customMinutes by remember { mutableFloatStateOf(20f) }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF1B1A22),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog Header
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2B3A60)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = Color(0xFF6B9DFE),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Sleep Timer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isActive) {
                    // Active Timer State Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF262532)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (pauseWhenSongEnd) {
                                    "Stops after current track finishes 🎵"
                                } else {
                                    "Stopping in ${formatTime(remainingTimeMs)}"
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color(0xFF6B9DFE),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Music will gently fade out over 3s before pausing",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    sleepTimer.cancel()
                                    onDismissRequest()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF422226),
                                    contentColor = Color(0xFFFF5252)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Turn Off Timer", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Timer Presets Grid / Rows
                    Text(
                        text = "Choose a duration or stop after the song ends",
                        fontSize = 13.sp,
                        color = Color(0xFF9E9EA8),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Chips
                    val presets = listOf(15, 30, 45, 60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { mins ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF262532),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        sleepTimer.start(mins)
                                        onDismissRequest()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Special: End of song option
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF262532),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sleepTimer.startEndAfterSong()
                                onDismissRequest()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFF6B9DFE),
                                modifier = Modifier.size(20.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "End of current song",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Pauses playback when this track completes",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom Slider Option
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Custom Timer",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "${customMinutes.roundToInt()} minutes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B9DFE)
                            )
                        }

                        Slider(
                            value = customMinutes,
                            onValueChange = { customMinutes = it },
                            valueRange = 5f..120f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF6B9DFE),
                                activeTrackColor = Color(0xFF6B9DFE),
                                inactiveTrackColor = Color(0xFF333240)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                sleepTimer.start(customMinutes.roundToInt())
                                onDismissRequest()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6B9DFE),
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Start Timer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
