package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelItem
import com.example.data.preferences.ChannelViewMode
import com.example.i18n.LocalAppStrings
import com.example.ui.components.ChannelGridCard
import com.example.ui.components.ChannelListRow
import com.example.ui.theme.CoralAccent
import com.example.ui.viewmodel.TeleWaveUiState

@Composable
fun FavoritesScreen(
    uiState: TeleWaveUiState,
    isTv: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onBrowseChannels: () -> Unit,
    onPlayChannel: (ChannelItem, List<ChannelItem>) -> Unit,
    onToggleFavorite: (ChannelItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val playingUrl = uiState.playerSession?.currentChannel?.streamUrl

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp || isTv
        val minCardWidth = if (isTv) 220.dp else 176.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = CoralAccent,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.favoritesTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = strings.favoritesSubtitle(uiState.favoriteChannels.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        IconButton(
                            onClick = onToggleViewMode,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("favorites_toggle_view_button")
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
                                }
                            )
                        }
                    }
                }

                if (uiState.favoriteChannels.isNotEmpty()) {
                    OutlinedTextField(
                        value = uiState.favoritesSearchQuery,
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
                            if (uiState.favoritesSearchQuery.isNotEmpty()) {
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
                            .fillMaxWidth()
                            .testTag("favorites_search_input")
                    )
                }
            }

            if (uiState.filteredFavorites.isEmpty()) {
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
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = CoralAccent,
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .size(36.dp)
                                )
                            }
                            Text(
                                text = strings.emptyFavoritesTitle,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.emptyFavoritesSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onBrowseChannels,
                                modifier = Modifier.testTag("favorites_browse_channels_button")
                            ) {
                                Text(strings.browseChannelsBtn)
                            }
                        }
                    }
                }
            } else if (uiState.viewMode == ChannelViewMode.GRID) {
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
                    itemsIndexed(
                        items = uiState.filteredFavorites,
                        key = { _, item -> "fav_${item.streamUrl}" }
                    ) { idx, channel ->
                        ChannelGridCard(
                            channel = channel,
                            indexNumber = idx + 1,
                            isPlaying = channel.streamUrl == playingUrl,
                            onPlay = { onPlayChannel(channel, uiState.filteredFavorites) },
                            onToggleFavorite = { onToggleFavorite(channel) }
                        )
                    }
                }
            } else {
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
                    itemsIndexed(
                        items = uiState.filteredFavorites,
                        key = { _, item -> "fav_${item.streamUrl}" }
                    ) { idx, channel ->
                        ChannelListRow(
                            channel = channel,
                            indexNumber = idx + 1,
                            isPlaying = channel.streamUrl == playingUrl,
                            onPlay = { onPlayChannel(channel, uiState.filteredFavorites) },
                            onToggleFavorite = { onToggleFavorite(channel) }
                        )
                    }
                }
            }
        }
    }
}
