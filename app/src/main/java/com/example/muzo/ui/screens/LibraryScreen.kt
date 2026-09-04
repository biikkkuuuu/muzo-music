package com.example.muzo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.muzo.data.local.HistoryEntity
import com.example.muzo.data.local.LikedSongEntity
import com.example.muzo.data.local.UserPlaylistEntity
import com.example.muzo.data.local.UserPlaylistSongEntity
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem

fun LikedSongEntity.toSongItem(): SongItem = SongItem(
    id = videoId,
    title = title,
    artists = listOf(Artist(name = artist, id = null)),
    album = null,
    duration = 0,
    thumbnail = thumbnailUrl ?: ""
)

fun HistoryEntity.toSongItem(): SongItem = SongItem(
    id = videoId,
    title = title,
    artists = listOf(Artist(name = artist, id = null)),
    album = null,
    duration = 0,
    thumbnail = thumbnailUrl
)

fun UserPlaylistSongEntity.toSongItem(): SongItem = SongItem(
    id = videoId,
    title = title,
    artists = listOf(Artist(name = artist, id = null)),
    album = null,
    duration = 0,
    thumbnail = thumbnailUrl ?: ""
)

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    onSongPlay: (SongItem, List<SongItem>) -> Unit,
    onSettingsClick: () -> Unit,
    onSongActionClick: ((SongItem, List<SongItem>) -> Unit)? = null,
    onPlaylistActionClick: ((ShelfItem) -> Unit)? = null
) {
    val activeSubScreen by libraryViewModel.activeSubScreen.collectAsStateWithLifecycle()
    val likedSongs by libraryViewModel.likedSongs.collectAsStateWithLifecycle()
    val likedCount by libraryViewModel.likedCount.collectAsStateWithLifecycle()
    val historySongs by libraryViewModel.historySongs.collectAsStateWithLifecycle()
    val top50Songs by libraryViewModel.top50Songs.collectAsStateWithLifecycle()
    val userPlaylists by libraryViewModel.userPlaylists.collectAsStateWithLifecycle()
    val selectedUserPlaylist by libraryViewModel.selectedUserPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistSongs by libraryViewModel.selectedPlaylistSongs.collectAsStateWithLifecycle()
    val selectedChip by libraryViewModel.selectedChip.collectAsStateWithLifecycle()
    val sortAscending by libraryViewModel.sortAscending.collectAsStateWithLifecycle()
    val localSongs by libraryViewModel.localSongs.collectAsStateWithLifecycle()
    val isLoadingLocal by libraryViewModel.isLoadingLocal.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songForPlaylistDialog by remember { mutableStateOf<SongItem?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            libraryViewModel.loadLocalAudio(context)
        } else {
            Toast.makeText(context, "Storage permission required to view local audio", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080A))
    ) {
        AnimatedContent(
            targetState = activeSubScreen,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 2 } + fadeOut()
                } else {
                    slideInHorizontally { -it / 2 } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "LibraryNavigation"
        ) { subScreen ->
            if (subScreen != null) {
                when (subScreen) {
                    LibrarySubScreen.LIKED -> {
                        // Matches Screenshot 1: Liked Songs Detail
                        val songs = likedSongs.map { it.toSongItem() }
                        val coverUrl = songs.firstOrNull()?.thumbnail
                        PlaylistDetailLayout(
                            title = "Liked",
                            subtitle = "${songs.size} song${if (songs.size > 1) "s" else ""} • ${songs.size * 3}:45",
                            coverUrl = coverUrl,
                            aboutText = "Liked is a personalized collection featuring ${songs.size} song${if (songs.size > 1) "s" else ""}. Total listening time is approx ${songs.size * 3} mins. This playlist is automatically curated for your musical enjoyment.",
                            songs = songs,
                            sortText = "Date added",
                            onBack = { libraryViewModel.setSubScreen(null) },
                            onSongPlay = onSongPlay,
                            onSongOptionsClick = { song ->
                                if (onSongActionClick != null) onSongActionClick(song, songs) else songForPlaylistDialog = song
                            },
                            onPlaylistOptionsClick = {
                                onPlaylistActionClick?.invoke(
                                    ShelfItem(
                                        id = "liked",
                                        title = "Liked",
                                        subtitle = "${songs.size} songs",
                                        imageUrls = listOfNotNull(coverUrl),
                                        type = ItemType.PLAYLIST
                                    )
                                )
                            }
                        )
                    }

                    LibrarySubScreen.USER_PLAYLIST -> {
                        // Matches Screenshot 3: Custom Playlist Detail
                        val playlist = selectedUserPlaylist
                        val songs = selectedPlaylistSongs.map { it.toSongItem() }
                        val coverUrl = playlist?.coverUrl ?: songs.firstOrNull()?.thumbnail

                        PlaylistDetailLayout(
                            title = playlist?.name ?: "Playlist",
                            subtitle = "${songs.size} song${if (songs.size > 1) "s" else ""} • ${songs.size * 3}:07",
                            coverUrl = coverUrl,
                            isCustomPlaylist = true,
                            aboutText = "${playlist?.name ?: "This"} is a custom playlist featuring ${songs.size} song${if (songs.size > 1) "s" else ""}. Combined duration is ${songs.size * 3}:07.",
                            songs = songs,
                            sortText = "Custom order",
                            onBack = { libraryViewModel.setSubScreen(null) },
                            onSongPlay = onSongPlay,
                            onSongOptionsClick = { song ->
                                if (onSongActionClick != null) onSongActionClick(song, songs) else songForPlaylistDialog = song
                            },
                            onPlaylistOptionsClick = {
                                playlist?.let { pl ->
                                    onPlaylistActionClick?.invoke(
                                        ShelfItem(
                                            id = pl.id.toString(),
                                            title = pl.name,
                                            subtitle = "${songs.size} songs",
                                            imageUrls = listOfNotNull(coverUrl),
                                            type = ItemType.PLAYLIST
                                        )
                                    )
                                }
                            }
                        )
                    }

                    LibrarySubScreen.TOP_50 -> {
                        val songs = top50Songs.map { it.toSongItem() }
                        val coverUrl = songs.firstOrNull()?.thumbnail
                        PlaylistDetailLayout(
                            title = "My top 50",
                            subtitle = "Most played songs on Muzi • ${songs.size} tracks",
                            coverUrl = coverUrl,
                            aboutText = "My top 50 is dynamically calculated based on how often you listen to each track on Muzi. Updated continuously with every play.",
                            songs = songs,
                            sortText = "Most played",
                            onBack = { libraryViewModel.setSubScreen(null) },
                            onSongPlay = onSongPlay,
                            onSongOptionsClick = { song ->
                                if (onSongActionClick != null) onSongActionClick(song, songs) else songForPlaylistDialog = song
                            },
                            onPlaylistOptionsClick = {
                                onPlaylistActionClick?.invoke(
                                    ShelfItem(
                                        id = "top_50",
                                        title = "My top 50",
                                        subtitle = "${songs.size} songs",
                                        imageUrls = listOfNotNull(coverUrl),
                                        type = ItemType.PLAYLIST
                                    )
                                )
                            }
                        )
                    }

                    LibrarySubScreen.HISTORY -> {
                        val songs = historySongs.map { it.toSongItem() }
                        val coverUrl = songs.firstOrNull()?.thumbnail
                        PlaylistDetailLayout(
                            title = "History",
                            subtitle = "${songs.size} recently played songs",
                            coverUrl = coverUrl,
                            aboutText = "A continuous chronological timeline of your recent musical listening sessions on Muzi.",
                            songs = songs,
                            sortText = "Recent first",
                            onBack = { libraryViewModel.setSubScreen(null) },
                            onSongPlay = onSongPlay,
                            onSongOptionsClick = { song ->
                                if (onSongActionClick != null) onSongActionClick(song, songs) else songForPlaylistDialog = song
                            },
                            onPlaylistOptionsClick = {
                                onPlaylistActionClick?.invoke(
                                    ShelfItem(
                                        id = "history",
                                        title = "History",
                                        subtitle = "${songs.size} songs",
                                        imageUrls = listOfNotNull(coverUrl),
                                        type = ItemType.PLAYLIST
                                    )
                                )
                            }
                        )
                    }

                    LibrarySubScreen.LOCAL -> {
                        LaunchedEffect(Unit) {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                libraryViewModel.loadLocalAudio(context)
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        }

                        PlaylistDetailLayout(
                            title = "Local",
                            subtitle = if (isLoadingLocal) "Scanning device..." else "${localSongs.size} songs on device",
                            coverUrl = localSongs.firstOrNull()?.thumbnail,
                            aboutText = "Audio files located in your phone's internal storage and SD card.",
                            songs = localSongs,
                            sortText = "A to Z",
                            onBack = { libraryViewModel.setSubScreen(null) },
                            onSongPlay = onSongPlay,
                            onSongOptionsClick = { song -> songForPlaylistDialog = song },
                            onPlaylistOptionsClick = {
                                onPlaylistActionClick?.invoke(
                                    ShelfItem(
                                        id = "local",
                                        title = "Local",
                                        subtitle = "${localSongs.size} songs",
                                        imageUrls = listOfNotNull(localSongs.firstOrNull()?.thumbnail),
                                        type = ItemType.PLAYLIST
                                    )
                                )
                            }
                        )
                    }

                    LibrarySubScreen.DOWNLOADED -> {
                        LibraryPlaceholderDetail(
                            title = "Downloaded",
                            icon = Icons.Default.CheckCircle,
                            message = "Downloaded offline tracks will appear here.",
                            onBack = { libraryViewModel.setSubScreen(null) }
                        )
                    }

                    LibrarySubScreen.CACHED -> {
                        LibraryPlaceholderDetail(
                            title = "Cached",
                            icon = Icons.Default.Sync,
                            message = "ExoPlayer dynamic cache stores frequently played audio for zero-latency instant replay.",
                            onBack = { libraryViewModel.setSubScreen(null) }
                        )
                    }

                    LibrarySubScreen.EXPORTED -> {
                        LibraryPlaceholderDetail(
                            title = "Exported",
                            icon = Icons.Default.FileDownload,
                            message = "Exported playlist archives and audio files appear here.",
                            onBack = { libraryViewModel.setSubScreen(null) }
                        )
                    }
                }
            } else {
                // Matches Screenshot 2: Main Library Screen
                MainLibraryScreenContent(
                    selectedChip = selectedChip,
                    onChipSelect = { libraryViewModel.setSelectedChip(it) },
                    sortAscending = sortAscending,
                    onToggleSort = { libraryViewModel.toggleSortOrder() },
                    likedCount = likedCount,
                    userPlaylists = userPlaylists,
                    likedSongs = likedSongs,
                    historySongs = historySongs,
                    onTileClick = { sub -> libraryViewModel.setSubScreen(sub) },
                    onPlaylistCardClick = { playlist -> libraryViewModel.openUserPlaylist(playlist) },
                    onHistoryIconClick = { libraryViewModel.setSubScreen(LibrarySubScreen.HISTORY) },
                    onStatsIconClick = { libraryViewModel.setSubScreen(LibrarySubScreen.TOP_50) },
                    onSettingsClick = onSettingsClick,
                    onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                    onSongPlay = onSongPlay,
                    onPlaylistLongClick = { playlist ->
                        onPlaylistActionClick?.invoke(
                            ShelfItem(
                                id = playlist.id.toString(),
                                title = playlist.name,
                                subtitle = "${playlist.songCount} songs",
                                imageUrls = listOfNotNull(playlist.coverUrl),
                                type = ItemType.PLAYLIST
                            )
                        )
                    },
                    onSongActionClick = onSongActionClick
                )
            }
        }

        // Dialog: Create New Playlist
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreate = { name ->
                    libraryViewModel.createPlaylist(name)
                    showCreatePlaylistDialog = false
                    Toast.makeText(context, "Playlist '$name' created", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Dialog: Add to playlist
        if (songForPlaylistDialog != null) {
            val song = songForPlaylistDialog!!
            AddToPlaylistDialog(
                song = song,
                playlists = userPlaylists,
                onDismiss = { songForPlaylistDialog = null },
                onSelectPlaylist = { playlistId ->
                    libraryViewModel.addSongToPlaylist(playlistId, song)
                    songForPlaylistDialog = null
                    Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                },
                onCreateNewPlaylist = {
                    songForPlaylistDialog = null
                    showCreatePlaylistDialog = true
                }
            )
        }
    }
}

