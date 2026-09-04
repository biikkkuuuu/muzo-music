package com.example.muzo.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.muzo.BuildConfig
import com.example.muzo.R

@Composable
fun WelcomeDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF14131A)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp, horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. App Header Card (Red vinyl disc icon + Muzi Music + Version)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1C1B24)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Vinyl disc icon
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFB71C1C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "Muzi Music",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 22.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // 2. Follow Developer Section
                WelcomeSectionCard(title = "Follow Developer") {
                    WelcomeActionRow(
                        icon = painterResource(R.drawable.ic_instagram_new),
                        title = "Instagram",
                        subtitle = "@biikkkuuuu",
                        onClick = { openUrl("https://instagram.com/biikkkuuuu") }
                    )
                    WelcomeDivider()
                    WelcomeActionRow(
                        icon = painterResource(R.drawable.ic_telegram_new),
                        title = "Telegram",
                        subtitle = "@biikkkuuuuu",
                        onClick = { openUrl("https://t.me/biikkkuuuuu") }
                    )
                    WelcomeDivider()
                    WelcomeActionRow(
                        icon = painterResource(R.drawable.github),
                        title = "GitHub",
                        subtitle = "biikkkuuuu",
                        onClick = { openUrl("https://github.com/biikkkuuuu") }
                    )
                }

                // 3. Sarcastic Note Card (Free Tier Reality Check & Telegram Bug Report)
                WelcomeSectionCard(title = "Free Tier Reality Check ☕") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bro, remember you're streaming 100% ad-free music straight from YouTube without paying a single penny. A minor bug or stream buffer is totally justifiable! 😉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "Found a real bug? Don't cry in reviews—report it directly to the dev:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                    }
                    WelcomeDivider()
                    WelcomeActionRow(
                        icon = painterResource(R.drawable.ic_telegram_new),
                        title = "Report Bug on Telegram",
                        subtitle = "@biikkkuuuuu",
                        onClick = { openUrl("https://t.me/biikkkuuuuu") }
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 4. Star the Repo Button
                Button(
                    onClick = { openUrl("https://github.com/biikkkuuuu/muzo-music") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF262530),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Star the Repo", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                // 5. Continue Button (Primary Blue)
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun WelcomeSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1C1B24),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun WelcomeActionRow(
    icon: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2B2A36)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun WelcomeDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.06f),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
