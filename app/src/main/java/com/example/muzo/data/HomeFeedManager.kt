package com.example.muzo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

data class FeedShelf(val title: String, val subtitle: String?, val items: List<Any>)

class HomeFeedManager(private val historyManager: LocalHistoryManager) {
    var isRefreshing by mutableStateOf(false)
    var dynamicShelves by mutableStateOf<List<FeedShelf>>(emptyList())

    suspend fun refreshFeed() {
        isRefreshing = true
        withContext(Dispatchers.IO) {
            try {
                val newShelves = mutableListOf<FeedShelf>()

                // EXACT SHELVES FROM YOUR VIDEO
                val queries = listOf(
                    Triple("New releases", null, "New Bollywood Songs Latest Albums"),
                    Triple("Rain Therapy", "FOR COZY DAYS AND ENDLESS CUPS OF TEA", "Monsoon Bollywood Acoustic playlist"),
                    Triple("Dancing on your own", "DANCE YOUR STRESS AWAY", "Bollywood Party Dance Playlist"),
                    Triple("Trending community playlists", null, "Trending Hindi Playlists"),
                    Triple("Featured playlists for you", null, "Weekly Top Videos Playlists India"),
                    Triple("Brb, Being Nostalgic!", "THROWBACK TO THE OG ERAS OF MUSIC", "90s Bollywood Romantic Hits Playlist")
                )

                // Parallel Fetching ONLY PLAYLISTS/ALBUMS (Like the video)
                val deferredResults = queries.map { (title, subtitle, query) ->
                    Triple(title, subtitle, async { YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM) }) // Or FILTER_PLAYLIST
                }

                for ((title, subtitle, deferred) in deferredResults) {
                    val res = deferred.await().getOrNull()?.items?.filterIsInstance<PlaylistItem>() ?: emptyList()
                    if (res.isNotEmpty()) {
                        // Insert "Mood and Genres" statically after the 3rd shelf, just like the video
                        if (newShelves.size == 3) {
                            newShelves.add(FeedShelf("Mood and Genres", null, emptyList())) // Marker for static grid
                        }
                        newShelves.add(FeedShelf(title, subtitle, res.take(10)))
                    }
                }

                withContext(Dispatchers.Main) {
                    dynamicShelves = newShelves
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { isRefreshing = false }
            }
        }
    }
}
