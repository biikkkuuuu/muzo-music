package com.example.muzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.muzo.data.model.HomeShelf
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.data.model.ShelfType

@Composable
fun ShimmerBrush(targetValue: Float = 1000f): Brush {
    val shimmerColors = listOf(
        Color(0xFF131317),
        Color(0xFF22222A),
        Color(0xFF131317)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

@Composable
fun ShelfRowSkeleton(brush: Brush, hasSubtitle: Boolean = true) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Shelf Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (hasSubtitle) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(brush)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Row of Cards (matching 130.dp actual card size)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.width(2.dp))
            repeat(4) {
                Column(
                    modifier = Modifier.width(130.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreenSkeleton() {
    val brush = ShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 120.dp)
    ) {
        // 1. Shelf 1 Skeleton (Matches Recently Played / Keep Listening!)
        ShelfRowSkeleton(brush = brush, hasSubtitle = true)

        // 2. Shelf 2 Skeleton (Matches New releases)
        ShelfRowSkeleton(brush = brush, hasSubtitle = false)

        // 3. Shelf 3 Skeleton (Matches Rain Therapy / Playlists)
        ShelfRowSkeleton(brush = brush, hasSubtitle = true)
    }
}

@Composable
fun PlaylistShelfRow(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onSeeAllClick: (String) -> Unit
) {
    if (shelf.items.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Shelf Header (ALL-CAPS subtitle + Bold Title + Arrow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { shelf.seeAllRoute?.let(onSeeAllClick) }
            ) {
                if (!shelf.subtitle.isNullOrEmpty()) {
                    Text(
                        text = shelf.subtitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B8AFD), // Accent blue as seen in the video
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = shelf.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            if (shelf.seeAllRoute != null) {
                IconButton(
                    onClick = { onSeeAllClick(shelf.seeAllRoute) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "See All",
                        tint = Color(0xFF6B8AFD)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (shelf.type) {
            ShelfType.GENRE_GRID -> {
                MoodAndGenresGrid(
                    items = shelf.items,
                    onItemClick = onItemClick
                )
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(shelf.items.take(12), key = { it.id }) { item ->
                        ShelfCard(
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShelfCard(item: ShelfItem, onClick: () -> Unit) {
    val brush = ShimmerBrush()
    val isArtist = item.type == ItemType.ARTIST
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(if (isArtist) CircleShape else RoundedCornerShape(8.dp))
                .background(brush)
        ) {
            if (item.imageUrls.size >= 4) {
                CollageCover(imageUrls = item.imageUrls.take(4))
            } else {
                SingleCover(imageUrl = item.imageUrls.firstOrNull() ?: "")
            }

            // Bottom-right Play Icon Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(if (isArtist) 4.dp else 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = if (isArtist) "Artist" else item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

@Composable
fun SingleCover(imageUrl: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(300)
            .build(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun CollageCover(imageUrls: List<String>) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrls.getOrNull(0)).crossfade(300).build(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrls.getOrNull(1)).crossfade(300).build(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrls.getOrNull(2)).crossfade(300).build(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrls.getOrNull(3)).crossfade(300).build(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun MoodAndGenresGrid(
    items: List<ShelfItem>,
    onItemClick: (ShelfItem) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in items.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MoodPillItem(
                    item = items[i],
                    modifier = Modifier.weight(1f),
                    onClick = { onItemClick(items[i]) }
                )
                if (i + 1 < items.size) {
                    MoodPillItem(
                        item = items[i + 1],
                        modifier = Modifier.weight(1f),
                        onClick = { onItemClick(items[i + 1]) }
                    )
                }
            }
        }
    }
}

@Composable
fun MoodPillItem(
    item: ShelfItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E24)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}
