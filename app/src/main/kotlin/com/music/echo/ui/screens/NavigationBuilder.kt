

package com.biikkkuuuu.muzi.ui.screens

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.biikkkuuuu.muzi.constants.DarkModeKey
import com.biikkkuuuu.muzi.constants.PureBlackKey
import com.biikkkuuuu.muzi.ui.screens.artist.ArtistAlbumsScreen
import com.biikkkuuuu.muzi.ui.screens.artist.ArtistItemsScreen
import com.biikkkuuuu.muzi.ui.screens.artist.ArtistScreen
import com.biikkkuuuu.muzi.ui.screens.artist.ArtistSongsScreen
import com.biikkkuuuu.muzi.ui.screens.equalizer.EqScreen
import com.biikkkuuuu.muzi.ui.screens.library.LibraryScreen
import com.biikkkuuuu.muzi.ui.screens.library.LocalSongScreen
import com.biikkkuuuu.muzi.ui.screens.playlist.AutoPlaylistScreen
import com.biikkkuuuu.muzi.ui.screens.playlist.CachePlaylistScreen
import com.biikkkuuuu.muzi.ui.screens.playlist.LocalPlaylistScreen
import com.biikkkuuuu.muzi.ui.screens.playlist.OnlinePlaylistScreen
import com.biikkkuuuu.muzi.ui.screens.playlist.TopPlaylistScreen
import com.biikkkuuuu.muzi.ui.screens.search.OnlineSearchResult
import com.biikkkuuuu.muzi.ui.screens.search.SearchScreen
import com.biikkkuuuu.muzi.ui.screens.settings.AboutScreen
import com.biikkkuuuu.muzi.ui.screens.settings.AppearanceSettings
import com.biikkkuuuu.muzi.ui.screens.settings.GlassEffectSettings
import com.biikkkuuuu.muzi.ui.screens.settings.BackupAndRestore
import com.biikkkuuuu.muzi.ui.screens.settings.ContentSettings
import com.biikkkuuuu.muzi.ui.screens.settings.UptimeScreen
import com.biikkkuuuu.muzi.ui.screens.settings.DarkMode
import com.biikkkuuuu.muzi.ui.screens.settings.PlayerSettings
import com.biikkkuuuu.muzi.ui.screens.settings.PrivacySettings
import com.biikkkuuuu.muzi.ui.screens.settings.RomanizationSettings
import com.biikkkuuuu.muzi.ui.screens.settings.SettingsScreen
import com.biikkkuuuu.muzi.ui.screens.settings.AccountSettingsScreen
import com.biikkkuuuu.muzi.ui.screens.settings.StorageSettings
import com.biikkkuuuu.muzi.ui.screens.settings.ThemeScreen
import com.biikkkuuuu.muzi.ui.screens.settings.AiSettings

