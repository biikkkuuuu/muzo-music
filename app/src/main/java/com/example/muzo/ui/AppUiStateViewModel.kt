package com.example.muzo.ui

import androidx.lifecycle.ViewModel
import com.example.muzo.theme.MetrolistThemePalettes
import com.example.muzo.theme.ThemePalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppUiStateViewModel : ViewModel() {

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _isThemeSettingsOpen = MutableStateFlow(false)
    val isThemeSettingsOpen: StateFlow<Boolean> = _isThemeSettingsOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // Theming
    private val _selectedPalette = MutableStateFlow(MetrolistThemePalettes[0])
    val selectedPalette: StateFlow<ThemePalette> = _selectedPalette.asStateFlow()

    private val _pureBlack = MutableStateFlow(true)
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(false)
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    fun setTab(tab: Int) {
        _currentTab.value = tab
    }

    fun openPlayer() {
        _isPlayerExpanded.value = true
    }

    fun closePlayer() {
        _isPlayerExpanded.value = false
    }

    fun openThemeSettings() {
        _isThemeSettingsOpen.value = true
    }

    fun closeThemeSettings() {
        _isThemeSettingsOpen.value = false
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun selectPalette(palette: ThemePalette) {
        _selectedPalette.value = palette
        _useDynamicColor.value = false
    }

    fun togglePureBlack(enabled: Boolean) {
        _pureBlack.value = enabled
    }

    fun toggleDynamicColor(enabled: Boolean) {
        _useDynamicColor.value = enabled
    }
}
