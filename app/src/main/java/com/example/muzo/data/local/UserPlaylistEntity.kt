package com.example.muzo.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_playlists")
data class UserPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0
)

@Entity(
    tableName = "user_playlist_songs",
    primaryKeys = ["playlistId", "videoId"]
)
data class UserPlaylistSongEntity(
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val durationText: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface UserPlaylistDao {
    @Query("SELECT * FROM user_playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<UserPlaylistEntity>>

    @Query("SELECT * FROM user_playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): UserPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: UserPlaylistEntity): Long

    @Query("DELETE FROM user_playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM user_playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<UserPlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(song: UserPlaylistSongEntity)

    @Query("DELETE FROM user_playlist_songs WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeSongFromPlaylist(playlistId: Long, videoId: String)

    @Query("UPDATE user_playlists SET songCount = (SELECT COUNT(*) FROM user_playlist_songs WHERE playlistId = :playlistId), coverUrl = (SELECT thumbnailUrl FROM user_playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt DESC LIMIT 1) WHERE id = :playlistId")
    suspend fun updatePlaylistMetadata(playlistId: Long)
}
