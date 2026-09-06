package com.example.muzo.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.draw.shadow
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

import com.example.muzo.ui.components.MuziSongRow
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
    artistDescription: String? = null,
    artistHeroThumbnail: String? = null,
    currentPlayingSongId: String? = null,
    isPlaybackPlaying: Boolean = false,
    artistPlaylists: List<ShelfItem> = emptyList(),
    similarArtists: List<ShelfItem> = emptyList(),
    relatedPlaylists: List<ShelfItem> = emptyList(),
    isLoading: Boolean,
    onBack: () -> Unit,
    onSearchClick: () -> Unit = {},
    onSongSelect: (SongItem, List<SongItem>) -> Unit,
    onPlayAll: () -> Unit = {},
    onRadioClick: (() -> Unit)? = null,
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
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 250
        }
    }
    val topBarBgColor by animateColorAsState(
        targetValue = if (showTopBarTitle) Color(0xF509080D) else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "topBarBg"
    )

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
                            AnimatedVisibility(
                                visible = showTopBarTitle,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(200))
                            ) {
                                Text(
                                    text = playlist.title ?: "Playlist",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
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
                            IconButton(onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, playlist.title)
                                    putExtra(Intent.EXTRA_TEXT, "${playlist.title} - Listen on Muzo\nhttps://music.youtube.com/playlist?list=${playlist.id}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = topBarBgColor
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
            // ==================== VIVI MUSIC ARTIST PROFILE SCREEN (vivi_3.png) ====================
            var isBioExpanded by remember { mutableStateOf(false) }
            var isSubscribed by remember { mutableStateOf(false) }

            val cleanMonthly = remember(artistMonthlyListeners) {
                artistMonthlyListeners?.let { raw ->
                    val t = raw.replace(Regex("(?i)monthly listeners"), "").replace(Regex("(?i)monthly"), "").replace(Regex("(?i)listeners"), "").trim()
                    if (t.isNotEmpty()) "$t Monthly" else null
                }
            }

            val cleanSubscribers = remember(artistSubscribers) {
                val raw = artistSubscribers?.trim().orEmpty()
                if (raw.isBlank() || raw.equals("Artist", ignoreCase = true)) {
                    "Artist"
                } else {
                    val t = raw.replace(Regex("(?i)subscribers"), "").replace(Regex("(?i)subscriber"), "").trim()
                    if (t.isNotEmpty()) "$t Subscribers" else raw
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070709)),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // 1. Immersive Hero Artwork Banner (vivi_3.png)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp)
                            .background(Color(0xFF13151D))
                    ) {
                        val thumb = (artistHeroThumbnail ?: playlist.thumbnail)?.let { getHighResThumbnail(it) } ?: ""
                        if (thumb.isNotBlank()) {
                            AsyncImage(
                                model = thumb,
                                contentDescription = playlist.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Vertical Gradient Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.40f),
                                            0.25f to Color.Transparent,
                                            0.60f to Color(0xFF070709).copy(alpha = 0.20f),
                                            0.82f to Color(0xFF070709).copy(alpha = 0.88f),
                                            1.0f to Color(0xFF070709)
                                        )
                                    )
                                )
                        )

                        // Top Nav overlay (Back + Share) - vivi_3.png
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
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Listen to ${playlist.title} on Muzi: https://music.youtube.com/channel/${playlist.id}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Artist"))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }
                        }

                        // Bottom Title on banner (vivi_3.png)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = playlist.title ?: "Artist",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. Stat Pills Row (vivi_3.png)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pill 1: Subscribers (dark capsule)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF232631)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFFD2D5E0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cleanSubscribers,
                                    color = Color(0xFFD2D5E0),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Pill 2: Monthly Listeners (vivi_3.png pastel lavender pill)
                        if (!cleanMonthly.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFE2D4F7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color(0xFF2C1948),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cleanMonthly,
                                        color = Color(0xFF2C1948),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 3. "About" Bio Section (vivi_3.png)
                    if (!artistDescription.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "About",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = artistDescription,
                                color = Color(0xFFA5A9B8),
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                maxLines = if (isBioExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.animateContentSize()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF232631),
                                modifier = Modifier.clickable { isBioExpanded = !isBioExpanded }
                            ) {
                                Text(
                                    text = if (isBioExpanded) "Less" else "More",
                                    color = Color(0xFFC8CBD6),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Action Buttons Row: [Subscribe], [Radio], [Shuffle] (vivi_3.png)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Subscribe
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSubscribed) Color(0xFF2E3240) else Color(0xFF1E2029),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable {
                                    isSubscribed = !isSubscribed
                                    Toast.makeText(
                                        context,
                                        if (isSubscribed) "Subscribed to ${playlist.title}" else "Unsubscribed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSubscribed) Icons.Default.Check else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFFD6D8E4),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSubscribed) "Subscribed" else "Subscribe",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Radio
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1E2029),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable {
                                    onRadioClick?.invoke() ?: onPlayAll()
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = null,
                                    tint = Color(0xFFD6D8E4),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Radio",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Shuffle
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1E2029),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable {
                                    if (songs.isNotEmpty()) {
                                        val sh = songs.shuffled()
                                        onSongSelect(sh[0], sh)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                    tint = Color(0xFFD6D8E4),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Shuffle",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 5. "Top songs" Heading with Arrow (vivi_3.png)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top songs",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "See all songs",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 6. Song List (vivi_3.png cards with active play capsule)
                itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { _, song ->
                    val isCurrentlyPlaying = song.id == currentPlayingSongId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSongSelect(song, songs) },
                        color = Color(0xFF14161E),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF222430)),
                                contentAlignment = Alignment.Center
                            ) {
                                val thumb = song.thumbnail?.let { getHighResThumbnail(it) } ?: song.thumbnail.orEmpty()
                                AsyncImage(
                                    model = thumb,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Title & Subtitle
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = if (isCurrentlyPlaying) Color(0xFFE2D4F7) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (song.explicit == true) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color(0xFF383A48),
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = "E",
                                                color = Color(0xFFB0B4C4),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    val artistSubtitle = song.artists.joinToString(", ") { it.name }.ifBlank { playlist.title ?: "Artist" }
                                    Text(
                                        text = artistSubtitle,
                                        color = Color(0xFF8E92A0),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Right action: prominent lavender play/pause pill if active (vivi_3.png), else 3-dots
                            if (isCurrentlyPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE2D4F7))
                                        .clickable {
                                            onSongSelect(song, songs)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaybackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Playing",
                                        tint = Color(0xFF2C1948),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { onSongActionClick?.invoke(song, songs) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. "Playlists & Albums by [Artist Name]"
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
                                text = "Playlists & Albums by ${playlist.title ?: "Artist"}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "See playlists",
                                tint = Color.White,
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
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF1E1E24))
                                    ) {
                                        SingleCover(imageUrl = item.imageUrls.firstOrNull() ?: "")
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

                // 8. "Fans might also like"
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
                                    SingleCover(
                                        imageUrl = item.imageUrls.firstOrNull() ?: "",
                                        modifier = Modifier.size(85.dp),
                                        isCircle = true
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
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val topContentPadding = statusBarTop + 64.dp + 16.dp

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topContentPadding, bottom = 150.dp)
            ) {
                // 1. Hero Artwork Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val thumb = playlist.thumbnail?.let { getHighResThumbnail(it) } ?: ""
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .shadow(14.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            SingleCover(imageUrl = thumb)
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

                        val authorName = playlist.author?.name
                        if (!authorName.isNullOrBlank() && authorName != "Artist" && authorName != playlist.title) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = authorName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Duration and Song count
                        Text(
                            text = formattedDuration,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // ViVi Dual Action Hero Buttons: [Play] & [Shuffle]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Play (Filled Capsule)
                            Button(
                                onClick = {
                                    if (songs.isNotEmpty()) onSongSelect(songs[0], songs) else onPlayAll()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = playBtnBg,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Play",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // 2. Shuffle (Tonal Capsule)
                            FilledTonalButton(
                                onClick = {
                                    if (songs.isNotEmpty()) {
                                        val shuffled = songs.shuffled()
                                        onSongSelect(shuffled[0], shuffled)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Shuffle",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Action Icons Row: Save, Download, Share, More
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    isSaved = !isSaved
                                    Toast.makeText(
                                        context,
                                        if (isSaved) "Added to library" else "Removed from library",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Save",
                                    tint = if (isSaved) Color(0xFFFF4B6E) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Downloading playlist...", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, playlist.title)
                                        putExtra(Intent.EXTRA_TEXT, "${playlist.title} - Listen on Muzi\nhttps://music.youtube.com/playlist?list=${playlist.id}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { onPlaylistActionClick?.invoke() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. Tracklist Items (Muzi Flow Unified Song Row)
                itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                    MuziSongRow(
                        song = song,
                        index = index + 1,
                        onClick = { onSongSelect(song, songs) },
                        onActionClick = { onSongActionClick?.invoke(song, songs) }
                    )
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