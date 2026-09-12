

package com.biikkkuuuu.muzi.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biikkkuuuu.muzi.db.MusicDatabase
import com.biikkkuuuu.muzi.db.entities.LyricsEntity
import com.biikkkuuuu.muzi.db.entities.Song
import com.biikkkuuuu.muzi.lyrics.LyricsHelper
import com.biikkkuuuu.muzi.lyrics.LyricsResult
import com.biikkkuuuu.muzi.models.MediaMetadata
import com.biikkkuuuu.muzi.utils.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
@Inject
constructor(
    private val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
    private val networkConnectivity: NetworkConnectivityObserver,
) : ViewModel() {
    private var job: Job? = null
    val results = MutableStateFlow(emptyList<LyricsResult>())
    val isLoading = MutableStateFlow(false)

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    init {
        viewModelScope.launch {
            networkConnectivity.networkStatus.collect { isConnected ->
                _isNetworkAvailable.value = isConnected
            }
        }

        _isNetworkAvailable.value = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true 
        }
    }

    fun setCurrentSong(song: Song) {
        _currentSong.value = song
    }

    fun search(
        mediaId: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ) {
        isLoading.value = true
        results.value = emptyList()
        job?.cancel()
        job =
            viewModelScope.launch(Dispatchers.IO) {
                lyricsHelper.getAllLyrics(mediaId, title, artist, duration, album) { result ->
                    results.update {
                        it + result
                    }
                }
                isLoading.value = false
            }
    }

    fun cancelSearch() {
        job?.cancel()
        job = null
    }

    fun refetchLyrics(
        mediaMetadata: MediaMetadata,
        lyricsEntity: LyricsEntity?,
    ) {
        database.query {
            lyricsEntity?.let(::delete)
            val lyricsWithProvider =
                runBlocking {
                    lyricsHelper.getLyrics(mediaMetadata)
                }
            upsert(LyricsEntity(mediaMetadata.id, lyricsWithProvider.lyrics ?: "", lyricsWithProvider.providerName))
        }
    }
}
