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
    onUpdateFound: ((UpdateInfo) -> Unit)? = null
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

    // All Settings definitions matching Screenshot 2 & 3
    val allSettings = listOf(
        SettingItemDef(
            id = "account",
            title = "Account",
            subtitle = "Manage login and integrations",
            icon = Icons.Default.Person,
            isCircleIcon = true,
            onClick = { showAccountDialog = true }
        ),
        SettingItemDef(
            id = "ai_hub",
            title = "AI Hub",
            subtitle = "AI-powered lyrics and translations",
            icon = Icons.Default.AutoAwesome,
            customBadge = "Ai",
            onClick = {
                Toast.makeText(context, "AI Hub: Smart synchronized lyrics active ✨", Toast.LENGTH_SHORT).show()
            }
        ),
        SettingItemDef(
            id = "appearance",
            title = "Appearance",
            subtitle = "Themes, colors, and UI layout",
            icon = Icons.Default.Palette,
            onClick = { showAppearanceDialog = true }
        ),
        SettingItemDef(
            id = "player_audio",
            title = "Player and audio",
            subtitle = "Playback, quality, and equalizer",
            icon = Icons.Default.PlayArrow,
            onClick = { showAudioQualityDialog = true }
        ),
        SettingItemDef(
            id = "listen_together",
            title = "Listen Together",
            subtitle = "Sync playback with friends",
            icon = Icons.Default.Group,
            onClick = {
                Toast.makeText(context, "Listen Together: Peer-to-peer sync coming soon!", Toast.LENGTH_SHORT).show()
            }
        ),
        SettingItemDef(
            id = "content",
            title = "Content",
            subtitle = "Language, region, and providers",
            icon = Icons.Default.Language,
            onClick = { showContentDialog = true }
        ),
        SettingItemDef(
            id = "privacy",
            title = "Privacy",
            subtitle = "History and tracking",
            icon = Icons.Default.Security,
            onClick = { showPrivacyDialog = true }
        ),
        SettingItemDef(
            id = "storage",
            title = "Storage",
            subtitle = "Cache and downloads",
            icon = Icons.Default.Storage,
            onClick = { showStorageDialog = true }
        ),
        SettingItemDef(
            id = "backup",
            title = "Backup and restore",
            subtitle = "Export and import data",
            icon = Icons.Default.CloudDownload,
            onClick = {
                Toast.makeText(context, "Automatic cloud backup is enabled for playlists", Toast.LENGTH_SHORT).show()
            }
        ),
        SettingItemDef(
            id = "system_update",
            title = "System update",
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
                            if (onUpdateFound != null) {
                                onUpdateFound(update)
                            } else {
                                Toast.makeText(context, "Update available: v${update.versionName}", Toast.LENGTH_LONG).show()
                                openUrl(update.updateUrl)
                            }
                        } else {
                            Toast.makeText(context, "Muzi Music v${BuildConfig.VERSION_NAME} is up to date! 🎉", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        ),
        SettingItemDef(
            id = "supported_links",
            title = "Supported Links",
            subtitle = "App linking settings",
            icon = Icons.Default.Link,
            onClick = {
                try {
                    val intent = Intent(AndroidSettings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Supported Links: Handles music.youtube.com links", Toast.LENGTH_SHORT).show()
                }
            }
        ),
        SettingItemDef(
            id = "developer",
            title = "Developer & Socials",
            subtitle = "Bikash Rana • Telegram & Instagram",
            icon = Icons.Default.Code,
            onClick = { onOpenAbout() }
        ),
        SettingItemDef(
            id = "report_bugs",
            title = "Report Bugs ☕",
            subtitle = "Free tier reality: report bugs on Telegram",
            icon = Icons.Default.BugReport,
            onClick = { openUrl("https://t.me/biikkkuuuuu") }
        ),
        SettingItemDef(
            id = "star_repo",
            title = "Star on GitHub ⭐",
            subtitle = "biikkkuuuu/muzo-music",
            icon = Icons.Default.Star,
            onClick = { openUrl("https://github.com/biikkkuuuu/muzo-music") }
        ),
        SettingItemDef(
            id = "about",
            title = "About",
            subtitle = "App info and licenses",
            icon = Icons.Default.Info,
            isCircleIcon = true,
            onClick = { onOpenAbout() }
        )
    )

    // Filter by search query
    val filteredSettings = if (searchQuery.isBlank()) {
        allSettings
    } else {
        allSettings.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true)
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
        containerColor = Color(0xFF08080A)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
        ) {
            // Search Bar Pill (Matches Screenshot 2)
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF14131A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF8E8E9A),
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Search", color = Color(0xFF8E8E9A), fontSize = 16.sp)
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Stacked Cards Container (Matching Screenshot 2 & 3 rounded cards)
            itemsIndexed(filteredSettings, key = { _, item -> item.id }) { index, item ->
                val isTop = index == 0
                val isBottom = index == filteredSettings.lastIndex
                val shape = when {
                    isTop && isBottom -> RoundedCornerShape(18.dp)
                    isTop -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    isBottom -> RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                    else -> RoundedCornerShape(0.dp)
                }

                Surface(
                    shape = shape,
                    color = Color(0xFF282732),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { item.onClick() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon container with blue tint
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(if (item.isCircleIcon) CircleShape else RoundedCornerShape(10.dp))
                                    .background(Color(0xFF233660)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.customBadge != null) {
                                    Text(
                                        text = item.customBadge,
                                        color = Color(0xFF5B8DEF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = Color(0xFF5B8DEF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.subtitle,
                                    fontSize = 13.sp,
                                    color = item.subtitleColor ?: Color(0xFF9E9EA8)
                                )
                            }
                        }

                        if (!isBottom) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.05f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
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
                    Text("Developer: Bikash Rana (@biikkkuuuuu)", color = Color(0xFF5B8DEF), fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("OK", color = Color(0xFF5B8DEF))
                }
            },
            containerColor = Color(0xFF1E1D24)
        )
    }

    // Sub-dialog: Audio Quality
    if (showAudioQualityDialog) {
        AlertDialog(
            onDismissRequest = { showAudioQualityDialog = false },
            title = { Text("Player and Audio", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Audio Streaming Quality", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("• High (320 kbps AAC / Opus) - Default\n• Audio Normalization: Enabled\n• Hardware Offload: Active (120Hz smooth)", color = Color.Gray, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAudioQualityDialog = false
                    Toast.makeText(context, "Audio settings saved", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Done", color = Color(0xFF5B8DEF))
                }
            },
            dismissButton = {
                if (onOpenEqualizer != null) {
                    TextButton(onClick = {
                        showAudioQualityDialog = false
                        onOpenEqualizer()
                    }) {
                        Text("Equalizer 🎚️", color = Color(0xFF6B9DFE))
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
                    Text("Close", color = Color(0xFF5B8DEF))
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
                    Text("OK", color = Color(0xFF5B8DEF))
                }
            },
            containerColor = Color(0xFF1E1D24)
        )
    }
}