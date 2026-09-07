package com.example.muzo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.data.model.ShelfItem
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/**
 * Material 3 Expressive Hero Carousel
 *
 * Signature Echo visual features:
 * - Dynamic edge-shrinking cards with depth perspective graphicsLayer.
 * - Auto-advancing pager with infinite loop wrapping.
 * - Glassmorphism gradient overlay with high-contrast typography.
 * - Elevated hero Play button.
 * - Glowing "Featured" tag.
 * - Smooth pill pager indicator.
 */
@Composable
fun HeroExpressiveCarousel(
    featuredItems: List<ShelfItem>,
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    onPlaylistSelect: (ShelfItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredItems.isEmpty()) return

    val pageCount = featuredItems.size
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    // Auto-scroll loop: auto advance every 5.5s unless touched
    var isUserInteracting by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState, isUserInteracting, pageCount) {
        if (!isUserInteracting && pageCount > 1) {
            while (true) {
                delay(5500)
                if (!isUserInteracting && pageCount > 1) {
                    val nextPage = (pagerState.currentPage + 1) % pageCount
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(700, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isUserInteracting = true
                            tryAwaitRelease()
                            isUserInteracting = false
                        }
                    )
                }
        ) { page ->
            val item = featuredItems[page]

            // Calculate shrink and depth offset relative to center
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue.coerceIn(0f, 1f)

            val cardScale = lerp(1f, 0.92f, pageOffset)
            val cardAlpha = lerp(1f, 0.65f, pageOffset)

            val songItem = remember(item) {
                SongItem(
                    id = item.id,
                    title = item.title,
                    artists = listOf(Artist(name = item.subtitle, id = null)),
                    album = null,
                    duration = 0,
                    thumbnail = item.imageUrls.firstOrNull() ?: ""
                )
            }

            val allSongs = remember(featuredItems) {
                featuredItems.map {
                    SongItem(
                        id = it.id,
                        title = it.title,
                        artists = listOf(Artist(name = it.subtitle, id = null)),
                        album = null,
                        duration = 0,
                        thumbnail = it.imageUrls.firstOrNull() ?: ""
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1B1A26),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        onSongSelect(songItem, allSongs)
                    }
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Cover Image
                    AsyncImage(
                        model = item.imageUrls.firstOrNull(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Cinematic Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Black.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )

                    // Top "Featured / Trending" Pill Badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "FEATURED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Bottom Song Title, Artist & Hero Play Action
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = item.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Pure White Hero Play Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    onSongSelect(songItem, allSongs)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // Pager Indicator Pill Row
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { index ->
                    val isSelected = pagerState.currentPage == index
                    val indicatorWidth by animateFloatAsState(
                        targetValue = if (isSelected) 22f else 6f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "indicatorWidth"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(indicatorWidth.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}
