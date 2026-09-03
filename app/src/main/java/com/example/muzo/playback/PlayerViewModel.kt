package com.example.muzo.playback

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.muzo.core.resolveStreamUrl
import com.example.muzo.data.local.HistoryDao
import com.example.muzo.data.local.HistoryEntity
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
    private val historyDao: HistoryDao,
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

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _duration.value = player.duration.coerceAtLeast(0L)
            } else if (playbackState == Player.STATE_ENDED) {
                playNext()
            }
        }
    }

    init {
        player.addListener(playerListener)
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

        // Reactive History: Save directly to Room Database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val artistName = song.artists.firstOrNull()?.name ?: "Unknown Artist"
                historyDao.insertOrUpdate(
                    HistoryEntity(
                        videoId = song.id,
                        title = song.title,
                        artist = artistName,
                        thumbnailUrl = song.thumbnail,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fetch stream URL asynchronously from InnerTube
        viewModelScope.launch {
            val streamUrl = withContext(Dispatchers.IO) {
                resolveStreamUrl(song.id)
            }

            if (!streamUrl.isNullOrBlank()) {
                val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                _isPlaying.value = true
                _statusText.value = ""
            } else {
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
    }

    class Factory(
        private val historyDao: HistoryDao,
        private val player: ExoPlayer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(historyDao, player) as T
        }
    }
}
