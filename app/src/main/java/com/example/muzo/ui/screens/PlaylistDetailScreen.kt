package com.example.muzo.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.ui.components.CollageCover
import com.example.muzo.ui.components.OnlineBlur
import com.example.muzo.ui.components.ShelfCard
import com.example.muzo.ui.components.SingleCover
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistItem,
    songs: List<SongItem>,
    isArtist: Boolean = false,
    artistSubscribers: String? = null,
    artistMonthlyListeners: String? = null,
    artistPlaylists: List<ShelfItem> = emptyList(),
    similarArtists: List<ShelfItem> = emptyList(),
    relatedPlaylists: List<ShelfItem> = emptyList(),
    isLoading: Boolean,
    onBack: () -> Unit,
    onSearchClick: () -> Unit = {},
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    onPlayAll: () -> Unit = {},
    onRelatedPlaylistClick: (ShelfItem) -> Unit = {},
    onSimilarArtistClick: (ShelfItem) -> Unit = {},
    onSongActionClick: ((SongItem, List<SongItem>) -> Unit)? = null,
    onPlaylistActionClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val fallbackAmbient = Color(0xFF1B2321)
    val fallbackAccent = Color(0xFFF6A89E)

    var extractedAmbientColor by remember(playlist.thumbnail) { mutableStateOf<Color?>(null) }
    var extractedPlayButtonColor by remember(playlist.thumbnail) { mutableStateOf<Color?>(null) }

    LaunchedEffect(playlist.thumbnail) {
        val thumbUrl = playlist.thumbnail?.let { getHighResThumbnail(it) }
        if (!thumbUrl.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(thumbUrl)
                        .allowHardware(false)
                        .size(120, 120)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(16)
                                    .generate()
                            }

                            val ambientSwatch = palette.vibrantSwatch
                                ?: palette.dominantSwatch
                                ?: palette.darkVibrantSwatch
                                ?: palette.mutedSwatch
                                ?: palette.lightVibrantSwatch

                            if (ambientSwatch != null) {
                                val argb = ambientSwatch.rgb
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(argb, hsv)
                                hsv[1] = (hsv[1] * 1.25f).coerceIn(0.40f, 0.95f)
                                hsv[2] = (hsv[2] * 0.70f).coerceIn(0.30f, 0.60f)
                                extractedAmbientColor = Color(android.graphics.Color.HSVToColor(hsv))
                            }

                            val playSwatch = palette.lightVibrantSwatch
                                ?: palette.vibrantSwatch
                                ?: palette.dominantSwatch

                            if (playSwatch != null) {
                                val playHsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(playSwatch.rgb, playHsv)
                                playHsv[1] = (playHsv[1] * 0.45f).coerceIn(0.20f, 0.45f)
                                playHsv[2] = 0.98f
                                extractedPlayButtonColor = Color(android.graphics.Color.HSVToColor(playHsv))
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } else {
            extractedAmbientColor = null
            extractedPlayButtonColor = null
        }
    }

    val animatedAmbientColor by animateColorAsState(
        targetValue = extractedAmbientColor ?: fallbackAmbient,
        animationSpec = tween(durationMillis = 600),
        label = "playlistAmbientBg"
    )

    val playBtnBg = extractedPlayButtonColor ?: fallbackAccent
    val amoledBlack = Color(0xFF09080D)

    val ambientBrush = Brush.verticalGradient(
        0.0f to animatedAmbientColor.copy(alpha = 0.85f),
        0.35f to animatedAmbientColor.copy(alpha = 0.55f),
        0.60f to animatedAmbientColor.copy(alpha = 0.20f),
        0.85f to amoledBlack,
        1.0f to amoledBlack
    )

    val listState = rememberLazyListState()
    val showTopBarTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    var isSaved by remember { mutableStateOf(false) }

    val totalSeconds = remember(songs) {
        songs.sumOf { (it.duration ?: 0).toLong() }
    }
    val formattedDuration = remember(songs, totalSeconds) {
        if (totalSeconds > 0) {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            if (hours > 0) {
                "${songs.size} songs • ${hours}h ${minutes}m"
            } else {
                "${songs.size} songs • ${minutes}m"
            }
        } else if (songs.isNotEmpty()) {
            "${songs.size} songs"
        } else {
            playlist.songCountText ?: "Playlist"
        }
    }

    val coverUrl = remember(playlist.thumbnail) {
        playlist.thumbnail?.let { getHighResThumbnail(it) } ?: playlist.thumbnail
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(amoledBlack)
    ) {
        // Blurred cover art background matching Echo Music (ONLY for playlists/albums, NOT artist)
        if (!isArtist && !coverUrl.isNullOrBlank()) {
            OnlineBlur(
                thumbnailUrl = coverUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp),
                blurRadius = 50.dp,
                bottomFade = 220.dp
            )
        }
        Scaffold(
            topBar = {
                if (!isArtist) {
                    TopAppBar(
                        title = {
                            if (showTopBarTitle) {
                                Text(
                                    text = playlist.title ?: "Playlist",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color(0xEE09080D)
                        )
                    )
                }
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isArtist) PaddingValues(0.dp) else PaddingValues(top = paddingValues.calculateTopPadding())),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = playBtnBg)
                }
            } else if (isArtist) {
            // ==================== ARTIST PROFILE SCREEN (Screenshots 1, 2, 3) ====================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // 1. Immersive Hero Banner (Screenshot 1)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {
                        val thumb = playlist.thumbnail?.let { getHighResThumbnail(it) } ?: ""
                        AsyncImage(
                            model = thumb,
                            contentDescription = playlist.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient Fade Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.45f),
                                            Color.Transparent,
                                            Color(0xFF08080A).copy(alpha = 0.85f),
                                            Color(0xFF08080A)
                                        )
                                    )
                                )
                        )
                        // Top Nav overlay (Back + Share)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }
                        }
                        // Bottom Title + Stats on banner
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = playlist.title ?: "Artist",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Stat Pills (Screenshot 1)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF23232A)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = artistSubscribers ?: "2.7M Subscribers",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFD4BEE4)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = Color(0xFF281335),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = artistMonthlyListeners ?: "70.9M Monthly",
                                            color = Color(0xFF281335),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Action Buttons: [Subscribe], [Radio], [Shuffle] (Screenshot 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1E1E24),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {}
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Subscribe", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1E1E24),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {}
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Radio, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Radio", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1E1E24),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (songs.isNotEmpty()) {
                                        val sh = songs.shuffled()
                                        onSongSelect(sh[0], sh)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. "Top songs" Heading with Arrow (Screenshot 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top songs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "See all songs",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 4. Songs List
                itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSongSelect(song, songs) },
                                onLongClick = { onSongActionClick?.invoke(song, songs) }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = getHighResThumbnail(song.thumbnail),
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val artistName = song.artists.joinToString(", ") { it.name }
                            val durationSec = song.duration
                            val durationText = if (durationSec != null && durationSec > 0) {
                                val m = durationSec / 60
                                val s = durationSec % 60
                                String.format(" • %02d:%02d", m, s)
                            } else ""
                            Text(
                                text = "$artistName$durationText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onSongActionClick?.invoke(song, songs) }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.LightGray
                            )
                        }
                    }
                }

                // 5. "Playlists by [Artist Name]" (Screenshot 2 & 3)
                if (artistPlaylists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Playlists by ${playlist.title ?: "Artist"}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "See playlists",
                                tint = Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(artistPlaylists, key = { it.id }) { item ->
                                Column(
                                    modifier = Modifier
                                        .width(135.dp)
                                        .clickable { onRelatedPlaylistClick(item) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(135.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1E1E24))
                                    ) {
                                        AsyncImage(
                                            model = item.imageUrls.firstOrNull(),
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle.ifBlank { "Playlist" },
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. "Fans might also like" (Screenshot 2 & 3)
                if (similarArtists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Fans might also like",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(similarArtists, key = { it.id }) { item ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(95.dp)
                                        .clickable { onSimilarArtistClick(item) }
                                ) {
                                    AsyncImage(
                                        model = item.imageUrls.firstOrNull(),
                                        contentDescription = item.title,
                                        modifier = Modifier
                                            .size(85.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E1E24)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Artist",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==================== NORMAL PLAYLIST SCREEN ====================
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = 150.dp)
            ) {
                // 1. Hero Artwork Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val thumb = playlist.thumbnail?.let { getHighResThumbnail(it) } ?: ""
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E1E24))
                        ) {
                            if (playlist.title?.contains("community", ignoreCase = true) == true) {
                                CollageCover(imageUrls = listOf(thumb, thumb, thumb, thumb))
                            } else {
                                SingleCover(imageUrl = thumb)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = playlist.title ?: "Untitled Playlist",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            lineHeight = 30.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Duration and Song count pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = formattedDuration,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD4D4D8),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Row 1: [Save] [Play] [Share]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Save pill
                            Surface(
                                onClick = {
                                    isSaved = !isSaved
                                    Toast.makeText(
                                        context,
                                        if (isSaved) "Playlist saved to library" else "Playlist removed from library",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Save",
                                        tint = if (isSaved) Color(0xFFFF4B6E) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSaved) "Saved" else "Save",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Play prominent wide pill
                            Surface(
                                onClick = {
                                    if (songs.isNotEmpty()) {
                                        onSongSelect(songs[0], songs)
                                    } else {
                                        onPlayAll()
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                color = playBtnBg,
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 28.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color(0xFF1E1015),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Play",
                                        color = Color(0xFF1E1015),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Share circle
                            Surface(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, playlist.title)
                                        putExtra(Intent.EXTRA_TEXT, "${playlist.title} - Listen on Muzo\nhttps://music.youtube.com/playlist?list=${playlist.id}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
                                },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 2: [⬇ Save] [🔀 Shuf...] [⋮ More]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Download / Save
                            Surface(
                                onClick = {
                                    Toast.makeText(context, "Downloading playlist songs...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Save",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Save",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Shuffle
                            Surface(
                                onClick = {
                                    if (songs.isNotEmpty()) {
                                        val shuffled = songs.shuffled()
                                        onSongSelect(shuffled[0], shuffled)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Shuf...",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // More
                            Surface(
                                onClick = {
                                    onPlaylistActionClick?.invoke()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "More",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Tracklist Items
                itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongSelect(song, songs) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = getHighResThumbnail(song.thumbnail),
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val artistName = song.artists.joinToString(", ") { it.name }
                            val durationSec = song.duration
                            val durationText = if (durationSec != null && durationSec > 0) {
                                val m = durationSec / 60
                                val s = durationSec % 60
                                String.format(" • %02d:%02d", m, s)
                            } else ""
                            Text(
                                text = "$artistName$durationText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = {
                            if (onSongActionClick != null) {
                                onSongActionClick(song, songs)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.LightGray
                            )
                        }
                    }
                }

                // 4. "Related Playlist" Shelf at Bottom
                if (relatedPlaylists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Related Playlist",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B8AFD),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(relatedPlaylists) { item ->
                                ShelfCard(item = item, onClick = { onRelatedPlaylistClick(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}
}