package com.example.muzo.mock

import com.example.muzo.playback.PlayerViewModel
import com.music.innertube.models.SongItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * MockPlaybackEngine: Wraps the real PlayerViewModel but maintains the MockSong interface
 * so the mock UI doesn't break, while actually streaming audio via InnerTube!
 */
class MockPlaybackEngine(
    val realPlayerViewModel: PlayerViewModel,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _queue = MutableStateFlow(MockDataRepository.allMockSongs)
    val queue: StateFlow<List<MockSong>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentSong = MutableStateFlow(MockDataRepository.allMockSongs[0])
    val currentSong: StateFlow<MockSong> = _currentSong.asStateFlow()

    // Expose REAL states from PlayerViewModel
    val isPlaying: StateFlow<Boolean> = realPlayerViewModel.isPlaying
    val currentPositionMs: StateFlow<Long> = realPlayerViewModel.currentPosition
    val durationMs: StateFlow<Long> = realPlayerViewModel.duration
    val isShuffle: StateFlow<Boolean> = realPlayerViewModel.isShuffleActive
    val isRepeat: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    private val _activeLyricIndex = MutableStateFlow(0)
    val activeLyricIndex: StateFlow<Int> = _activeLyricIndex.asStateFlow()

    init {
        scope.launch {
            currentPositionMs.collectLatest { pos ->
                updateActiveLyric(pos)
            }
        }
    }

    private fun updateActiveLyric(posMs: Long) {
        val lyrics = _currentSong.value.lyrics
        if (lyrics.isEmpty()) return
        var matchIdx = 0
        for (i in lyrics.indices) {
            if (posMs >= lyrics[i].timeMs) {
                matchIdx = i
            } else {
                break
            }
        }
        if (_activeLyricIndex.value != matchIdx) {
            _activeLyricIndex.value = matchIdx
        }
    }

    fun togglePlayPause() {
        realPlayerViewModel.togglePlayPause()
    }

    fun play() {
        if (!isPlaying.value) {
            realPlayerViewModel.togglePlayPause()
        }
    }

    fun pause() {
        if (isPlaying.value) {
            realPlayerViewModel.togglePlayPause()
        }
    }

    fun seekTo(positionMs: Long) {
        realPlayerViewModel.seekTo(positionMs)
        updateActiveLyric(positionMs)
    }

    private fun MockSong.toSongItem(): SongItem {
        return SongItem(
            id = this.id,
            title = this.title,
            artists = listOf(com.music.innertube.models.Artist(this.artist, null)),
            album = com.music.innertube.models.Album(this.album, ""),
            duration = (this.durationMs / 1000).toInt(),
            thumbnail = this.thumbnailUrl
        )
    }

    fun playSong(song: MockSong, newQueue: List<MockSong>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
        }
        val targetIdx = _queue.value.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _currentIndex.value = targetIdx
        _currentSong.value = _queue.value[targetIdx]
        
        // Pass the entire mock queue to the real player so it can prefetch and play next!
        val realQueue = _queue.value.map { it.toSongItem() }
        realPlayerViewModel.playTrack(targetIdx, realQueue)
    }

    fun playNext() {
        realPlayerViewModel.playNext()
        syncCurrentSongFromReal()
    }

    fun playPrevious() {
        realPlayerViewModel.playPrevious()
        syncCurrentSongFromReal()
    }
    
    private fun syncCurrentSongFromReal() {
        val realSong = realPlayerViewModel.currentSong.value ?: return
        val mockIndex = _queue.value.indexOfFirst { it.id == realSong.id }
        if (mockIndex >= 0) {
            _currentIndex.value = mockIndex
            _currentSong.value = _queue.value[mockIndex]
        }
    }

    fun toggleShuffle() {
        realPlayerViewModel.toggleShuffle()
    }

    fun toggleRepeat() {
        // Mock UI repeat toggle
    }

    fun release() {
        scope.cancel()
    }
}
