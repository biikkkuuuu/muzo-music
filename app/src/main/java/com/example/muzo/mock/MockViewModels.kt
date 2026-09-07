package com.example.muzo.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.muzo.playback.PlayerViewModel
import com.example.muzo.theme.DefaultThemeColor
import com.example.muzo.theme.MetrolistThemePalettes
import com.example.muzo.theme.ThemePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MockPlayerViewModel(val realPlayerViewModel: PlayerViewModel) : ViewModel() {
    val engine = MockPlaybackEngine(realPlayerViewModel)
    
    class Factory(private val playerViewModel: PlayerViewModel) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MockPlayerViewModel(playerViewModel) as T
        }
    }

    val currentSong: StateFlow<MockSong> = engine.currentSong
    val isPlaying: StateFlow<Boolean> = engine.isPlaying
    val currentPositionMs: StateFlow<Long> = engine.currentPositionMs
    val durationMs: StateFlow<Long> = engine.durationMs
    val activeLyricIndex: StateFlow<Int> = engine.activeLyricIndex
    val queue: StateFlow<List<MockSong>> = engine.queue
    val isShuffle: StateFlow<Boolean> = engine.isShuffle
    val isRepeat: StateFlow<Boolean> = engine.isRepeat

    // Navigation & Sheet States
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _isLyricsOpen = MutableStateFlow(false)
    val isLyricsOpen: StateFlow<Boolean> = _isLyricsOpen.asStateFlow()

    private val _isQueueOpen = MutableStateFlow(false)
    val isQueueOpen: StateFlow<Boolean> = _isQueueOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isThemeSettingsOpen = MutableStateFlow(false)
    val isThemeSettingsOpen: StateFlow<Boolean> = _isThemeSettingsOpen.asStateFlow()

    // Metrolist Theming Options
    private val _selectedPalette = MutableStateFlow(
        MetrolistThemePalettes.firstOrNull { it.color == DefaultThemeColor } ?: MetrolistThemePalettes[0]
    )
    val selectedPalette: StateFlow<ThemePalette> = _selectedPalette.asStateFlow()

    private val _pureBlack = MutableStateFlow(false)
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(true)
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    fun setTab(tab: Int) {
        _currentTab.value = tab
    }

    fun openPlayer() {
        _isPlayerExpanded.value = true
    }

    fun closePlayer() {
        _isPlayerExpanded.value = false
        _isLyricsOpen.value = false
        _isQueueOpen.value = false
    }

    fun toggleLyrics() {
        _isLyricsOpen.value = !_isLyricsOpen.value
    }

    fun toggleQueue() {
        _isQueueOpen.value = !_isQueueOpen.value
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openThemeSettings() {
        _isThemeSettingsOpen.value = true
    }

    fun closeThemeSettings() {
        _isThemeSettingsOpen.value = false
    }

    fun setPalette(palette: ThemePalette) {
        _selectedPalette.value = palette
        _useDynamicColor.value = false
    }

    fun togglePureBlack() {
        _pureBlack.value = !_pureBlack.value
    }

    fun setUseDynamicColor(enable: Boolean) {
        _useDynamicColor.value = enable
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}

class MockHomeViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val historyDao = com.example.muzo.data.local.MuziDatabase.getInstance(application).historyDao()
    private val realHomeViewModel = com.example.muzo.data.HomeFeedViewModel(historyDao)

    val shelves: StateFlow<List<MockShelf>> = realHomeViewModel.homeShelves.map { realShelves ->
        realShelves.map { rs ->
            MockShelf(
                id = rs.id,
                title = rs.title,
                songs = rs.items.map { item ->
                    MockSong(
                        id = item.id,
                        title = item.title,
                        artist = item.subtitle ?: "Unknown",
                        album = "",
                        durationMs = 0L,
                        thumbnailUrl = item.imageUrls.firstOrNull() ?: "",
                        lyrics = emptyList()
                    )
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isRefreshing: StateFlow<Boolean> = realHomeViewModel.isRefreshing

    private val _selectedFilterChip = MutableStateFlow<String?>(null)
    val selectedFilterChip: StateFlow<String?> = _selectedFilterChip.asStateFlow()

    fun selectFilterChip(chip: String) {
        _selectedFilterChip.value = if (_selectedFilterChip.value == chip) null else chip
    }

    fun refresh() {
        realHomeViewModel.refreshFeed()
    }
}

class MockSearchViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MockSong>>(emptyList())
    val searchResults: StateFlow<List<MockSong>> = _searchResults.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        _recentSearches.value = com.example.muzo.data.local.SearchHistoryManager.getHistory(application)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        
        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // debounce
            com.example.muzo.data.local.SearchHistoryManager.addQuery(getApplication(), newQuery)
            _recentSearches.value = com.example.muzo.data.local.SearchHistoryManager.getHistory(getApplication())
            
            val result = com.music.innertube.YouTube.search(newQuery, com.music.innertube.YouTube.SearchFilter.FILTER_SONG)
            result.getOrNull()?.items?.let { items ->
                val songs = items.filterIsInstance<com.music.innertube.models.SongItem>().map { song ->
                    MockSong(
                        id = song.id,
                        title = song.title,
                        artist = song.artists.joinToString { it.name },
                        album = song.album?.name ?: "",
                        durationMs = (song.duration ?: 0) * 1000L,
                        thumbnailUrl = song.thumbnail,
                        lyrics = emptyList()
                    )
                }
                _searchResults.value = songs
            }
        }
    }
}

class MockLibraryViewModel : ViewModel() {
    private val _selectedCategory = MutableStateFlow("Playlists")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val playlists = MutableStateFlow(MockDataRepository.mockPlaylists)
    val artists = MutableStateFlow(MockDataRepository.mockArtists)
    val likedSongs = MutableStateFlow(MockDataRepository.allMockSongs)

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }
}
