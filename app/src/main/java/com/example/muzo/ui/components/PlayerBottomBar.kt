package com.example.muzo.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muzo.ui.components.floatingtabbar.FloatingTabBar
import com.example.muzo.ui.components.floatingtabbar.FloatingTabBarDefaults
import com.example.muzo.ui.components.floatingtabbar.FloatingTabBarInlineBehavior
import com.example.muzo.ui.components.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.music.innertube.models.SongItem

/**
 * Echo-Music style Floating Tab Bar & Floating Mini Player Dock.
 * Features an iOS-style centered floating pill dock with shared-element
 * transitions, standalone circular search FAB, and docked mini-player accessory.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerWithBottomNav(
    currentSong: SongItem?,
    isPlaying: Boolean,
    currentPosition: Long = 0L,
    duration: Long = 0L,
    hasPrev: Boolean = true,
    hasNext: Boolean = true,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSongClick: () -> Unit,
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollConnection = rememberFloatingTabBarScrollConnection(
        initialIsInline = false,
        inlineBehavior = FloatingTabBarInlineBehavior.Never
    )

    val selectedContentColor = MaterialTheme.colorScheme.primary
    val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingTabBar(
            selectedTabKey = currentTab,
            scrollConnection = scrollConnection,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            inlineAccessory = if (currentSong != null) {
                { accessoryModifier, _ ->
                    FloatingMiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        hasPrev = hasPrev,
                        hasNext = hasNext,
                        isInline = true,
                        onClick = onSongClick,
                        onPlayPause = onPlayPause,
                        onPrev = onPrevious,
                        onNext = onNext,
                        modifier = accessoryModifier
                    )
                }
            } else null,
            expandedAccessory = if (currentSong != null) {
                { accessoryModifier, _ ->
                    FloatingMiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        hasPrev = hasPrev,
                        hasNext = hasNext,
                        isInline = false,
                        onClick = onSongClick,
                        onPlayPause = onPlayPause,
                        onPrev = onPrevious,
                        onNext = onNext,
                        modifier = accessoryModifier.fillMaxWidth()
                    )
                }
            } else null,
            colors = FloatingTabBarDefaults.colors(
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                accessoryBackgroundColor = Color.Transparent
            ),
            contentKey = listOf(currentTab, currentSong?.id, isPlaying, selectedContentColor)
        ) {
            // Grouped Tab 1: Home
            tab(
                key = 0,
                title = {
                    Text(
                        text = "Home",
                        color = if (currentTab == 0) selectedContentColor else unselectedContentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = if (currentTab == 0) selectedContentColor else unselectedContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = { onTabSelected(0) }
            )

            // Grouped Tab 2: Library
            tab(
                key = 2,
                title = {
                    Text(
                        text = "Library",
                        color = if (currentTab == 2) selectedContentColor else unselectedContentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Library",
                        tint = if (currentTab == 2) selectedContentColor else unselectedContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = { onTabSelected(2) }
            )

            // Standalone Tab: Search (Centered circular action pill)
            standaloneTab(
                key = 1,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (currentTab == 1) selectedContentColor else unselectedContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = { onTabSelected(1) }
            )
        }
    }
}