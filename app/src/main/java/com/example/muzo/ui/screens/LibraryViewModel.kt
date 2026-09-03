package com.example.muzo.ui.screens

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.muzo.data.local.HistoryDao
import com.example.muzo.data.local.HistoryEntity
import com.example.muzo.data.local.LikedSongDao
import com.example.muzo.data.local.LikedSongEntity
import com.example.muzo.data.local.UserPlaylistDao
import com.example.muzo.data.local.UserPlaylistEntity
import com.example.muzo.data.local.UserPlaylistSongEntity
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibrarySubScreen {
    LIKED,
    USER_PLAYLIST,
    HISTORY,
    TOP_50,
    LOCAL,
    DOWNLOADED,
    CACHED,
    EXPORTED
}

class LibraryViewModel(
    private val likedSongDao: LikedSongDao,
    private val historyDao: HistoryDao,
    private val userPlaylistDao: UserPlaylistDao
) : ViewModel() {

    val likedSongs: StateFlow<List<LikedSongEntity>> = likedSongDao.getAllLikedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedCount: StateFlow<Int> = likedSongDao.getLikedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val historySongs: StateFlow<List<HistoryEntity>> = historyDao.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val top50Songs: StateFlow<List<HistoryEntity>> = historyDao.getTop50Songs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPlaylists: StateFlow<List<UserPlaylistEntity>> = userPlaylistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSubScreen = MutableStateFlow<LibrarySubScreen?>(null)
    val activeSubScreen: StateFlow<LibrarySubScreen?> = _activeSubScreen.asStateFlow()

    private val _selectedUserPlaylist = MutableStateFlow<UserPlaylistEntity?>(null)
    val selectedUserPlaylist: StateFlow<UserPlaylistEntity?> = _selectedUserPlaylist.asStateFlow()

    private val _selectedPlaylistSongs = MutableStateFlow<List<UserPlaylistSongEntity>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<UserPlaylistSongEntity>> = _selectedPlaylistSongs.asStateFlow()

    private val _selectedChip = MutableStateFlow("Playlists")
    val selectedChip: StateFlow<String> = _selectedChip.asStateFlow()

    private val _sortAscending = MutableStateFlow(false)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _localSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val localSongs: StateFlow<List<SongItem>> = _localSongs.asStateFlow()

    private val _isLoadingLocal = MutableStateFlow(false)
    val isLoadingLocal: StateFlow<Boolean> = _isLoadingLocal.asStateFlow()

    fun setSubScreen(sub: LibrarySubScreen?) {
        _activeSubScreen.value = sub
        if (sub == null) {
            _selectedUserPlaylist.value = null
        }
    }

    fun openUserPlaylist(playlist: UserPlaylistEntity) {
        _selectedUserPlaylist.value = playlist
        _activeSubScreen.value = LibrarySubScreen.USER_PLAYLIST
        viewModelScope.launch {
            userPlaylistDao.getSongsForPlaylist(playlist.id).collect { songs ->
                _selectedPlaylistSongs.value = songs
            }
        }
    }

    fun setSelectedChip(chip: String) {
        _selectedChip.value = chip
    }

    fun toggleSortOrder() {
        _sortAscending.value = !_sortAscending.value
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            userPlaylistDao.insertPlaylist(
                UserPlaylistEntity(
                    name = name.trim(),
                    createdAt = System.currentTimeMillis(),
                    songCount = 0
                )
            )
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: SongItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val artistName = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" }
            userPlaylistDao.addSongToPlaylist(
                UserPlaylistSongEntity(
                    playlistId = playlistId,
                    videoId = song.id,
                    title = song.title,
                    artist = artistName,
                    thumbnailUrl = song.thumbnail,
                    durationText = "",
                    addedAt = System.currentTimeMillis()
                )
            )
            userPlaylistDao.updatePlaylistMetadata(playlistId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPlaylistDao.removeSongFromPlaylist(playlistId, videoId)
            userPlaylistDao.updatePlaylistMetadata(playlistId)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userPlaylistDao.deletePlaylist(id)
            if (_selectedUserPlaylist.value?.id == id) {
                withContext(Dispatchers.Main) {
                    setSubScreen(null)
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.clearHistory()
        }
    }

    fun unlikeSong(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            likedSongDao.delete(videoId)
        }
    }

    fun loadLocalAudio(context: Context) {
        viewModelScope.launch {
            _isLoadingLocal.value = true
            val songs = withContext(Dispatchers.IO) {
                val list = mutableListOf<SongItem>()
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

                try {
                    val cursor = context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        null,
                        sortOrder
                    )
                    cursor?.use { c ->
                        val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                        val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                        while (c.moveToNext()) {
                            val id = c.getLong(idCol)
                            val title = c.getString(titleCol) ?: "Unknown Title"
                            val artist = c.getString(artistCol) ?: "Unknown Artist"
                            val durationMs = c.getLong(durationCol)
                            val albumId = c.getLong(albumIdCol)

                            val artworkUri = ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            ).toString()

                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                id
                            ).toString()

                            list.add(
                                SongItem(
                                    id = contentUri,
                                    title = title,
                                    artists = listOf(Artist(name = artist, id = null)),
                                    album = null,
                                    duration = (durationMs / 1000).toInt(),
                                    thumbnail = artworkUri
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                list
            }
            _localSongs.value = songs
            _isLoadingLocal.value = false
        }
    }

    class Factory(
        private val likedSongDao: LikedSongDao,
        private val historyDao: HistoryDao,
        private val userPlaylistDao: UserPlaylistDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(likedSongDao, historyDao, userPlaylistDao) as T
        }
    }
}
