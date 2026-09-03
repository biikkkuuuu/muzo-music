package com.example.muzo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
