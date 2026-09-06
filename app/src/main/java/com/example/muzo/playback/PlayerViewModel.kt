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
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
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

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isShuffleActive = MutableStateFlow(false)
    val isShuffleActive: StateFlow<Boolean> = _isShuffleActive.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0 = off, 1 = repeat all, 2 = repeat one
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val playbackPrefs = context.getSharedPreferences("muzi_playback_prefs", Context.MODE_PRIVATE)

    private val _crossfadeSeconds = MutableStateFlow(playbackPrefs.getInt("crossfade_seconds", 4))
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _isGaplessEnabled = MutableStateFlow(playbackPrefs.getBoolean("gapless_playback", true))
    val isGaplessEnabled: StateFlow<Boolean> = _isGaplessEnabled.asStateFlow()

    fun setCrossfadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 12)
        _crossfadeSeconds.value = clamped
        playbackPrefs.edit().putInt("crossfade_seconds", clamped).apply()
        val label = if (clamped == 0) "Crossfade: Off" else "Crossfade: ${clamped}s"
        android.widget.Toast.makeText(context, label, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun setGaplessEnabled(enabled: Boolean) {
        _isGaplessEnabled.value = enabled
        playbackPrefs.edit().putBoolean("gapless_playback", enabled).apply()
        if (!enabled && player.mediaItemCount > 1) {
            while (player.mediaItemCount > 1) {
                player.removeMediaItem(player.mediaItemCount - 1)
            }
        } else if (enabled) {
            scheduleGaplessPreload(_currentIndex.value, _playbackQueue.value)
        }
        val label = if (enabled) "Gapless Playback: On" else "Gapless Playback: Off"
        android.widget.Toast.makeText(context, label, android.widget.Toast.LENGTH_SHORT).show()
    }

    @Volatile private var isFadingOutForManualSkip = false
    @Volatile private var isFadingIn = false
    @Volatile private var fadeInStartTime = 0L
    @Volatile private var fadeInDurationMs = 1200L

    private fun startFadeIn(durationMs: Long = 1200L) {
        if (_crossfadeSeconds.value == 0) {
            player.volume = 1.0f
            isFadingIn = false
            return
        }
        player.volume = 0.05f
        fadeInStartTime = System.currentTimeMillis()
        fadeInDurationMs = durationMs
        isFadingIn = true
    }

    private suspend fun quickFadeOut() {
        if (!_isPlaying.value || _crossfadeSeconds.value == 0) return
        isFadingOutForManualSkip = true
        val startVol = player.volume
        val steps = 4
        for (i in (steps - 1) downTo 0) {
            player.volume = (startVol * (i.toFloat() / steps)).coerceAtLeast(0f)
            delay(35)
        }
        isFadingOutForManualSkip = false
    }

    private fun handleCrossfadeVolume(pos: Long, dur: Long) {
        if (isFadingOutForManualSkip) return

        if (isFadingIn) {
            val elapsed = System.currentTimeMillis() - fadeInStartTime
            if (elapsed >= fadeInDurationMs) {
                player.volume = 1.0f
                isFadingIn = false
            } else {
                val fraction = (elapsed.toFloat() / fadeInDurationMs).coerceIn(0.05f, 1.0f)
                player.volume = fraction
            }
            return
        }

        val crossfadeSec = _crossfadeSeconds.value
        if (crossfadeSec > 0 && dur > 6000L) {
            val crossfadeMs = crossfadeSec * 1000L
            val remaining = dur - pos
            if (remaining in 0L..crossfadeMs) {
                val fraction = (remaining.toFloat() / crossfadeMs).coerceIn(0.05f, 1.0f)
                player.volume = fraction
                return
            }
        }

        if (player.volume != 1.0f) {
            player.volume = 1.0f
        }
    }

    fun getNextIndex(current: Int, queue: List<SongItem>): Int? {
        if (queue.isEmpty()) return null
        if (_repeatMode.value == 2) return current
        if (_isShuffleActive.value && queue.size > 1) {
            val unplayed = queue.indices.filter { it != current }
            if (unplayed.isNotEmpty()) return unplayed.random()
        }
        val next = current + 1
        if (next in queue.indices) return next
        if (_repeatMode.value == 1) return 0
        return null
    }

    private var preloadJob: kotlinx.coroutines.Job? = null

    private fun scheduleGaplessPreload(currentIndex: Int, queue: List<SongItem>) {
        if (!_isGaplessEnabled.value) return
        val nextIdx = getNextIndex(currentIndex, queue) ?: return
        val nextSong = queue.getOrNull(nextIdx) ?: return

        preloadJob?.cancel()
        preloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val alreadyQueued = withContext(Dispatchers.Main) {
                    if (player.mediaItemCount > 1) {
                        player.getMediaItemAt(1).mediaId == nextSong.id
                    } else false
                }
                if (alreadyQueued) return@launch

                val localFile = com.example.muzo.data.download.SongDownloadManager.getInstance(context).getDownloadedFile(nextSong.id)
                val mediaUri = if (localFile != null && localFile.exists() && localFile.length() > 0) {
                    Uri.fromFile(localFile)
                } else {
                    val streamUrl = resolveStreamUrl(nextSong.id)
                    if (!streamUrl.isNullOrBlank()) Uri.parse(streamUrl) else null
                }

                if (mediaUri != null) {
                    val artistName = nextSong.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" }
                    val highResThumb = nextSong.thumbnail?.let { getHighResThumbnail(it) }
                    val artworkUri = highResThumb?.let { Uri.parse(it) }

                    val metadata = MediaMetadata.Builder()
                        .setTitle(nextSong.title)
                        .setArtist(artistName)
                        .setDisplayTitle(nextSong.title)
                        .setArtworkUri(artworkUri)
                        .build()

                    val nextMediaItem = MediaItem.Builder()
                        .setMediaId(nextSong.id)
                        .setUri(mediaUri)
                        .setMediaMetadata(metadata)
                        .build()

                    withContext(Dispatchers.Main) {
                        while (player.mediaItemCount > 1) {
                            player.removeMediaItem(player.mediaItemCount - 1)
                        }
                        player.addMediaItem(nextMediaItem)
                        Log.d("PlayerVM", "Gapless preloaded next song: ${nextSong.title}")
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to preload gapless media item", e)
            }
        }
    }

    fun toggleShuffle() {
        val next = !_isShuffleActive.value
        _isShuffleActive.value = next
        scheduleGaplessPreload(_currentIndex.value, _playbackQueue.value)
        android.widget.Toast.makeText(context, if (next) "Shuffle On 🔀" else "Shuffle Off", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun toggleRepeat() {
        val next = (_repeatMode.value + 1) % 3
        _repeatMode.value = next
        scheduleGaplessPreload(_currentIndex.value, _playbackQueue.value)
        val msg = when (next) {
            1 -> "Repeat All 🔁"
            2 -> "Repeat One 🔂"
            else -> "Repeat Off"
        }
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 2.5f)
        _playbackSpeed.value = clamped
        player.playbackParameters = androidx.media3.common.PlaybackParameters(clamped)
    }

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

    val sleepTimer = SleepTimer(viewModelScope, player)
    val equalizerController = EqualizerController(context)

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
                try {
                    equalizerController.attachAudioSession(player.audioSessionId)
                } catch (_: Exception) {}
            } else if (playbackState == Player.STATE_ENDED) {
                if (sleepTimer.pauseWhenSongEnd.value) {
                    sleepTimer.notifySongEnded()
                } else {
                    playNext()
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d("PlayerVM", "onMediaItemTransition: reason=$reason, id=${mediaItem?.mediaId}")
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                if (sleepTimer.pauseWhenSongEnd.value) {
                    sleepTimer.notifySongEnded()
                    return
                }

                val currentQueue = _playbackQueue.value
                val nextIdx = getNextIndex(_currentIndex.value, currentQueue)
                if (nextIdx != null && nextIdx in currentQueue.indices) {
                    _currentIndex.value = nextIdx
                    val newSong = currentQueue[nextIdx]
                    _currentSong.value = newSong
                    _currentPosition.value = 0L
                    _duration.value = player.duration.coerceAtLeast(0L)

                    if (player.mediaItemCount > 1) {
                        player.removeMediaItem(0)
                    }

                    startFadeIn(durationMs = (_crossfadeSeconds.value * 600L).coerceIn(800L, 2500L))

                    viewModelScope.launch(Dispatchers.IO) {
                        val isLiked = likedSongDao.isLiked(newSong.id)
                        withContext(Dispatchers.Main) {
                            _isCurrentSongLiked.value = isLiked
                            MuziMediaSessionService.isSongLiked = isLiked
                        }
                        val artistName = newSong.artists.firstOrNull()?.name ?: "Unknown Artist"
                        historyDao.recordPlay(
                            videoId = newSong.id,
                            title = newSong.title,
                            artist = artistName,
                            thumbnailUrl = newSong.thumbnail ?: ""
                        )
                    }

                    scheduleGaplessPreload(nextIdx, currentQueue)
                }
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
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    _currentPosition.value = pos
                    _duration.value = dur
                    handleCrossfadeVolume(pos, dur)
                    delay(80)
                } else {
                    delay(250)
                }
            }
        }
    }

    fun playTrack(index: Int, queue: List<SongItem>) {
        if (index !in queue.indices) return
        playJob?.cancel()
        playJob = viewModelScope.launch {
            if (_isPlaying.value) {
                quickFadeOut()
            }
            startTrackPlayback(index, queue)
        }
    }

    private suspend fun startTrackPlayback(index: Int, queue: List<SongItem>) {
        if (index !in queue.indices) return
        _playbackQueue.value = queue
        _currentIndex.value = index
        val song = queue[index]
        _currentSong.value = song
        _statusText.value = "Loading ${song.title}..."
        Log.d("PlayerVM", "startTrackPlayback: index=$index, title=${song.title}, id=${song.id}")

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

        // Check if downloaded offline file exists first
        val localFile = withContext(Dispatchers.IO) {
            com.example.muzo.data.download.SongDownloadManager.getInstance(context).getDownloadedFile(song.id)
        }

        val mediaUri = if (localFile != null && localFile.exists() && localFile.length() > 0) {
            Log.d("PlayerVM", "Playing from offline download: ${localFile.absolutePath}")
            Uri.fromFile(localFile)
        } else {
            val streamUrl = withContext(Dispatchers.IO) {
                resolveStreamUrl(song.id)
            }
            Log.d("PlayerVM", "streamUrl resolved: $streamUrl")
            if (!streamUrl.isNullOrBlank()) Uri.parse(streamUrl) else null
        }

        if (mediaUri != null) {
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
                .setUri(mediaUri)
                .setMediaMetadata(metadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _isPlaying.value = true
            _statusText.value = ""
            startFadeIn(durationMs = 600L)
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

            // Schedule gapless preload of the next MediaItem into ExoPlayer
            scheduleGaplessPreload(index, queue)
        } else {
            Log.e("PlayerVM", "Failed to resolve streamUrl for ${song.id}")
            _statusText.value = "Unable to load stream"
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
        val queue = _playbackQueue.value
        if (queue.isEmpty()) return

        if (_repeatMode.value == 2) {
            seekTo(0L)
            player.play()
            return
        }

        if (_isShuffleActive.value && queue.size > 1) {
            val unplayedIndices = queue.indices.filter { it != _currentIndex.value }
            if (unplayedIndices.isNotEmpty()) {
                val randomIndex = unplayedIndices.random()
                playTrack(randomIndex, queue)
                return
            }
        }

        val nextIndex = _currentIndex.value + 1
        if (nextIndex in queue.indices) {
            playTrack(nextIndex, queue)
        } else if (_repeatMode.value == 1 && queue.isNotEmpty()) {
            playTrack(0, queue)
        }
    }

    fun playPrevious() {
        if (_currentPosition.value > 4000L) {
            seekTo(0L)
            return
        }
        val prevIndex = _currentIndex.value - 1
        if (prevIndex >= 0) {
            playTrack(prevIndex, _playbackQueue.value)
        } else if (_repeatMode.value == 1 && _playbackQueue.value.isNotEmpty()) {
            playTrack(_playbackQueue.value.lastIndex, _playbackQueue.value)
        } else {
            seekTo(0L)
        }
    }

    fun addToQueueNext(song: SongItem) {
        val currentQueue = _playbackQueue.value.toMutableList()
        if (currentQueue.isEmpty()) {
            playTrack(0, listOf(song))
        } else {
            val insertIdx = (_currentIndex.value + 1).coerceIn(0, currentQueue.size)
            currentQueue.add(insertIdx, song)
            _playbackQueue.value = currentQueue
            scheduleGaplessPreload(_currentIndex.value, currentQueue)
        }
        android.widget.Toast.makeText(context, "Playing next: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun addToQueueNext(songs: List<SongItem>) {
        if (songs.isEmpty()) return
        val currentQueue = _playbackQueue.value.toMutableList()
        if (currentQueue.isEmpty()) {
            playTrack(0, songs)
        } else {
            val insertIdx = (_currentIndex.value + 1).coerceIn(0, currentQueue.size)
            currentQueue.addAll(insertIdx, songs)
            _playbackQueue.value = currentQueue
            scheduleGaplessPreload(_currentIndex.value, currentQueue)
        }
        android.widget.Toast.makeText(context, "Added ${songs.size} songs to play next", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun addToQueueEnd(song: SongItem) {
        val currentQueue = _playbackQueue.value.toMutableList()
        if (currentQueue.isEmpty()) {
            playTrack(0, listOf(song))
        } else {
            currentQueue.add(song)
            _playbackQueue.value = currentQueue
            scheduleGaplessPreload(_currentIndex.value, currentQueue)
        }
        android.widget.Toast.makeText(context, "Added to queue: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun addToQueueEnd(songs: List<SongItem>) {
        if (songs.isEmpty()) return
        val currentQueue = _playbackQueue.value.toMutableList()
        if (currentQueue.isEmpty()) {
            playTrack(0, songs)
        } else {
            currentQueue.addAll(songs)
            _playbackQueue.value = currentQueue
            scheduleGaplessPreload(_currentIndex.value, currentQueue)
        }
        android.widget.Toast.makeText(context, "Added ${songs.size} songs to queue", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun startRadio(song: SongItem) {
        viewModelScope.launch {
            _statusText.value = "Starting radio..."
            val radioSongs = withContext(Dispatchers.IO) {
                try {
                    val res = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()
                    val endpointSongs = res?.items?.filterIsInstance<SongItem>().orEmpty()
                    if (endpointSongs.isNotEmpty()) {
                        listOf(song) + endpointSongs.filter { it.id != song.id }
                    } else {
                        val artistName = song.artists.firstOrNull()?.name.orEmpty()
                        val searchRes = YouTube.search("${song.title} $artistName", YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                        listOf(song) + searchRes.filter { it.id != song.id }
                    }
                } catch (_: Exception) {
                    listOf(song)
                }
            }
            playTrack(0, radioSongs)
            android.widget.Toast.makeText(context, "Radio started 📻", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun playShuffled(songs: List<SongItem>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        playTrack(0, shuffled)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentList = _playbackQueue.value.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) return

        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _playbackQueue.value = currentList

        // Adjust currentIndex if affected
        val current = _currentIndex.value
        if (current == fromIndex) {
            _currentIndex.value = toIndex
        } else if (fromIndex < current && toIndex >= current) {
            _currentIndex.value = current - 1
        } else if (fromIndex > current && toIndex <= current) {
            _currentIndex.value = current + 1
        }
        scheduleGaplessPreload(_currentIndex.value, currentList)
    }

    fun removeQueueItem(index: Int) {
        val currentList = _playbackQueue.value.toMutableList()
        if (index !in currentList.indices) return

        val current = _currentIndex.value
        currentList.removeAt(index)
        _playbackQueue.value = currentList

        if (index == current) {
            if (currentList.isEmpty()) {
                player.stop()
                _currentSong.value = null
                _currentIndex.value = -1
                _isPlaying.value = false
            } else {
                val nextIdx = index.coerceAtMost(currentList.lastIndex)
                playTrack(nextIdx, currentList)
            }
        } else if (index < current) {
            _currentIndex.value = current - 1
            scheduleGaplessPreload(_currentIndex.value, currentList)
        } else {
            scheduleGaplessPreload(_currentIndex.value, currentList)
        }
    }

    fun clearUpcomingQueue() {
        val current = _currentIndex.value
        val currentList = _playbackQueue.value
        if (current >= 0 && current < currentList.size) {
            // Keep everything up to current playing song, clear upcoming
            val newQueue = currentList.take(current + 1)
            _playbackQueue.value = newQueue
            // Remove preloaded item since upcoming queue is cleared
            while (player.mediaItemCount > 1) {
                player.removeMediaItem(player.mediaItemCount - 1)
            }
            android.widget.Toast.makeText(context, "Upcoming queue cleared", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimer.cancel()
        equalizerController.release()
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
