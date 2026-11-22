package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChannelItem
import com.example.data.preferences.ChannelViewMode
import com.example.i18n.LocalAppStrings
import com.example.ui.components.ChannelGridCard
import com.example.ui.components.ChannelListRow
import com.example.ui.components.ChannelLogo
import com.example.ui.components.tvFocusHighlight
import com.example.ui.viewmodel.TeleWaveUiState

@Composable
fun ChannelsScreen(
    uiState: TeleWaveUiState,
    isTv: Boolean,
    onSelectPlaylist: (Long?) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onRefresh: () -> Unit,
    onManagePlaylists: () -> Unit,
    onClearFilters: () -> Unit,
    onPlayChannel: (ChannelItem, List<ChannelItem>) -> Unit,
    onToggleFavorite: (ChannelItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val enabledPlaylists = uiState.playlists.filter { it.isEnabled }
    val playingUrl = uiState.playerSession?.currentChannel?.streamUrl

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp || isTv
        val minCardWidth = if (isTv) 220.dp else 176.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Controls: Playlist Switcher + Search + Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Playlist Switcher Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "all_playlists") {
                        SwitcherPill(
                            label = strings.allPlaylists,
                            badge = uiState.playlists.filter { it.isEnabled }.sumOf { it.channelCount },
                            selected = uiState.activePlaylistId == null,
                            onClick = { onSelectPlaylist(null) },
                            testTag = "playlist_pill_all"
                        )
                    }

                    items(enabledPlaylists, key = { it.id }) { playlist ->
                        SwitcherPill(
                            label = playlist.name,
                            badge = playlist.channelCount,
                            selected = uiState.activePlaylistId == playlist.id,
                            onClick = { onSelectPlaylist(playlist.id) },
                            testTag = "playlist_pill_${playlist.id}"
                        )
                    }

                    item(key = "manage_playlists") {
                        val shape = RoundedCornerShape(50)
                        Surface(
                            modifier = Modifier
                                .tvFocusHighlight(shape = shape)
                                .clip(shape)
                                .clickable(onClick = onManagePlaylists)
                                .minimumInteractiveComponentSize()
                                .testTag("manage_playlists_shortcut"),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = strings.managePlaylists,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.managePlaylists,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Search & View Mode / Refresh Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = strings.searchChannelsHint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = strings.clearFilters)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("channels_search_input")
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        IconButton(
                            onClick = onToggleViewMode,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_view_mode_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.viewMode == ChannelViewMode.GRID) {
                                    Icons.AutoMirrored.Filled.ViewList
                                } else {
                                    Icons.Filled.GridView
                                },
                                contentDescription = if (uiState.viewMode == ChannelViewMode.GRID) {
                                    strings.listViewLabel
                                } else {
                                    strings.gridViewLabel
                                },
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_channels_button")
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
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
                }

                // Category Chips
                if (uiState.categories.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item(key = "cat_all") {
                            CategoryChip(
                                label = "${strings.allCategories} (${uiState.allChannels.size})",
                                selected = uiState.selectedCategory == null,
                                onClick = { onSelectCategory(null) }
                            )
                        }
                        items(uiState.categories, key = { it.ifEmpty { "__general__" } }) { cat ->
                            val displayCat = cat.ifBlank { strings.generalCategory }
                            CategoryChip(
                                label = displayCat,
                                selected = uiState.selectedCategory == cat,
                                onClick = { onSelectCategory(cat) }
                            )
                        }
                    }
                }
            }

            // Main Content
            when {
                uiState.isSyncing && uiState.allChannels.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = strings.syncingPlaylist,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.filteredChannels.isEmpty() -> {
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
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LiveTv,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = strings.noChannelsTitle,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = strings.noChannelsSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (uiState.searchQuery.isNotEmpty() || uiState.selectedCategory != null) {
                                        OutlinedButton(onClick = onClearFilters) {
                                            Text(strings.clearFilters)
                                        }
                                    }
                                    Button(onClick = onRefresh) {
                                        Text(strings.retryLoad)
                                    }
                                }
                            }
                        }
                    }
                }

                uiState.viewMode == ChannelViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = minCardWidth),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = if (isWide) 24.dp else 96.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.recentChannels.isNotEmpty() &&
                            uiState.searchQuery.isBlank() &&
                            uiState.selectedCategory == null
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "recent_strip") {
                                RecentHeroStrip(
                                    recents = uiState.recentChannels,
                                    onPlay = { ch -> onPlayChannel(ch, uiState.filteredChannels) }
                                )
                            }
                        }

                        itemsIndexed(
                            items = uiState.filteredChannels,
                            key = { _, item -> "${item.playlistId}_${item.id}_${item.streamUrl}" }
                        ) { idx, channel ->
                            ChannelGridCard(
                                channel = channel,
                                indexNumber = idx + 1,
                                isPlaying = channel.streamUrl == playingUrl,
                                onPlay = { onPlayChannel(channel, uiState.filteredChannels) },
                                onToggleFavorite = { onToggleFavorite(channel) }
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = if (isWide) 24.dp else 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.recentChannels.isNotEmpty() &&
                            uiState.searchQuery.isBlank() &&
                            uiState.selectedCategory == null
                        ) {
                            item(key = "recent_strip_list") {
                                RecentHeroStrip(
                                    recents = uiState.recentChannels,
                                    onPlay = { ch -> onPlayChannel(ch, uiState.filteredChannels) }
                                )
                            }
                        }

                        itemsIndexed(
                            items = uiState.filteredChannels,
                            key = { _, item -> "${item.playlistId}_${item.id}_${item.streamUrl}" }
                        ) { idx, channel ->
                            ChannelListRow(
                                channel = channel,
                                indexNumber = idx + 1,
                                isPlaying = channel.streamUrl == playingUrl,
                                onPlay = { onPlayChannel(channel, uiState.filteredChannels) },
                                onToggleFavorite = { onToggleFavorite(channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentHeroStrip(
    recents: List<ChannelItem>,
    onPlay: (ChannelItem) -> Unit
) {
    val strings = LocalAppStrings.current
    val shape = RoundedCornerShape(20.dp)

    Surface(
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_tv_backdrop_1795372895882),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.continueWatching,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recents, key = { it.streamUrl }) { ch ->
                        val chipShape = RoundedCornerShape(14.dp)
                        Surface(
                            modifier = Modifier
                                .tvFocusHighlight(shape = chipShape)
                                .clip(chipShape)
                                .clickable { onPlay(ch) },
                            shape = chipShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ChannelLogo(name = ch.name, logoUrl = ch.logoUrl, size = 32.dp)
                                Text(
                                    text = ch.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitcherPill(
    label: String,
    badge: Int,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = RoundedCornerShape(50)
    Surface(
        modifier = Modifier
            .tvFocusHighlight(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlaylistPlay,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badge > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Text(
                        text = badge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .tvFocusHighlight(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}
