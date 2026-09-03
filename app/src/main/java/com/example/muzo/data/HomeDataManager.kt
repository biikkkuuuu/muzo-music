package com.example.muzo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.innertube.YouTube
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class HomeDataManager(val historyManager: LocalHistoryManager) {
    var isRefreshing by mutableStateOf(false)
    
    var dailyDiscover by mutableStateOf<List<SongItem>>(emptyList())
    var hotHits by mutableStateOf<List<SongItem>>(emptyList())
    var similarRecs by mutableStateOf<List<SongItem>>(emptyList())
    var communityPlaylists by mutableStateOf<List<SongItem>>(emptyList())
    var artistsList by mutableStateOf<List<ArtistItem>>(emptyList())
    var playlistsList by mutableStateOf<List<PlaylistItem>>(emptyList())

    suspend fun loadNetworkPhase(currentHistory: List<SongItem>, userGenres: List<String>) {
        isRefreshing = true
        withContext(Dispatchers.IO) {
            try {
                val seenIds = mutableSetOf<String>()
                
                // 1. DYNAMIC GENRES
                val activeGenres = if (userGenres.isNotEmpty()) userGenres.shuffled() else listOf("Latest Trending", "Global Music").shuffled()
                val g1 = activeGenres.getOrElse(0) { "Music" }
                val g2 = activeGenres.getOrElse(1 % activeGenres.size) { g1 }
                val g3 = activeGenres.getOrElse(2 % activeGenres.size) { g1 }

                // 2. MIX WITH ACTUAL LISTENING HISTORY
                val historyTitles = currentHistory.map { it.title }.shuffled()
                val h1 = historyTitles.getOrNull(0)

                // 3. 100% DYNAMIC LIVE NETWORK REQUESTS (PARALLEL)
                val discoverDeferred = async { YouTube.search("$g1 new releases", YouTube.SearchFilter.FILTER_SONG) }
                val hitsDeferred = async { YouTube.search("$g2 top hits", YouTube.SearchFilter.FILTER_SONG) }
                
                val recsQuery = if (h1 != null) "$h1 mix" else "$g3 favorite songs"
                val recsDeferred = async { YouTube.search(recsQuery, YouTube.SearchFilter.FILTER_SONG) }
                
                val commDeferred = async { YouTube.search("$g1 viral trending", YouTube.SearchFilter.FILTER_SONG) }
                val playlistsDeferred = async { YouTube.search("$g2 mood", YouTube.SearchFilter.FILTER_ALBUM) }
                val artistsDeferred = async { YouTube.search("$g3", YouTube.SearchFilter.FILTER_ARTIST) }

                // Wait for all APIs to finish loading
                val discoverRes = discoverDeferred.await()
                val hitsRes = hitsDeferred.await()
                val recsRes = recsDeferred.await()
                val commRes = commDeferred.await()
                val playlistsRes = playlistsDeferred.await()
                val artistsRes = artistsDeferred.await()

                // Update UI State
                withContext(Dispatchers.Main) {
                    dailyDiscover = discoverRes.getOrNull()?.items?.filterIsInstance<SongItem>()?.filter { seenIds.add(it.id) }?.take(10) ?: emptyList()
                    hotHits = hitsRes.getOrNull()?.items?.filterIsInstance<SongItem>()?.filter { seenIds.add(it.id) }?.take(10) ?: emptyList()
                    similarRecs = recsRes.getOrNull()?.items?.filterIsInstance<SongItem>()?.filter { seenIds.add(it.id) }?.take(10) ?: emptyList()
                    communityPlaylists = commRes.getOrNull()?.items?.filterIsInstance<SongItem>()?.filter { seenIds.add(it.id) }?.take(10) ?: emptyList()
                    
                    playlistsList = playlistsRes.getOrNull()?.items?.filterIsInstance<PlaylistItem>()?.take(10) ?: emptyList()
                    artistsList = artistsRes.getOrNull()?.items?.filterIsInstance<ArtistItem>()?.take(10) ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { isRefreshing = false }
            }
        }
    }
}
