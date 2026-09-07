package com.example.muzo.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muzo.theme.DefaultThemeColor
import com.example.muzo.theme.MetrolistThemePalettes
import com.example.muzo.theme.ThemePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MockPlayerViewModel : ViewModel() {
    val engine = MockPlaybackEngine()

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

class MockHomeViewModel : ViewModel() {
    private val _shelves = MutableStateFlow(MockDataRepository.mockHomeShelves)
    val shelves: StateFlow<List<MockShelf>> = _shelves.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedFilterChip = MutableStateFlow<String?>(null)
    val selectedFilterChip: StateFlow<String?> = _selectedFilterChip.asStateFlow()

    fun selectFilterChip(chip: String) {
        _selectedFilterChip.value = if (_selectedFilterChip.value == chip) null else chip
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1200L)
            _isRefreshing.value = false
        }
    }
}

class MockSearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val searchResults: StateFlow<List<MockSong>> = _query.map { q ->
        if (q.isBlank()) {
            emptyList()
        } else {
            MockDataRepository.allMockSongs.filter {
                it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentSearches = MutableStateFlow(
        listOf("Arijit Singh", "Heavy Thunderstorm", "Pritam", "Rain Therapy", "Focus Beats")
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
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
