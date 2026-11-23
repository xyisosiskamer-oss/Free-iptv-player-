package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TeleWaveDao {
    @Query("SELECT * FROM playlists ORDER BY sortOrder ASC, id ASC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY sortOrder ASC, id ASC")
    suspend fun getPlaylistsOnce(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Query("SELECT * FROM channels ORDER BY playlistId ASC, position ASC")
    fun observeAllChannels(): Flow<List<ChannelEntity>>

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsForPlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Transaction
    suspend fun replacePlaylistChannels(playlistId: Long, channels: List<ChannelEntity>) {
        deleteChannelsForPlaylist(playlistId)
        if (channels.isNotEmpty()) {
            insertChannels(channels)
        }
    }

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE streamUrl = :streamUrl")
    suspend fun deleteFavoriteByUrl(streamUrl: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE streamUrl = :streamUrl)")
    suspend fun isFavorite(streamUrl: String): Boolean

    @Query("SELECT * FROM favorites WHERE playlistId = :playlistId AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findFavoriteByPlaylistAndName(playlistId: Long, name: String): FavoriteEntity?

    @Query("SELECT * FROM recent_channels ORDER BY lastWatchedAt DESC LIMIT 12")
    fun observeRecentChannels(): Flow<List<RecentChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecent(recent: RecentChannelEntity)

    @Query("DELETE FROM recent_channels")
    suspend fun clearRecentChannels()

    // EPG queries
    @Query("SELECT * FROM epg_programs ORDER BY startTimeMillis ASC")
    fun observeAllEpgPrograms(): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE endTimeMillis >= :fromMillis ORDER BY startTimeMillis ASC")
    fun observeUpcomingEpgPrograms(fromMillis: Long): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_channel_aliases")
    fun observeEpgAliases(): Flow<List<EpgChannelAliasEntity>>

    @Query("SELECT COUNT(*) FROM epg_programs")
    fun observeEpgProgramCount(): Flow<Int>

    @Query("DELETE FROM epg_programs WHERE sourceUrl = :sourceUrl")
    suspend fun deleteEpgProgramsForSource(sourceUrl: String)

    @Query("DELETE FROM epg_channel_aliases WHERE sourceUrl = :sourceUrl")
    suspend fun deleteEpgAliasesForSource(sourceUrl: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpgPrograms(programs: List<EpgProgramEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpgAliases(aliases: List<EpgChannelAliasEntity>)

    @Query("DELETE FROM epg_programs WHERE endTimeMillis < :cutoffMillis")
    suspend fun pruneOldEpgPrograms(cutoffMillis: Long)

    @Transaction
    suspend fun replaceEpgForSource(
        sourceUrl: String,
        aliases: List<EpgChannelAliasEntity>,
        programs: List<EpgProgramEntity>
    ) {
        deleteEpgProgramsForSource(sourceUrl)
        deleteEpgAliasesForSource(sourceUrl)
        if (aliases.isNotEmpty()) {
            insertEpgAliases(aliases)
        }
        if (programs.isNotEmpty()) {
            // Insert in chunks to stay well under SQLite bind variable limits
            programs.chunked(500).forEach { chunk ->
                insertEpgPrograms(chunk)
            }
        }
    }
}
