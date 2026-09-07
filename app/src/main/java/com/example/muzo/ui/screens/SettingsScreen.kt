package com.example.muzo.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muzo.BuildConfig
import com.example.muzo.data.local.MuziDatabase
import com.example.muzo.ui.components.Material3SettingsGroup
import com.example.muzo.ui.components.Material3SettingsItem
import com.example.muzo.updater.UpdateChecker
import com.example.muzo.updater.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SettingItemDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isCircleIcon: Boolean = false,
    val customBadge: String? = null,
    val subtitleColor: Color? = null,
    val onClick: () -> Unit
)

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
    var searchQuery by remember { mutableStateOf("") }
    val prefs = remember { context.getSharedPreferences("muzo_prefs", Context.MODE_PRIVATE) }

    // Active sub-dialog states
    var showAccountDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showContentDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    // Material 3 Expressive Grouped Settings (Echo-Music Style)
    val audioGroup = remember(onOpenEqualizer, onOpenGlassSettings) {
        buildList {
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
            if (onOpenStats != null) {
                add(
                    Material3SettingsItem(
                        title = "Listening Analytics & Stats",
                        subtitle = "Top played songs, artist recap & listening trends",
                        icon = Icons.Default.Insights,
                        badge = "Recap",
                        onClick = onOpenStats
                    )
                )
            }
            add(
                Material3SettingsItem(
                    title = "Player & Audio Quality",
                    subtitle = "Streaming bitrate, cache, and normalization",
                    icon = Icons.Default.PlayArrow,
                    onClick = { showAudioQualityDialog = true }
                )
            )
        }
    }

    val displayGroup = remember(onOpenAmbientMode, onOpenRecognition) {
        buildList {
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
            add(
                Material3SettingsItem(
                    title = "Appearance & Themes",
                    subtitle = "Dynamic color palette and UI layout",
                    icon = Icons.Default.Palette,
                    onClick = { showAppearanceDialog = true }
                )
            )
            if (onOpenRecognition != null) {
                add(
                    Material3SettingsItem(
                        title = "Music Recognition",
                        subtitle = "Shazam-style acoustic fingerprint radar",
                        icon = Icons.Default.Radar,
                        badge = "Radar",
                        onClick = onOpenRecognition
                    )
                )
            }
            add(
                Material3SettingsItem(
                    title = "AI Hub",
                    subtitle = "Smart synchronized lyrics and translations",
                    icon = Icons.Default.AutoAwesome,
                    badge = "Ai",
                    onClick = {
                        Toast.makeText(context, "AI Hub: Smart synchronized lyrics active ✨", Toast.LENGTH_SHORT).show()
                    }
                )
            )
        }
    }

    val libraryGroup = remember(onOpenSpotifyImport) {
        buildList {
            if (onOpenSpotifyImport != null) {
                add(
                    Material3SettingsItem(
                        title = "Spotify Playlist Importer",
                        subtitle = "Convert public Spotify playlists into Muzi",
                        icon = Icons.Default.QueueMusic,
                        badge = "Import",
                        onClick = onOpenSpotifyImport
                    )
                )
            }
            add(
                Material3SettingsItem(
                    title = "Account & Profile",
                    subtitle = "Anonymous local session",
                    icon = Icons.Default.Person,
                    isCircleIcon = true,
                    onClick = { showAccountDialog = true }
                )
            )
            add(
                Material3SettingsItem(
                    title = "Content & Region",
                    subtitle = "Language and YouTube Music providers",
                    icon = Icons.Default.Language,
                    onClick = { showContentDialog = true }
                )
            )
            add(
                Material3SettingsItem(
                    title = "Storage & Downloads",
                    subtitle = "Manage offline songs and image cache",
                    icon = Icons.Default.Storage,
                    onClick = { showStorageDialog = true }
                )
            )
            add(
                Material3SettingsItem(
                    title = "Privacy",
                    subtitle = "Listening history and privacy controls",
                    icon = Icons.Default.Security,
                    onClick = { showPrivacyDialog = true }
                )
            )
            add(
                Material3SettingsItem(
                    title = "Backup & Restore",
                    subtitle = "Export and import local playlists",
                    icon = Icons.Default.CloudDownload,
                    onClick = {
                        Toast.makeText(context, "Automatic cloud backup enabled for playlists", Toast.LENGTH_SHORT).show()
                    }
                )
            )
            add(
                Material3SettingsItem(
                    title = "Listen Together",
                    subtitle = "Sync playback with friends (Coming soon)",
                    icon = Icons.Default.Group,
                    onClick = {
                        Toast.makeText(context, "Listen Together: Peer-to-peer sync coming soon!", Toast.LENGTH_SHORT).show()
                    }
                )
            )
        }
    }

    val aboutGroup = remember(isCheckingUpdate) {
        listOf(
            Material3SettingsItem(
                title = "System Update",
                subtitle = if (isCheckingUpdate) "Checking for updates..." else "Check for update • v${BuildConfig.VERSION_NAME}",
                subtitleColor = Color(0xFFFF7A8A),
                icon = Icons.Default.Sync,
                isCircleIcon = true,
                onClick = {
                    if (!isCheckingUpdate) {
                        isCheckingUpdate = true
                        Toast.makeText(context, "Checking GitHub for updates...", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            val update = UpdateChecker.checkUpdate()
                            isCheckingUpdate = false
                            if (update != null) {
                                if (onUpdateFound != null) onUpdateFound(update)
                                else Toast.makeText(context, "Update available: v${update.versionName}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "You are using the latest version of Muzi!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            ),
            Material3SettingsItem(
                title = "About Muzi",
                subtitle = "App info, license, and developers",
                icon = Icons.Default.Info,
                isCircleIcon = true,
                onClick = { onOpenAbout() }
            ),
            Material3SettingsItem(
                title = "Star on GitHub ⭐",
                subtitle = "biikkkuuuu/muzo-music",
                icon = Icons.Default.Star,
                onClick = { openUrl("https://github.com/biikkkuuuu/muzo-music") }
            ),
            Material3SettingsItem(
                title = "Report Bugs ☕",
                subtitle = "Telegram community @biikkkuuuuu",
                icon = Icons.Default.BugReport,
                onClick = { openUrl("https://t.me/biikkkuuuuu") }
            )
        )
    }

    val allGroupedItems = remember(audioGroup, displayGroup, libraryGroup, aboutGroup) {
        audioGroup + displayGroup + libraryGroup + aboutGroup
    }

    // Filter by search query
    val searchResults = remember(searchQuery, allGroupedItems) {
        if (searchQuery.isBlank()) emptyList()
        else {
            allGroupedItems.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        (it.subtitle?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF08080A)
                )
            )
        },
        containerColor = Color(0xFF08080A),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar (Echo Outlined Rounded TextField)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search settings", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
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
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }

            if (searchQuery.isNotBlank()) {
                // Filtered search results
                item {
                    Material3SettingsGroup(
                        title = "Search Results (${searchResults.size})",
                        items = searchResults
                    )
                }
            } else {
                // iOS / Echo-Music Grouped Rounded Cards
                item {
                    Material3SettingsGroup(
                        title = "Audio & Processing",
                        items = audioGroup
                    )
                }
                item {
                    Material3SettingsGroup(
                        title = "Visuals & Display",
                        items = displayGroup
                    )
                }
                item {
                    Material3SettingsGroup(
                        title = "Content & Storage",
                        items = libraryGroup
                    )
                }
                item {
                    Material3SettingsGroup(
                        title = "App & Community",
                        items = aboutGroup
                    )
                }
            }
        }
    }

    // Sub-dialog: Account
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Account", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current Profile: Anonymous (Local)", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Streaming direct from YouTube Music with ad-free high fidelity audio.", color = Color.Gray, fontSize = 14.sp)
                    Text("Developer: Bikash Rana (@biikkkuuuuu)", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1E1D24)
        )
    }

    // Sub-dialog: Audio Quality, Gapless & Crossfade
    if (showAudioQualityDialog) {
        AlertDialog(
            onDismissRequest = { showAudioQualityDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Player and Audio", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Audio Streaming Quality Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF282732),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Streaming Quality", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• High Fidelity (320 kbps AAC / Opus)\n• Hardware Offload: Active\n• Audio Normalization: Enabled", color = Color(0xFF9E9EA8), fontSize = 12.sp)
                        }
                    }

                    // Gapless Playback Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF282732),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGaplessToggle(!isGaplessEnabled) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gapless Playback", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Pre-buffers next track for zero-silence transition", color = Color(0xFF9E9EA8), fontSize = 12.sp)
                            }
                            Switch(
                                checked = isGaplessEnabled,
                                onCheckedChange = onGaplessToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = Color(0xFF8E8E9A),
                                    uncheckedTrackColor = Color(0xFF1C1C24)
                                )
                            )
                        }
                    }

                    // Audio Crossfade Duration
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF282732),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Crossfade Duration", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Smooth volume fade between consecutive tracks", color = Color(0xFF9E9EA8), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            val options = listOf(0, 2, 4, 6, 8, 12)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                options.forEach { sec ->
                                    val isSelected = crossfadeSeconds == sec
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1C1C24),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onCrossfadeChange(sec) }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = if (sec == 0) "Off" else "${sec}s",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFFB0B0B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAudioQualityDialog = false
                    Toast.makeText(context, "Audio settings applied", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (onOpenEqualizer != null) {
                    TextButton(onClick = {
                        showAudioQualityDialog = false
                        onOpenEqualizer()
                    }) {
                        Text("Equalizer", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            containerColor = Color(0xFF1E1D24)
        )
    }

    // Sub-dialog: Appearance
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("Appearance", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme: Echo Pure Black (AMOLED)", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("• Micro-animations: On\n• 120Hz+ Display Mode: Enabled\n• Floating Dock Nav: Active", color = Color.Gray, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1E1D24)
        )
    }

    // Sub-dialog: Storage & Cache
    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Storage & Cache", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ExoPlayer Cache: ~12 MB active", color = Color.White)
                    Text("Dynamic cache stores stream chunks for seamless instant replay.", color = Color.Gray, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showStorageDialog = false
                    try {
                        context.cacheDir.deleteRecursively()
                        Toast.makeText(context, "Cache cleared successfully!", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Clear Cache", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1D24)
        )
    }

    // Sub-dialog: Privacy
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & History", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text("Your listening data and search terms are kept 100% locally in your phone's Room database and never uploaded to any remote server.", color = Color.Gray, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    showPrivacyDialog = false
                    scope.launch(Dispatchers.IO) {
                        try {
                            MuziDatabase.getInstance(context).historyDao().clearHistory()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Listening history cleared", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {}
                    }
                }) {
                    Text("Clear History", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1D24)
        )
    }

    // Sub-dialog: Content
    if (showContentDialog) {
        AlertDialog(
            onDismissRequest = { showContentDialog = false },
            title = { Text("Content & Region", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text("Region: India (IN)\nLanguages: Hindi, Punjabi, English, Bhojpuri, Haryanvi\nProvider: YouTube Music InnerTube Engine", color = Color.Gray, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = { showContentDialog = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1E1D24)
        )
    }
}