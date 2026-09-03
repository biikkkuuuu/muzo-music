package com.example.muzo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.music.innertube.models.SongItem
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom 8-petal scalloped flower shape matching the Android Expressive play button.
 */
class ScallopedPetalShape(private val petals: Int = 8, private val innerRatio: Float = 0.84f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy)
        val innerR = r * innerRatio
        val angleStep = (2 * Math.PI / petals).toFloat()

        for (i in 0 until petals) {
            val theta0 = i * angleStep - (Math.PI / 2f).toFloat()
            val thetaMid = theta0 + angleStep / 2f
            val theta1 = theta0 + angleStep

            val v0x = cx + innerR * cos(theta0)
            val v0y = cy + innerR * sin(theta0)
            val px = cx + r * 1.06f * cos(thetaMid)
            val py = cy + r * 1.06f * sin(thetaMid)
            val v1x = cx + innerR * cos(theta1)
            val v1y = cy + innerR * sin(theta1)

            if (i == 0) {
                path.moveTo(v0x, v0y)
            }
            path.quadraticTo(px, py, v1x, v1y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun PlayerWithBottomNav(
    currentSong: SongItem?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSongClick: () -> Unit,
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Floating Mini Player Capsule (Matching screenshot)
        if (currentSong != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .shadow(12.dp, RoundedCornerShape(36.dp))
                    .clickable { onSongClick() },
                shape = RoundedCornerShape(36.dp),
                color = Color(0xFF221614),
                border = BorderStroke(1.dp, Color(0xFF382522)),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular framed album art
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E1E1C)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = getHighResThumbnail(currentSong.thumbnail),
                            contentDescription = currentSong.title,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Artist
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentSong.artists.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFFC7B1AF)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Controls: Skip Previous + 8-petal Flower Play/Pause + Skip Next
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(22.dp),
                                tint = Color.White
                            )
                        }

                        // Scalloped 8-petal flower Play/Pause button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(ScallopedPetalShape(petals = 8))
                                .background(Color(0xFFF7B4A7))
                                .clickable { onPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF381510)
                            )
                        }

                        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(22.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // 2. Floating Bottom Navigation Bar (Centered pill, no 3-dot button)
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(56.dp)
                .shadow(12.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF221614),
            border = BorderStroke(1.dp, Color(0xFF382522))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navItems = listOf(
                    Icons.Default.Home to "Home",
                    Icons.Default.Search to "Search",
                    Icons.Default.Mic to "Voice",
                    Icons.Default.LibraryMusic to "Library"
                )

                navItems.forEachIndexed { index, (icon, desc) ->
                    val isSelected = currentTab == index
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF3D2A28) else Color.Transparent)
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = desc,
                            tint = if (isSelected) Color.White else Color(0xFFB8A2A0),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}