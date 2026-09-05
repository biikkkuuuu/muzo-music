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
    val id: String,
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
        loadFeed(isUserRefresh = false)
    }

    fun refreshFeed() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true

            // Immediately reshuffle existing items in memory so user gets instant fresh content
            val current = _remoteShelves.value
            if (current.isNotEmpty()) {
                _remoteShelves.value = current.map { shelf ->
                    shelf.copy(items = shelf.items.shuffled())
                }
            }

            // Snappy refresh indicator dismissal: spin for 550ms then smoothly retract
            launch {
                kotlinx.coroutines.delay(550L)
                _isRefreshing.value = false
            }

            try {
                loadNetworkData()
            } catch (e: Exception) {
                Log.e("HomeFeedVM", "Failed to refresh feed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun loadFeed(isUserRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isUserRefresh) {
                _isRefreshing.value = true
            }
            try {
                loadNetworkData()
            } catch (e: Exception) {
                Log.e("HomeFeedVM", "Failed to load feed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun loadNetworkData() {
        val fetchedShelves = withContext(Dispatchers.IO) {
                    val configs = listOf(
                        FeedShelfConfig(
                            id = "shelf_new_releases",
                            title = "New releases",
                            subtitle = null,
                            query = listOf("Bollywood new releases", "Latest Hindi Songs", "New Punjabi Songs", "Fresh Bollywood Hits").random(),
                            filter = YouTube.SearchFilter.FILTER_SONG,
                            isPlaylistShelf = false
                        ),
                        FeedShelfConfig(
                            id = "shelf_rain_therapy",
                            title = "Rain Therapy 🌧️☘️",
                            subtitle = "FOR COZY DAYS AND ENDLESS CUPS OF TEA",
                            query = listOf("Monsoon Hindi acoustic songs", "Cozy rainy day Bollywood", "Rain therapy songs", "Lofi Bollywood Rain").random(),
                            filter = YouTube.SearchFilter.FILTER_SONG,
                            isPlaylistShelf = false
                        ),
                        FeedShelfConfig(
                            id = "shelf_dancing",
                            title = "Dancing on your own",
                            subtitle = "DANCE YOUR STRESS AWAY",
                            query = listOf("Bollywood party dance songs", "Hindi dance hits", "Punjabi dance party", "High energy Bollywood").random(),
                            filter = YouTube.SearchFilter.FILTER_SONG,
                            isPlaylistShelf = false
                        ),
                        FeedShelfConfig(
                            id = "shelf_community",
                            title = "Trending community playlists",
                            subtitle = null,
                            query = listOf("Hindi Hits", "Bollywood Top 50", "Trending Punjabi", "Best of Arijit Singh").random(),
                            filter = YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST,
                            isPlaylistShelf = true
                        ),
                        FeedShelfConfig(
                            id = "shelf_featured",
                            title = "Featured playlists for you",
                            subtitle = null,
                            query = listOf("Top Weekly India", "Bollywood Romance", "Chill Hits Hindi", "Viral 50 India").random(),
                            filter = YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST,
                            isPlaylistShelf = true
                        ),
                        FeedShelfConfig(
                            id = "shelf_nostalgic",
                            title = "Brb, Being Nostalgic!",
                            subtitle = "THROWBACK TO THE OG ERAS OF MUSIC",
                            query = listOf("90s Bollywood romantic hits", "Retro Hindi Hits", "2000s Bollywood Nostalgia", "Classic Hindi Songs").random(),
                            filter = YouTube.SearchFilter.FILTER_SONG,
                            isPlaylistShelf = false
                        ),
                        FeedShelfConfig(
                            id = "shelf_top_artists",
                            title = "Top Artists",
                            subtitle = "YOUR FAVORITE STARS",
                            query = "Top Artists",
                            filter = YouTube.SearchFilter.FILTER_ARTIST,
                            isPlaylistShelf = false
                        )
                    )

                    val artistBatches = listOf(
                        "Arijit Singh Atif Aslam Pritam Mohit Chauhan",
                        "Diljit Dosanjh Karan Aujla AP Dhillon Shubh",
                        "Shreya Ghoshal Sunidhi Chauhan Neha Kakkar Jonita Gandhi",
                        "Anuv Jain Prateek Kuhad Jasleen Royal The Local Train",
                        "KK Sonu Nigam Lucky Ali Kumar Sanu Alka Yagnik",
                        "Divine King Seedhe Maut Raftaar MC Stan",
                        "A.R. Rahman Amit Trivedi Vishal-Shekhar Shankar Mahadevan",
                        "Darshan Raval Armaan Malik Jubin Nautiyal B Praak",
                        "The Weeknd Taylor Swift Ed Sheeran Bruno Mars",
                        "Talwiinder Harrdy Sandhu Ammy Virk Parmish Verma",
                        "Sidhu Moose Wala Amrit Maan Jassie Gill Akhil",
                        "Javed Ali Papon Shilpa Rao Monali Thakur"
                    ).shuffled()

                    val deferredList = configs.map { config ->
                        Pair(
                            config,
                            async(Dispatchers.IO) {
                                try {
                                    // 2200ms tight timeout for lightning-fast responsive refresh cycle
                                    kotlinx.coroutines.withTimeoutOrNull(2200L) {
                                        if (config.filter == YouTube.SearchFilter.FILTER_ARTIST) {
                                            // Concurrently fetch 2 artist batches to avoid sequential network delays
                                            val q1 = artistBatches[0]
                                            val q2 = artistBatches[1]
                                            val d1 = async(Dispatchers.IO) {
                                                YouTube.search(q1, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items.orEmpty()
                                            }
                                            val d2 = async(Dispatchers.IO) {
                                                YouTube.search(q2, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items.orEmpty()
                                            }
                                            val combinedArtists = (d1.await() + d2.await()).filterIsInstance<ArtistItem>().distinctBy { it.id }
                                            if (combinedArtists.isNotEmpty()) {
                                                combinedArtists
                                            } else {
                                                YouTube.search("Indian top artists", YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items ?: emptyList()
                                            }
                                        } else {
                                            val primary = YouTube.search(config.query, config.filter).getOrNull()?.items
                                            if (!primary.isNullOrEmpty()) {
                                                primary
                                            } else {
                                                YouTube.search(config.query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items ?: emptyList()
                                            }
                                        }
                                    } ?: emptyList()
                                } catch (e: Exception) {
                                    Log.e("HomeFeedVM", "Error searching for ${config.title}: ${e.message}")
                                    emptyList()
                                }
                            }
                        )
                    }

                    val resultShelves = mutableListOf<HomeShelf>()
                    for (pair in deferredList) {
                        val (config, deferred) = pair
                        val rawItems = deferred.await()

                        val parsedItems = rawItems.mapNotNull { raw ->
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
                        }.shuffled()

                        // If freshly fetched items exist, use them; otherwise retain previous shelf items so no shelf disappears
                        val shelfItems = if (parsedItems.isNotEmpty()) {
                            parsedItems
                        } else {
                            _remoteShelves.value.firstOrNull { it.id == config.id }?.items.orEmpty()
                        }

                        if (shelfItems.isNotEmpty()) {
                            val shelfType = if (config.isPlaylistShelf) {
                                ShelfType.PLAYLIST_CARDS
                            } else {
                                ShelfType.SONG_CARDS
                            }

                            resultShelves.add(
                                HomeShelf(
                                    id = config.id,
                                    title = config.title,
                                    subtitle = config.subtitle,
                                    type = shelfType,
                                    items = shelfItems,
                                    seeAllRoute = "see_all/${config.id}"
                                )
                            )
                        }
                    }

                    if (resultShelves.none { it.type == ShelfType.GENRE_GRID }) {
                        resultShelves.add(createMoodAndGenresShelf())
                    }

                    resultShelves
                }

                _remoteShelves.value = fetchedShelves
                Log.d("HomeFeedVM", "Loaded ${fetchedShelves.size} shelves")

                // Pre-resolve stream URLs of first 3 visible songs for instantaneous 0ms playback on tap
                viewModelScope.launch(Dispatchers.IO) {
                    fetchedShelves.flatMap { it.items }
                        .filter { it.type == ItemType.SONG }
                        .take(3)
                        .forEach { songItem ->
                            try {
                                com.example.muzo.core.resolveStreamUrl(songItem.id)
                            } catch (_: Exception) {}
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
