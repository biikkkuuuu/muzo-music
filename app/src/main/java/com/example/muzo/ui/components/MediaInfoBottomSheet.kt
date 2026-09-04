package com.example.muzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.innertube.models.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoBottomSheet(
    song: SongItem,
    onDismiss: () -> Unit
) {
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
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = Color(0xFF6B9DFE),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Audio & Stream Quality",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Lossless-Equivalent 320 kbps Stream",
                        fontSize = 13.sp,
                        color = Color(0xFF6B9DFE)
                    )
                }
            }

            // Specs Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF201F2B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MediaInfoRow(label = "Format & Codec", value = "Opus / AAC (Hardware Decoded)")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    MediaInfoRow(label = "Bitrate", value = "320 kbps High Fidelity")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    MediaInfoRow(label = "Sample Rate", value = "48.0 kHz (Stereo 2-Channel)")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    MediaInfoRow(label = "Loudness Normalization", value = "Enabled (Peak Limiting)")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    MediaInfoRow(label = "Streaming Pipeline", value = "Direct Stream • Ad-Free")
                }
            }

            // Track Details Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF201F2B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("TRACK METADATA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                    Text("Title: ${song.title}", fontSize = 13.sp, color = Color.White)
                    Text("Artist: ${song.artists.joinToString(", ") { it.name }}", fontSize = 13.sp, color = Color(0xFFB0B0BE))
                    Text("Video ID: ${song.id}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun MediaInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF9E9EA8))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
