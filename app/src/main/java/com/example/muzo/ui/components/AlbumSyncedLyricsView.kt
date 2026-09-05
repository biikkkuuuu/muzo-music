package com.example.muzo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class LyricLineItem(val timeMs: Long, val text: String)

data class LyricsResult(
    val syncedLines: List<LyricLineItem> = emptyList(),
    val plainLyrics: String? = null,
    val source: String = "None"
)

// In-memory cache for instant switching between Album Art and Lyrics
private val lyricsCache = ConcurrentHashMap<String, LyricsResult>()

/**
 * BetterLyrics-style Synchronized Lyrics View rendered directly in the Album Artwork space.
 * Features:
 *  - Auto-scrolls smoothly to keep the current active line centered.
 *  - High-contrast pure white bold active line with glowing accent indicator.
 *  - Translucent inactive lines.
 *  - Click-to-seek on any lyric line.
 *  - Top & bottom gradient edge fading masks.
 *  - Frosted glass backdrop with blurred album artwork glowing through.
 *  - Horizontal swipe gesture support for next/previous track navigation.
 *  - Quick toggle button to return to Album Cover art.
 */
@Composable
fun AlbumSyncedLyricsView(
    song: SongItem,
    currentPosition: Long,
    onSeek: (Long) -> Unit,
    onCloseLyrics: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    hasNext: Boolean,
    hasPrev: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember(song.id) { mutableStateOf(!lyricsCache.containsKey(song.id)) }
    var lyricsData by remember(song.id) { mutableStateOf(lyricsCache[song.id] ?: LyricsResult()) }
    var hasError by remember(song.id) { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var swipeOffsetAccumulator by remember { mutableFloatStateOf(0f) }

    // Fetch lyrics logic with multi-source fallback
    val loadLyrics: () -> Unit = {
        isLoading = true
        hasError = false
        scope.launch {
            withContext(Dispatchers.IO) {
                // Check cache first
                val cached = lyricsCache[song.id]
                if (cached != null) {
                    withContext(Dispatchers.Main) {
                        lyricsData = cached
                        isLoading = false
                    }
                    return@withContext
                }

                var foundSynced: List<LyricLineItem>? = null
                var foundPlain: String? = null
                var source = "None"

                val cleanTitle = cleanSongTitle(song.title)
                val artistName = song.artists.firstOrNull()?.name ?: ""
                val cleanArtist = cleanArtistName(artistName)
                val durSec = song.duration ?: 0

                // 1. First try LRCLIB exact get
                try {
                    val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
                    val encArtist = URLEncoder.encode(cleanArtist, "UTF-8")
                    val urlStr = "https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist&duration=$durSec"
                    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 3500
                        readTimeout = 3500
                        setRequestProperty("User-Agent", "MuziMusic/1.0")
                    }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        val synced = json.optString("syncedLyrics", "")
                        val plain = json.optString("plainLyrics", "")

                        if (synced.isNotBlank()) {
                            val parsed = parseLrcString(synced)
                            if (parsed.isNotEmpty()) {
                                foundSynced = parsed
                                source = "BetterLyrics (Synced)"
                            }
                        }
                        if (foundSynced == null && plain.isNotBlank()) {
                            foundPlain = plain
                            source = "BetterLyrics (Plain)"
                        }
                    }
                } catch (_: Exception) {}

                // 2. If exact get fails, try LRCLIB search query
                if (foundSynced == null) {
                    try {
                        val searchQuery = "$cleanTitle $cleanArtist".trim()
                        val encQuery = URLEncoder.encode(searchQuery, "UTF-8")
                        val searchUrl = "https://lrclib.net/api/search?q=$encQuery"
                        val conn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 4000
                            readTimeout = 4000
                            setRequestProperty("User-Agent", "MuziMusic/1.0")
                        }
                        if (conn.responseCode == 200) {
                            val body = conn.inputStream.bufferedReader().use { it.readText() }
                            val array = JSONArray(body)
                            for (i in 0 until array.length().coerceAtMost(6)) {
                                val item = array.getJSONObject(i)
                                val synced = item.optString("syncedLyrics", "")
                                val plain = item.optString("plainLyrics", "")

                                if (synced.isNotBlank()) {
                                    val parsed = parseLrcString(synced)
                                    if (parsed.isNotEmpty()) {
                                        foundSynced = parsed
                                        source = "BetterLyrics (Synced)"
                                        break
                                    }
                                } else if (foundPlain == null && plain.isNotBlank()) {
                                    foundPlain = plain
                                    source = "BetterLyrics (Plain)"
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 3. Fallback to YouTube Music lyrics endpoint
                if (foundSynced == null && foundPlain == null) {
                    try {
                        val nextRes = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()
                        val endpoint = nextRes?.lyricsEndpoint
                        if (endpoint != null) {
                            val ytLyrics = YouTube.lyrics(endpoint).getOrNull()
                            if (!ytLyrics.isNullOrBlank()) {
                                val parsed = parseLrcString(ytLyrics)
                                if (parsed.isNotEmpty()) {
                                    foundSynced = parsed
                                    source = "YouTube Music (Synced)"
                                } else {
                                    foundPlain = ytLyrics
                                    source = "YouTube Music"
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                val result = LyricsResult(
                    syncedLines = foundSynced ?: emptyList(),
                    plainLyrics = foundPlain,
                    source = source
                )

                if (result.syncedLines.isNotEmpty() || !result.plainLyrics.isNullOrBlank()) {
                    lyricsCache[song.id] = result
                }

                withContext(Dispatchers.Main) {
                    lyricsData = result
                    isLoading = false
                    hasError = result.syncedLines.isEmpty() && result.plainLyrics.isNullOrBlank()
                }
            }
        }
    }

    LaunchedEffect(song.id) {
        loadLyrics()
    }

    // Active line calculation based on current track position
    val activeIndex = remember(currentPosition, lyricsData.syncedLines) {
        if (lyricsData.syncedLines.isEmpty()) -1
        else lyricsData.syncedLines.indexOfLast { it.timeMs <= currentPosition }
    }

    // Smooth auto-scroll to keep singing line centered
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyricsData.syncedLines.indices) {
            try {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -140
                )
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0F0E13))
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffsetAccumulator = (swipeOffsetAccumulator + dragAmount).coerceIn(-180f, 180f)
                    },
                    onDragEnd = {
                        if (swipeOffsetAccumulator < -75f && hasNext) {
                            onNext()
                        } else if (swipeOffsetAccumulator > 75f && hasPrev) {
                            onPrev()
                        }
                        swipeOffsetAccumulator = 0f
                    },
                    onDragCancel = { swipeOffsetAccumulator = 0f }
                )
            }
    ) {
        // 1. Ambient blurred background artwork glow
        AsyncImage(
            model = getHighResThumbnail(song.thumbnail),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.18f }
        )

        // Dark gradient glass overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 2. Main Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Syncing lyrics...",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            lyricsData.syncedLines.isNotEmpty() -> {
                // Synchronized Lyrics List
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 46.dp, bottom = 64.dp, start = 18.dp, end = 18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = lyricsData.syncedLines,
                        key = { index, item -> "$index-${item.timeMs}" }
                    ) { index, line ->
                        val isCurrent = index == activeIndex
                        val textColor by animateColorAsState(
                            targetValue = if (isCurrent) Color.White else Color.White.copy(alpha = 0.35f),
                            animationSpec = tween(220),
                            label = "lyricColor"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isCurrent) 1.03f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "lyricScale"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSeek(line.timeMs) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 3.5.dp, height = 22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Text(
                                text = line.text,
                                fontSize = if (isCurrent) 21.sp else 16.5.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = textColor,
                                lineHeight = if (isCurrent) 28.sp else 24.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                    }
                            )
                        }
                    }
                }
            }

            !lyricsData.plainLyrics.isNullOrBlank() -> {
                // Plain Lyrics Fallback
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(scrollState)
                        .padding(top = 46.dp, bottom = 64.dp)
                ) {
                    Text(
                        text = lyricsData.plainLyrics ?: "",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.88f),
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            else -> {
                // No Lyrics Found / Error State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Lyrics not available for this song",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { loadLyrics() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Retry",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onCloseLyrics() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Cover",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cover",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Top edge gradient fade scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.92f), Color.Transparent)
                    )
                )
        )

        // 4. Bottom edge gradient fade scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                    )
                )
        )

        // 5. Floating Header Badge & Cover Art Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (lyricsData.syncedLines.isNotEmpty()) "SYNCED LYRICS" else "LYRICS",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Cover Art Quick Flip Button
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onCloseLyrics() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Show Album Cover",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Parses LRC formatted strings "[mm:ss.xx] text" into sorted LyricLineItem list.
 */
