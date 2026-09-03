package com.example.muzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun CardArtwork(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    cornerRadius: Dp = 10.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF1E1E22))
    ) {
        when {
            imageUrls.size >= 4 -> {
                // 2x2 Image Collage for Playlists
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = imageUrls[0],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = imageUrls[1],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = imageUrls[2],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = imageUrls[3],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            imageUrls.isNotEmpty() -> {
                // Single Cover
                AsyncImage(
                    model = imageUrls.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
