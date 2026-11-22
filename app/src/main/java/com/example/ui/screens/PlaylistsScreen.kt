package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaylistEntity
import com.example.data.repository.IptvRepository
import com.example.i18n.LocalAppStrings
import com.example.ui.components.AddPlaylistDialog
import com.example.ui.components.DeletePlaylistDialog
import com.example.ui.components.EditPlaylistDialog
import com.example.ui.components.tvFocusHighlight
import com.example.ui.theme.EmeraldLive
import com.example.ui.viewmodel.TeleWaveUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun PlaylistsScreen(
    uiState: TeleWaveUiState,
    isTv: Boolean,
    onAddPlaylistUrl: (name: String, url: String, epgUrl: String?) -> Unit,
    onImportPlaylistUri: (name: String, uri: Uri, displayName: String, epgUrl: String?) -> Unit,
    onEditPlaylist: (PlaylistEntity, newName: String, newUrl: String, newEpgUrl: String?) -> Unit,
    onTogglePlaylistEnabled: (PlaylistEntity, Boolean) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onRefreshPlaylist: (PlaylistEntity) -> Unit,
    onOpenPlaylistChannels: (Long) -> Unit,
    onRestoreDefaultPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var deletingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    val hasDefaultPlaylist = uiState.playlists.any {
        it.url == IptvRepository.DEFAULT_PLAYLIST_URL
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp || isTv

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.playlistsTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.playlistsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_playlist_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.addPlaylist)
                }
            }

            if (uiState.playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistPlay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = strings.emptyPlaylistsTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = strings.emptyPlaylistsSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onRestoreDefaultPlaylist) {
                                    Icon(Icons.Filled.Restore, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.restoreDefaultPlaylist)
                                }
                                Button(onClick = { showAddDialog = true }) {
                                    Text(strings.addPlaylist)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = if (isWide) 24.dp else 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            isActiveFilter = uiState.activePlaylistId == playlist.id,
                            isSyncing = uiState.syncingPlaylistIds.contains(playlist.id),
                            onOpenChannels = { onOpenPlaylistChannels(playlist.id) },
                            onToggleEnabled = { enabled -> onTogglePlaylistEnabled(playlist, enabled) },
                            onRefresh = { onRefreshPlaylist(playlist) },
                            onEdit = { editingPlaylist = playlist },
                            onDelete = { deletingPlaylist = playlist }
                        )
                    }

                    if (!hasDefaultPlaylist) {
                        item(key = "restore_default") {
                            OutlinedButton(
                                onClick = onRestoreDefaultPlaylist,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("restore_default_playlist_button")
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.restoreDefaultPlaylist)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddDialog = false },
            onAddUrl = onAddPlaylistUrl,
            onImportFile = onImportPlaylistUri
        )
    }

    editingPlaylist?.let { target ->
        EditPlaylistDialog(
            playlist = target,
            onDismiss = { editingPlaylist = null },
            onSave = { newName, newUrl, newEpgUrl ->
                onEditPlaylist(target, newName, newUrl, newEpgUrl)
            }
        )
    }

    deletingPlaylist?.let { target ->
        DeletePlaylistDialog(
            playlist = target,
            onDismiss = { deletingPlaylist = null },
            onConfirm = { onDeletePlaylist(target) }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistEntity,
    isActiveFilter: Boolean,
    isSyncing: Boolean,
    onOpenChannels: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val shape = RoundedCornerShape(20.dp)
    val dateFormatter = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusHighlight(
                shape = shape,
                focusedScale = 1.01f,
                unfocusedBorderColor = if (isActiveFilter) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (playlist.isDefault) {
                            BadgePill(
                                text = strings.preloadedBadge,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!playlist.epgUrl.isNullOrBlank()) {
                            BadgePill(
                                text = strings.epgBadge,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (isActiveFilter) {
                            BadgePill(
                                text = strings.activeBadge,
                                containerColor = EmeraldLive.copy(alpha = 0.16f),
                                contentColor = EmeraldLive
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = playlist.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = playlist.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("playlist_enable_switch_${playlist.id}")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.channelsCountLabel(playlist.channelCount)} • ${
                        if (playlist.lastUpdatedAt > 0L) {
                            "${strings.lastUpdatedLabel}: ${dateFormatter.format(Date(playlist.lastUpdatedAt))}"
                        } else {
                            strings.neverUpdated
                        }
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!playlist.lastError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.playlistLoadError(playlist.lastError),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenChannels,
                    enabled = playlist.isEnabled,
                    modifier = Modifier.testTag("open_playlist_channels_${playlist.id}")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.switchToPlaylist)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!playlist.isLocalFile) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_playlist_${playlist.id}")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = strings.refreshPlaylist,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_playlist_${playlist.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = strings.edit
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_playlist_${playlist.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = strings.delete,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
