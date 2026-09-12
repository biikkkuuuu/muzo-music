package com.biikkkuuuu.muzi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.biikkkuuuu.muzi.BuildConfig
import com.biikkkuuuu.muzi.R
import com.biikkkuuuu.muzi.ui.component.Material3SettingsGroup
import com.biikkkuuuu.muzi.ui.component.Material3SettingsItem
import com.biikkkuuuu.muzi.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDialoge(
    onDismissRequest: () -> Unit,
    onNavigate: (String) -> Unit,
    homeViewModel: HomeViewModel
) {
    val uriHandler = LocalUriHandler.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        val onSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Material3SettingsGroup(
                title = "Muzi Music",
                compact = true,
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(androidx.compose.ui.res.stringResource(R.string.ai_lyrics_translation)) },
                        description = { Text(androidx.compose.ui.res.stringResource(R.string.setting_desc_ai)) },
                        customIcon = {
                            Text(
                                text = "Ai",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        },
                        onClick = {
                            onDismissRequest()
                            onNavigate("settings/ai")
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(androidx.compose.ui.res.stringResource(R.string.settings)) },
                        description = { Text(androidx.compose.ui.res.stringResource(R.string.setting_desc_settings_main)) },
                        icon = painterResource(R.drawable.settings),
                        onClick = { 
                            onDismissRequest()
                            onNavigate("settings") 
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text("About") },
                        icon = painterResource(R.drawable.info),
                        trailingContent = { 
                            Text(
                                BuildConfig.VERSION_NAME, 
                                style = MaterialTheme.typography.bodyMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        onClick = { 
                            onDismissRequest()
                            onNavigate("settings/about") 
                        }
                    )
                )
            )

            // Footer Links
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Muzi Music on GitHub",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSecondaryColor,
                    modifier = Modifier
                        .clickable { uriHandler.openUri("https://github.com/biikkkuuuu/muzi-music") }
                        .padding(4.dp)
                )
            }
        }
    }
}
