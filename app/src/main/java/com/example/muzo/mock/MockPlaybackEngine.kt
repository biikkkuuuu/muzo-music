package com.example.muzo.mock

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MockPlaybackEngine: High-precision simulated playback engine.
 * Runs on a lightweight coroutine ticker (100ms interval) to move seekbars,
 * track playback progress, update durations, and synchronously highlight lyrics
 * in real-time without needing any audio hardware or ExoPlayer backend.
 */
class MockPlaybackEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _queue = MutableStateFlow(MockDataRepository.allMockSongs)
    val queue: StateFlow<List<MockSong>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentSong = MutableStateFlow(MockDataRepository.allMockSongs[0])
    val currentSong: StateFlow<MockSong> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(_currentSong.value.durationMs)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _activeLyricIndex = MutableStateFlow(0)
    val activeLyricIndex: StateFlow<Int> = _activeLyricIndex.asStateFlow()

    private var tickerJob: Job? = null

    init {
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(100L)
                if (_isPlaying.value) {
                    val nextPos = _currentPositionMs.value + 100L
                    val totalDuration = _durationMs.value

                    if (nextPos >= totalDuration) {
                        if (_isRepeat.value) {
                            _currentPositionMs.value = 0L
                        } else {
                            playNext()
                        }
                    } else {
                        _currentPositionMs.value = nextPos
                    }

                    // Update live synchronized lyrics index
                    updateActiveLyric(nextPos)
                }
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
        _isPlaying.value = !_isPlaying.value
    }

    fun play() {
        _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _durationMs.value)
        _currentPositionMs.value = clamped
        updateActiveLyric(clamped)
    }

    fun playSong(song: MockSong, newQueue: List<MockSong>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
        }
        val targetIdx = _queue.value.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _currentIndex.value = targetIdx
        _currentSong.value = _queue.value[targetIdx]
        _durationMs.value = _currentSong.value.durationMs
        _currentPositionMs.value = 0L
        _isPlaying.value = true
        updateActiveLyric(0L)
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val nextIdx = if (_isShuffle.value) {
            (0 until q.size).random()
        } else {
            (_currentIndex.value + 1) % q.size
        }
        _currentIndex.value = nextIdx
        _currentSong.value = q[nextIdx]
        _durationMs.value = _currentSong.value.durationMs
        _currentPositionMs.value = 0L
        updateActiveLyric(0L)
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return
        val prevIdx = if (_currentIndex.value - 1 < 0) q.size - 1 else _currentIndex.value - 1
        _currentIndex.value = prevIdx
        _currentSong.value = q[prevIdx]
        _durationMs.value = _currentSong.value.durationMs
        _currentPositionMs.value = 0L
        updateActiveLyric(0L)
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun release() {
        tickerJob?.cancel()
        scope.cancel()
    }
}
