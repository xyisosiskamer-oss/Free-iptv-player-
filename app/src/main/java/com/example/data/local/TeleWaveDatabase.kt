package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        FavoriteEntity::class,
        RecentChannelEntity::class,
        EpgProgramEntity::class,
        EpgChannelAliasEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TeleWaveDatabase : RoomDatabase() {
    abstract fun teleWaveDao(): TeleWaveDao

    companion object {
        @Volatile
        private var INSTANCE: TeleWaveDatabase? = null

        fun getInstance(context: Context): TeleWaveDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TeleWaveDatabase::class.java,
                    "telewave_iptv.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
