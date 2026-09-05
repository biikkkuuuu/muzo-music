package com.example.muzo.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedSongDao {
    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun getAllDownloaded(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    suspend fun getAllDownloadedSync(): List<DownloadedSongEntity>

    @Query("SELECT * FROM downloaded_songs WHERE videoId = :videoId LIMIT 1")
    suspend fun getDownloadedSong(videoId: String): DownloadedSongEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE videoId = :videoId)")
    fun isDownloadedFlow(videoId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE videoId = :videoId)")
    suspend fun isDownloaded(videoId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloaded(song: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE videoId = :videoId")
    suspend fun deleteDownloaded(videoId: String)
}
