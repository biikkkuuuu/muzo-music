package com.example.muzo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM playback_history ORDER BY playCount DESC, timestamp DESC LIMIT 50")
    fun getTop50Songs(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getSong(videoId: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: HistoryEntity)

    @Transaction
    suspend fun recordPlay(videoId: String, title: String, artist: String, thumbnailUrl: String) {
        val existing = getSong(videoId)
        val newCount = (existing?.playCount ?: 0) + 1
        insertOrUpdate(
            HistoryEntity(
                videoId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                timestamp = System.currentTimeMillis(),
                playCount = newCount
            )
        )
    }

    @Query("DELETE FROM playback_history WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
