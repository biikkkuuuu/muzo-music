package com.example.muzo.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import com.example.muzo.data.local.MuziDatabase
import com.example.muzo.data.local.UserPlaylistEntity
import com.example.muzo.data.local.UserPlaylistSongEntity
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

data class ParsedSpotifyTrack(
    val title: String,
    val artist: String,
    var matchedSong: SongItem? = null,
    var isMatching: Boolean = false
)

data class SpotifyPlaylistInfo(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val tracks: List<ParsedSpotifyTrack>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyImportScreen(
    onBack: () -> Unit,
    onPlaylistImported: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { MuziDatabase.getInstance(context) }

    var playlistUrl by remember { mutableStateOf("") }
    var isLoadingMetadata by remember { mutableStateOf(false) }
    var isImportingTracks by remember { mutableStateOf(false) }
    var playlistInfo by remember { mutableStateOf<SpotifyPlaylistInfo?>(null) }
    var currentImportIndex by remember { mutableIntStateOf(0) }
    var totalImportCount by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var importedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    fun pasteFromClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString().orEmpty().trim()
                if (text.isNotBlank()) {
                    playlistUrl = text
                }
            }
        } catch (_: Exception) {}
    }

    fun extractPlaylist(inputUrl: String) {
        if (inputUrl.isBlank()) {
            Toast.makeText(context, "Please paste a valid Spotify playlist link", Toast.LENGTH_SHORT).show()
            return
        }

        isLoadingMetadata = true
        statusMessage = "Fetching playlist details from Spotify..."

        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Extract playlist ID from URL
                val regex = Pattern.compile("playlist[/:]([a-zA-Z0-9]+)")
                val matcher = regex.matcher(inputUrl)
                val playlistId = if (matcher.find()) matcher.group(1) else ""

                if (playlistId.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        isLoadingMetadata = false
                        statusMessage = "Could not find a valid playlist ID in link"
                        Toast.makeText(context, "Invalid Spotify URL", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 1. Fetch oEmbed metadata
                var title = "Spotify Playlist"
                var author = "Spotify User"
                var cover = ""

                try {
                    val oembedUrl = "https://open.spotify.com/oembed?url=" + URLEncoder.encode("https://open.spotify.com/playlist/$playlistId", "UTF-8")
                    val conn = URL(oembedUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    if (conn.responseCode == 200) {
                        val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(jsonStr)
                        title = json.optString("title", title)
                        author = json.optString("author_name", author)
                        cover = json.optString("thumbnail_url", cover)
                    }
                } catch (_: Exception) {}

                // 2. Fetch tracks via Spotify Embed page HTML
                val tracks = mutableListOf<ParsedSpotifyTrack>()
                try {
                    val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
                    val conn = URL(embedUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    if (conn.responseCode == 200) {
                        val html = conn.inputStream.bufferedReader().use { it.readText() }
                        // Look for __NEXT_DATA__ JSON script tag
                        val nextDataRegex = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>")
                        val ndMatcher = nextDataRegex.matcher(html)
                        if (ndMatcher.find()) {
                            val nextJson = JSONObject(ndMatcher.group(1) ?: "{}")
                            val entityData = nextJson.optJSONObject("props")
                                ?.optJSONObject("pageProps")
                                ?.optJSONObject("state")
                                ?.optJSONObject("data")
                                ?.optJSONObject("entity")

                            if (entityData != null) {
                                title = entityData.optString("name", title)
                                val trackList = entityData.optJSONObject("trackList")?.optJSONArray("items")
                                    ?: entityData.optJSONArray("trackList")
                                if (trackList != null) {
                                    for (i in 0 until trackList.length()) {
                                        val item = trackList.optJSONObject(i)
                                        val tTitle = item?.optString("title") ?: item?.optString("name").orEmpty()
                                        val tArtist = item?.optString("subtitle") ?: item?.optString("artist").orEmpty()
                                        if (tTitle.isNotBlank()) {
                                            tracks.add(ParsedSpotifyTrack(tTitle, tArtist))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                // Fallback: If empty, add placeholder tracks for preview
                if (tracks.isEmpty()) {
                    tracks.add(ParsedSpotifyTrack("Featured Hits", author))
                }

                withContext(Dispatchers.Main) {
                    isLoadingMetadata = false
                    statusMessage = null
                    playlistInfo = SpotifyPlaylistInfo(
                        id = playlistId,
                        title = title,
                        author = author,
                        coverUrl = cover,
                        tracks = tracks
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoadingMetadata = false
                    statusMessage = "Failed to parse: ${e.localizedMessage}"
                    Toast.makeText(context, "Error reading playlist", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startImportAndMatch() {
        val info = playlistInfo ?: return
        if (isImportingTracks) return

        isImportingTracks = true
        totalImportCount = info.tracks.size
        currentImportIndex = 0

        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Create local playlist in Room
                val newPlaylist = UserPlaylistEntity(
                    name = info.title,
                    coverUrl = info.coverUrl.ifBlank { null }
                )
                val playlistId = db.userPlaylistDao().insertPlaylist(newPlaylist)

                for ((idx, track) in info.tracks.withIndex()) {
                    withContext(Dispatchers.Main) {
                        currentImportIndex = idx + 1
                        track.isMatching = true
                    }

                    try {
                        val searchQuery = "${track.title} ${track.artist}".trim()
                        val searchResult = YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                        val matchedSong = searchResult?.items?.filterIsInstance<SongItem>()?.firstOrNull()

                        if (matchedSong != null) {
                            track.matchedSong = matchedSong
                            // Insert song into playlist
                            db.userPlaylistDao().addSongToPlaylist(
                                UserPlaylistSongEntity(
                                    playlistId = playlistId,
                                    videoId = matchedSong.id,
                                    title = matchedSong.title,
                                    artist = matchedSong.artists.joinToString { it.name },
                                    thumbnailUrl = matchedSong.thumbnail,
                                    durationText = ""
                                )
                            )
                        }
                    } catch (_: Exception) {}

                    withContext(Dispatchers.Main) {
                        track.isMatching = false
                    }
                }

                // Update metadata count & cover
                db.userPlaylistDao().updatePlaylistMetadata(playlistId)

                withContext(Dispatchers.Main) {
                    isImportingTracks = false
                    importedPlaylistId = playlistId
                    showSuccessDialog = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isImportingTracks = false
                    Toast.makeText(context, "Import stopped: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Spotify Importer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0E17)
                )
            )
        },
        containerColor = Color(0xFF0F0E17)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
        ) {
            // Hero Intro Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1DB954)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Import Spotify Playlists",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Convert any public Spotify playlist URL into your local Muzi library.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Link Input & Actions Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = playlistUrl,
                            onValueChange = { playlistUrl = it },
                            placeholder = {
                                Text(
                                    "https://open.spotify.com/playlist/...",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = Color(0xFF1DB954)
                                )
                            },
                            trailingIcon = {
                                if (playlistUrl.isNotBlank()) {
                                    IconButton(onClick = { playlistUrl = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1DB954),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pasteFromClipboard() },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste", color = Color.White, fontSize = 14.sp)
                            }

                            Button(
                                onClick = { extractPlaylist(playlistUrl) },
                                enabled = !isLoadingMetadata && !isImportingTracks,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1DB954),
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                if (isLoadingMetadata) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reading...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fetch Playlist", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (statusMessage != null) {
                            Text(
                                text = statusMessage!!,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Playlist Preview & Import Trigger
            playlistInfo?.let { info ->
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF1E2638),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color(0xFF4A80F0).copy(alpha = 0.35f)), RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AsyncImage(
                                    model = info.coverUrl.ifBlank { null },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = info.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Curator: ${info.author}",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${info.tracks.size} tracks found",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                            }

                            if (isImportingTracks) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Matching tracks with YouTube...",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "$currentImportIndex / $totalImportCount",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6B9DFE)
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = {
                                            if (totalImportCount > 0) currentImportIndex.toFloat() / totalImportCount.toFloat() else 0f
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = Color(0xFF1DB954),
                                        trackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { startImportAndMatch() },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1DB954),
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DownloadDone,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Import ${info.tracks.size} Songs into Muzi",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Extracted Tracks Header
                item {
                    Text(
                        text = "Tracks in Playlist (${info.tracks.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                    )
                }

                // Track rows
                itemsIndexed(info.tracks) { index, track ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.width(24.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist.ifBlank { "Spotify Track" },
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.55f),
                                    maxLines = 1
                                )
                            }

                            when {
                                track.isMatching -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF1DB954)
                                    )
                                }
                                track.matchedSong != null -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Matched",
                                        tint = Color(0xFF1DB954),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(26.dp)
                    )
                    Text("Import Complete!", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    "Your playlist has been saved to your local library! You can now play and download songs anytime.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        importedPlaylistId?.let { onPlaylistImported(it) } ?: onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954), contentColor = Color.Black)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1D2B),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
