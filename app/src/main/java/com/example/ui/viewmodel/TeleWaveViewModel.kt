package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlaylistEntity
import com.example.data.local.TeleWaveDatabase
import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgramUi
import com.example.data.parser.CatchupUrlBuilder
import com.example.data.parser.PlaylistImportValidator
import com.example.data.parser.PlaylistValidationResult
import com.example.data.preferences.ChannelViewMode
import com.example.data.preferences.PlayerResizeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.IptvRepository
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    CHANNELS,
    FAVORITES,
    PLAYLISTS,
    SETTINGS
}

data class ArchivePlayback(
    val program: EpgProgramUi,
    val archiveUrl: String
)

data class PlayerSession(
    val currentChannel: ChannelItem,
    val queue: List<ChannelItem>,
    val archivePlayback: ArchivePlayback? = null
)

sealed interface PendingExternalImport {
    val suggestedName: String
    val sourceLabel: String
    val possibleDuplicateId: Long?
    val possibleDuplicateName: String?

    data class LocalContent(
        override val suggestedName: String,
        override val sourceLabel: String,
        val rawContent: String,
        val channelCount: Int,
        val detectedEpgUrl: String?,
        override val possibleDuplicateId: Long?,
        override val possibleDuplicateName: String?
    ) : PendingExternalImport

    data class RemoteUrl(
        override val suggestedName: String,
        override val sourceLabel: String,
        val url: String,
        override val possibleDuplicateId: Long?,
        override val possibleDuplicateName: String?
    ) : PendingExternalImport
}

data class TeleWaveUiState(
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val currentTab: AppTab = AppTab.CHANNELS,
    val playlists: List<PlaylistEntity> = emptyList(),
    val activePlaylistId: Long? = null,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val favoritesSearchQuery: String = "",
    val viewMode: ChannelViewMode = ChannelViewMode.GRID,
    val resizeMode: PlayerResizeMode = PlayerResizeMode.FIT,
    val customEpgUrl: String = "",
    val epgLastUpdatedAt: Long = 0L,
    val epgProgramCount: Int = 0,
    val isSyncingEpg: Boolean = false,
    val allChannels: List<ChannelItem> = emptyList(),
    val filteredChannels: List<ChannelItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val favoriteChannels: List<ChannelItem> = emptyList(),
    val filteredFavorites: List<ChannelItem> = emptyList(),
    val recentChannels: List<ChannelItem> = emptyList(),
    val isSyncing: Boolean = false,
    val syncingPlaylistIds: Set<Long> = emptySet(),
    val playerSession: PlayerSession? = null,
    val pendingExternalImport: PendingExternalImport? = null
)

class TeleWaveViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TeleWaveDatabase.getInstance(application)
    private val prefs = UserPreferencesRepository(application)
    val repository = IptvRepository(application, database.teleWaveDao(), prefs)

    private val currentTabFlow = MutableStateFlow(AppTab.CHANNELS)
    private val selectedCategoryFlow = MutableStateFlow<String?>(null)
    private val searchQueryFlow = MutableStateFlow("")
    private val favoritesSearchQueryFlow = MutableStateFlow("")
    private val isSyncingFlow = MutableStateFlow(false)
    private val isSyncingEpgFlow = MutableStateFlow(false)
    private val syncingPlaylistIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val playerSessionFlow = MutableStateFlow<PlayerSession?>(null)
    private val pendingExternalImportFlow = MutableStateFlow<PendingExternalImport?>(null)

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val baseDataFlow = combine(
        prefs.languageFlow,
        prefs.activePlaylistIdFlow,
        prefs.viewModeFlow,
        prefs.resizeModeFlow,
        repository.playlistsFlow
    ) { language, activePlaylistId, viewMode, resizeMode, playlists ->
        BasePrefsState(language, activePlaylistId, viewMode, resizeMode, playlists)
    }

    private val epgMetaFlow = combine(
        prefs.customEpgUrlFlow,
        prefs.epgLastUpdatedAtFlow,
        repository.epgProgramCountFlow,
        isSyncingEpgFlow
    ) { customUrl, updatedAt, count, syncingEpg ->
        EpgMetaState(customUrl, updatedAt, count, syncingEpg)
    }

    private val channelsDataFlow = combine(
        repository.allChannelsFlow,
        repository.favoriteChannelItemsFlow,
        repository.recentChannelsFlow,
        selectedCategoryFlow,
        searchQueryFlow
    ) { allChannels, favorites, recents, selectedCategory, searchQuery ->
        ChannelsDataState(allChannels, favorites, recents, selectedCategory, searchQuery)
    }

    private val uiControlsFlow = combine(
        currentTabFlow,
        favoritesSearchQueryFlow,
        isSyncingFlow,
        syncingPlaylistIdsFlow,
        playerSessionFlow
    ) { tab, favSearch, isSyncing, syncingIds, playerSession ->
        UiControlState(tab, favSearch, isSyncing, syncingIds, playerSession)
    }

    val uiState: StateFlow<TeleWaveUiState> = combine(
        baseDataFlow,
        epgMetaFlow,
        channelsDataFlow,
        uiControlsFlow,
        pendingExternalImportFlow
    ) { base, epgMeta, chData, controls, pendingImport ->
        val validActivePlaylistId = base.activePlaylistId?.takeIf { id ->
            base.playlists.any { it.id == id && it.isEnabled }
        }

        val playlistScopedChannels = if (validActivePlaylistId == null) {
            chData.allChannels
        } else {
            chData.allChannels.filter { it.playlistId == validActivePlaylistId }
        }

        val categories = playlistScopedChannels
            .map { it.groupTitle }
            .distinct()
            .sortedWith(compareBy<String> { it.isEmpty() }.thenBy { it.lowercase() })

        val effectiveCategory = chData.selectedCategory?.takeIf { categories.contains(it) }

        val query = chData.searchQuery.trim()
        val filteredChannels = playlistScopedChannels.filter { channel ->
            val matchesCategory = effectiveCategory == null || channel.groupTitle == effectiveCategory
            val matchesQuery = query.isEmpty() ||
                channel.name.contains(query, ignoreCase = true) ||
                channel.groupTitle.contains(query, ignoreCase = true) ||
                (channel.currentProgram?.title?.contains(query, ignoreCase = true) == true)
            matchesCategory && matchesQuery
        }

        val favQuery = controls.favSearch.trim()
        val filteredFavorites = chData.favorites.filter { channel ->
            favQuery.isEmpty() ||
                channel.name.contains(favQuery, ignoreCase = true) ||
                channel.groupTitle.contains(favQuery, ignoreCase = true) ||
                (channel.currentProgram?.title?.contains(favQuery, ignoreCase = true) == true)
        }

        val favUrls = chData.favorites.map { it.streamUrl }.toHashSet()
        val channelByUrl = chData.allChannels.associateBy { it.streamUrl }
        val recentItems = chData.recents.mapIndexed { idx, rec ->
            val matched = channelByUrl[rec.streamUrl]
            ChannelItem(
                id = -1000L - idx,
                playlistId = rec.playlistId,
                playlistName = base.playlists.firstOrNull { it.id == rec.playlistId }?.name ?: "",
                name = rec.name,
                streamUrl = rec.streamUrl,
                logoUrl = rec.logoUrl,
                groupTitle = rec.groupTitle,
                tvgId = matched?.tvgId,
                userAgent = rec.userAgent,
                referrer = rec.referrer,
                position = idx,
                isFavorite = favUrls.contains(rec.streamUrl),
                catchupType = matched?.catchupType,
                catchupDays = matched?.catchupDays ?: 0,
                catchupSource = matched?.catchupSource,
                currentProgram = matched?.currentProgram,
                nextProgram = matched?.nextProgram,
                schedule = matched?.schedule ?: emptyList(),
                archivePrograms = matched?.archivePrograms ?: emptyList()
            )
        }

        val syncedSession = controls.playerSession?.let { session ->
            val liveCurrent = channelByUrl[session.currentChannel.streamUrl]
            val updatedCurrent = (liveCurrent ?: session.currentChannel).copy(
                isFavorite = favUrls.contains(session.currentChannel.streamUrl)
            )
            val updatedQueue = session.queue.map { q ->
                val liveQ = channelByUrl[q.streamUrl]
                (liveQ ?: q).copy(isFavorite = favUrls.contains(q.streamUrl))
            }
            session.copy(currentChannel = updatedCurrent, queue = updatedQueue)
        }

        TeleWaveUiState(
            language = base.language,
            currentTab = controls.tab,
            playlists = base.playlists,
            activePlaylistId = validActivePlaylistId,
            selectedCategory = effectiveCategory,
            searchQuery = chData.searchQuery,
            favoritesSearchQuery = controls.favSearch,
            viewMode = base.viewMode,
            resizeMode = base.resizeMode,
            customEpgUrl = epgMeta.customUrl,
            epgLastUpdatedAt = epgMeta.updatedAt,
            epgProgramCount = epgMeta.programCount,
            isSyncingEpg = epgMeta.isSyncingEpg,
            allChannels = playlistScopedChannels,
            filteredChannels = filteredChannels,
            categories = categories,
            favoriteChannels = chData.favorites,
            filteredFavorites = filteredFavorites,
            recentChannels = recentItems,
            isSyncing = controls.isSyncing,
            syncingPlaylistIds = controls.syncingIds,
            playerSession = syncedSession,
            pendingExternalImport = pendingImport
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TeleWaveUiState()
    )

    init {
        viewModelScope.launch {
            isSyncingFlow.value = true
            val lang = prefs.languageFlow.first()
            val defaultName = stringsFor(lang).defaultPlaylistName
            repository.ensureDefaultPlaylistAndSync(defaultName)
            isSyncingFlow.value = false
        }
    }

    fun selectTab(tab: AppTab) {
        currentTabFlow.value = tab
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefs.setLanguage(language)
        }
    }

    fun selectActivePlaylist(playlistId: Long?) {
        viewModelScope.launch {
            prefs.setActivePlaylistId(playlistId)
            selectedCategoryFlow.value = null
        }
    }

    fun openPlaylistInChannels(playlistId: Long) {
        viewModelScope.launch {
            prefs.setActivePlaylistId(playlistId)
            selectedCategoryFlow.value = null
            currentTabFlow.value = AppTab.CHANNELS
        }
    }

    fun selectCategory(category: String?) {
        selectedCategoryFlow.value = category
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun updateFavoritesSearchQuery(query: String) {
        favoritesSearchQueryFlow.value = query
    }

    fun clearChannelFilters() {
        selectedCategoryFlow.value = null
        searchQueryFlow.value = ""
    }

    fun toggleViewMode() {
        val next = if (uiState.value.viewMode == ChannelViewMode.GRID) {
            ChannelViewMode.LIST
        } else {
            ChannelViewMode.GRID
        }
        setViewMode(next)
    }

    fun setViewMode(mode: ChannelViewMode) {
        viewModelScope.launch {
            prefs.setViewMode(mode)
        }
    }

    fun setResizeMode(mode: PlayerResizeMode) {
        viewModelScope.launch {
            prefs.setResizeMode(mode)
        }
    }

    fun cycleResizeMode() {
        val next = when (uiState.value.resizeMode) {
            PlayerResizeMode.FIT -> PlayerResizeMode.FILL
            PlayerResizeMode.FILL -> PlayerResizeMode.ZOOM
            PlayerResizeMode.ZOOM -> PlayerResizeMode.FIT
        }
        setResizeMode(next)
    }

    fun saveAndSyncCustomEpgUrl(url: String) {
        viewModelScope.launch {
            prefs.setCustomEpgUrl(url)
            syncAllEpg()
        }
    }

    fun syncAllEpg() {
        viewModelScope.launch {
            isSyncingEpgFlow.value = true
            val result = repository.syncAllEpgSources()
            isSyncingEpgFlow.value = false
            val strings = stringsFor(uiState.value.language)
            result.onSuccess { count ->
                if (count > 0) {
                    _snackbarEvents.tryEmit(strings.epgProgramsLoadedMsg(count))
                }
            }.onFailure { err ->
                _snackbarEvents.tryEmit(strings.epgLoadErrorMsg(err.message ?: "Error"))
            }
        }
    }

    fun toggleFavorite(channel: ChannelItem) {
        viewModelScope.launch {
            val added = repository.toggleFavorite(channel)
            val strings = stringsFor(uiState.value.language)
            val msg = if (added) {
                strings.addedToFavoritesMsg(channel.name)
            } else {
                strings.removedFromFavoritesMsg(channel.name)
            }
            _snackbarEvents.tryEmit(msg)
        }
    }

    fun playChannel(channel: ChannelItem, queue: List<ChannelItem>) {
        val safeQueue = if (queue.any { it.streamUrl == channel.streamUrl }) {
            queue
        } else {
            listOf(channel) + queue
        }
        playerSessionFlow.value = PlayerSession(currentChannel = channel, queue = safeQueue)
        viewModelScope.launch {
            repository.recordRecent(channel)
        }
    }

    fun switchPlayerChannel(channel: ChannelItem) {
        val currentSession = playerSessionFlow.value ?: return
        val newQueue = if (currentSession.queue.any { it.streamUrl == channel.streamUrl }) {
            currentSession.queue
        } else {
            uiState.value.allChannels.ifEmpty { listOf(channel) }
        }
        playerSessionFlow.value = PlayerSession(currentChannel = channel, queue = newQueue)
        viewModelScope.launch {
            repository.recordRecent(channel)
        }
    }

    fun zapNextChannel() {
        val session = playerSessionFlow.value ?: return
        val queue = session.queue
        if (queue.size <= 1) return
        val idx = queue.indexOfFirst { it.streamUrl == session.currentChannel.streamUrl }
        val nextIdx = if (idx < 0 || idx >= queue.lastIndex) 0 else idx + 1
        switchPlayerChannel(queue[nextIdx])
    }

    fun zapPreviousChannel() {
        val session = playerSessionFlow.value ?: return
        val queue = session.queue
        if (queue.size <= 1) return
        val idx = queue.indexOfFirst { it.streamUrl == session.currentChannel.streamUrl }
        val prevIdx = if (idx <= 0) queue.lastIndex else idx - 1
        switchPlayerChannel(queue[prevIdx])
    }

    fun playArchiveProgram(program: EpgProgramUi) {
        val currentSession = playerSessionFlow.value ?: return
        val archiveUrl = CatchupUrlBuilder.buildArchiveUrl(currentSession.currentChannel, program)
        playerSessionFlow.value = currentSession.copy(
            archivePlayback = ArchivePlayback(program = program, archiveUrl = archiveUrl)
        )
    }

    fun returnToLive() {
        val currentSession = playerSessionFlow.value ?: return
        if (currentSession.archivePlayback != null) {
            playerSessionFlow.value = currentSession.copy(archivePlayback = null)
        }
    }

    fun closePlayer() {
        playerSessionFlow.value = null
    }

    fun addPlaylistFromUrl(name: String, url: String, epgUrl: String?) {
        viewModelScope.launch {
            isSyncingFlow.value = true
            val result = repository.addPlaylistFromUrl(name, url, epgUrl)
            isSyncingFlow.value = false
            val strings = stringsFor(uiState.value.language)
            result.onSuccess { (newId, count) ->
                prefs.setActivePlaylistId(newId)
                _snackbarEvents.tryEmit(strings.playlistRefreshedMsg(name.trim(), count))
            }.onFailure { err ->
                _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
            }
        }
    }

    fun importPlaylistFromUri(name: String, uri: Uri, displayName: String, epgUrl: String?) {
        viewModelScope.launch {
            isSyncingFlow.value = true
            val strings = stringsFor(uiState.value.language)
            val rawResult = repository.readTextFromUri(uri)
            rawResult.onSuccess { text ->
                val resolvedName = name.trim().ifEmpty {
                    displayName.substringBeforeLast('.').ifBlank { "M3U Playlist" }
                }
                repository.addPlaylistFromLocalContent(resolvedName, "file://$displayName", text, epgUrl)
                    .onSuccess { (newId, count) ->
                        prefs.setActivePlaylistId(newId)
                        _snackbarEvents.tryEmit(strings.fileImportedHint(count))
                    }
                    .onFailure { err ->
                        _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
                    }
            }.onFailure { err ->
                _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
            }
            isSyncingFlow.value = false
        }
    }

    fun editPlaylist(playlist: PlaylistEntity, newName: String, newUrl: String, newEpgUrl: String?) {
        viewModelScope.launch {
            val urlChanged = playlist.url != newUrl.trim()
            syncingPlaylistIdsFlow.value = syncingPlaylistIdsFlow.value + playlist.id
            val result = repository.updatePlaylist(
                playlistId = playlist.id,
                newName = newName,
                newUrl = newUrl,
                newEpgUrl = newEpgUrl,
                refreshAfter = urlChanged
            )
            syncingPlaylistIdsFlow.value = syncingPlaylistIdsFlow.value - playlist.id
            val strings = stringsFor(uiState.value.language)
            if (urlChanged) {
                result.onSuccess { count ->
                    _snackbarEvents.tryEmit(strings.playlistRefreshedMsg(newName.trim(), count))
                }.onFailure { err ->
                    _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
                }
            }
        }
    }

    fun togglePlaylistEnabled(playlist: PlaylistEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.setPlaylistEnabled(playlist.id, enabled)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            if (uiState.value.activePlaylistId == playlist.id) {
                prefs.setActivePlaylistId(null)
            }
            repository.deletePlaylist(playlist.id)
        }
    }

    fun refreshPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            syncingPlaylistIdsFlow.value = syncingPlaylistIdsFlow.value + playlist.id
            val result = repository.refreshPlaylist(playlist.id)
            playlist.epgUrl?.takeIf { it.isNotBlank() }?.let { repository.syncEpgFromUrl(it) }
            syncingPlaylistIdsFlow.value = syncingPlaylistIdsFlow.value - playlist.id
            val strings = stringsFor(uiState.value.language)
            result.onSuccess { count ->
                _snackbarEvents.tryEmit(strings.playlistRefreshedMsg(playlist.name, count))
            }.onFailure { err ->
                _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
            }
        }
    }

    fun refreshActiveOrAllPlaylists() {
        viewModelScope.launch {
            isSyncingFlow.value = true
            val activeId = uiState.value.activePlaylistId
            val strings = stringsFor(uiState.value.language)
            if (activeId != null) {
                val target = uiState.value.playlists.firstOrNull { it.id == activeId }
                if (target != null) {
                    repository.refreshPlaylist(target.id)
                        .onSuccess { count ->
                            _snackbarEvents.tryEmit(strings.playlistRefreshedMsg(target.name, count))
                        }
                        .onFailure { err ->
                            _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
                        }
                    repository.syncAllEpgSources()
                }
            } else {
                repository.refreshAllEnabledPlaylists()
            }
            isSyncingFlow.value = false
        }
    }

    fun restoreDefaultPlaylist() {
        viewModelScope.launch {
            isSyncingFlow.value = true
            val strings = stringsFor(uiState.value.language)
            repository.restoreDefaultPlaylist(strings.defaultPlaylistName)
                .onSuccess { count ->
                    _snackbarEvents.tryEmit(strings.playlistRefreshedMsg(strings.defaultPlaylistName, count))
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
                }
            isSyncingFlow.value = false
        }
    }

    fun clearRecentChannels() {
        viewModelScope.launch {
            repository.clearRecentChannels()
        }
    }

    fun handleIncomingUri(uri: Uri, displayName: String?) {
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme == "http" || scheme == "https") {
            handleIncomingText(uri.toString())
            return
        }
        viewModelScope.launch {
            val strings = stringsFor(uiState.value.language)
            val rawResult = repository.readTextFromUri(uri)
            rawResult.onSuccess { text ->
                val cleanName = displayName?.takeIf { it.isNotBlank() } ?: "playlist.m3u"
                when (val validation = PlaylistImportValidator.validateM3uContent(text)) {
                    is PlaylistValidationResult.Valid -> {
                        val suggested = PlaylistImportValidator.suggestNameFromDisplayName(cleanName)
                        val sourceLabel = "file://$cleanName"
                        val dup = uiState.value.playlists.firstOrNull {
                            it.url == sourceLabel ||
                                (it.name.equals(suggested, ignoreCase = true) &&
                                    it.channelCount == validation.parseResult.channels.size)
                        }
                        pendingExternalImportFlow.value = PendingExternalImport.LocalContent(
                            suggestedName = suggested,
                            sourceLabel = cleanName,
                            rawContent = text,
                            channelCount = validation.parseResult.channels.size,
                            detectedEpgUrl = validation.parseResult.epgUrl,
                            possibleDuplicateId = dup?.id,
                            possibleDuplicateName = dup?.name
                        )
                    }
                    is PlaylistValidationResult.Invalid -> {
                        _snackbarEvents.tryEmit(strings.playlistLoadError(validation.reason))
                    }
                }
            }.onFailure { err ->
                _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
            }
        }
    }

    fun handleIncomingText(rawText: String) {
        val strings = stringsFor(uiState.value.language)
        val url = PlaylistImportValidator.extractFirstHttpUrl(rawText)
        if (url != null) {
            val dup = uiState.value.playlists.firstOrNull { it.url == url }
            val suggested = PlaylistImportValidator.suggestNameFromDisplayName(url, fallback = "IPTV Playlist")
            pendingExternalImportFlow.value = PendingExternalImport.RemoteUrl(
                suggestedName = suggested,
                sourceLabel = url,
                url = url,
                possibleDuplicateId = dup?.id,
                possibleDuplicateName = dup?.name
            )
            return
        }
        if (rawText.contains("#EXTINF", ignoreCase = true)) {
            when (val validation = PlaylistImportValidator.validateM3uContent(rawText)) {
                is PlaylistValidationResult.Valid -> {
                    pendingExternalImportFlow.value = PendingExternalImport.LocalContent(
                        suggestedName = "Shared M3U",
                        sourceLabel = "Shared text",
                        rawContent = rawText,
                        channelCount = validation.parseResult.channels.size,
                        detectedEpgUrl = validation.parseResult.epgUrl,
                        possibleDuplicateId = null,
                        possibleDuplicateName = null
                    )
                }
                is PlaylistValidationResult.Invalid -> {
                    _snackbarEvents.tryEmit(strings.playlistLoadError(validation.reason))
                }
            }
        } else {
            _snackbarEvents.tryEmit(strings.invalidPlaylistInput)
        }
    }

    fun dismissIncomingImport() {
        pendingExternalImportFlow.value = null
    }

    fun confirmIncomingAsNew(customName: String) {
        val pending = pendingExternalImportFlow.value ?: return
        pendingExternalImportFlow.value = null
        viewModelScope.launch {
            isSyncingFlow.value = true
            val strings = stringsFor(uiState.value.language)
            val result = when (pending) {
                is PendingExternalImport.LocalContent -> repository.addPlaylistFromLocalContent(
                    name = customName,
                    sourceLabel = "file://${pending.sourceLabel}",
                    rawContent = pending.rawContent,
                    manualEpgUrl = pending.detectedEpgUrl
                )
                is PendingExternalImport.RemoteUrl -> repository.addPlaylistFromUrl(
                    name = customName,
                    url = pending.url
                )
            }
            isSyncingFlow.value = false
            result.onSuccess { (newId, count) ->
                prefs.setActivePlaylistId(newId)
                currentTabFlow.value = AppTab.CHANNELS
                _snackbarEvents.tryEmit(strings.playlistRefreshedMsg(customName, count))
            }.onFailure { err ->
                _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
            }
        }
    }

    fun confirmIncomingReplace(targetId: Long, customName: String) {
        val pending = pendingExternalImportFlow.value ?: return
        pendingExternalImportFlow.value = null
        viewModelScope.launch {
            isSyncingFlow.value = true
            val strings = stringsFor(uiState.value.language)
            val result = when (pending) {
                is PendingExternalImport.LocalContent -> repository.replacePlaylistWithLocalContent(
                    targetPlaylistId = targetId,
                    newName = customName,
                    sourceLabel = "file://${pending.sourceLabel}",
                    rawContent = pending.rawContent,
                    manualEpgUrl = pending.detectedEpgUrl
                )
                is PendingExternalImport.RemoteUrl -> repository.replacePlaylistWithUrl(
                    targetPlaylistId = targetId,
                    newName = customName,
                    newUrl = pending.url
                )
            }
            isSyncingFlow.value = false
            result.onSuccess { (updatedId, count) ->
                prefs.setActivePlaylistId(updatedId)
                currentTabFlow.value = AppTab.CHANNELS
                _snackbarEvents.tryEmit(strings.playlistRefreshedMsg(customName, count))
            }.onFailure { err ->
                _snackbarEvents.tryEmit(strings.playlistLoadError(err.message ?: "Error"))
            }
        }
    }

    private data class BasePrefsState(
        val language: AppLanguage,
        val activePlaylistId: Long?,
        val viewMode: ChannelViewMode,
        val resizeMode: PlayerResizeMode,
        val playlists: List<PlaylistEntity>
    )

    private data class EpgMetaState(
        val customUrl: String,
        val updatedAt: Long,
        val programCount: Int,
        val isSyncingEpg: Boolean
    )

    private data class ChannelsDataState(
        val allChannels: List<ChannelItem>,
        val favorites: List<ChannelItem>,
        val recents: List<com.example.data.local.RecentChannelEntity>,
        val selectedCategory: String?,
        val searchQuery: String
    )

    private data class UiControlState(
        val tab: AppTab,
        val favSearch: String,
        val isSyncing: Boolean,
        val syncingIds: Set<Long>,
        val playerSession: PlayerSession?
    )
}
