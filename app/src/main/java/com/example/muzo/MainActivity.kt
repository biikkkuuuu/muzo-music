package com.example.muzo

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muzo.mock.MockHomeViewModel
import com.example.muzo.mock.MockLibraryViewModel
import com.example.muzo.mock.MockPlayerViewModel
import com.example.muzo.mock.MockSearchViewModel
import com.example.muzo.theme.MetrolistTheme
import com.example.muzo.ui.component.MetrolistNavBar
import com.example.muzo.ui.player.MetrolistFullPlayer
import com.example.muzo.ui.player.MetrolistMiniPlayer
import com.example.muzo.ui.screens.HomeScreen
import com.example.muzo.ui.screens.LibraryScreen
import com.example.muzo.ui.screens.SearchScreen
import com.example.muzo.ui.screens.SettingsScreen
import com.example.muzo.ui.screens.ThemeScreen
import com.example.muzo.data.local.MuziDatabase
import com.example.muzo.playback.MuziMediaSessionService
import com.example.muzo.playback.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge transparent system bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        setContent {
            val db = MuziDatabase.getInstance(applicationContext)
            val player = MuziMediaSessionService.getPlayer(applicationContext)
            val realPlayerViewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.Factory(applicationContext, db.historyDao(), db.likedSongDao(), player)
            )
            val playerViewModel: MockPlayerViewModel = viewModel(
                factory = MockPlayerViewModel.Factory(realPlayerViewModel)
            )
            val homeViewModel: MockHomeViewModel = viewModel()
            val searchViewModel: MockSearchViewModel = viewModel()
            val libraryViewModel: MockLibraryViewModel = viewModel()

            val selectedPalette by playerViewModel.selectedPalette.collectAsState()
            val pureBlack by playerViewModel.pureBlack.collectAsState()
            val useDynamicColor by playerViewModel.useDynamicColor.collectAsState()

            MetrolistTheme(
                pureBlack = true,
                useDynamicColor = false,
                themeColor = selectedPalette.color
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MetrolistApp(
                        playerViewModel = playerViewModel,
                        homeViewModel = homeViewModel,
                        searchViewModel = searchViewModel,
                        libraryViewModel = libraryViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MetrolistApp(
    playerViewModel: MockPlayerViewModel,
    homeViewModel: MockHomeViewModel,
    searchViewModel: MockSearchViewModel,
    libraryViewModel: MockLibraryViewModel
) {
    val currentTab by playerViewModel.currentTab.collectAsState()
    val isPlayerExpanded by playerViewModel.isPlayerExpanded.collectAsState()
    val isThemeSettingsOpen by playerViewModel.isThemeSettingsOpen.collectAsState()
    val isSettingsOpen by playerViewModel.isSettingsOpen.collectAsState()

    // Back handling
    BackHandler(enabled = isThemeSettingsOpen || isSettingsOpen || isPlayerExpanded || currentTab != 0) {
        when {
            isThemeSettingsOpen -> playerViewModel.closeThemeSettings()
            isSettingsOpen -> playerViewModel.closeSettings()
            isPlayerExpanded -> playerViewModel.closePlayer()
            currentTab != 0 -> playerViewModel.setTab(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen Tabs with Fluid Crossfade Transition
        Crossfade(targetState = currentTab, label = "TabCrossfade") { tab ->
            when (tab) {
                0 -> HomeScreen(
                    homeViewModel = homeViewModel,
                    playerViewModel = playerViewModel
                )
                1 -> SearchScreen(
                    searchViewModel = searchViewModel,
                    playerViewModel = playerViewModel
                )
                2 -> LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel
                )
            }
        }

        // Floating Bottom Dock (MiniPlayer + Floating Pill Navigation Bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MetrolistMiniPlayer(viewModel = playerViewModel)

            MetrolistNavBar(
                selectedTab = currentTab,
                onTabSelected = { playerViewModel.setTab(it) },
                onThemeClick = { playerViewModel.openThemeSettings() }
            )
        }

        // Full Expandable Player Bottom Sheet with Live Synced Lyrics
        if (isPlayerExpanded) {
            MetrolistFullPlayer(viewModel = playerViewModel)
        }

        // Fullscreen Theme Switcher (19 Palettes + AMOLED + Dynamic Color)
        AnimatedVisibility(
            visible = isThemeSettingsOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            ThemeScreen(
                viewModel = playerViewModel,
                onBack = { playerViewModel.closeThemeSettings() }
            )
        }

        // Settings Screen
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            SettingsScreen(
                onBack = { playerViewModel.closeSettings() },
                onOpenThemes = {
                    playerViewModel.closeSettings()
                    playerViewModel.openThemeSettings()
                }
            )
        }
    }
}
