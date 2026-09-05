package com.example.muzo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: Int?,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)

fun DownloadedSongEntity.toSongItem(): com.music.innertube.models.SongItem = com.music.innertube.models.SongItem(
    id = videoId,
    title = title,
    artists = listOf(com.music.innertube.models.Artist(name = artist, id = null)),
    album = null,
    duration = duration ?: 0,
    thumbnail = thumbnailUrl ?: ""
)
