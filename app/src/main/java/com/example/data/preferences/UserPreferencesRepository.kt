package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ChannelViewMode {
    GRID,
    LIST
}

enum class PlayerResizeMode {
    FIT,
    FILL,
    ZOOM
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "telewave_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val ACTIVE_PLAYLIST_ID = longPreferencesKey("active_playlist_id")
        val VIEW_MODE = stringPreferencesKey("channel_view_mode")
        val RESIZE_MODE = stringPreferencesKey("player_resize_mode")
        val DEFAULT_SEEDED = booleanPreferencesKey("default_playlist_seeded")
        val CUSTOM_EPG_URL = stringPreferencesKey("custom_epg_url")
        val EPG_LAST_UPDATED_AT = longPreferencesKey("epg_last_updated_at")
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[Keys.LANGUAGE])
    }

    val activePlaylistIdFlow: Flow<Long?> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.ACTIVE_PLAYLIST_ID]
        if (raw == null || raw <= 0L) null else raw
    }

    val viewModeFlow: Flow<ChannelViewMode> = context.dataStore.data.map { prefs ->
        runCatching {
            ChannelViewMode.valueOf(prefs[Keys.VIEW_MODE] ?: ChannelViewMode.GRID.name)
        }.getOrDefault(ChannelViewMode.GRID)
    }

    val resizeModeFlow: Flow<PlayerResizeMode> = context.dataStore.data.map { prefs ->
        runCatching {
            PlayerResizeMode.valueOf(prefs[Keys.RESIZE_MODE] ?: PlayerResizeMode.FIT.name)
        }.getOrDefault(PlayerResizeMode.FIT)
    }

    val customEpgUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_EPG_URL].orEmpty()
    }

    val epgLastUpdatedAtFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.EPG_LAST_UPDATED_AT] ?: 0L
    }

    suspend fun isDefaultSeeded(): Boolean {
        return context.dataStore.data.first()[Keys.DEFAULT_SEEDED] == true
    }

    suspend fun markDefaultSeeded() {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_SEEDED] = true
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun setActivePlaylistId(playlistId: Long?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PLAYLIST_ID] = playlistId ?: -1L
        }
    }

    suspend fun setViewMode(mode: ChannelViewMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VIEW_MODE] = mode.name
        }
    }

    suspend fun setResizeMode(mode: PlayerResizeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RESIZE_MODE] = mode.name
        }
    }

    suspend fun setCustomEpgUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_EPG_URL] = url.trim()
        }
    }

    suspend fun setEpgLastUpdatedAt(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EPG_LAST_UPDATED_AT] = timestamp
        }
    }
}