import com.biikkkuuuu.muzi.ui.screens.settings.integrations.ListenTogetherSettings
import com.biikkkuuuu.muzi.ui.screens.recognition.RecognitionScreen
import com.biikkkuuuu.muzi.ui.screens.recognition.RecognitionHistoryScreen
import com.biikkkuuuu.muzi.ui.screens.settings.UpdateSettings
import com.biikkkuuuu.muzi.echomusic.updater.UpdateScreen
import com.biikkkuuuu.muzi.utils.rememberEnumPreference
import com.biikkkuuuu.muzi.utils.rememberPreference
import com.biikkkuuuu.muzi.echomusic.changelog.ChangelogScreen
import com.biikkkuuuu.muzi.echomusic.commitscreen.CommitScreen
import com.biikkkuuuu.muzi.ui.screens.equalizer.axion.AxionEqScreen
import com.biikkkuuuu.muzi.ui.screens.ambient.AmbientModeScreen

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity,
    snackbarHostState: SnackbarHostState
) {
    composable(Screens.Home.route) {
        HomeScreen(navController = navController, snackbarHostState = snackbarHostState)
    }

    composable(Screens.Search.route) {
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }
        val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }
        SearchScreen(
            navController = navController,
            pureBlack = pureBlack
        )
    }

    composable(Screens.Library.route) {
        LibraryScreen(navController)
    }

    composable(Screens.ListenTogether.route) {
        ListenTogetherScreen(navController, showTopBar = false)
    }

    composable(
        route = "listen_together_from_topbar",
    ) {
        ListenTogetherScreen(navController, showTopBar = true)
    }

    composable("listen_together/chat") {
        CommentTogetherScreen(navController)
    }

    composable("history") {
        HistoryScreen(navController)
    }

    composable("ambient_mode") {
        AmbientModeScreen(navController)
    }

    composable("local_songs") {
        LocalSongScreen(navController)
    }

    composable("stats") {
        StatsScreen(navController)
    }

    composable("mood_and_genres") {
        MoodAndGenresScreen(navController, scrollBehavior)
    }

    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }

    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }

    composable("charts_screen") {
        ChartsScreen(navController)
    }

    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            }
        )
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId")
        )
    }

    composable(
        route = "search/{query}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            },
        ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) {
        OnlineSearchResult(navController)
    }

    composable(
        route = "album/{albumId}",
        arguments = listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/songs",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "cache_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "top_playlist/{top}",
        arguments = listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        YouTubeBrowseScreen(navController)
    }

    composable("settings") {
        SettingsScreen(navController, scrollBehavior)
    }


    composable(
        route = "settings/update?highlightKey={highlightKey}",
        arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true })
    ) { backStackEntry ->
       UpdateSettings(navController, scrollBehavior, highlightKey = backStackEntry.arguments?.getString("highlightKey"))
    }

    composable(
        route = "settings/account?highlightKey={highlightKey}",
        arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true })
    ) { backStackEntry ->
        AccountSettingsScreen(navController, scrollBehavior, highlightKey = backStackEntry.arguments?.getString("highlightKey"))
    }

    composable(
        route = "settings/appearance?highlightKey={highlightKey}",
        arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true })
    ) { backStackEntry ->
        AppearanceSettings(navController, scrollBehavior, activity, snackbarHostState, highlightKey = backStackEntry.arguments?.getString("highlightKey"))
    }

    composable("settings/appearance/theme") {
        ThemeScreen(navController)
    }

    composable("settings/appearance/liquidglass") {
        GlassEffectSettings(navController, scrollBehavior)
    }

    composable(
        route = "settings/content?highlightKey={highlightKey}",
        arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true })
    ) { backStackEntry ->
        ContentSettings(navController, scrollBehavior, highlightKey = backStackEntry.arguments?.getString("highlightKey"))
    }

    composable("uptime") {
        UptimeScreen(navController, scrollBehavior)
    }

    composable("settings/content/romanization") {
        RomanizationSettings(navController, scrollBehavior)
    }

    composable(
        route = "settings/ai?highlightKey={highlightKey}",
        arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true })
    ) { backStackEntry ->
        AiSettings(navController, scrollBehavior, highlightKey = backStackEntry.arguments?.getString("highlightKey"))
    }
    

    composable(
        route = "settings/player?highlightKey={highlightKey}",
        arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true })
    ) { backStackEntry ->
        PlayerSettings(navController, scrollBehavior, highlightKey = backStackEntry.arguments?.getString("highlightKey"))
    }

    composable(
        route = "settings/storage?autoOpenExportPicker={autoOpenExportPicker}&highlightKey={highlightKey}",
        arguments = listOf(
            navArgument("autoOpenExportPicker") {
                type = NavType.BoolType
                defaultValue = false
            },
            navArgument("highlightKey") { type = NavType.StringType; nullable = true }
        )
    ) { backStackEntry ->
        val autoOpenExportPicker =
            backStackEntry.arguments?.getBoolean("autoOpenExportPicker") ?: false
        StorageSettings(
            navController = navController,
            scrollBehavior = scrollBehavior,
            autoOpenExportPicker = autoOpenExportPicker,
            highlightKey = backStackEntry.arguments?.getString("highlightKey")
        )
    }

    composable("settings/equalizer") {
        AxionEqScreen(onBackClick = { navController.navigateUp() })
    }

    composable(
        route = "settings/privacy?highlightKey={highlightKey}",
        arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true })
    ) { backStackEntry ->
        PrivacySettings(navController, scrollBehavior, highlightKey = backStackEntry.arguments?.getString("highlightKey"))
    }

    composable(
        route = "settings/backup_restore?highlightKey={highlightKey}",
        arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true })
    ) { backStackEntry ->
        BackupAndRestore(navController, scrollBehavior, highlightKey = backStackEntry.arguments?.getString("highlightKey"))
    }

    composable("settings/discord") {
        com.biikkkuuuu.muzi.ui.screens.settings.DiscordSettings(navController, scrollBehavior)
    }

    composable("settings/lastfm") {
        com.biikkkuuuu.muzi.ui.screens.settings.LastFMSettingsScreen(navController)
    }

    composable("settings/discord/experimental") {
        com.biikkkuuuu.muzi.ui.screens.settings.DiscordExperimental(navController)
    }

    composable("settings/spotify_import") {
        SpotifyImportScreen(navController)
    }

    composable(route = "settings/integrations/listen_together") {
        ListenTogetherSettings(navController, scrollBehavior)
    }

    composable(
        route = "settings/about?highlightKey={highlightKey}",
        arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true })
    ) { backStackEntry ->
        AboutScreen(navController, scrollBehavior, highlightKey = backStackEntry.arguments?.getString("highlightKey"))
    }

    composable("update") {
        UpdateScreen(navController)
    }

    composable("login") {
        LoginScreen(navController)
    }

    dialog("equalizer") {
        EqScreen(navController = navController)
    }

    composable("recognition") {
        RecognitionScreen(navController)
    }

    composable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }
    composable("settings/changelog") {
        ChangelogScreen(navController,scrollBehavior)
    }
    composable("settings/commits") {
        CommitScreen(navController, scrollBehavior)
    }
}
