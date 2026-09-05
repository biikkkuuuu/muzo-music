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
import androidx.compose.material3.IconButton
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

internal val lyricsCache = ConcurrentHashMap<String, LyricsResult>()
internal val songOffsetMap = ConcurrentHashMap<String, Long>()

/**
 * Echo Music-style Open Canvas Synchronized Lyrics View (Matches Image 2).
 * Features:
 *  - Edge-to-edge ambient player gradient background (no boxed card frame).
 *  - Centered subtle "Lyrics from Kugou/LRCLIB" header.
 *  - Bold, high-impact synchronized lyrics typography (26sp active, 22sp inactive).
 *  - Smooth auto-scroll keeping the active lyric line centered.
 *  - Click-to-seek directly on any lyric line.
 *  - Top & bottom gradient edge fading masks.
 *  - Horizontal swipe gesture support for next/previous track navigation.
 */
@Composable
fun AlbumSyncedLyricsView(
    song: SongItem,
    currentPosition: Long,
    manualOffsetMs: Long = 0L,
    onOffsetChange: ((Long) -> Unit)? = null,
    onSeek: (Long) -> Unit,
    onCloseLyrics: () -> Unit = {},
    onClose: () -> Unit = onCloseLyrics,
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
    var currentOffsetMs by remember(song.id, manualOffsetMs) { 
        mutableLongStateOf(if (manualOffsetMs != 0L) manualOffsetMs else (songOffsetMap[song.id] ?: 0L)) 
    }

    val listState = rememberLazyListState()
    var swipeOffsetAccumulator by remember { mutableFloatStateOf(0f) }

    val currentCandidate = lyricsResult.candidates.getOrNull(candidateIndex)
    val syncedLines = currentCandidate?.syncedLines ?: emptyList()
    val plainLyrics = currentCandidate?.plainLyrics

    val fetchLyrics: (Boolean) -> Unit = { forceRefresh ->
        if (forceRefresh) lyricsCache.remove(song.id)
        isLoading = true
        scope.launch {
            withContext(Dispatchers.IO) {
                if (!forceRefresh) {
                    val cached = lyricsCache[song.id]
                    if (cached != null && cached.candidates.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            lyricsResult = cached
                            candidateIndex = cached.activeCandidateIndex
                            isLoading = false
                        }
                        return@withContext
                    }
                }

                val candidates = mutableListOf<LyricsCandidate>()
                val cleanTitle = cleanSongTitle(song.title)
                val cleanArtist = cleanArtistName(song.artists.firstOrNull()?.name ?: "")
                val targetDur = song.duration ?: 0

                try {
                    val searchQuery = "$cleanTitle $cleanArtist".trim()
                    val encQuery = URLEncoder.encode(searchQuery, "UTF-8")
                    val searchUrl = "https://lrclib.net/api/search?q=$encQuery"
                    val conn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        setRequestProperty("User-Agent", "Muzi/2.0")
                    }
                    if (conn.responseCode == 200) {
                        val resp = conn.inputStream.bufferedReader().readText()
                        val arr = JSONArray(resp)
                        for (i in 0 until arr.length().coerceAtMost(5)) {
                            val obj = arr.getJSONObject(i)
                            val syn = obj.optString("syncedLyrics")
                            val pln = obj.optString("plainLyrics")
                            val dur = obj.optInt("duration", 0)
                            if (!syn.isNullOrBlank()) {
                                candidates.add(
                                    LyricsCandidate(
                                        syncedLines = parseLrcString(syn),
                                        plainLyrics = pln.ifBlank { null },
                                        source = "LRCLIB",
                                        durationSec = dur
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}

                if (candidates.isEmpty()) {
                    try {
                        val durParam = if (targetDur > 0) "&duration=$targetDur" else ""
                        val encT = URLEncoder.encode(cleanTitle, "UTF-8")
                        val encA = URLEncoder.encode(cleanArtist, "UTF-8")
                        val getUrl = "https://lrclib.net/api/get?track_name=$encT&artist_name=$encA$durParam"
                        val conn = (URL(getUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 4000
                            readTimeout = 4000
                            setRequestProperty("User-Agent", "Muzi/2.0")
                        }
                        if (conn.responseCode == 200) {
                            val resp = conn.inputStream.bufferedReader().readText()
                            val obj = JSONObject(resp)
                            val syn = obj.optString("syncedLyrics")
                            val pln = obj.optString("plainLyrics")
                            val dur = obj.optInt("duration", targetDur)
                            if (!syn.isNullOrBlank()) {
                                candidates.add(
                                    LyricsCandidate(
                                        syncedLines = parseLrcString(syn),
                                        plainLyrics = pln.ifBlank { null },
                                        source = "LRCLIB",
                                        durationSec = dur
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (candidates.isEmpty()) {
                    try {
                        val watchResult = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()
                        val endpoint = watchResult?.lyricsEndpoint
                        if (endpoint != null) {
                            val ytLyrics = YouTube.lyrics(endpoint).getOrNull()
                            if (!ytLyrics.isNullOrBlank()) {
                                val parsed = parseLrcString(ytLyrics)
                                if (parsed.isNotEmpty()) {
                                    candidates.add(
                                        LyricsCandidate(
                                            syncedLines = parsed,
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

                val res = LyricsResult(candidates = candidates, activeCandidateIndex = 0)
                lyricsCache[song.id] = res

                withContext(Dispatchers.Main) {
                    lyricsResult = res
                    candidateIndex = 0
                    val initialOffset = songOffsetMap[song.id] ?: 0L
                    currentOffsetMs = initialOffset
                    onOffsetChange?.invoke(initialOffset)
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(song.id) {
        fetchLyrics(false)
    }

    val effectivePosition = (currentPosition - currentOffsetMs).coerceAtLeast(0L)
    val activeIndex = remember(effectivePosition, syncedLines) {
        if (syncedLines.isEmpty()) -1
        else syncedLines.indexOfLast { it.timeMs <= effectivePosition }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex in syncedLines.indices) {
            try {
                listState.animateScrollToItem(
                    index = (activeIndex - 1).coerceAtLeast(0),
                    scrollOffset = 0
                )
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffsetAccumulator = (swipeOffsetAccumulator + dragAmount).coerceIn(-180f, 180f)
                    },
                    onDragEnd = {
                        if (swipeOffsetAccumulator < -75f && hasNext) onNext()
                        else if (swipeOffsetAccumulator > 75f && hasPrev) onPrev()
                        swipeOffsetAccumulator = 0f
                    },
                    onDragCancel = { swipeOffsetAccumulator = 0f }
                )
            }
    ) {
        // 1. TOP HEADER: Centered Source Caption + Collapse Chevron (Separate row, never overlaps lyrics!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    modifier = Modifier.size(26.dp),
                    tint = Color.White.copy(alpha = 0.55f)
                )
            }

            val sourceText = currentCandidate?.source ?: "LRCLIB"
            Text(
                text = "Lyrics from $sourceText",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.60f),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 2. MAIN LYRICS CANVAS (No dark masking boxes; clean ambient background)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                            Text(text = "Syncing Lyrics...", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                syncedLines.isNotEmpty() -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp, start = 8.dp, end = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(items = syncedLines, key = { idx, item -> "$idx-${item.timeMs}" }) { index, line ->
                            val isCurrent = index == activeIndex
                            val textColor by animateColorAsState(
                                targetValue = if (isCurrent) Color.White else Color.White.copy(alpha = 0.28f),
                                animationSpec = tween(220),
                                label = "lyricColor"
                            )

                            Text(
                                text = line.text,
                                fontSize = if (isCurrent) 28.sp else 23.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                                color = textColor,
                                lineHeight = if (isCurrent) 38.sp else 32.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSeek(line.timeMs + currentOffsetMs) }
                                    .padding(vertical = 14.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                !plainLyrics.isNullOrBlank() -> {
                    val scrollState = rememberScrollState()
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(scrollState).padding(top = 16.dp, bottom = 32.dp)) {
                        Text(text = plainLyrics, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f), lineHeight = 28.sp, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
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
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { fetchLyrics(true) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Retry", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.18f),
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onCloseLyrics() }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                        Icon(imageVector = Icons.Default.Image, contentDescription = "Cover", tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Cover", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LyricsResyncBottomSheet(
    song: SongItem,
    manualOffsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lyricsResult = lyricsCache[song.id] ?: LyricsResult()
    var candidateIdx by remember(song.id) { mutableIntStateOf(lyricsResult.activeCandidateIndex) }
    var currentOffset by remember(manualOffsetMs) { mutableLongStateOf(manualOffsetMs) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1C24),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(vertical = 10.dp).width(38.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)))
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Lyrics Sync & Options", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = song.title, fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f), maxLines = 1)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "AUDIO SYNC OFFSET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
                val offsetSec = currentOffset / 1000f
                Text(text = if (currentOffset == 0L) "0.0s" else String.format("%+.1fs", offsetSec), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = if (currentOffset != 0L) Color(0xFFFFD54F) else Color.White)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.15f), modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).clickable { val n = currentOffset - 500L; currentOffset = n; songOffsetMap[song.id] = n; onOffsetChange(n) }) {
                        Box(contentAlignment = Alignment.Center) { Text("-0.5s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = if (currentOffset != 0L) Color.White else Color.White.copy(alpha = 0.25f), modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).clickable { currentOffset = 0L; songOffsetMap[song.id] = 0L; onOffsetChange(0L); Toast.makeText(context, "Offset Reset to 0.0s", Toast.LENGTH_SHORT).show() }) {
                        Box(contentAlignment = Alignment.Center) { Text("Reset", color = if (currentOffset != 0L) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.15f), modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).clickable { val n = currentOffset + 500L; currentOffset = n; songOffsetMap[song.id] = n; onOffsetChange(n) }) {
                        Box(contentAlignment = Alignment.Center) { Text("+0.5s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
            }

            if (lyricsResult.candidates.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "AVAILABLE VERSIONS (${lyricsResult.candidates.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
                    lyricsResult.candidates.forEachIndexed { idx, cand ->
                        val isSel = idx == candidateIdx
                        Surface(shape = RoundedCornerShape(12.dp), color = if (isSel) Color.White else Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { candidateIdx = idx; lyricsCache[song.id] = lyricsResult.copy(activeCandidateIndex = idx); Toast.makeText(context, "Selected Version ${idx + 1} (${cand.source})", Toast.LENGTH_SHORT).show() }) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(text = "Version ${idx + 1} • ${cand.source}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                                    Text(text = if (cand.syncedLines.isNotEmpty()) "${cand.syncedLines.size} synced lines" else "Plain text lyrics", fontSize = 11.5.sp, color = if (isSel) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f))
                                }
                                if (isSel) Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

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
