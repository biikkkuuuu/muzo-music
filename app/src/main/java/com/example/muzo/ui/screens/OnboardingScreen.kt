package com.example.muzo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muzo.data.UserPreferencesManager

@Composable
fun OnboardingScreen(prefsManager: UserPreferencesManager, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("What's your vibe?", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select your favorite regional genres", color = Color.Gray, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            val genres = listOf("Bollywood", "Bhojpuri", "Magahi", "Khortha", "Punjabi", "Haryanvi", "Hip Hop", "Lo-Fi", "Devotional", "Pop", "EDM", "Acoustic", "Indie")
            var selected by remember { mutableStateOf(setOf<String>()) }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).padding(vertical = 8.dp)
            ) {
                items(genres.size) { index ->
                    val genre = genres[index]
                    val isSelected = selected.contains(genre)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) Color(0xFF6200EA) else Color.DarkGray, shape = RoundedCornerShape(12.dp))
                            .clickable { selected = if (isSelected) selected - genre else selected + genre }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(genre, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { 
                    if (selected.isNotEmpty()) {
                        prefsManager.saveGenres(selected.toList())
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Let's Go", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
