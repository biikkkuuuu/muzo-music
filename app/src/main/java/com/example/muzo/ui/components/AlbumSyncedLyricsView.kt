package com.example.muzo.ui.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.abs

data class LyricLineItem(val timeMs: Long, val text: String)

data class LyricsCandidate(
    val syncedLines: List<LyricLineItem>,
    val plainLyrics: String?,
    val source: String,
    val durationSec: Int
)

data class LyricsResult(
    val candidates: List<LyricsCandidate> = emptyList(),
    val activeCandidateIndex: Int = 0
) {
    val activeCandidate: LyricsCandidate?
        get() = candidates.getOrNull(activeCandidateIndex)
}

// In-memory cache for instant switching between Album Art and Lyrics
private val lyricsCache = ConcurrentHashMap<String, LyricsResult>()
private val songOffsetMap = ConcurrentHashMap<String, Long>()

/**
 * BetterLyrics-style Synchronized Lyrics View rendered directly in the Album Artwork space.
 * Features:
 *  - Blurred ambient album cover in background ("vlur jesa").
 *  - Clean time-synced lyrics with active line pure-white highlight.
 *  - Interactive Resync Controls: [-0.5s], [+0.5s], [Sync Offset], [↻ Resync], [🖼 Cover].
 *  - Auto-scrolls smoothly to keep the current active line centered.
 *  - Click-to-seek on any lyric line.
 *  - Top & bottom gradient edge fading masks.
 *  - Horizontal swipe gesture support for next/previous track navigation.
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember(song.id) { mutableStateOf(!lyricsCache.containsKey(song.id)) }
    var lyricsResult by remember(song.id) { mutableStateOf(lyricsCache[song.id] ?: LyricsResult()) }
    var candidateIndex by remember(song.id) { mutableIntStateOf(0) }
    var manualOffsetMs by remember(song.id) { mutableLongStateOf(songOffsetMap[song.id] ?: 0L) }

    val listState = rememberLazyListState()
    var swipeOffsetAccumulator by remember { mutableFloatStateOf(0f) }

    val currentCandidate = lyricsResult.candidates.getOrNull(candidateIndex)
    val syncedLines = currentCandidate?.syncedLines ?: emptyList()
    val plainLyrics = currentCandidate?.plainLyrics

    // Fetch lyrics logic with multi-candidate collection
    val fetchLyrics: (Boolean) -> Unit = { forceRefresh ->
        if (forceRefresh) {
            lyricsCache.remove(song.id)
        }
        isLoading = true
        scope.launch {
            withContext(Dispatchers.IO) {
                if (!forceRefresh) {
                    val cached = lyricsCache[song.id]
                    if (cached != null && cached.candidates.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            lyricsResult = cached
                            candidateIndex = cached.activeCandidateIndex.coerceIn(0, cached.candidates.lastIndex)
                            isLoading = false
                        }
                        return@withContext
                    }
                }

                val candidates = mutableListOf<LyricsCandidate>()
                val cleanTitle = cleanSongTitle(song.title)
                val artistName = song.artists.firstOrNull()?.name ?: ""
                val cleanArtist = cleanArtistName(artistName)
                val targetDur = song.duration ?: 0

                // 1. Try LRCLIB search to gather multiple candidates
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
                        for (i in 0 until array.length().coerceAtMost(10)) {
                            val item = array.getJSONObject(i)
                            val synced = item.optString("syncedLyrics", "")
                            val plain = item.optString("plainLyrics", "")
                            val dur = item.optDouble("duration", 0.0).toInt()
                            val trackName = item.optString("trackName", "")

                            if (synced.isNotBlank()) {
                                val parsed = parseLrcString(synced)
                                if (parsed.isNotEmpty()) {
                                    candidates.add(
                                        LyricsCandidate(
                                            syncedLines = parsed,
                                            plainLyrics = plain.ifBlank { null },
                                            source = "BetterLyrics (Synced)",
                                            durationSec = dur
                                        )
                                    )
                                }
                            } else if (plain.isNotBlank() && candidates.none { it.plainLyrics == plain }) {
                                candidates.add(
                                    LyricsCandidate(
                                        syncedLines = emptyList(),
                                        plainLyrics = plain,
                                        source = "BetterLyrics (Plain)",
                                        durationSec = dur
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 2. Exact get fallback if search returned nothing
                if (candidates.isEmpty()) {
                    try {
                        val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
                        val encArtist = URLEncoder.encode(cleanArtist, "UTF-8")
                        val urlStr = "https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist&duration=$targetDur"
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
                            val dur = json.optDouble("duration", 0.0).toInt()

                            if (synced.isNotBlank()) {
                                val parsed = parseLrcString(synced)
                                if (parsed.isNotEmpty()) {
                                    candidates.add(
                                        LyricsCandidate(
                                            syncedLines = parsed,
                                            plainLyrics = plain.ifBlank { null },
                                            source = "BetterLyrics (Synced)",
                                            durationSec = dur
                                        )
                                    )
                                }
                            } else if (plain.isNotBlank()) {
                                candidates.add(
                                    LyricsCandidate(
                                        syncedLines = emptyList(),
                                        plainLyrics = plain,
                                        source = "BetterLyrics (Plain)",
                                        durationSec = dur
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 3. YouTube Music endpoint fallback
                if (candidates.isEmpty()) {
                    try {
                        val nextRes = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()
                        val endpoint = nextRes?.lyricsEndpoint
                        if (endpoint != null) {
                            val ytLyrics = YouTube.lyrics(endpoint).getOrNull()
                            if (!ytLyrics.isNullOrBlank()) {
                                val parsed = parseLrcString(ytLyrics)
                                if (parsed.isNotEmpty()) {
                                    candidates.add(
                                        LyricsCandidate(
                                            syncedLines = parsed,
                                            plainLyrics = null,
                                            source = "YouTube Music (Synced)",
                                            durationSec = targetDur
                                        )
                                    )
                                } else {
                                    candidates.add(
                                        LyricsCandidate(
                                            syncedLines = emptyList(),
                                            plainLyrics = ytLyrics,
                                            source = "YouTube Music",
                                            durationSec = targetDur
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Sort candidates to place closest duration match first
                val sorted = if (targetDur > 0) {
                    candidates.sortedBy { abs(it.durationSec - targetDur) }
                } else candidates

                val res = LyricsResult(candidates = sorted, activeCandidateIndex = 0)
                lyricsCache[song.id] = res

                // Check for intro duration difference offset
                val topCand = sorted.firstOrNull()
                if (topCand != null && targetDur > 0 && topCand.durationSec > 0) {
                    val diff = targetDur - topCand.durationSec
                    if (diff in 2..55 && !songOffsetMap.containsKey(song.id)) {
                        songOffsetMap[song.id] = diff * 1000L
                    }
                }

                withContext(Dispatchers.Main) {
                    lyricsResult = res
                    candidateIndex = 0
                    manualOffsetMs = songOffsetMap[song.id] ?: 0L
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(song.id) {
        fetchLyrics(false)
    }

    // Effective playback position considering manual/auto sync offset
    val effectivePosition = (currentPosition - manualOffsetMs).coerceAtLeast(0L)

    // Active line calculation based on effective position
    val activeIndex = remember(effectivePosition, syncedLines) {
        if (syncedLines.isEmpty()) -1
        else syncedLines.indexOfLast { it.timeMs <= effectivePosition }
    }

    // Smooth auto-scroll to keep singing line centered
    LaunchedEffect(activeIndex) {
        if (activeIndex in syncedLines.indices) {
            try {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -120
                )
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0D0B12))
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
        // ==============================================================
        // 1. AMBIENT BLURRED ALBUM COVER IN BACKGROUND ("vlur jesa")
        // ==============================================================
        AsyncImage(
            model = getHighResThumbnail(song.thumbnail),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(22.dp)
                .graphicsLayer { alpha = 0.38f }
        )

        // Dark frosted gradient scrim over the blurred image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.70f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // ==============================================================
        // 2. MAIN CONTENT (Synced Lyrics, Plain Lyrics, Loading, or Empty)
        // ==============================================================
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
                            text = "Syncing BetterLyrics...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            syncedLines.isNotEmpty() -> {
                // Synchronized Lyrics List
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 56.dp, bottom = 54.dp, start = 18.dp, end = 18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = syncedLines,
                        key = { idx, item -> "$idx-${item.timeMs}" }
                    ) { index, line ->
                        val isCurrent = index == activeIndex
                        val textColor by animateColorAsState(
                            targetValue = if (isCurrent) Color.White else Color.White.copy(alpha = 0.35f),
                            animationSpec = tween(180),
                            label = "lyricColor"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isCurrent) 1.04f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "lyricScale"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSeek(line.timeMs + manualOffsetMs) }
                                .padding(vertical = 7.dp, horizontal = 4.dp)
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
                                fontSize = if (isCurrent) 20.5.sp else 16.5.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = textColor,
                                lineHeight = if (isCurrent) 27.sp else 23.sp,
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

            !plainLyrics.isNullOrBlank() -> {
                // Plain Lyrics Fallback
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(scrollState)
                        .padding(top = 56.dp, bottom = 54.dp)
                ) {
                    Text(
                        text = plainLyrics,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
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
                            text = "Lyrics not available for this track",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { fetchLyrics(true) }
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
                                color = Color.White.copy(alpha = 0.18f),
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

        // ==============================================================
        // 3. TOP GRADIENT FADE SCRIM
        // ==============================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent)
                    )
                )
        )

        // ==============================================================
        // 4. BOTTOM GRADIENT FADE SCRIM
        // ==============================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                    )
                )
        )

        // ==============================================================
        // 5. RESYNC CONTROLS BAR (Top Header)
        // ==============================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Synced badge with cycle-candidate support
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        if (lyricsResult.candidates.size > 1) {
                            val nextIdx = (candidateIndex + 1) % lyricsResult.candidates.size
                            candidateIndex = nextIdx
                            val cand = lyricsResult.candidates[nextIdx]
                            Toast.makeText(context, "Version ${nextIdx + 1}/${lyricsResult.candidates.size} (${cand.source})", Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (syncedLines.isNotEmpty()) "SYNCED" else "LYRICS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.4.sp
                    )
                    if (lyricsResult.candidates.size > 1) {
                        Text(
                            text = " (${candidateIndex + 1}/${lyricsResult.candidates.size})",
                            fontSize = 9.5.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Real-Time Resync Offset Buttons: [-0.5s] [Offset Display] [+0.5s]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // -0.5s button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.20f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            val newOff = manualOffsetMs - 500L
                            manualOffsetMs = newOff
                            songOffsetMap[song.id] = newOff
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Offset Chip (Tap to reset offset)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (manualOffsetMs != 0L) Color.White else Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            manualOffsetMs = 0L
                            songOffsetMap[song.id] = 0L
                            Toast.makeText(context, "Offset Reset to 0.0s", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    val sec = manualOffsetMs / 1000f
                    val label = if (manualOffsetMs == 0L) "Sync" else String.format("%+.1fs", sec)
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (manualOffsetMs != 0L) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // +0.5s button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.20f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            val newOff = manualOffsetMs + 500L
                            manualOffsetMs = newOff
                            songOffsetMap[song.id] = newOff
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Action Buttons: Resync Reload & Return to Cover
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Reload / Re-fetch
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.20f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            fetchLyrics(true)
                            Toast.makeText(context, "Resyncing lyrics...", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Resync",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Return to Cover
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onCloseLyrics() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Cover",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
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