// -------------------------------------------------------------
// MAIN LIBRARY CONTENT (Matches Screenshot 2)
// -------------------------------------------------------------
@Composable
private fun MainLibraryScreenContent(
    selectedChip: String,
    onChipSelect: (String) -> Unit,
    sortAscending: Boolean,
    onToggleSort: () -> Unit,
    likedCount: Int,
    userPlaylists: List<UserPlaylistEntity>,
    likedSongs: List<LikedSongEntity>,
    historySongs: List<HistoryEntity>,
    onTileClick: (LibrarySubScreen) -> Unit,
    onPlaylistCardClick: (UserPlaylistEntity) -> Unit,
    onHistoryIconClick: () -> Unit,
    onStatsIconClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onSongPlay: (SongItem, List<SongItem>) -> Unit,
    onPlaylistLongClick: ((UserPlaylistEntity) -> Unit)? = null,
    onSongActionClick: ((SongItem, List<SongItem>) -> Unit)? = null
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onHistoryIconClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
                    }
                    IconButton(onClick = onStatsIconClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = "Stats", tint = Color.White)
                    }
                    IconButton(
                        onClick = { Toast.makeText(context, "Community", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = "Community", tint = Color.White)
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Filter Chips Row: Playlists, Songs, Albums, Artists
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Playlists", "Songs", "Albums", "Artists")) { chip ->
                    val isSelected = chip == selectedChip
                    Surface(
                        onClick = { onChipSelect(chip) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF282732) else Color(0xFF16151C)
                    ) {
                        Text(
                            text = chip,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isSelected) Color.White else Color(0xFFB0B0C0),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Sort Row: Date added + Caret
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16151C)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date added", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onToggleSort,
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16151C),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (sortAscending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Sort Direction",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Action Grid (2x3 tiles matching Screenshot 2)
        item {
            val tiles = listOf(
                GridTileData(
                    title = "Liked",
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFFFF4B6E),
                    subScreen = LibrarySubScreen.LIKED
                ),
                GridTileData(
                    title = "Downloaded",
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color.White,
                    subScreen = LibrarySubScreen.DOWNLOADED
                ),
                GridTileData(
                    title = "Exported",
                    icon = Icons.Default.FileDownload,
                    iconColor = Color.White,
                    subScreen = LibrarySubScreen.EXPORTED
                ),
                GridTileData(
                    title = "Cached",
                    icon = Icons.Default.Sync,
                    iconColor = Color.White,
                    subScreen = LibrarySubScreen.CACHED
                ),
                GridTileData(
                    title = "My top 50",
                    icon = Icons.Default.TrendingUp,
                    iconColor = Color.White,
                    subScreen = LibrarySubScreen.TOP_50
                ),
                GridTileData(
                    title = "Local",
                    icon = Icons.Default.Folder,
                    iconColor = Color.White,
                    subScreen = LibrarySubScreen.LOCAL
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in tiles.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LibraryCardTile(
                            data = tiles[i],
                            onClick = { onTileClick(tiles[i].subScreen) },
                            modifier = Modifier.weight(1f)
                        )
                        if (i + 1 < tiles.size) {
                            LibraryCardTile(
                                data = tiles[i + 1],
                                onClick = { onTileClick(tiles[i + 1].subScreen) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Section Below: Playlists / Songs / Albums / Artists
        when (selectedChip) {
            "Playlists" -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Playlists",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        IconButton(onClick = onCreatePlaylistClick) {
                            Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (userPlaylists.isEmpty()) {
                    item {
                        Surface(
                            onClick = onCreatePlaylistClick,
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF16151C),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF282732), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Create new playlist", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Add songs to listen anytime", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                } else {
                    // Playlist Cards: Matches Screenshot 2 (landscape image with title & song count below)
                    items(userPlaylists) { playlist ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .combinedClickable(
                                    onClick = { onPlaylistCardClick(playlist) },
                                    onLongClick = { onPlaylistLongClick?.invoke(playlist) }
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16151C))
                            ) {
                                if (!playlist.coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = playlist.coverUrl,
                                        contentDescription = playlist.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(listOf(Color(0xFF2F60FF), Color(0xFF1E294B)))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = playlist.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${playlist.songCount} song${if (playlist.songCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            "Songs" -> {
                item {
                    Text("Songs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val allSongs = (likedSongs.map { it.toSongItem() } + historySongs.map { it.toSongItem() }).distinctBy { it.id }
                items(allSongs) { song ->
                    PlaylistItemCard(
                        song = song,
                        onClick = { onSongPlay(song, allSongs) },
                        onOptionsClick = { onSongActionClick?.invoke(song, allSongs) }
                    )
                }
            }

            "Artists" -> {
                item {
                    Text("Artists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val artists = (likedSongs.map { it.artist } + historySongs.map { it.artist }).distinct()
                items(artists) { artistName ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF16151C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF282732)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(artistName, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }

            "Albums" -> {
                item {
                    Text("Albums", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Saved albums will appear here.", color = Color.Gray)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PLAYLIST DETAIL LAYOUT (Matches Screenshot 1 & 3)
// -------------------------------------------------------------
@Composable
private fun PlaylistDetailLayout(
    title: String,
    subtitle: String,
    coverUrl: String?,
    aboutText: String,
    songs: List<SongItem>,
    sortText: String,
    isCustomPlaylist: Boolean = false,
    onBack: () -> Unit,
    onSongPlay: (SongItem, List<SongItem>) -> Unit,
    onSongOptionsClick: (SongItem) -> Unit,
    onPlaylistOptionsClick: (() -> Unit)? = null
) {
    var isAboutExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Back button on left, Search & More icons on right
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Search, contentDescription = "Search in playlist", tint = Color.White)
                    }
                    if (onPlaylistOptionsClick != null) {
                        IconButton(onClick = onPlaylistOptionsClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Playlist options", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Centered Big Artwork (Cover image)
        item {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF16151C))
                    .shadow(12.dp, RoundedCornerShape(16.dp))
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(listOf(Color(0xFF2F60FF), Color(0xFF1E294B)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (title == "Liked") Icons.Default.Favorite else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (title == "Liked") Color(0xFFFF3366) else Color.White,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                // Edit pencil icon in bottom right (as seen in Screenshot 3)
                if (isCustomPlaylist) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Playlist", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Playlist Title (Centered, Bold)
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle: "1 song • 1:01:35"
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9292A2),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Action Buttons Row: Play, Shuffle, 3-dots
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button (Filled light pill)
                Button(
                    onClick = {
                        if (songs.isNotEmpty()) onSongPlay(songs.first(), songs)
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC7D2FE),
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Shuffle Button (Dark pill)
                Button(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.shuffled()
                            onSongPlay(shuffled.first(), shuffled)
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1D26),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3-dots Menu Button (Dark circle)
                Surface(
                    onClick = {},
                    shape = CircleShape,
                    color = Color(0xFF1E1D26),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // "About" Section (Matches Screenshot 1 & 3)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = aboutText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9292A2),
                    maxLines = if (isAboutExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    onClick = { isAboutExpanded = !isAboutExpanded },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1D26)
                ) {
                    Text(
                        text = if (isAboutExpanded) "Less" else "More",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sort & Order Row: [Date added v] [^] or [Custom order v] [Lock]
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF16151C)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sortText, style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF16151C),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (isCustomPlaylist) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF16151C),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked Order", tint = Color(0xFFC7D2FE), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Song Tracks List
        if (songs.isNotEmpty()) {
            items(songs) { song ->
                PlaylistItemCard(
                    song = song,
                    onClick = { onSongPlay(song, songs) },
                    onOptionsClick = { onSongOptionsClick(song) }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks in this playlist yet.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PLAYLIST ITEM CARD (Matches Track Card in Screenshot 1 & 3)
// -------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistItemCard(
    song: SongItem,
    onClick: () -> Unit,
    onOptionsClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16151C),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onOptionsClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with Play overlay on bottom left
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = song.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(3.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Subtitle (Heart + artist name + duration)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Liked",
                        tint = Color(0xFFFF3366),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = song.artists.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9292A2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3-dots Menu Button
            IconButton(onClick = { onOptionsClick?.invoke() }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Song options",
                    tint = Color(0xFF9292A2),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// QUICK ACTION GRID TILE
// -------------------------------------------------------------
private data class GridTileData(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val subScreen: LibrarySubScreen
)

@Composable
private fun LibraryCardTile(
    data: GridTileData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF16151C)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = data.title,
                tint = data.iconColor,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = data.title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LibraryPlaceholderDetail(
    title: String,
    icon: ImageVector,
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16151C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2F60FF), modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1D26)
    )
}

@Composable
private fun AddToPlaylistDialog(
    song: SongItem,
    playlists: List<UserPlaylistEntity>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9292A2),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (playlists.isEmpty()) {
                    Text("No playlists yet. Create your first playlist!", color = Color.Gray, fontSize = 14.sp)
                } else {
                    playlists.forEach { playlist ->
                        Surface(
                            onClick = { onSelectPlaylist(playlist.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF16151C),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(playlist.name, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCreateNewPlaylist) {
                Text("+ New playlist")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1D26)
    )
}