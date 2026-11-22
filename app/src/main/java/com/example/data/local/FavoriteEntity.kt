package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val streamUrl: String,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String,
    val playlistId: Long,
    val userAgent: String?,
    val referrer: String?,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_channels")
data class RecentChannelEntity(
    @PrimaryKey val streamUrl: String,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String,
    val playlistId: Long,
    val userAgent: String?,
    val referrer: String?,
    val lastWatchedAt: Long = System.currentTimeMillis()
)
