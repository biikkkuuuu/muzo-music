package com.example.muzo.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.data.SpeedDialManager
import com.example.muzo.data.local.LikedSongDao
import com.example.muzo.data.local.LikedSongEntity
import com.example.muzo.data.local.UserPlaylistDao
import com.example.muzo.data.local.UserPlaylistEntity
import com.example.muzo.data.local.UserPlaylistSongEntity
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.data.download.SongDownloadManager
import com.example.muzo.playback.PlayerViewModel
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ActionMenuTarget {
    data class Song(val song: SongItem, val queue: List<SongItem> = emptyList()) : ActionMenuTarget
    data class Playlist(val shelfItem: ShelfItem, val songs: List<SongItem> = emptyList()) : ActionMenuTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionBottomSheet(
    target: ActionMenuTarget,
    playerViewModel: PlayerViewModel,
    likedSongDao: LikedSongDao,
    userPlaylistDao: UserPlaylistDao,
    onDismiss: () -> Unit,
    onOpenPlaylistDetail: ((ShelfItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Determine basic metadata
    val id = when (target) {
        is ActionMenuTarget.Song -> target.song.id
        is ActionMenuTarget.Playlist -> target.shelfItem.id
    }
    val title = when (target) {
        is ActionMenuTarget.Song -> target.song.title
        is ActionMenuTarget.Playlist -> target.shelfItem.title
    }
    val subtitle = when (target) {
        is ActionMenuTarget.Song -> target.song.artists.joinToString(", ") { it.name }.ifBlank { "Song" }
        is ActionMenuTarget.Playlist -> target.shelfItem.subtitle ?: "Playlist"
    }
    val rawThumb = when (target) {
        is ActionMenuTarget.Song -> target.song.thumbnail
        is ActionMenuTarget.Playlist -> target.shelfItem.imageUrls.firstOrNull()
    }
    val thumbnailUrl = getHighResThumbnail(rawThumb)
    val isSong = target is ActionMenuTarget.Song

    // Liked state
    var isLiked by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (isSong) {
            withContext(Dispatchers.IO) {
                isLiked = likedSongDao.isLiked(id)
            }
        }
    }

    // Pinned to Speed dial state
    var isPinned by remember { mutableStateOf(SpeedDialManager.isPinned(context, id)) }

    // Add to playlist sub-dialog
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16151B),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            // 1. Song Info Header Card (Matches screenshot exactly)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF222129)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E9EA8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isSong) {
                                val song = (target as ActionMenuTarget.Song).song
                                val newLiked = !isLiked
                                isLiked = newLiked
                                scope.launch(Dispatchers.IO) {
                                    if (newLiked) {
                                        likedSongDao.insert(
                                            LikedSongEntity(
                                                videoId = song.id,
                                                title = song.title,
                                                artist = subtitle,
                                                thumbnailUrl = song.thumbnail,
                                                timestamp = System.currentTimeMillis()
                                            )
                                        )
                                    } else {
                                        likedSongDao.delete(song.id)
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            if (newLiked) "Added to Liked Songs ❤️" else "Removed from Liked Songs",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Saved playlist", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isLiked) Color(0xFFFF3B30) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            fun executePlaylistAction(shelfItem: ShelfItem, existingSongs: List<SongItem>, actionName: String, block: (List<SongItem>) -> Unit) {
                if (existingSongs.isNotEmpty()) {
                    block(existingSongs)
                } else {
                    Toast.makeText(context, "Loading ${shelfItem.title}...", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        val fetchedSongs = withContext(Dispatchers.IO) {
                            try {
                                val albumRes = YouTube.album(shelfItem.id).getOrNull()
                                if (albumRes != null && albumRes.songs.isNotEmpty()) {
                                    albumRes.songs
                                } else {
                                    val playlistRes = YouTube.playlist(shelfItem.id).getOrNull()
                                    if (playlistRes != null && playlistRes.songs.isNotEmpty()) {
                                        playlistRes.songs
                                    } else {
                                        val artistRes = YouTube.artist(shelfItem.id).getOrNull()
                                        val artistSongs = artistRes?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>()
                                        if (!artistSongs.isNullOrEmpty()) {
                                            artistSongs
                                        } else {
                                            YouTube.search("${shelfItem.title} songs", YouTube.SearchFilter.FILTER_SONG)
                                                .getOrNull()?.items?.filterIsInstance<SongItem>() ?: emptyList()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                        if (fetchedSongs.isNotEmpty()) {
                            block(fetchedSongs)
                        } else {
                            Toast.makeText(context, "Unable to load songs for ${shelfItem.title}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // 2. Three Quick Action Pills: [ > Play ] [ 🔀 Shuffle ] [ 📶 Start Radio ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickPillButton(
                    icon = Icons.Default.PlayArrow,
                    text = "Play",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        when (target) {
                            is ActionMenuTarget.Song -> {
                                val queue = if (target.queue.isNotEmpty()) target.queue else listOf(target.song)
                                val idx = queue.indexOfFirst { it.id == target.song.id }.coerceAtLeast(0)
                                playerViewModel.playTrack(idx, queue)
                            }
                            is ActionMenuTarget.Playlist -> {
                                executePlaylistAction(target.shelfItem, target.songs, "Play") { songs ->
                                    playerViewModel.playTrack(0, songs)
                                }
                            }
                        }
                    }
                )

                QuickPillButton(
                    icon = Icons.Default.Shuffle,
                    text = "Shuffle",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        when (target) {
                            is ActionMenuTarget.Song -> {
                                val list = if (target.queue.isNotEmpty()) target.queue else listOf(target.song)
                                playerViewModel.playShuffled(list)
                            }
                            is ActionMenuTarget.Playlist -> {
                                executePlaylistAction(target.shelfItem, target.songs, "Shuffle") { songs ->
                                    playerViewModel.playShuffled(songs)
                                }
                            }
                        }
                    }
                )

                QuickPillButton(
                    icon = Icons.Default.CellTower,
                    text = "Start Radio",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        when (target) {
                            is ActionMenuTarget.Song -> {
                                playerViewModel.startRadio(target.song)
                            }
                            is ActionMenuTarget.Playlist -> {
                                executePlaylistAction(target.shelfItem, target.songs, "Start Radio") { songs ->
                                    playerViewModel.startRadio(songs.first())
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Action Items List (Matching exact cards in screenshot)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play next
                ActionCardItem(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Play next",
                    subtitle = "Add to the top of your queue",
                    onClick = {
                        onDismiss()
                        when (target) {
                            is ActionMenuTarget.Song -> playerViewModel.addToQueueNext(target.song)
                            is ActionMenuTarget.Playlist -> {
                                executePlaylistAction(target.shelfItem, target.songs, "Play next") { songs ->
                                    playerViewModel.addToQueueNext(songs)
                                    Toast.makeText(context, "Added ${songs.size} tracks to play next", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )

                // Add to queue
                ActionCardItem(
                    icon = Icons.Default.PlaylistAdd,
                    title = "Add to queue",
                    subtitle = "Add to the bottom of your queue",
                    onClick = {
                        onDismiss()
                        when (target) {
                            is ActionMenuTarget.Song -> playerViewModel.addToQueueEnd(target.song)
                            is ActionMenuTarget.Playlist -> {
                                executePlaylistAction(target.shelfItem, target.songs, "Add to queue") { songs ->
                                    playerViewModel.addToQueueEnd(songs)
                                    Toast.makeText(context, "Added ${songs.size} tracks to queue", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )

                // Download Song (Offline playback)
                if (isSong) {
                    val song = (target as ActionMenuTarget.Song).song
                    val downloadManager = remember { SongDownloadManager.getInstance(context.applicationContext) }
                    val downloadedIds by downloadManager.downloadedVideoIds.collectAsState()
                    val isDownloaded = downloadedIds.contains(song.id)
                    ActionCardItem(
                        icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                        title = if (isDownloaded) "Remove download" else "Download song",
                        subtitle = if (isDownloaded) "Delete offline copy from storage" else "Download for offline listening",
                        onClick = {
                            onDismiss()
                            if (isDownloaded) {
                                downloadManager.removeDownload(song.id, song.title)
                            } else {
                                downloadManager.downloadSong(song)
                            }
                        }
                    )
                }

                // Add to playlist (for Song)
                if (isSong) {
                    ActionCardItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        title = "Add to playlist",
                        subtitle = "Add to one of your playlists",
                        onClick = {
                            showAddToPlaylistDialog = true
                        }
                    )
                }

                // Pin to Speed dial
                ActionCardItem(
                    icon = if (isPinned) Icons.Default.Check else Icons.Default.Add,
                    title = if (isPinned) "Unpin from Speed dial" else "Pin to Speed dial",
                    subtitle = null,
                    onClick = {
                        val itemType = if (isSong) "SONG" else "PLAYLIST"
                        val nowPinned = SpeedDialManager.togglePin(
                            context = context,
                            id = id,
                            title = title,
                            subtitle = subtitle,
                            thumbnailUrl = thumbnailUrl,
                            type = itemType
                        )
                        isPinned = nowPinned
                        Toast.makeText(
                            context,
                            if (nowPinned) "Pinned to Speed dial 📌" else "Unpinned from Speed dial",
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    }
                )

                // Share
                ActionCardItem(
                    icon = Icons.Default.Share,
                    title = "Share",
                    subtitle = "Share a link to this item",
                    onClick = {
                        onDismiss()
                        val shareUrl = if (isSong) {
                            "https://music.youtube.com/watch?v=$id"
                        } else {
                            "https://music.youtube.com/playlist?list=$id"
                        }
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, title)
                            putExtra(Intent.EXTRA_TEXT, "$title - $shareUrl")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share with"))
                    }
                )
            }
        }
    }

    // Sub-dialog: Add to Playlist
    if (showAddToPlaylistDialog && isSong) {
        val song = (target as ActionMenuTarget.Song).song
        AddToPlaylistDialog(
            song = song,
            userPlaylistDao = userPlaylistDao,
            onDismiss = { showAddToPlaylistDialog = false },
            onSongAdded = {
                showAddToPlaylistDialog = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun QuickPillButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF26252E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionCardItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF222129)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9EA8),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    song: SongItem,
    userPlaylistDao: UserPlaylistDao,
    onDismiss: () -> Unit,
    onSongAdded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlists by userPlaylistDao.getAllPlaylists().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1D26),
        title = {
            Text(
                text = "Add to playlist",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                // Button to create new playlist
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { showCreateDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New playlist",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (playlists.isEmpty()) {
                    Text(
                        text = "No custom playlists yet. Create one above!",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch(Dispatchers.IO) {
                                            val artistName = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown" }
                                            userPlaylistDao.addSongToPlaylist(
                                                UserPlaylistSongEntity(
                                                    playlistId = pl.id,
                                                    videoId = song.id,
                                                    title = song.title,
                                                    artist = artistName,
                                                    thumbnailUrl = song.thumbnail
                                                )
                                            )
                                            userPlaylistDao.updatePlaylistMetadata(pl.id)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Added to ${pl.name} 🎶", Toast.LENGTH_SHORT).show()
                                                onSongAdded()
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF262530)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = pl.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${pl.songCount} songs",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF1E1D26),
            title = { Text("Create Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                val newId = userPlaylistDao.insertPlaylist(
                                    UserPlaylistEntity(name = newPlaylistName.trim())
                                )
                                val artistName = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown" }
                                userPlaylistDao.addSongToPlaylist(
                                    UserPlaylistSongEntity(
                                        playlistId = newId,
                                        videoId = song.id,
                                        title = song.title,
                                        artist = artistName,
                                        thumbnailUrl = song.thumbnail
                                    )
                                )
                                userPlaylistDao.updatePlaylistMetadata(newId)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Created & added to ${newPlaylistName.trim()}", Toast.LENGTH_SHORT).show()
                                    showCreateDialog = false
                                    onSongAdded()
                                }
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
