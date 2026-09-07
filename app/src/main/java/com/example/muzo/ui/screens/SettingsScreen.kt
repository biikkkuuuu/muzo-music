package com.example.muzo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muzo.theme.GoogleSansFlex
import com.example.muzo.ui.utils.bounceClick

data class MetrolistSettingRow(
    val icon: ImageVector? = null,
    val customBadge: String? = null,
    val title: String,
    val subtitle: String,
    val subtextColor: Color? = null,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenThemes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val settingItems = remember {
        listOf(
            MetrolistSettingRow(Icons.Default.Person, null, "Account", "Manage login and integrations") {},
            MetrolistSettingRow(null, "Ai", "AI Hub", "AI-powered lyrics and translations") {},
            MetrolistSettingRow(Icons.Default.PlayArrow, null, "Player and audio", "Playback, quality, and equalizer") {},
            MetrolistSettingRow(Icons.Default.Group, null, "Listen Together", "Sync playback with friends") {},
            MetrolistSettingRow(Icons.Default.Language, null, "Content", "Language, region, and providers") {},
            MetrolistSettingRow(Icons.Default.Security, null, "Privacy", "History and tracking") {},
            MetrolistSettingRow(Icons.Default.Storage, null, "Storage", "Cache and downloads") {},
            MetrolistSettingRow(Icons.Default.CloudUpload, null, "Backup and restore", "Export and import data") {},
            MetrolistSettingRow(Icons.Default.Update, null, "System update", "Update", subtextColor = Color(0xFFE57373)) {},
            MetrolistSettingRow(Icons.Default.Link, null, "Supported Links", "App linking settings") {},
            MetrolistSettingRow(Icons.Default.Info, null, "About", "v1.2.2 • App info and licenses") {}
        )
    }

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) settingItems
        else settingItems.filter { it.title.contains(searchQuery, true) || it.subtitle.contains(searchQuery, true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pill Search Bar matching Screenshots 4 & 5
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search",
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
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Continuous Grouped Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column {
                    filtered.forEachIndexed { index, item ->
                        SettingRowItem(item = item)

                        if (index < filtered.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRowItem(item: MetrolistSettingRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle 40dp Icon Container with 10% Primary Background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (item.customBadge != null) {
                Text(
                    text = item.customBadge,
                    fontFamily = GoogleSansFlex,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                fontFamily = GoogleSansFlex,
                fontSize = 12.5.sp,
                color = item.subtextColor ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
