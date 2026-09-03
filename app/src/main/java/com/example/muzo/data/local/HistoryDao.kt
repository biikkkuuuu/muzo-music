package com.example.muzo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: HistoryEntity)

    @Query("DELETE FROM playback_history WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
