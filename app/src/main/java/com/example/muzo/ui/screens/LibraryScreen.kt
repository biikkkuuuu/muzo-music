package com.example.muzo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.data.local.DownloadedSongEntity
import com.example.muzo.data.local.HistoryEntity
import com.example.muzo.data.local.LikedSongEntity
import com.example.muzo.data.local.UserPlaylistEntity
import com.example.muzo.data.local.UserPlaylistSongEntity
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.theme.MuziThemeTokens
import com.example.muzo.ui.components.AnimatedChipsRow
import com.example.muzo.ui.components.MuziSongRow
import com.example.muzo.ui.components.NavigationTitle
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

fun DownloadedSongEntity.toSongItem(): SongItem = SongItem(
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
    val downloadedSongs by libraryViewModel.downloadedSongs.collectAsStateWithLifecycle()

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
            .background(MaterialTheme.colorScheme.background)
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
                        val songs = likedSongs.map { it.toSongItem() }
                        val coverUrl = songs.firstOrNull()?.thumbnail
                        PlaylistDetailLayout(
                            title = "Liked Songs",
                            subtitle = "${songs.size} song${if (songs.size > 1) "s" else ""}",
                            coverUrl = coverUrl,
                            aboutText = "Liked is a personalized collection featuring ${songs.size} song${if (songs.size > 1) "s" else ""}. Automatically curated for your musical enjoyment.",
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
                                        title = "Liked Songs",
                                        subtitle = "${songs.size} songs",
                                        imageUrls = listOfNotNull(coverUrl),
                                        type = ItemType.PLAYLIST
                                    )
                                )
                            }
                        )
                    }

                    LibrarySubScreen.USER_PLAYLIST -> {
                        val playlist = selectedUserPlaylist
                        if (playlist != null) {
                            val songs = selectedPlaylistSongs.map { it.toSongItem() }
                            val coverUrl = playlist.coverUrl ?: songs.firstOrNull()?.thumbnail
                            PlaylistDetailLayout(
                                title = playlist.name,
                                subtitle = "${songs.size} song${if (songs.size > 1) "s" else ""}",
                                coverUrl = coverUrl,
                                aboutText = "Custom playlist created on ${java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(playlist.createdAt))}.",
                                songs = songs,
                                sortText = "Custom order",
                                isCustomPlaylist = true,
                                onBack = { libraryViewModel.setSubScreen(null) },
                                onSongPlay = onSongPlay,
                                onSongOptionsClick = { song ->
                                    if (onSongActionClick != null) onSongActionClick(song, songs) else songForPlaylistDialog = song
                                },
                                onPlaylistOptionsClick = {
                                    onPlaylistActionClick?.invoke(
                                        ShelfItem(
                                            id = playlist.id.toString(),
                                            title = playlist.name,
                                            subtitle = "${songs.size} songs",
                                            imageUrls = listOfNotNull(coverUrl),
                                            type = ItemType.PLAYLIST
                                        )
                                    )
                                }
                            )
                        }
                    }

                    LibrarySubScreen.HISTORY -> {
                        val songs = historySongs.map { it.toSongItem() }
                        val coverUrl = songs.firstOrNull()?.thumbnail
                        PlaylistDetailLayout(
                            title = "History",
                            subtitle = "${songs.size} recently played",
                            coverUrl = coverUrl,
                            aboutText = "Your recent listening history. Tracks are automatically saved as you play them.",
                            songs = songs,
                            sortText = "Most recent",
                            onBack = { libraryViewModel.setSubScreen(null) },
                            onSongPlay = onSongPlay,
                            onSongOptionsClick = { song ->
                                if (onSongActionClick != null) onSongActionClick(song, songs) else songForPlaylistDialog = song
                            }
                        )
                    }

                    LibrarySubScreen.TOP_50 -> {
                        val songs = top50Songs.map { it.toSongItem() }
                        val coverUrl = songs.firstOrNull()?.thumbnail
                        PlaylistDetailLayout(
                            title = "My Top 50",
                            subtitle = "${songs.size} top played tracks",
                            coverUrl = coverUrl,
                            aboutText = "Your most played tracks calculated from your playback activity.",
                            songs = songs,
                            sortText = "Play count",
                            onBack = { libraryViewModel.setSubScreen(null) },
                            onSongPlay = onSongPlay,
                            onSongOptionsClick = { song ->
                                if (onSongActionClick != null) onSongActionClick(song, songs) else songForPlaylistDialog = song
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

                        if (localSongs.isEmpty() && !isLoadingLocal) {
                            LibraryPlaceholderDetail(
                                title = "Local Audio",
                                icon = Icons.Default.Folder,
                                message = "No local audio files found on device storage.",
                                onBack = { libraryViewModel.setSubScreen(null) }
                            )
                        } else {
                            val coverUrl = localSongs.firstOrNull()?.thumbnail
                            PlaylistDetailLayout(
                                title = "Local Audio",
                                subtitle = "${localSongs.size} song${if (localSongs.size > 1) "s" else ""}",
                                coverUrl = coverUrl,
                                aboutText = "Tracks discovered on device internal storage.",
                                songs = localSongs,
                                sortText = "File name",
                                onBack = { libraryViewModel.setSubScreen(null) },
                                onSongPlay = onSongPlay,
                                onSongOptionsClick = { song ->
                                    if (onSongActionClick != null) onSongActionClick(song, localSongs) else songForPlaylistDialog = song
                                }
                            )
                        }
                    }

                    LibrarySubScreen.DOWNLOADED -> {
                        val songs = downloadedSongs.map { it.toSongItem() }
                        val coverUrl = songs.firstOrNull()?.thumbnail
                        if (songs.isEmpty()) {
                            LibraryPlaceholderDetail(
                                title = "Downloaded",
                                icon = Icons.Default.CheckCircle,
                                message = "No downloaded tracks yet.\nTap the download button on any song to listen offline.",
                                onBack = { libraryViewModel.setSubScreen(null) }
                            )
                        } else {
                            PlaylistDetailLayout(
                                title = "Downloaded",
                                subtitle = "${songs.size} song${if (songs.size > 1) "s" else ""} • Offline Available",
                                coverUrl = coverUrl,
                                aboutText = "Downloaded tracks are saved to device storage and can be played offline with zero data usage.",
                                songs = songs,
                                sortText = "Date downloaded",
                                onBack = { libraryViewModel.setSubScreen(null) },
                                onSongPlay = onSongPlay,
                                onSongOptionsClick = { song ->
                                    if (onSongActionClick != null) onSongActionClick(song, songs) else songForPlaylistDialog = song
                                }
                            )
                        }
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
                // Main ViVi Library Screen
                MainLibraryScreenContent(
                    selectedChip = selectedChip,
                    onChipSelect = { libraryViewModel.setSelectedChip(it) },
                    sortAscending = sortAscending,
                    onToggleSort = { libraryViewModel.toggleSortOrder() },
                    likedCount = likedCount,
                    userPlaylists = userPlaylists,
                    likedSongs = likedSongs,
                    historySongs = historySongs,
                    downloadedCount = downloadedSongs.size,
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
// MAIN LIBRARY CONTENT (ViVi Style)
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
    downloadedCount: Int,
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
    val filterOptions = remember { listOf("Playlists", "Songs", "Albums", "Artists", "Downloaded") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp)
    ) {
        // 1. ViVi Top Bar
        item(key = "vivi_top_bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onHistoryIconClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onStatsIconClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Stats",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. ViVi Filter Chips Row (Morphing Corner Radii)
        item(key = "vivi_chips_row") {
            AnimatedChipsRow(
                chips = filterOptions,
                selectedChip = selectedChip,
                onChipSelect = onChipSelect,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 3. ViVi Quick Access 2x2 Hero Cards
        item(key = "hero_tiles_grid") {
            val tiles = listOf(
                GridTileData(
                    title = "Liked",
                    subtitle = "$likedCount songs",
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFFFF4B6E),
                    subScreen = LibrarySubScreen.LIKED
                ),
                GridTileData(
                    title = "Downloaded",
                    subtitle = "$downloadedCount songs",
                    icon = Icons.Default.CheckCircle,
                    iconColor = MaterialTheme.colorScheme.primary,
                    subScreen = LibrarySubScreen.DOWNLOADED
                ),
                GridTileData(
                    title = "Top 50",
                    subtitle = "Most played",
                    icon = Icons.Default.TrendingUp,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    subScreen = LibrarySubScreen.TOP_50
                ),
                GridTileData(
                    title = "Local",
                    subtitle = "Device audio",
                    icon = Icons.Default.Folder,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    subScreen = LibrarySubScreen.LOCAL
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
        }

        // 4. Sort & Order Row
        item(key = "sort_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date added",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onToggleSort,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (sortAscending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Sort Direction",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 5. Category-Specific Content
        when (selectedChip) {
            "Playlists" -> {
                item(key = "playlists_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NavigationTitle(
                            title = "Playlists",
                            label = "${userPlaylists.size} playlists"
                        )

                        IconButton(
                            onClick = onCreatePlaylistClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Playlist",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (userPlaylists.isEmpty()) {
                    item(key = "empty_playlists") {
                        Surface(
                            onClick = onCreatePlaylistClick,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Create new playlist",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Add songs to listen anytime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ViVi 2-Column Grid for User Playlists
                    item(key = "playlists_grid") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            userPlaylists.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        UserPlaylistGridCard(
                                            playlist = pair[0],
                                            onClick = { onPlaylistCardClick(pair[0]) },
                                            onLongClick = { onPlaylistLongClick?.invoke(pair[0]) }
                                        )
                                    }
                                    if (pair.size > 1) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            UserPlaylistGridCard(
                                                playlist = pair[1],
                                                onClick = { onPlaylistCardClick(pair[1]) },
                                                onLongClick = { onPlaylistLongClick?.invoke(pair[1]) }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Songs" -> {
                val allSongs = (likedSongs.map { it.toSongItem() } + historySongs.map { it.toSongItem() }).distinctBy { it.id }
                item(key = "songs_header") {
                    NavigationTitle(
                        title = "Songs",
                        label = "${allSongs.size} tracks",
                        onPlayAllClick = {
                            if (allSongs.isNotEmpty()) onSongPlay(allSongs.first(), allSongs)
                        }
                    )
                }

                items(allSongs, key = { "song_${it.id}" }) { song ->
                    MuziSongRow(
                        song = song,
                        onClick = { onSongPlay(song, allSongs) },
                        onLongClick = { onSongActionClick?.invoke(song, allSongs) },
                        onActionClick = { onSongActionClick?.invoke(song, allSongs) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            "Artists" -> {
                val artists = (likedSongs.map { it.artist } + historySongs.map { it.artist }).distinct()
                item(key = "artists_header") {
                    NavigationTitle(
                        title = "Artists",
                        label = "${artists.size} artists"
                    )
                }

                items(artists, key = { "art_$it" }) { artistName ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = artistName,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            "Albums" -> {
                item(key = "albums_header") {
                    NavigationTitle(
                        title = "Albums",
                        label = "Saved albums"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Albums you save will appear here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            "Downloaded" -> {
                val songs = (likedSongs.map { it.toSongItem() }).take(0) // or downloadedSongs
                item(key = "downloaded_header") {
                    NavigationTitle(
                        title = "Downloaded",
                        label = "$downloadedCount offline tracks"
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PLAYLIST GRID CARD (ViVi 14dp Corners)
// -------------------------------------------------------------
@Composable
private fun UserPlaylistGridCard(
    playlist: UserPlaylistEntity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = playlist.name,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${playlist.songCount} song${if (playlist.songCount > 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// PLAYLIST DETAIL LAYOUT (ViVi Style)
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
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Back button on left, Search & More icons on right
        item(key = "detail_top_bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onPlaylistOptionsClick != null) {
                        IconButton(onClick = onPlaylistOptionsClick, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Centered Hero Artwork (ViVi 16dp rounded corners)
        item(key = "hero_artwork") {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (title == "Liked Songs" || title == "Liked") Icons.Default.Favorite else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (title == "Liked Songs" || title == "Liked") Color(0xFFFF3366) else Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

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
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Title & Subtitle
        item(key = "playlist_title") {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Action Buttons Row: Play, Shuffle
        item(key = "play_shuffle_actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (songs.isNotEmpty()) onSongPlay(songs.first(), songs)
                    },
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.shuffled()
                            onSongPlay(shuffled.first(), shuffled)
                        }
                    },
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // About Section (if present)
        if (aboutText.isNotBlank()) {
            item(key = "about_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = aboutText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isAboutExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = { isAboutExpanded = !isAboutExpanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isAboutExpanded) "Show less" else "Read more",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Song Tracks List
        if (songs.isNotEmpty()) {
            items(songs, key = { "pl_song_${it.id}" }) { song ->
                MuziSongRow(
                    song = song,
                    onClick = { onSongPlay(song, songs) },
                    onLongClick = { onSongOptionsClick(song) },
                    onActionClick = { onSongOptionsClick(song) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        } else {
            item(key = "empty_tracks") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks in this playlist yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// QUICK ACTION GRID TILE (ViVi 14dp Corners)
// -------------------------------------------------------------
private data class GridTileData(
    val title: String,
    val subtitle: String,
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
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(data.iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = data.title,
                    tint = data.iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = data.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
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
        title = { Text("New playlist", fontWeight = FontWeight.Bold) },
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
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
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
        title = { Text("Add to playlist", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                item {
                    ListItem(
                        headlineContent = { Text("Create new playlist", fontWeight = FontWeight.SemiBold) },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                        modifier = Modifier.clickable { onCreateNewPlaylist() }
                    )
                    HorizontalDivider()
                }

                items(playlists) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("${playlist.songCount} songs") },
                        leadingContent = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                        modifier = Modifier.clickable { onSelectPlaylist(playlist.id) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}