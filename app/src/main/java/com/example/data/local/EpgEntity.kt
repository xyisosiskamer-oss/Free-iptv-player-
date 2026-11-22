package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["channelKey", "startTimeMillis"]),
        Index(value = ["endTimeMillis"])
    ]
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceUrl: String,
    val channelKey: String,
    val title: String,
    val description: String?,
    val category: String?,
    val startTimeMillis: Long,
    val endTimeMillis: Long
)

@Entity(
    tableName = "epg_channel_aliases",
    indices = [Index(value = ["displayNameNormalized"])]
)
data class EpgChannelAliasEntity(
    @PrimaryKey val displayNameNormalized: String,
    val channelKey: String,
    val sourceUrl: String
)