private fun parseLrcString(lrcContent: String): List<LyricLineItem> {
    val lines = mutableListOf<LyricLineItem>()
    val regex = Regex("""^\[(\d{1,2}):(\d{1,2}(?:\.\d{1,3})?)\](.*)$""")

    lrcContent.lineSequence().forEach { line ->
        val trimmed = line.trim()
        val match = regex.find(trimmed)
        if (match != null) {
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toFloatOrNull() ?: 0f
            val text = match.groupValues[3].trim()
            if (text.isNotBlank()) {
                val timeMs = (minutes * 60 * 1000) + (seconds * 1000).toLong()
                lines.add(LyricLineItem(timeMs, text))
            }
        }
    }
    return lines.sortedBy { it.timeMs }
}

/**
 * Cleans song title by removing YouTube noise like (Official Video), [HD], etc.
 */
private fun cleanSongTitle(title: String): String {
    return title
        .replace(Regex("""(?i)\(.*?official.*?\)|\[.*?official.*?\]"""), "")
        .replace(Regex("""(?i)\(.*?video.*?\)|\[.*?video.*?\]"""), "")
        .replace(Regex("""(?i)\(.*?audio.*?\)|\[.*?audio.*?\]"""), "")
        .replace(Regex("""(?i)\(.*?lyrics.*?\)|\[.*?lyrics.*?\]"""), "")
        .replace(Regex("""(?i)\|.*$"""), "")
        .replace(Regex("""(?i)-.*video.*$"""), "")
        .trim()
}

/**
 * Cleans artist name.
 */
private fun cleanArtistName(artist: String): String {
    return artist
        .replace(Regex("""(?i) - topic$"""), "")
        .trim()
}
