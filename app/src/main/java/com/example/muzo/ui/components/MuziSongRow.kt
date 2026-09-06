package com.example.muzo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.theme.MuziThemeTokens
import com.music.innertube.models.SongItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MuziSongRow(
    song: SongItem,
    isPlaying: Boolean = false,
    index: Int? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = MuziThemeTokens.HorizontalPadding, vertical = 7.dp),
    modifier: Modifier = Modifier
) {
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Optional Track Index
        if (index != null) {
            Text(
                text = "$index",
                color = if (isPlaying) MuziThemeTokens.AccentRose else MuziThemeTokens.TextMuted,
                fontSize = 14.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Thumbnail with Playing State Indicator
        Box(
            modifier = Modifier
                .size(MuziThemeTokens.ThumbnailSize)
                .clip(MuziThemeTokens.ShapeThumbnail)
                .background(MuziThemeTokens.SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            val thumb = song.thumbnail?.let { getHighResThumbnail(it) } ?: song.thumbnail.orEmpty()
            AsyncImage(
                model = thumb,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Playing",
                        tint = MuziThemeTokens.AccentRose,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Title and Subtitle Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = song.title,
                color = if (isPlaying) MuziThemeTokens.AccentRose else MuziThemeTokens.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            val artistNames = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" }
            val durationText = song.duration?.let { seconds ->
                if (seconds > 0) {
                    val m = seconds / 60
                    val s = seconds % 60
                    String.format("%d:%02d", m, s)
                } else null
            }

            val subtitle = if (durationText != null) {
                "$artistNames • $durationText"
            } else {
                artistNames
            }

            Text(
                text = subtitle,
                color = MuziThemeTokens.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3-Dots Menu Action
        if (onActionClick != null) {
            IconButton(
                onClick = onActionClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MuziThemeTokens.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
