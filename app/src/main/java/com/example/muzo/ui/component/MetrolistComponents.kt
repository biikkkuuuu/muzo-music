package com.example.muzo.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.mock.MockSong
import com.example.muzo.theme.GoogleSansFlex
import com.example.muzo.ui.utils.bounceClick
import kotlin.math.sin

/**
 * Metrolist signature ChipsRow with dynamic corner radius animation (20dp selected / 8dp unselected).
 */
@Composable
fun MetrolistChipsRow(
    chips: List<String>,
    selectedChip: String?,
    onChipSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        chips.forEach { chip ->
            val isSelected = selectedChip == chip

            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 20.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "chip_corner_radius"
            )

            FilterChip(
                label = {
                    Text(
                        text = chip,
                        fontFamily = GoogleSansFlex,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp
                    )
                },
                selected = isSelected,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = { onChipSelect(chip) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(cornerRadius),
                border = null,
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    .padding(end = 8.dp)
            )
        }
    }
}

/**
 * Metrolist NavigationTitle Header with uppercase subtitle and primary title.
 */
@Composable
fun NavigationTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            label?.let {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                fontSize = 22.sp
            )
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "See All",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 2-column MoodTile with 14dp rounded corners.
 */
@Composable
fun MoodTile(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .bounceClick(scaleDown = 0.96f, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Metrolist horizontal card item with rounded artwork.
 */
@Composable
fun MetrolistSongCard(
    song: MockSong,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(150.dp)
            .bounceClick(scaleDown = 0.95f, onClick = onClick)
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song.artist,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Metrolist 12-point scalloped flower/starburst refresh animation.
 */
@Composable
fun EchoLoadingIndicator(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 38.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RefreshSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RefreshRotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RefreshScale"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = if (isRefreshing) rotation else 0f
                scaleX = if (isRefreshing) pulseScale else 1f
                scaleY = if (isRefreshing) pulseScale else 1f
            }
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerRadius = this.size.minDimension / 2f
        val innerRadius = outerRadius * 0.76f
        val points = 12
        val path = Path()

        for (i in 0 until points) {
            val angle = (i * 2 * Math.PI / points).toFloat()
            val nextAngle = ((i + 1) * 2 * Math.PI / points).toFloat()
            val midAngle = (angle + nextAngle) / 2f

            val px = center.x + outerRadius * kotlin.math.cos(angle)
            val py = center.y + outerRadius * kotlin.math.sin(angle)

            val cx = center.x + innerRadius * kotlin.math.cos(midAngle)
            val cy = center.y + innerRadius * kotlin.math.sin(midAngle)

            val npx = center.x + outerRadius * kotlin.math.cos(nextAngle)
            val npy = center.y + outerRadius * kotlin.math.sin(nextAngle)

            if (i == 0) path.moveTo(px, py)
            path.quadraticBezierTo(cx, cy, npx, npy)
        }
        path.close()

        drawPath(path = path, color = color)
    }
}

/**
 * Metrolist signature SquigglySlider: A sinusoidal waveform seekbar with dynamic progress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquigglySlider(
    value: Float, // 0f to 1f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
    isPlaying: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveSliderPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2 * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = activeColor,
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveColor
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        },
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            ) {
                val trackWidth = size.width
                val activeWidth = trackWidth * sliderState.value
                val centerY = size.height / 2f
                val amplitude = 3.5.dp.toPx()
                val wavelength = 24.dp.toPx()

                // Inactive track (smooth line)
                drawLine(
                    color = inactiveColor,
                    start = Offset(activeWidth, centerY),
                    end = Offset(trackWidth, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Active squiggly track (sinusoidal wave)
                if (activeWidth > 0f) {
                    val path = Path()
                    var x = 0f
                    path.moveTo(0f, centerY)

                    while (x <= activeWidth) {
                        val y = centerY + amplitude * sin((x / wavelength) * (2 * Math.PI).toFloat() + phase)
                        path.lineTo(x, y)
                        x += 2f
                    }

                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    )
}

/**
 * 3-bar animated equalizer playing indicator.
 */
@Composable
fun PlayingIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "equalizer")

    val bar1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Row(
        modifier = modifier.size(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val h1 = if (isPlaying) bar1 else 0.4f
        val h2 = if (isPlaying) bar2 else 0.7f
        val h3 = if (isPlaying) bar3 else 0.3f

        Box(modifier = Modifier.weight(1f).fillMaxHeight(h1).clip(RoundedCornerShape(1.dp)).background(color))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(h2).clip(RoundedCornerShape(1.dp)).background(color))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(h3).clip(RoundedCornerShape(1.dp)).background(color))
    }
}
