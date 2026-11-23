package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.ChannelEntity
import com.example.data.local.EpgChannelAliasEntity
import com.example.data.local.EpgProgramEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.RecentChannelEntity
import com.example.data.local.TeleWaveDao
import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgramUi
import com.example.data.parser.M3uParser
import com.example.data.parser.ParsedChannel
import com.example.data.parser.PlaylistImportValidator
import com.example.data.parser.XmltvParser
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class IptvRepository(
    private val context: Context,
    private val dao: TeleWaveDao,
    private val prefs: UserPreferencesRepository
) {
    companion object {
        const val DEFAULT_PLAYLIST_URL = "https://m3u.work/ofRjF2G"
        const val DEFAULT_PLAYLIST_NAME = "Основной ТВ Плейлист"
        private const val AUTO_REFRESH_STALE_MS = 20L * 60L * 1000L
    }

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    val playerHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val playlistsFlow: Flow<List<PlaylistEntity>> = dao.observePlaylists()
    val favoritesFlow: Flow<List<FavoriteEntity>> = dao.observeFavorites()
    val recentChannelsFlow: Flow<List<RecentChannelEntity>> = dao.observeRecentChannels()
    val epgProgramCountFlow: Flow<Int> = dao.observeEpgProgramCount()

    private val epgLookupFlow: Flow<EpgLookup> = combine(
        dao.observeAllEpgPrograms(),
        dao.observeEpgAliases()
    ) { programs, aliases ->
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val groupedPrograms = programs
            .filter { it.endTimeMillis >= cutoff }
            .groupBy { it.channelKey }
        val aliasMap = aliases.associate { it.displayNameNormalized to it.channelKey }
        EpgLookup(groupedPrograms, aliasMap)
    }

    val allChannelsFlow: Flow<List<ChannelItem>> = combine(
        dao.observeAllChannels(),
        dao.observePlaylists(),
        dao.observeFavorites(),
        epgLookupFlow
    ) { channels, playlists, favorites, epg ->
        val playlistMap = playlists.associateBy { it.id }
        val favoriteUrls = favorites.map { it.streamUrl }.toHashSet()
        val favoritePlaylistNames = favorites.map { "${it.playlistId}|${it.name.trim().lowercase()}" }.toHashSet()
        val now = System.currentTimeMillis()
        channels.mapNotNull { ch ->
            val playlist = playlistMap[ch.playlistId]
            if (playlist != null && !playlist.isEnabled) {
                null
            } else {
                val resolved = resolveEpgForChannel(ch.tvgId, ch.name, ch.catchupDays, epg, now)
                val isFav = favoriteUrls.contains(ch.streamUrl) ||
                    favoritePlaylistNames.contains("${ch.playlistId}|${ch.name.trim().lowercase()}")
                ChannelItem(
                    id = ch.id,
                    playlistId = ch.playlistId,
                    playlistName = playlist?.name ?: "",
                    name = ch.name,
                    streamUrl = ch.streamUrl,
                    logoUrl = ch.logoUrl,
                    groupTitle = ch.groupTitle,
                    tvgId = ch.tvgId,
                    userAgent = ch.userAgent,
                    referrer = ch.referrer,
                    position = ch.position,
                    isFavorite = isFav,
                    catchupType = ch.catchupType,
                    catchupDays = ch.catchupDays,
                    catchupSource = ch.catchupSource,
                    currentProgram = resolved.current,
                    nextProgram = resolved.next,
                    schedule = resolved.schedule,
                    archivePrograms = resolved.archivePrograms
                )
            }
        }
    }

    val favoriteChannelItemsFlow: Flow<List<ChannelItem>> = combine(
        dao.observeFavorites(),
        dao.observePlaylists(),
        dao.observeAllChannels(),
        epgLookupFlow
    ) { favorites, playlists, allChannels, epg ->
        val playlistMap = playlists.associateBy { it.id }
        val channelByUrl = allChannels.associateBy { it.streamUrl }
        val channelByPlaylistAndName = allChannels.associateBy { "${it.playlistId}|${it.name.trim().lowercase()}" }
        val now = System.currentTimeMillis()
        favorites.mapIndexed { index, fav ->
            val matched = channelByUrl[fav.streamUrl]
                ?: channelByPlaylistAndName["${fav.playlistId}|${fav.name.trim().lowercase()}"]
            val tvgId = matched?.tvgId
            val catchupDays = matched?.catchupDays ?: 0
            val resolved = resolveEpgForChannel(tvgId, fav.name, catchupDays, epg, now)
            ChannelItem(
                id = -1L - index,
                playlistId = fav.playlistId,
                playlistName = playlistMap[fav.playlistId]?.name ?: "",
                name = fav.name,
                streamUrl = matched?.streamUrl ?: fav.streamUrl,
                logoUrl = matched?.logoUrl ?: fav.logoUrl,
                groupTitle = matched?.groupTitle ?: fav.groupTitle,
                tvgId = tvgId,
                userAgent = matched?.userAgent ?: fav.userAgent,
                referrer = matched?.referrer ?: fav.referrer,
                position = index,
                isFavorite = true,
                catchupType = matched?.catchupType,
                catchupDays = catchupDays,
                catchupSource = matched?.catchupSource,
                currentProgram = resolved.current,
                nextProgram = resolved.next,
                schedule = resolved.schedule,
                archivePrograms = resolved.archivePrograms
            )
        }
    }

    suspend fun ensureDefaultPlaylistAndSync(defaultName: String): List<PlaylistEntity> {
        val count = dao.getPlaylistCount()
        if (count == 0 && !prefs.isDefaultSeeded()) {
            val id = dao.insertPlaylist(
                PlaylistEntity(
                    name = defaultName,
                    url = DEFAULT_PLAYLIST_URL,
                    isEnabled = true,
                    isDefault = true,
                    sortOrder = 0
                )
            )
            prefs.markDefaultSeeded()
            refreshPlaylist(id)
        } else {
            val now = System.currentTimeMillis()
            val playlists = dao.getPlaylistsOnce()
            playlists.filter {
                it.isEnabled && !it.isLocalFile &&
                    (it.lastUpdatedAt == 0L || (now - it.lastUpdatedAt) >= AUTO_REFRESH_STALE_MS)
            }.forEach { refreshPlaylist(it.id) }
        }
        syncAllEpgSources()
        return dao.getPlaylistsOnce()
    }

    suspend fun restoreDefaultPlaylist(defaultName: String): Result<Int> {
        val existing = dao.getPlaylistsOnce().firstOrNull { it.url == DEFAULT_PLAYLIST_URL }
        val targetId = if (existing != null) {
            dao.updatePlaylist(existing.copy(isEnabled = true, isDefault = true))
            existing.id
        } else {
            val nextOrder = (dao.getPlaylistsOnce().maxOfOrNull { it.sortOrder } ?: -1) + 1
            dao.insertPlaylist(
                PlaylistEntity(
                    name = defaultName,
                    url = DEFAULT_PLAYLIST_URL,
                    isEnabled = true,
                    isDefault = true,
                    sortOrder = nextOrder
                )
            )
        }
        val result = refreshPlaylist(targetId)
        syncAllEpgSources()
        return result
    }

    suspend fun addPlaylistFromUrl(
        name: String,
        url: String,
        epgUrl: String? = null
    ): Result<Pair<Long, Int>> {
        val trimmedName = name.trim()
        val trimmedUrl = url.trim()
        val trimmedEpg = epgUrl?.trim()?.takeIf { it.isNotBlank() }
        val nextOrder = (dao.getPlaylistsOnce().maxOfOrNull { it.sortOrder } ?: -1) + 1
        val id = dao.insertPlaylist(
            PlaylistEntity(
                name = trimmedName,
                url = trimmedUrl,
                epgUrl = trimmedEpg,
                isEnabled = true,
                isDefault = false,
                isLocalFile = false,
                sortOrder = nextOrder
            )
        )
        val syncResult = refreshPlaylist(id)
        if (syncResult.isSuccess) {
            val updatedPlaylist = dao.getPlaylistById(id)
            updatedPlaylist?.epgUrl?.let { syncEpgFromUrl(it) }
        }
        return syncResult.map { count -> id to count }
    }

    suspend fun addPlaylistFromLocalContent(
        name: String,
        sourceLabel: String,
        rawContent: String,
        manualEpgUrl: String? = null
    ): Result<Pair<Long, Int>> {
        val parseResult = withContext(Dispatchers.Default) { M3uParser.parsePlaylist(rawContent) }
        if (parseResult.channels.isEmpty()) {
            return Result.failure(IllegalArgumentException("No channels found in M3U"))
        }
        val resolvedEpg = manualEpgUrl?.trim()?.takeIf { it.isNotBlank() } ?: parseResult.epgUrl
        val nextOrder = (dao.getPlaylistsOnce().maxOfOrNull { it.sortOrder } ?: -1) + 1
        val id = dao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                url = sourceLabel,
                epgUrl = resolvedEpg,
                isEnabled = true,
                isDefault = false,
                isLocalFile = true,
                channelCount = parseResult.channels.size,
                lastUpdatedAt = System.currentTimeMillis(),
                lastError = null,
                sortOrder = nextOrder
            )
        )
        dao.replacePlaylistChannels(id, parseResult.channels.toEntities(id))
        if (!resolvedEpg.isNullOrBlank()) {
            syncEpgFromUrl(resolvedEpg)
        }
        return Result.success(id to parseResult.channels.size)
    }

    suspend fun replacePlaylistWithLocalContent(
        targetPlaylistId: Long,
        newName: String,
        sourceLabel: String,
        rawContent: String,
        manualEpgUrl: String? = null
    ): Result<Pair<Long, Int>> {
        val existing = dao.getPlaylistById(targetPlaylistId)
            ?: return Result.failure(IllegalStateException("Playlist not found"))
        val parseResult = withContext(Dispatchers.Default) { M3uParser.parsePlaylist(rawContent) }
        if (parseResult.channels.isEmpty()) {
            return Result.failure(IllegalArgumentException("No channels found in M3U"))
        }
        val resolvedEpg = manualEpgUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: parseResult.epgUrl
            ?: existing.epgUrl
        dao.updatePlaylist(
            existing.copy(
                name = newName.trim().ifBlank { existing.name },
                url = sourceLabel,
                epgUrl = resolvedEpg,
                isEnabled = true,
                isLocalFile = true,
                channelCount = parseResult.channels.size,
                lastUpdatedAt = System.currentTimeMillis(),
                lastError = null
            )
        )
        dao.replacePlaylistChannels(existing.id, parseResult.channels.toEntities(existing.id))
        if (!resolvedEpg.isNullOrBlank()) {
            syncEpgFromUrl(resolvedEpg)
        }
        return Result.success(existing.id to parseResult.channels.size)
    }

    suspend fun replacePlaylistWithUrl(
        targetPlaylistId: Long,
        newName: String,
        newUrl: String,
        newEpgUrl: String? = null
    ): Result<Pair<Long, Int>> {
        val existing = dao.getPlaylistById(targetPlaylistId)
            ?: return Result.failure(IllegalStateException("Playlist not found"))
        val cleanEpg = newEpgUrl?.trim()?.takeIf { it.isNotBlank() } ?: existing.epgUrl
        dao.updatePlaylist(
            existing.copy(
                name = newName.trim().ifBlank { existing.name },
                url = newUrl.trim(),
                epgUrl = cleanEpg,
                isEnabled = true,
                isLocalFile = false
            )
        )
        val syncResult = refreshPlaylist(existing.id)
        if (syncResult.isSuccess) {
            dao.getPlaylistById(existing.id)?.epgUrl?.let { syncEpgFromUrl(it) }
        }
        return syncResult.map { count -> existing.id to count }
    }

    suspend fun updatePlaylist(
        playlistId: Long,
        newName: String,
        newUrl: String,
        newEpgUrl: String?,
        refreshAfter: Boolean
    ): Result<Int> {
        val existing = dao.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalStateException("Playlist not found"))
        val cleanEpg = newEpgUrl?.trim()?.takeIf { it.isNotBlank() }
        val updated = existing.copy(
            name = newName.trim(),
            url = newUrl.trim(),
            epgUrl = cleanEpg
        )
        dao.updatePlaylist(updated)
        val result = if (refreshAfter && !updated.isLocalFile) {
            refreshPlaylist(playlistId)
        } else {
            Result.success(updated.channelCount)
        }
        if (!cleanEpg.isNullOrBlank() && cleanEpg != existing.epgUrl) {
            syncEpgFromUrl(cleanEpg)
        }
        return result
    }

    suspend fun setPlaylistEnabled(playlistId: Long, enabled: Boolean) {
        val existing = dao.getPlaylistById(playlistId) ?: return
        dao.updatePlaylist(existing.copy(isEnabled = enabled))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylistById(playlistId)
    }

    suspend fun refreshPlaylist(playlistId: Long): Result<Int> {
        val playlist = dao.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalStateException("Playlist not found"))
        if (playlist.isLocalFile) {
            return Result.success(playlist.channelCount)
        }

        return try {
            val raw = fetchM3uText(playlist.url)
            val parseResult = withContext(Dispatchers.Default) {
                M3uParser.parsePlaylist(raw, baseUrl = playlist.url)
            }
            if (parseResult.channels.isEmpty()) {
                val err = "0 channels parsed"
                dao.updatePlaylist(playlist.copy(lastError = err))
                Result.failure(IllegalStateException(err))
            } else {
                val effectiveEpg = playlist.epgUrl?.takeIf { it.isNotBlank() } ?: parseResult.epgUrl
                dao.replacePlaylistChannels(playlist.id, parseResult.channels.toEntities(playlist.id))
                dao.updatePlaylist(
                    playlist.copy(
                        epgUrl = effectiveEpg,
                        channelCount = parseResult.channels.size,
                        lastUpdatedAt = System.currentTimeMillis(),
                        lastError = null
                    )
                )
                Result.success(parseResult.channels.size)
            }
        } catch (e: Exception) {
            val message = e.message ?: "Network error"
            dao.updatePlaylist(playlist.copy(lastError = message))
            Result.failure(e)
        }
    }

    data class RefreshAllSummary(
        val refreshedPlaylists: Int,
        val totalChannels: Int,
        val lastError: String?
    )

    suspend fun refreshAllEnabledPlaylists(): RefreshAllSummary {
        val playlists = dao.getPlaylistsOnce().filter { it.isEnabled && !it.isLocalFile }
        var total = 0
        var succeeded = 0
        var lastErr: String? = null
        for (playlist in playlists) {
            refreshPlaylist(playlist.id)
                .onSuccess {
                    total += it
                    succeeded++
                }
                .onFailure {
                    lastErr = it.message ?: "Network error"
                }
        }
        syncAllEpgSources()
        return RefreshAllSummary(
            refreshedPlaylists = succeeded,
            totalChannels = total,
            lastError = lastErr
        )
    }

    suspend fun syncAllEpgSources(): Result<Int> {
        val urls = buildSet {
            dao.getPlaylistsOnce()
                .filter { it.isEnabled && !it.epgUrl.isNullOrBlank() }
                .forEach { it.epgUrl?.trim()?.let(::add) }
            val customUrl = prefs.customEpgUrlFlow.first().trim()
            if (customUrl.isNotBlank()) add(customUrl)
        }
        if (urls.isEmpty()) return Result.success(0)

        var totalPrograms = 0
        var lastFailure: Throwable? = null
        dao.pruneOldEpgPrograms(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)

        for (epgUrl in urls) {
            syncEpgFromUrl(epgUrl)
                .onSuccess { totalPrograms += it }
                .onFailure { lastFailure = it }
        }
        if (totalPrograms > 0) {
            prefs.setEpgLastUpdatedAt(System.currentTimeMillis())
            return Result.success(totalPrograms)
        }
        return if (lastFailure != null) Result.failure(lastFailure!!) else Result.success(0)
    }

    suspend fun syncEpgFromUrl(epgUrl: String): Result<Int> = withContext(Dispatchers.IO) {
        val cleanUrl = epgUrl.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
            !cleanUrl.startsWith("https://", ignoreCase = true)
        ) {
            return@withContext Result.failure(IllegalArgumentException("Invalid EPG URL"))
        }

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 KobaidzeIPTV/1.0")
                .header("Accept", "*/*")
                .build()

            val parsed = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                val stream = response.body?.byteStream() ?: throw IOException("Empty EPG response")
                stream.use { XmltvParser.parse(it) }
            }

            val aliasEntities = parsed.aliases.map {
                EpgChannelAliasEntity(
                    displayNameNormalized = it.displayNameNormalized,
                    channelKey = it.channelId,
                    sourceUrl = cleanUrl
                )
            }
            val programEntities = parsed.programs.map {
                EpgProgramEntity(
                    sourceUrl = cleanUrl,
                    channelKey = it.channelId,
                    title = it.title,
                    description = it.description,
                    category = it.category,
                    startTimeMillis = it.startTimeMillis,
                    endTimeMillis = it.endTimeMillis
                )
            }
            dao.replaceEpgForSource(cleanUrl, aliasEntities, programEntities)
            prefs.setEpgLastUpdatedAt(System.currentTimeMillis())
            Result.success(programEntities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(channel: ChannelItem): Boolean {
        val currentlyFav = dao.isFavorite(channel.streamUrl)
        if (currentlyFav) {
            dao.deleteFavoriteByUrl(channel.streamUrl)
            return false
        }
        val fallbackMatch = dao.findFavoriteByPlaylistAndName(channel.playlistId, channel.name)
        if (fallbackMatch != null) {
            dao.deleteFavoriteByUrl(fallbackMatch.streamUrl)
            return false
        }
        dao.insertFavorite(
            FavoriteEntity(
                streamUrl = channel.streamUrl,
                name = channel.name,
                logoUrl = channel.logoUrl,
                groupTitle = channel.groupTitle,
                playlistId = channel.playlistId,
                userAgent = channel.userAgent,
                referrer = channel.referrer,
                addedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun recordRecent(channel: ChannelItem) {
        dao.upsertRecent(
            RecentChannelEntity(
                streamUrl = channel.streamUrl,
                name = channel.name,
                logoUrl = channel.logoUrl,
                groupTitle = channel.groupTitle,
                playlistId = channel.playlistId,
                userAgent = channel.userAgent,
                referrer = channel.referrer,
                lastWatchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearRecentChannels() {
        dao.clearRecentChannels()
    }

    suspend fun readTextFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val maxBytes = PlaylistImportValidator.MAX_LOCAL_M3U_BYTES
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(16 * 1024)
                val out = ByteArrayOutputStream()
                var totalRead = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    totalRead += read
                    if (totalRead > maxBytes) {
                        return@withContext Result.failure(
                            IOException("File is too large (max ${maxBytes / (1024 * 1024)} MB)")
                        )
                    }
                    out.write(buffer, 0, read)
                }
                Result.success(out.toString(Charsets.UTF_8.name()))
            } ?: Result.failure(IOException("Unable to open file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchM3uText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 KobaidzeIPTV/1.0")
            .header("Accept", "*/*")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty response")
            val contentLength = body.contentLength()
            val maxBytes = 20L * 1024L * 1024L
            if (contentLength > maxBytes) {
                throw IOException("Playlist is too large")
            }
            body.string()
        }
    }

    private fun resolveEpgForChannel(
        tvgId: String?,
        channelName: String,
        catchupDays: Int,
        epg: EpgLookup,
        nowMillis: Long
    ): EpgResolved {
        if (epg.programsByKey.isEmpty()) return EpgResolved(null, null, emptyList(), emptyList())

        val candidates = buildList {
            if (!tvgId.isNullOrBlank()) {
                add(tvgId.trim().lowercase())
            }
            val normalizedName = XmltvParser.normalizeChannelName(channelName)
            epg.aliasToKey[normalizedName]?.let(::add)
            if (normalizedName.isNotEmpty()) add(normalizedName)
        }

        val matchedPrograms = candidates.firstNotNullOfOrNull { key ->
            epg.programsByKey[key]?.takeIf { it.isNotEmpty() }
        } ?: return EpgResolved(null, null, emptyList(), emptyList())

        val allSorted = matchedPrograms
            .sortedBy { it.startTimeMillis }
            .map {
                EpgProgramUi(
                    title = it.title,
                    description = it.description,
                    category = it.category,
                    startTimeMillis = it.startTimeMillis,
                    endTimeMillis = it.endTimeMillis
                )
            }

        val upcoming = allSorted.filter { it.endTimeMillis > nowMillis }
        val current = upcoming.firstOrNull { nowMillis in it.startTimeMillis..it.endTimeMillis }
        val next = if (current != null) {
            upcoming.firstOrNull { it.startTimeMillis >= current.endTimeMillis }
        } else {
            upcoming.firstOrNull { it.startTimeMillis > nowMillis }
        }

        val maxArchiveDays = if (catchupDays > 0) catchupDays.coerceAtMost(14) else 7
        val archiveCutoff = nowMillis - maxArchiveDays.toLong() * 24L * 60 * 60 * 1000L
        val archivePrograms = allSorted
            .filter { it.endTimeMillis <= nowMillis && it.endTimeMillis >= archiveCutoff }
            .sortedByDescending { it.startTimeMillis }
            .take(80)

        return EpgResolved(
            current = current,
            next = next,
            schedule = upcoming.take(24),
            archivePrograms = archivePrograms
        )
    }

    private fun List<ParsedChannel>.toEntities(playlistId: Long): List<ChannelEntity> {
        return map { ch ->
            ChannelEntity(
                playlistId = playlistId,
                name = ch.name,
                streamUrl = ch.streamUrl,
                logoUrl = ch.logoUrl,
                groupTitle = ch.groupTitle,
                tvgId = ch.tvgId,
                userAgent = ch.userAgent,
                referrer = ch.referrer,
                position = ch.position,
                catchupType = ch.catchupType,
                catchupDays = ch.catchupDays,
                catchupSource = ch.catchupSource
            )
        }
    }

    private data class EpgResolved(
        val current: EpgProgramUi?,
        val next: EpgProgramUi?,
        val schedule: List<EpgProgramUi>,
        val archivePrograms: List<EpgProgramUi>
    )

    private data class EpgLookup(
        val programsByKey: Map<String, List<EpgProgramEntity>>,
        val aliasToKey: Map<String, String>
    )
}
