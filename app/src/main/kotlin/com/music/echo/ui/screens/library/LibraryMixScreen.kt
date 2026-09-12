

package com.biikkkuuuu.muzi.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.biikkkuuuu.muzi.LocalPlayerAwareWindowInsets
import com.biikkkuuuu.muzi.LocalPlayerConnection
import com.biikkkuuuu.muzi.R
import com.biikkkuuuu.muzi.constants.AlbumViewTypeKey
import com.biikkkuuuu.muzi.constants.CONTENT_TYPE_HEADER
import com.biikkkuuuu.muzi.constants.CONTENT_TYPE_PLAYLIST
import com.biikkkuuuu.muzi.constants.GridItemSize
import com.biikkkuuuu.muzi.constants.GridItemsSizeKey
import com.biikkkuuuu.muzi.constants.GridThumbnailHeight
import com.biikkkuuuu.muzi.constants.LibraryViewType
import com.biikkkuuuu.muzi.constants.MixSortDescendingKey
import com.biikkkuuuu.muzi.constants.MixSortType
import com.biikkkuuuu.muzi.constants.MixSortTypeKey
import com.biikkkuuuu.muzi.constants.ShowCachedPlaylistKey
import com.biikkkuuuu.muzi.constants.ShowExportedPlaylistKey
import com.biikkkuuuu.muzi.constants.ShowDownloadedPlaylistKey
import com.biikkkuuuu.muzi.constants.ShowLikedPlaylistKey
import com.biikkkuuuu.muzi.constants.ShowTopPlaylistKey
import com.biikkkuuuu.muzi.constants.ShowUploadedPlaylistKey
import com.biikkkuuuu.muzi.constants.YtmSyncKey
import com.biikkkuuuu.muzi.db.entities.Album
import com.biikkkuuuu.muzi.db.entities.Artist
import com.biikkkuuuu.muzi.db.entities.Playlist
import com.biikkkuuuu.muzi.db.entities.PlaylistEntity
import com.biikkkuuuu.muzi.extensions.reversed
import com.biikkkuuuu.muzi.ui.component.AlbumGridItem
import com.biikkkuuuu.muzi.ui.component.AlbumListItem
import com.biikkkuuuu.muzi.ui.component.ArtistGridItem
import com.biikkkuuuu.muzi.ui.component.ArtistListItem
import com.biikkkuuuu.muzi.ui.component.LocalMenuState
import com.biikkkuuuu.muzi.ui.component.PlaylistGridItem
import com.biikkkuuuu.muzi.ui.component.PlaylistListItem
import com.biikkkuuuu.muzi.ui.component.SortHeader
import com.biikkkuuuu.muzi.ui.menu.AlbumMenu
import com.biikkkuuuu.muzi.ui.menu.ArtistMenu
import com.biikkkuuuu.muzi.ui.menu.PlaylistMenu
import com.biikkkuuuu.muzi.utils.rememberEnumPreference
import com.biikkkuuuu.muzi.utils.rememberPreference
import com.biikkkuuuu.muzi.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import com.biikkkuuuu.muzi.ui.component.AutoPlaylistButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.IconButton

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val uploadedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.uploaded_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showExported) = rememberPreference(ShowExportedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)


    val albums = viewModel.albums.collectAsState()
    val artist = viewModel.artists.collectAsState()
    val playlist = viewModel.playlists.collectAsState()

    val collator = Collator.getInstance(Locale.getDefault())
    collator.strength = Collator.PRIMARY

    fun <T> List<T>.sortItems(): List<T> {
        return when (sortType) {
            MixSortType.CREATE_DATE ->
                this.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        else -> LocalDateTime.now()
                    }
                }

            MixSortType.NAME ->
                this.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            is Playlist -> item.playlist.name
                            else -> ""
                        }
                    },
                )

            MixSortType.LAST_UPDATED ->
                this.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        is Playlist -> item.playlist.lastUpdateTime
                        else -> LocalDateTime.now()
                    }
                }
        }.reversed(sortDescending)
    }

    val sortedPlaylists = playlist.value.sortItems()
    val pinnedPlaylists = sortedPlaylists.filter { it.playlist.isPinned }
    val otherPlaylists = sortedPlaylists.filterNot { it.playlist.isPinned }
    val sortedArtists = artist.value.sortItems()
    val sortedAlbums = albums.value.sortItems()

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
         if (ytmSync) {
             withContext(Dispatchers.IO) {
                 viewModel.syncAllLibrary()
             }
         }
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                        MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        MixSortType.NAME -> R.string.sort_by_name
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            Spacer(modifier = Modifier.width(16.dp))
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    ) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    item(
                        key = "auto_playlists_grid",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        FlowRow(
                            maxItemsInEachRow = 2,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            val itemModifier = Modifier.weight(1f)
                            if (showLiked) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.liked),
                                    icon = R.drawable.favorite,
                                    iconTint = Color(0xFFE57373),
                                    onClick = { navController.navigate("auto_playlist/liked") },
                                    modifier = itemModifier
                                )
                            }
                            if (showDownloaded) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.offline),
                                    icon = R.drawable.offline,
                                    iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    onClick = { navController.navigate("auto_playlist/downloaded") },
                                    modifier = itemModifier
                                )
                            }
                            if (showExported) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.action_exported),
                                    icon = R.drawable.download,
                                    iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    onClick = { navController.navigate("auto_playlist/exported") },
                                    modifier = itemModifier
                                )
                            }
                            if (showCached) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.cached_playlist),
                                    icon = R.drawable.cached,
                                    iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    onClick = { navController.navigate("cache_playlist/cached") },
                                    modifier = itemModifier
                                )
                            }

                            if (showTop) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.my_top) + " $topSize",
                                    icon = R.drawable.trending_up,
                                    iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    onClick = { navController.navigate("top_playlist/$topSize") },
                                    modifier = itemModifier
                                )
                            }
                            AutoPlaylistButton(
                                title = stringResource(R.string.filter_local),
                                icon = R.drawable.snippet_folder,
                                iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                onClick = { navController.navigate("local_songs") },
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .padding(end = 4.dp)
                            )
                        }
                    }

                    item(
                        key = "playlists_header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.filter_playlists),
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    val allPlaylists = pinnedPlaylists + otherPlaylists
                    items(
                        items = allPlaylists.distinctBy { it.id },
                        key = { "playlist_${it.id}" },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        PlaylistListItem(
                            playlist = item,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            PlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("local_playlist/${item.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            PlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                                .animateItem(),
                        )
                    }

                    if (sortedArtists.isNotEmpty()) {
                        item(
                            key = "artists_header",
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.filter_artists),
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(
                            items = sortedArtists.distinctBy { it.id },
                            key = { "artist_${it.id}" },
                            contentType = { "artist" },
                        ) { item ->
                            ArtistListItem(
                                artist = item,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                ArtistMenu(
                                                    originalArtist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("artist/${item.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                ArtistMenu(
                                                    originalArtist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (sortedAlbums.isNotEmpty()) {
                        item(
                            key = "albums_header",
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.filter_albums),
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(
                            items = sortedAlbums.distinctBy { it.id },
                            key = { "album_${it.id}" },
                            contentType = { "album" },
                        ) { item ->
                            AlbumListItem(
                                album = item,
                                isActive = item.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${item.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    item(
                        key = "auto_playlists_grid",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        FlowRow(
                            maxItemsInEachRow = 2,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            val itemModifier = Modifier.weight(1f)
                            if (showLiked) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.liked),
                                    icon = R.drawable.favorite,
                                    iconTint = Color(0xFFE57373),
                                    onClick = { navController.navigate("auto_playlist/liked") },
                                    modifier = itemModifier
                                )
                            }
                            if (showDownloaded) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.offline),
                                    icon = R.drawable.offline,
                                    iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    onClick = { navController.navigate("auto_playlist/downloaded") },
                                    modifier = itemModifier
                                )
                            }
                            if (showExported) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.action_exported),
                                    icon = R.drawable.download,
                                    iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    onClick = { navController.navigate("auto_playlist/exported") },
                                    modifier = itemModifier
                                )
                            }
                            if (showCached) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.cached_playlist),
                                    icon = R.drawable.cached,
                                    iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    onClick = { navController.navigate("cache_playlist/cached") },
                                    modifier = itemModifier
                                )
                            }

                            if (showTop) {
                                AutoPlaylistButton(
                                    title = stringResource(R.string.my_top) + " $topSize",
                                    icon = R.drawable.trending_up,
                                    iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    onClick = { navController.navigate("top_playlist/$topSize") },
                                    modifier = itemModifier
                                )
                            }
                            AutoPlaylistButton(
                                title = stringResource(R.string.filter_local),
                                icon = R.drawable.snippet_folder,
                                iconTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                onClick = { navController.navigate("local_songs") },
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .padding(end = 4.dp)
                            )
                        }
                    }

                    item(
                        key = "playlists_header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.filter_playlists),
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    val allPlaylists = pinnedPlaylists + otherPlaylists
                    items(
                        items = allPlaylists.distinctBy { it.id },
                        key = { "playlist_${it.id}" },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        PlaylistGridItem(
                            playlist = item,
                            fillMaxWidth = true,
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("local_playlist/${item.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            PlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                                .animateItem(),
                        )
                    }

                    if (sortedArtists.isNotEmpty()) {
                        item(
                            key = "artists_header",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.filter_artists),
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(
                            items = sortedArtists.distinctBy { it.id },
                            key = { "artist_${it.id}" },
                            contentType = { "artist" },
                        ) { item ->
                            ArtistGridItem(
                                artist = item,
                                fillMaxWidth = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("artist/${item.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                ArtistMenu(
                                                    originalArtist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (sortedAlbums.isNotEmpty()) {
                        item(
                            key = "albums_header",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.filter_albums),
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(
                            items = sortedAlbums.distinctBy { it.id },
                            key = { "album_${it.id}" },
                            contentType = { "album" },
                        ) { item ->
                            AlbumGridItem(
                                album = item,
                                isActive = item.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                coroutineScope = coroutineScope,
                                fillMaxWidth = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${item.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }
                }
        }
    }
}
