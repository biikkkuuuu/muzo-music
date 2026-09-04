package com.example.muzo.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.core.resolveStreamUrl
import com.example.muzo.data.local.HistoryDao
import com.example.muzo.data.local.LikedSongDao
import com.example.muzo.data.local.LikedSongEntity
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(
    private val context: Context,
    private val historyDao: HistoryDao,
    private val likedSongDao: LikedSongDao,
    val player: ExoPlayer
) : ViewModel() {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<SongItem?>(null)
    val currentSong: StateFlow<SongItem?> = _currentSong.asStateFlow()

    private val _playbackQueue = MutableStateFlow<List<SongItem>>(emptyList())
    val playbackQueue: StateFlow<List<SongItem>> = _playbackQueue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isCurrentSongLiked = MutableStateFlow(false)
    val isCurrentSongLiked: StateFlow<Boolean> = _isCurrentSongLiked.asStateFlow()

    fun toggleLikeCurrentSong(forceValue: Boolean? = null) {
        val song = _currentSong.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val currentLiked = likedSongDao.isLiked(song.id)
            val newLiked = forceValue ?: !currentLiked
            if (newLiked) {
                val artistName = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" }
                likedSongDao.insert(
                    LikedSongEntity(
                        videoId = song.id,
                        title = song.title,
                        artist = artistName,
                        thumbnailUrl = song.thumbnail,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                likedSongDao.delete(song.id)
            }
            withContext(Dispatchers.Main) {
                _isCurrentSongLiked.value = newLiked
                MuziMediaSessionService.isSongLiked = newLiked
                android.widget.Toast.makeText(
                    context,
                    if (newLiked) "Added to Liked Songs ❤️" else "Removed from Liked Songs",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private var playJob: kotlinx.coroutines.Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            Log.d("PlayerVM", "onIsPlayingChanged: $playing")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d("PlayerVM", "onPlaybackStateChanged: $playbackState")
            if (playbackState == Player.STATE_READY) {
                _duration.value = player.duration.coerceAtLeast(0L)
                _statusText.value = ""
            } else if (playbackState == Player.STATE_ENDED) {
                playNext()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e("PlayerVM", "ExoPlayer Error: ${error.errorCodeName} - ${error.message}", error)
            val current = _currentSong.value
            if (current != null) {
                com.example.muzo.core.streamUrlCache.remove(current.id)
                viewModelScope.launch {
                    _statusText.value = "Retrying..."
                    val fallbackUrl = withContext(Dispatchers.IO) {
                        try {
                            val streamPairs = com.music.innertube.NewPipeExtractor.newPipePlayer(current.id)
                            val audioItags = listOf(140, 251, 250, 249)
                            val audioMatch = streamPairs.firstOrNull { it.first in audioItags }
                            audioMatch?.second ?: streamPairs.firstOrNull()?.second
                        } catch (_: Exception) { null }
                    }
                    if (!fallbackUrl.isNullOrBlank()) {
                        com.example.muzo.core.streamUrlCache[current.id] = fallbackUrl
                        val mediaItem = MediaItem.Builder()
                            .setMediaId(current.id)
                            .setUri(Uri.parse(fallbackUrl))
                            .build()
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()
                        _isPlaying.value = true
                        _statusText.value = ""
                    } else {
                        _statusText.value = "Playback error"
                    }
                }
            } else {
                _statusText.value = "Error: ${error.errorCodeName}"
            }
        }
    }

    init {
        player.addListener(playerListener)

        // Connect media notification and bluetooth headphone controls to ViewModel
        MuziMediaSessionService.onNextCallback = {
            viewModelScope.launch(Dispatchers.Main) { playNext() }
        }
        MuziMediaSessionService.onPreviousCallback = {
            viewModelScope.launch(Dispatchers.Main) { playPrevious() }
        }
        MuziMediaSessionService.onLikeToggled = { newLiked ->
            viewModelScope.launch(Dispatchers.Main) {
                toggleLikeCurrentSong(newLiked)
            }
        }

        viewModelScope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                    _duration.value = player.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    fun playTrack(index: Int, queue: List<SongItem>) {
        if (index !in queue.indices) return
        _playbackQueue.value = queue
        _currentIndex.value = index
        val song = queue[index]
        _currentSong.value = song
        _statusText.value = "Loading ${song.title}..."
        Log.d("PlayerVM", "playTrack called: index=$index, title=${song.title}, id=${song.id}")

        // Check if song is liked in Room DB
        viewModelScope.launch(Dispatchers.IO) {
            val isLiked = likedSongDao.isLiked(song.id)
            withContext(Dispatchers.Main) {
                _isCurrentSongLiked.value = isLiked
                MuziMediaSessionService.isSongLiked = isLiked
            }
        }

        // Ensure background MediaSessionService is running
        MuziMediaSessionService.start(context)

        // Stop old playing song immediately on tap for instant response
        player.stop()
        player.clearMediaItems()
        _currentPosition.value = 0L
        _isPlaying.value = false

        // Reactive History: Save directly to Room Database with play count tracking
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val artistName = song.artists.firstOrNull()?.name ?: "Unknown Artist"
                historyDao.recordPlay(
                    videoId = song.id,
                    title = song.title,
                    artist = artistName,
                    thumbnailUrl = song.thumbnail ?: ""
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Cancel any existing stream loading job so rapid clicks don't conflict
        playJob?.cancel()
        playJob = viewModelScope.launch {
            val streamUrl = withContext(Dispatchers.IO) {
                resolveStreamUrl(song.id)
            }
            Log.d("PlayerVM", "streamUrl resolved: $streamUrl")

            if (!streamUrl.isNullOrBlank()) {
                val artistName = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" }
                val highResThumb = song.thumbnail?.let { getHighResThumbnail(it) }
                val artworkUri = highResThumb?.let { Uri.parse(it) }

                val metadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(artistName)
                    .setDisplayTitle(song.title)
                    .setArtworkUri(artworkUri)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(song.id)
                    .setUri(Uri.parse(streamUrl))
                    .setMediaMetadata(metadata)
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                _isPlaying.value = true
                _statusText.value = ""
                Log.d("PlayerVM", "player.play() executed successfully")

                // Pre-fetch next 2 tracks in background for instantaneous next-song playback
                for (offset in 1..2) {
                    val prefetchIdx = index + offset
                    if (prefetchIdx in queue.indices) {
                        val nextSong = queue[prefetchIdx]
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                resolveStreamUrl(nextSong.id)
                            } catch (_: Exception) {}
                        }
                    }
                }
            } else {
                Log.e("PlayerVM", "Failed to resolve streamUrl for ${song.id}")
                _statusText.value = "Unable to load stream"
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun playNext() {
        val nextIndex = _currentIndex.value + 1
        if (nextIndex in _playbackQueue.value.indices) {
            playTrack(nextIndex, _playbackQueue.value)
        }
    }

    fun playPrevious() {
        val prevIndex = _currentIndex.value - 1
        if (prevIndex >= 0) {
            playTrack(prevIndex, _playbackQueue.value)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    override fun onCleared() {
        super.onCleared()
        player.removeListener(playerListener)
        MuziMediaSessionService.onNextCallback = null
        MuziMediaSessionService.onPreviousCallback = null
    }

    class Factory(
        private val context: Context,
        private val historyDao: HistoryDao,
        private val likedSongDao: LikedSongDao,
        private val player: ExoPlayer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(context, historyDao, likedSongDao, player) as T
        }
    }
}
