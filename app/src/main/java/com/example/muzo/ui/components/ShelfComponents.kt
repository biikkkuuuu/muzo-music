package com.example.muzo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

        // Horizontal Row of Cards (matching 140.dp actual card size)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            userScrollEnabled = false
        ) {
            items(4) {
                Column(
                    modifier = Modifier.width(140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
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
    onSeeAllClick: (String) -> Unit,
    onItemLongClick: ((ShelfItem) -> Unit)? = null
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
                val rowState = rememberLazyListState()
                LazyRow(
                    state = rowState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = rowState),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(shelf.items.take(12), key = { it.id }) { item ->
                        ShelfCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongClick = onItemLongClick?.let { { it(item) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroCarousel(
    items: List<ShelfItem>,
    currentSongId: String? = null,
    isPlaying: Boolean = false,
    onItemClick: (ShelfItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val heroState = rememberLazyListState()
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        LazyRow(
            state = heroState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = heroState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val isActive = item.id == currentSongId

                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .height(134.dp)
                        .clickable { onItemClick(item) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B1B22),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column: Category tag, Title, Subtitle, Play chip
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = "SPOTLIGHT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.fillMaxWidth().basicMarquee()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Compact Play button chip
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isActive && isPlaying) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isActive && isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = if (isActive && isPlaying) MaterialTheme.colorScheme.onPrimary else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isActive && isPlaying) "Playing" else "Play",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isActive && isPlaying) MaterialTheme.colorScheme.onPrimary else Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Right: Square Artwork with subtle 10dp rounded corners
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.imageUrls.firstOrNull() ?: "")
                                    .crossfade(100)
                                    .build(),
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val isArtist = item.type == ItemType.ARTIST
    Column(
        modifier = Modifier
            .width(140.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(6.dp, if (isArtist) CircleShape else RoundedCornerShape(10.dp), spotColor = Color.Black.copy(alpha = 0.4f))
                .clip(if (isArtist) CircleShape else RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            if (item.imageUrls.size >= 4) {
                CollageCover(imageUrls = item.imageUrls.take(4))
            } else {
                SingleCover(imageUrl = item.imageUrls.firstOrNull() ?: "")
            }

            // Top-left Play Indicator Badge (Echo-Music signature)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
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
            .crossfade(100)
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
                model = ImageRequest.Builder(context).data(imageUrls.getOrNull(0)).crossfade(100).build(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrls.getOrNull(1)).crossfade(100).build(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrls.getOrNull(2)).crossfade(100).build(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrls.getOrNull(3)).crossfade(100).build(),
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
