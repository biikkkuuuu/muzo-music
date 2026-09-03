package com.example.muzo.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.muzo.core.getHighResThumbnail
import com.example.muzo.data.local.HistoryDao
import com.example.muzo.data.local.HistoryEntity
import com.example.muzo.data.model.HomeShelf
import com.example.muzo.data.model.ItemType
import com.example.muzo.data.model.ShelfItem
import com.example.muzo.data.model.ShelfType
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FeedShelfConfig(
    val title: String,
    val subtitle: String?,
    val query: String,
    val filter: YouTube.SearchFilter,
    val isPlaylistShelf: Boolean
)

class HomeFeedViewModel(
    private val historyDao: HistoryDao
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _remoteShelves = MutableStateFlow<List<HomeShelf>>(emptyList())

    // CRITICAL: Merge reactive Room history flow with remote InnerTube shelves via .combine()
    val homeShelves: StateFlow<List<HomeShelf>> = historyDao.getRecentHistory()
        .combine(_remoteShelves) { history, remoteShelves ->
            val shelves = mutableListOf<HomeShelf>()

            // 1. DYNAMIC "Keep Listening" shelf updated REACTIVELY from Room Database
            if (history.isNotEmpty()) {
                shelves.add(
                    HomeShelf(
                        id = "keep_listening",
                        title = "Keep Listening",
                        subtitle = "RECENTLY PLAYED",
                        type = ShelfType.SONG_CARDS,
                        items = history.take(15).map { entity ->
                            ShelfItem(
                                id = entity.videoId,
                                title = entity.title,
                                subtitle = entity.artist,
                                imageUrls = listOf(getHighResThumbnail(entity.thumbnailUrl)),
                                type = ItemType.SONG
                            )
                        }
                    )
                )
            }

            // 2. Append remote InnerTube shelves (Curated from the screen recording)
            shelves.addAll(remoteShelves)
            shelves
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadFeed()
    }

    fun refreshFeed() {
        loadFeed()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val fetchedShelves = withContext(Dispatchers.IO) {
                    val configs = listOf(
                        FeedShelfConfig(
                            title = "New releases",
                            subtitle = null,
                            query = listOf("Bollywood new releases", "Latest Hindi Songs", "New Punjabi Songs", "Fresh Bollywood Hits").random(),
                            filter = YouTube.SearchFilter.FILTER_SONG,
                            isPlaylistShelf = false
                        ),
                        FeedShelfConfig(
                            title = "Rain Therapy 🌧️☘️",
                            subtitle = "FOR COZY DAYS AND ENDLESS CUPS OF TEA",
                            query = listOf("Monsoon Hindi acoustic songs", "Cozy rainy day Bollywood", "Rain therapy songs", "Lofi Bollywood Rain").random(),
                            filter = YouTube.SearchFilter.FILTER_SONG,
                            isPlaylistShelf = false
                        ),
                        FeedShelfConfig(
                            title = "Dancing on your own",
                            subtitle = "DANCE YOUR STRESS AWAY",
                            query = listOf("Bollywood party dance songs", "Hindi dance hits", "Punjabi dance party", "High energy Bollywood").random(),
                            filter = YouTube.SearchFilter.FILTER_SONG,
                            isPlaylistShelf = false
                        ),
                        FeedShelfConfig(
                            title = "Trending community playlists",
                            subtitle = null,
                            query = listOf("Hindi Hits", "Bollywood Top 50", "Trending Punjabi", "Best of Arijit Singh").random(),
                            filter = YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST,
                            isPlaylistShelf = true
                        ),
                        FeedShelfConfig(
                            title = "Featured playlists for you",
                            subtitle = null,
                            query = listOf("Top Weekly India", "Bollywood Romance", "Chill Hits Hindi", "Viral 50 India").random(),
                            filter = YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST,
                            isPlaylistShelf = true
                        ),
                        FeedShelfConfig(
                            title = "Brb, Being Nostalgic!",
                            subtitle = "THROWBACK TO THE OG ERAS OF MUSIC",
                            query = listOf("90s Bollywood romantic hits", "Retro Hindi Hits", "2000s Bollywood Nostalgia", "Classic Hindi Songs").random(),
                            filter = YouTube.SearchFilter.FILTER_SONG,
                            isPlaylistShelf = false
                        ),
                        FeedShelfConfig(
                            title = "Top Artists",
                            subtitle = "YOUR FAVORITE STARS",
                            query = listOf("Bollywood top artists", "Trending Indian singers", "Top Punjabi Artists", "Best Hindi Singers").random(),
                            filter = YouTube.SearchFilter.FILTER_ARTIST,
                            isPlaylistShelf = false
                        )
                    )

                    val deferredList = configs.map { config ->
                        Pair(
                            config,
                            async {
                                try {
                                    val primary = YouTube.search(config.query, config.filter).getOrNull()?.items
                                    if (!primary.isNullOrEmpty()) {
                                        primary
                                    } else {
                                        // Fallback to song search if category/playlist filter returned empty
                                        YouTube.search(config.query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items ?: emptyList()
                                    }
                                } catch (e: Exception) {
                                    Log.e("HomeFeedVM", "Error searching for ${config.title}: ${e.message}")
                                    emptyList()
                                }
                            }
                        )
                    }

                    val resultShelves = mutableListOf<HomeShelf>()
                    for ((index, pair) in deferredList.withIndex()) {
                        val (config, deferred) = pair
                        val rawItems = deferred.await()

                        val shelfItems = rawItems.mapNotNull { raw ->
                            when (raw) {
                                is SongItem -> {
                                    val thumb = getHighResThumbnail(raw.thumbnail)
                                    ShelfItem(
                                        id = raw.id,
                                        title = raw.title,
                                        subtitle = raw.artists.joinToString(", ") { it.name },
                                        imageUrls = listOf(thumb),
                                        type = ItemType.SONG
                                    )
                                }
                                is PlaylistItem -> {
                                    val thumb = raw.thumbnail?.let { getHighResThumbnail(it) } ?: ""
                                    val images = if (config.title.contains("community", ignoreCase = true)) {
                                        // 4-Image collage preview
                                        listOf(thumb, thumb, thumb, thumb)
                                    } else {
                                        listOf(thumb)
                                    }
                                    ShelfItem(
                                        id = raw.id,
                                        title = raw.title,
                                        subtitle = raw.author?.name ?: "Various Artists",
                                        imageUrls = images,
                                        type = ItemType.PLAYLIST
                                    )
                                }
                                is AlbumItem -> {
                                    val thumb = getHighResThumbnail(raw.thumbnail)
                                    ShelfItem(
                                        id = raw.id,
                                        title = raw.title,
                                        subtitle = raw.artists?.joinToString(", ") { it.name } ?: "Album",
                                        imageUrls = listOf(thumb),
                                        type = ItemType.ALBUM
                                    )
                                }
                                is ArtistItem -> {
                                    val thumb = getHighResThumbnail(raw.thumbnail ?: "")
                                    ShelfItem(
                                        id = raw.id,
                                        title = raw.title,
                                        subtitle = raw.subtext ?: "Artist",
                                        imageUrls = listOf(thumb),
                                        type = ItemType.ARTIST
                                    )
                                }
                                else -> null
                            }
                        }.shuffled().take(12)

                        if (shelfItems.isNotEmpty()) {
                            val shelfType = if (config.isPlaylistShelf) {
                                ShelfType.PLAYLIST_CARDS
                            } else {
                                ShelfType.SONG_CARDS
                            }

                            resultShelves.add(
                                HomeShelf(
                                    id = "shelf_$index",
                                    title = config.title,
                                    subtitle = config.subtitle,
                                    type = shelfType,
                                    items = shelfItems,
                                    seeAllRoute = "see_all/$index"
                                )
                            )
                        }
                    }

                    if (resultShelves.none { it.type == ShelfType.GENRE_GRID }) {
                        resultShelves.add(createMoodAndGenresShelf())
                    }

                    // Randomly shuffle the shelves on every refresh
                    resultShelves.shuffled()
                }

                _remoteShelves.value = fetchedShelves
                Log.d("HomeFeedVM", "Loaded ${fetchedShelves.size} shelves")
            } catch (e: Exception) {
                Log.e("HomeFeedVM", "Failed to load feed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun createMoodAndGenresShelf(): HomeShelf {
        val moods = listOf(
            "Chill", "Focus", "Commute", "Gaming",
            "Energize", "Party", "Feel good", "Romance"
        )
        return HomeShelf(
            id = "mood_and_genres",
            title = "Mood and Genres",
            subtitle = null,
            type = ShelfType.GENRE_GRID,
            items = moods.map { mood ->
                ShelfItem(
                    id = mood,
                    title = mood,
                    subtitle = "Mood",
                    imageUrls = emptyList(),
                    type = ItemType.CHART
                )
            },
            seeAllRoute = "mood_and_genres_all"
        )
    }

    class Factory(private val historyDao: HistoryDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeFeedViewModel(historyDao) as T
        }
    }
}
