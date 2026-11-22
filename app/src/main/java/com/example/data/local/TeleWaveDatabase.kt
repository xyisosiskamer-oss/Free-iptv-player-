package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        FavoriteEntity::class,
        RecentChannelEntity::class,
        EpgProgramEntity::class,
        EpgChannelAliasEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TeleWaveDatabase : RoomDatabase() {
    abstract fun teleWaveDao(): TeleWaveDao

    companion object {
        @Volatile
        private var INSTANCE: TeleWaveDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE channels ADD COLUMN catchupType TEXT")
                db.execSQL("ALTER TABLE channels ADD COLUMN catchupDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE channels ADD COLUMN catchupSource TEXT")
            }
        }

        fun getInstance(context: Context): TeleWaveDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TeleWaveDatabase::class.java,
                    "telewave_iptv.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
