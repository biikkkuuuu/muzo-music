package com.example.muzo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HistoryEntity::class,
        LikedSongEntity::class,
        UserPlaylistEntity::class,
        UserPlaylistSongEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MuziDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun likedSongDao(): LikedSongDao
    abstract fun userPlaylistDao(): UserPlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: MuziDatabase? = null

        fun getInstance(context: Context): MuziDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MuziDatabase::class.java,
                    "muzi_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
