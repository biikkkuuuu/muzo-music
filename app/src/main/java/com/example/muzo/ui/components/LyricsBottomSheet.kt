package com.example.muzo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(val timeMs: Long, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(
    song: SongItem,
    currentPosition: Long,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember(song.id) { mutableStateOf(true) }
    var syncedLines by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var plainLyrics by remember(song.id) { mutableStateOf<String?>(null) }
    var lyricsSource by remember(song.id) { mutableStateOf("Fetching...") }
    var hasError by remember(song.id) { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Fetch lyrics function
    val loadLyrics: () -> Unit = {
        isLoading = true
        hasError = false
        scope.launch {
            withContext(Dispatchers.IO) {
                var foundSynced: List<LyricLine>? = null
                var foundPlain: String? = null
                var source = "None"

                // 1. First try LRCLIB for synchronized time-stamped lyrics
                try {
                    val encodedTitle = URLEncoder.encode(song.title, "UTF-8")
                    val artistName = song.artists.firstOrNull()?.name ?: ""
                    val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
                    val durSec = song.duration ?: 0
                    val urlStr = "https://lrclib.net/api/get?track_name=$encodedTitle&artist_name=$encodedArtist&duration=$durSec"
                    val url = URL(urlStr)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        setRequestProperty("User-Agent", "MuziMusic/1.0")
                    }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        val synced = json.optString("syncedLyrics", "")
                        val plain = json.optString("plainLyrics", "")

                        if (synced.isNotBlank()) {
                            val parsed = parseLrc(synced)
                            if (parsed.isNotEmpty()) {
                                foundSynced = parsed
                                source = "LRCLIB (Synchronized)"
                            }
                        }
                        if (foundSynced == null && plain.isNotBlank()) {
                            foundPlain = plain
                            source = "LRCLIB (Plain)"
                        }
                    }
                } catch (_: Exception) {}

                // 2. If not found, try YouTube Music lyrics endpoint
                if (foundSynced == null && foundPlain == null) {
                    try {
                        val nextRes = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()
                        val endpoint = nextRes?.lyricsEndpoint
                        if (endpoint != null) {
                            val ytLyrics = YouTube.lyrics(endpoint).getOrNull()
                            if (!ytLyrics.isNullOrBlank()) {
                                val parsed = parseLrc(ytLyrics)
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

                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (foundSynced != null) {
                        syncedLines = foundSynced
                        lyricsSource = source
                    } else if (foundPlain != null) {
                        plainLyrics = foundPlain
                        lyricsSource = source
                    } else {
                        hasError = true
                        lyricsSource = "Unavailable"
                    }
                }
            }
        }
    }

    LaunchedEffect(song.id) {
        loadLyrics()
    }

    // Active line index for synchronized lyrics
    val activeIndex = remember(currentPosition, syncedLines) {
        if (syncedLines.isEmpty()) -1
        else {
            val idx = syncedLines.indexOfLast { it.timeMs <= currentPosition }
            idx.coerceAtLeast(0)
        }
    }

    // Auto-scroll to keep active line centered
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && syncedLines.isNotEmpty() && !listState.isScrollInProgress) {
            val target = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14131A),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header: Title, Artist, and Copy Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF263352)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = Color(0xFF6B9DFE),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = song.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = lyricsSource,
                            fontSize = 12.sp,
                            color = Color(0xFF6B9DFE)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        val textToCopy = if (syncedLines.isNotEmpty()) {
                            syncedLines.joinToString("\n") { it.text }
                        } else {
                            plainLyrics ?: ""
                        }
                        if (textToCopy.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Lyrics", textToCopy))
                            Toast.makeText(context, "Lyrics copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF6B9DFE),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Loading synchronized lyrics...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                syncedLines.isNotEmpty() -> {
                    // Synchronized Lyrics View
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 24.dp)
                    ) {
                        itemsIndexed(syncedLines) { index, line ->
                            val isActive = index == activeIndex
                            val textColor by animateColorAsState(
                                targetValue = if (isActive) Color(0xFF6B9DFE) else Color.White.copy(alpha = 0.38f),
                                animationSpec = tween(300),
                                label = "lyricTextColor"
                            )
                            val fontSize = if (isActive) 22.sp else 18.sp
                            val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                            Text(
                                text = line.text,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                color = textColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSeek(line.timeMs) }
                                    .padding(vertical = 4.dp),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                plainLyrics != null -> {
                    // Plain Lyrics View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = plainLyrics!!,
                            fontSize = 17.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                hasError -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No lyrics found for this song",
                                color = Color.Gray,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedButton(
                                onClick = loadLyrics,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B9DFE)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseLrc(lrcContent: String): List<LyricLine> {
    val lines = mutableListOf<LyricLine>()
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
                lines.add(LyricLine(timeMs, text))
            }
        }
    }
    return lines.sortedBy { it.timeMs }
}
