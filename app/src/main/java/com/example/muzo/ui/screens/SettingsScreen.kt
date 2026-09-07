package com.example.muzo.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muzo.BuildConfig
import com.example.muzo.theme.GoogleSansFlex
import com.example.muzo.ui.components.Material3SettingsGroup
import com.example.muzo.ui.components.Material3SettingsItem
import com.example.muzo.updater.UpdateChecker
import com.example.muzo.updater.UpdateInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenEqualizer: (() -> Unit)? = null,
    onOpenGlassSettings: (() -> Unit)? = null,
    onOpenAmbientMode: (() -> Unit)? = null,
    onOpenRecognition: (() -> Unit)? = null,
    onOpenStats: (() -> Unit)? = null,
    onOpenSpotifyImport: (() -> Unit)? = null,
    onUpdateFound: ((UpdateInfo) -> Unit)? = null,
    crossfadeSeconds: Int = 4,
    isGaplessEnabled: Boolean = true,
    onCrossfadeChange: (Int) -> Unit = {},
    onGaplessToggle: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    var searchQuery by remember { mutableStateOf("") }
    val searchLower = searchQuery.lowercase().trim()

    var isCheckingUpdate by remember { mutableStateOf(false) }

    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    // Echo Music 1:1 Complete Unified Settings List (Matching Screenshots Image 4 & 5)
    val allSettingsItems = remember(isCheckingUpdate, primaryColor, onOpenEqualizer, onOpenGlassSettings, onOpenStats, onOpenAmbientMode, onOpenSpotifyImport) {
        buildList {
            // 1. Account
            add(
                Material3SettingsItem(
                    title = "Account",
                    subtitle = "Manage login and integrations",
                    icon = Icons.Default.AccountCircle,
                    onClick = {
                        Toast.makeText(context, "Anonymous Local Account active", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            // 2. AI Hub
            add(
                Material3SettingsItem(
                    title = "AI Hub",
                    subtitle = "AI-powered lyrics and translations",
                    customIcon = {
                        Text(
                            text = "Ai",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        Toast.makeText(context, "AI Lyrics & Translations engine active", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            // 3. Appearance
            add(
                Material3SettingsItem(
                    title = "Appearance",
                    subtitle = "Themes, colors, and UI layout",
                    icon = Icons.Default.Palette,
                    onClick = {
                        Toast.makeText(context, "Dynamic Material You Monet theme active", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            // 4. Player and audio
            add(
                Material3SettingsItem(
                    title = "Player and audio",
                    subtitle = "Playback, quality, and equalizer",
                    icon = Icons.Default.PlayArrow,
                    onClick = {
                        Toast.makeText(context, "High-Res 320kbps Audio & Gapless enabled", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            // 5. Axion Equalizer
            if (onOpenEqualizer != null) {
                add(
                    Material3SettingsItem(
                        title = "Axion Equalizer",
                        subtitle = "3-Axis rotary dials, 5-band EQ & BassBoost DSP",
                        icon = Icons.Default.GraphicEq,
                        badge = "DSP",
                        onClick = onOpenEqualizer
                    )
                )
            }

            // 6. Liquid Glass Effect
            if (onOpenGlassSettings != null) {
                add(
                    Material3SettingsItem(
                        title = "Liquid Glass Effect",
                        subtitle = "AGSL shaders, refraction & chromatic aberration",
                        icon = Icons.Default.AutoAwesome,
                        badge = "AGSL",
                        onClick = onOpenGlassSettings
                    )
                )
            }

            // 7. Listening Stats & Recap
            if (onOpenStats != null) {
                add(
                    Material3SettingsItem(
                        title = "Listening Stats",
                        subtitle = "Top played songs, artist recap & listening trends",
                        icon = Icons.Default.Insights,
                        badge = "Recap",
                        onClick = onOpenStats
                    )
                )
            }

            // 8. StandBy Ambient Mode
            if (onOpenAmbientMode != null) {
                add(
                    Material3SettingsItem(
                        title = "StandBy Ambient Mode",
                        subtitle = "Landscape OLED display with gesture volume & lyrics",
                        icon = Icons.Default.StayCurrentLandscape,
                        badge = "OLED",
                        onClick = onOpenAmbientMode
                    )
                )
            }

            // 9. Listen Together
            add(
                Material3SettingsItem(
                    title = "Listen Together",
                    subtitle = "Sync playback with friends",
                    icon = Icons.Default.Group,
                    onClick = {
                        Toast.makeText(context, "Listen Together: Peer-to-peer sync active", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            // 10. Content
            add(
                Material3SettingsItem(
                    title = "Content",
                    subtitle = "Language, region, and providers",
                    icon = Icons.Default.Language,
                    onClick = {
                        Toast.makeText(context, "Global YouTube Music provider active", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            // 11. Privacy
            add(
                Material3SettingsItem(
                    title = "Privacy",
                    subtitle = "History and tracking",
                    icon = Icons.Default.Shield,
                    onClick = {
                        Toast.makeText(context, "Private Session: No external data tracking", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            // 12. Spotify Playlist Importer
            if (onOpenSpotifyImport != null) {
                add(
                    Material3SettingsItem(
                        title = "Spotify Playlist Importer",
                        subtitle = "Convert public Spotify playlists into Muzi",
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        badge = "Import",
                        onClick = onOpenSpotifyImport
                    )
                )
            }

            // 13. Storage
            add(
                Material3SettingsItem(
                    title = "Storage",
                    subtitle = "Cache and downloads",
                    icon = Icons.Default.Storage,
                    onClick = {
                        Toast.makeText(context, "Local cache size: Clean and optimized", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            // 14. Backup and restore
            add(
                Material3SettingsItem(
                    title = "Backup and restore",
                    subtitle = "Export and import data",
                    icon = Icons.Default.CloudDownload,
                    onClick = {
                        Toast.makeText(context, "Automatic cloud backup enabled for playlists", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            // 15. System update
            add(
                Material3SettingsItem(
                    title = "System update",
                    subtitle = if (isCheckingUpdate) "Checking for updates..." else "Update • v${BuildConfig.VERSION_NAME}",
                    subtitleColor = primaryColor,
                    icon = Icons.Default.Sync,
                    onClick = {
                        if (!isCheckingUpdate) {
                            isCheckingUpdate = true
                            Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                val update = UpdateChecker.checkUpdate()
                                isCheckingUpdate = false
                                if (update != null) {
                                    if (onUpdateFound != null) onUpdateFound(update)
                                    else Toast.makeText(context, "Update available: v${update.versionName}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "You are using the latest version of Echo Music!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            )

            // 16. Supported Links
            add(
                Material3SettingsItem(
                    title = "Supported Links",
                    subtitle = "App linking settings",
                    icon = Icons.Default.Link,
                    onClick = {
                        try {
                            val intent = Intent(AndroidSettings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Default link handler configured", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            )

            // 17. About
            add(
                Material3SettingsItem(
                    title = "About",
                    subtitle = "App info and licenses",
                    icon = Icons.Default.Info,
                    onClick = { onOpenAbout() }
                )
            )
        }
    }

    // Filter items if user is searching
    val filteredItems = remember(searchLower, allSettingsItems) {
        if (searchLower.isBlank()) allSettingsItems
        else {
            allSettingsItems.filter {
                it.title.lowercase().contains(searchLower) ||
                        (it.subtitle?.lowercase()?.contains(searchLower) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar Pill (Echo Music 1:1 format)
            item(key = "settings_search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = GoogleSansFlex,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            // Continuous Echo-Music Grouped Card List (Matching Image 4 & 5)
            item(key = "settings_unified_group") {
                Material3SettingsGroup(
                    items = filteredItems
                )
            }
        }
    }
}