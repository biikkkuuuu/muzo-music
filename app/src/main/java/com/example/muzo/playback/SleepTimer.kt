package com.example.muzo.playback

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * Manages Sleep Timer functionality with smooth volume fade-out and pause.
 * Supports:
 * - Specific minutes (15, 30, 45, 60, custom)
 * - Pause after current song finishes
 * - Real-time countdown tracking
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val player: ExoPlayer
) {
    private var timerJob: Job? = null

    private val _remainingTimeMs = MutableStateFlow<Long>(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _pauseWhenSongEnd = MutableStateFlow(false)
    val pauseWhenSongEnd: StateFlow<Boolean> = _pauseWhenSongEnd.asStateFlow()

    fun start(minutes: Int) {
        cancel()
        if (minutes <= 0) return

        val totalDurationMs = TimeUnit.MINUTES.toMillis(minutes.toLong())
        val targetEndTime = System.currentTimeMillis() + totalDurationMs

        _pauseWhenSongEnd.value = false
        _isActive.value = true
        _remainingTimeMs.value = totalDurationMs

        timerJob = scope.launch(Dispatchers.Main) {
            while (_isActive.value && System.currentTimeMillis() < targetEndTime) {
                val remaining = (targetEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
                _remainingTimeMs.value = remaining
                delay(1000L)
            }

            if (_isActive.value && !_pauseWhenSongEnd.value) {
                fadeOutAndPause()
                cancel()
            }
        }
    }

    fun startEndAfterSong() {
        cancel()
        _pauseWhenSongEnd.value = true
        _isActive.value = true
        _remainingTimeMs.value = -1L // Special flag indicating end-of-song mode
    }

    fun notifySongEnded() {
        if (_pauseWhenSongEnd.value) {
            scope.launch(Dispatchers.Main) {
                fadeOutAndPause()
                cancel()
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _isActive.value = false
        _pauseWhenSongEnd.value = false
        _remainingTimeMs.value = 0L
    }

    /**
     * Smooth 3-second volume attenuation to avoid jarring cutoffs while sleeping.
     */
    private suspend fun fadeOutAndPause() {
        withContext(Dispatchers.Main) {
            try {
                val initialVolume = player.volume
                val steps = 30
                val fadeDurationMs = 3000L
                val stepDelay = fadeDurationMs / steps

                for (i in steps downTo 1) {
                    player.volume = initialVolume * (i.toFloat() / steps)
                    delay(stepDelay)
                }

                player.pause()
                player.volume = initialVolume
            } catch (_: Exception) {
                player.pause()
            }
        }
    }
}
