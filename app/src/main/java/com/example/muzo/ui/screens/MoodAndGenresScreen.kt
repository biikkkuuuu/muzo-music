package com.example.muzo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAndGenresScreen(
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val moods = listOf(
        "Chill", "Commute", "Energize", "Feel good",
        "Focus", "Gaming", "Party", "Romance",
        "Sad", "Sleep", "Workout"
    )

    val genres = listOf(
        "African", "Arabic", "Bengali", "Bhojpuri",
        "Carnatic classical", "Classical", "Country & Americana", "Dance & electronic",
        "Hindi", "Hindustani classical", "Hip-hop", "Indian indie",
        "Indian pop", "Indie & alternative", "J-Pop", "Jazz",
        "K-Pop", "Kannada", "Latin", "Malayalam",
        "Marathi", "Metal", "Monsoon", "Pop",
        "Punjabi", "R&B & soul", "Reggae & caribbean", "Rock",
        "Tamil", "Telugu"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mood and Genres",
                        fontWeight = FontWeight.Bold,
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
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Moods & moments
            item {
                Text(
                    text = "Moods & moments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B8AFD)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in moods.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryPill(title = moods[i], modifier = Modifier.weight(1f), onClick = { onCategoryClick(moods[i]) })
                            if (i + 1 < moods.size) {
                                CategoryPill(title = moods[i + 1], modifier = Modifier.weight(1f), onClick = { onCategoryClick(moods[i + 1]) })
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 2. Genres
            item {
                Text(
                    text = "Genres",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B8AFD)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in genres.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryPill(title = genres[i], modifier = Modifier.weight(1f), onClick = { onCategoryClick(genres[i]) })
                            if (i + 1 < genres.size) {
                                CategoryPill(title = genres[i + 1], modifier = Modifier.weight(1f), onClick = { onCategoryClick(genres[i + 1]) })
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E24)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}
