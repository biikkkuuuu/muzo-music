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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.data.model.HomeShelf
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.data.model.ShelfType

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
                    items(shelf.items, key = { it.id }) { item ->
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
    Column(
        modifier = Modifier
            .width(145.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(145.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1B1B1F))
        ) {
            if (item.imageUrls.size >= 4) {
                CollageCover(imageUrls = item.imageUrls.take(4))
            } else {
                SingleCover(imageUrl = item.imageUrls.firstOrNull() ?: "")
            }

            // Bottom-right Play Icon Badge (like in the screen recording)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SingleCover(imageUrl: String) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun CollageCover(imageUrls: List<String>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AsyncImage(
                model = imageUrls.getOrNull(0),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            AsyncImage(
                model = imageUrls.getOrNull(1),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AsyncImage(
                model = imageUrls.getOrNull(2),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            AsyncImage(
                model = imageUrls.getOrNull(3),
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
