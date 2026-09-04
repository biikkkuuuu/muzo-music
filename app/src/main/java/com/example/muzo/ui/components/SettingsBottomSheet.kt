package com.example.muzo.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muzo.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1D24),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 20.dp)
        ) {
            // Header Title: Matches Screenshot 1
            Text(
                text = "Muzi Music",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Account (Blue header)
            Text(
                text = "Account",
                color = Color(0xFF5B8DEF),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF282732)
            ) {
                Column {
                    SettingsSheetItem(
                        icon = Icons.Default.Person,
                        isCircleIcon = true,
                        title = "Anonymous",
                        subtitle = "Not Logged In",
                        onClick = {
                            Toast.makeText(context, "Logged in as Anonymous (Free Tier)", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    SettingsSheetItem(
                        icon = Icons.Default.AutoAwesome,
                        customBadge = "Ai",
                        title = "AI Hub",
                        subtitle = "AI-powered lyrics and translations",
                        onClick = {
                            Toast.makeText(context, "AI Hub: Smart lyrics synchronization enabled", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 2: App (Blue header)
            Text(
                text = "App",
                color = Color(0xFF5B8DEF),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF282732)
            ) {
                Column {
                    SettingsSheetItem(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        subtitle = "App preferences and configurations",
                        onClick = {
                            onDismiss()
                            onOpenSettings()
                        }
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    SettingsSheetItem(
                        icon = Icons.Default.Info,
                        isCircleIcon = true,
                        title = "About",
                        subtitle = "v${BuildConfig.VERSION_NAME}",
                        onClick = {
                            onDismiss()
                            onOpenAbout()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Footer Links
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy Policy",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/biikkkuuuu/muzo-music")))
                        } catch (_: Exception) {}
                    }
                )

                Text(
                    text = "  •  ",
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                Text(
                    text = "Terms of Service",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/biikkkuuuu/muzo-music")))
                        } catch (_: Exception) {}
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSheetItem(
    icon: ImageVector,
    isCircleIcon: Boolean = false,
    customBadge: String? = null,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container matching blue badge in screenshot
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(if (isCircleIcon) CircleShape else RoundedCornerShape(10.dp))
                .background(Color(0xFF233660)),
            contentAlignment = Alignment.Center
        ) {
            if (customBadge != null) {
                Text(
                    text = customBadge,
                    color = Color(0xFF5B8DEF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF5B8DEF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color(0xFF9E9EA8)
            )
        }
    }
}
