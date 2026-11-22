package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val epgUrl: String? = null,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val isLocalFile: Boolean = false,
    val channelCount: Int = 0,
    val lastUpdatedAt: Long = 0L,
    val lastError: String? = null,
    val sortOrder: Int = 0
)
