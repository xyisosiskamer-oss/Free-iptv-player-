package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.i18n.LocalAppStrings
import com.example.i18n.stringsFor
import com.example.ui.components.LanguageSwitcherChip
import com.example.ui.components.isTelevisionDevice
import com.example.ui.components.tvFocusHighlight
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.TeleWaveTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.TeleWaveViewModel

@Composable
fun TeleWaveApp(viewModel: TeleWaveViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = remember(uiState.language) { stringsFor(uiState.language) }
    val context = LocalContext.current
    val isTv = remember(context) { isTelevisionDevice(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    TeleWaveTheme(language = uiState.language) {
        CompositionLocalProvider(LocalAppStrings provides strings) {
            val activeSession = uiState.playerSession
            if (activeSession != null) {
                PlayerScreen(
                    session = activeSession,
                    favoriteChannels = uiState.favoriteChannels,
                    resizeMode = uiState.resizeMode,
                    isTv = isTv,
                    httpClient = viewModel.repository.playerHttpClient,
                    onBack = { viewModel.closePlayer() },
                    onSelectChannel = { ch -> viewModel.switchPlayerChannel(ch) },
                    onNextChannel = { viewModel.zapNextChannel() },
                    onPreviousChannel = { viewModel.zapPreviousChannel() },
                    onToggleFavorite = { ch -> viewModel.toggleFavorite(ch) },
                    onCycleResizeMode = { viewModel.cycleResizeMode() },
                    onPlayArchiveProgram = { prog -> viewModel.playArchiveProgram(prog) },
                    onReturnToLive = { viewModel.returnToLive() }
                )
            } else {
                BackHandler(enabled = uiState.currentTab != AppTab.CHANNELS) {
                    viewModel.selectTab(AppTab.CHANNELS)
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = isTv || maxWidth >= 680.dp

                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.navigationBarsPadding()
                            )
                        },
                        topBar = {
                            TeleWaveTopBar(
                                appName = strings.appName,
                                subtitle = strings.tagline,
                                language = uiState.language,
                                onSelectLanguage = { viewModel.setLanguage(it) },
                                onOpenSearch = { viewModel.selectTab(AppTab.CHANNELS) },
                                modifier = Modifier.statusBarsPadding()
                            )
                        },
                        bottomBar = {
                            if (!useRail) {
                                TeleWaveBottomBar(
                                    currentTab = uiState.currentTab,
                                    favoritesCount = uiState.favoriteChannels.size,
                                    playlistsCount = uiState.playlists.size,
                                    onSelectTab = { viewModel.selectTab(it) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                TeleWaveNavigationRail(
                                    currentTab = uiState.currentTab,
                                    favoritesCount = uiState.favoriteChannels.size,
                                    playlistsCount = uiState.playlists.size,
                                    onSelectTab = { viewModel.selectTab(it) }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                when (uiState.currentTab) {
                                    AppTab.CHANNELS -> ChannelsScreen(
                                        uiState = uiState,
                                        isTv = isTv,
                                        onSelectPlaylist = { viewModel.selectActivePlaylist(it) },
                                        onSelectCategory = { viewModel.selectCategory(it) },
                                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                        onToggleViewMode = { viewModel.toggleViewMode() },
                                        onRefresh = { viewModel.refreshActiveOrAllPlaylists() },
                                        onManagePlaylists = { viewModel.selectTab(AppTab.PLAYLISTS) },
                                        onClearFilters = { viewModel.clearChannelFilters() },
                                        onPlayChannel = { ch, queue -> viewModel.playChannel(ch, queue) },
                                        onToggleFavorite = { ch -> viewModel.toggleFavorite(ch) }
                                    )

                                    AppTab.FAVORITES -> FavoritesScreen(
                                        uiState = uiState,
                                        isTv = isTv,
                                        onSearchQueryChange = { viewModel.updateFavoritesSearchQuery(it) },
                                        onToggleViewMode = { viewModel.toggleViewMode() },
                                        onBrowseChannels = { viewModel.selectTab(AppTab.CHANNELS) },
                                        onPlayChannel = { ch, queue -> viewModel.playChannel(ch, queue) },
                                        onToggleFavorite = { ch -> viewModel.toggleFavorite(ch) }
                                    )

                                    AppTab.PLAYLISTS -> PlaylistsScreen(
                                        uiState = uiState,
                                        isTv = isTv,
                                        onAddPlaylistUrl = { name, url, epgUrl ->
                                            viewModel.addPlaylistFromUrl(name, url, epgUrl)
                                        },
                                        onImportPlaylistUri = { name, uri, displayName, epgUrl ->
                                            viewModel.importPlaylistFromUri(name, uri, displayName, epgUrl)
                                        },
                                        onEditPlaylist = { pl, newName, newUrl, newEpgUrl ->
                                            viewModel.editPlaylist(pl, newName, newUrl, newEpgUrl)
                                        },
                                        onTogglePlaylistEnabled = { pl, enabled ->
                                            viewModel.togglePlaylistEnabled(pl, enabled)
                                        },
                                        onDeletePlaylist = { pl -> viewModel.deletePlaylist(pl) },
                                        onRefreshPlaylist = { pl -> viewModel.refreshPlaylist(pl) },
                                        onOpenPlaylistChannels = { id ->
                                            viewModel.openPlaylistInChannels(id)
                                        },
                                        onRestoreDefaultPlaylist = { viewModel.restoreDefaultPlaylist() }
                                    )

                                    AppTab.SETTINGS -> SettingsScreen(
                                        uiState = uiState,
                                        isTv = isTv,
                                        onSelectLanguage = { viewModel.setLanguage(it) },
                                        onSelectViewMode = { viewModel.setViewMode(it) },
                                        onSelectResizeMode = { viewModel.setResizeMode(it) },
                                        onSaveCustomEpgUrl = { viewModel.saveAndSyncCustomEpgUrl(it) },
                                        onSyncEpg = { viewModel.syncAllEpg() },
                                        onRefreshAll = { viewModel.refreshActiveOrAllPlaylists() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeleWaveTopBar(
    appName: String,
    subtitle: String,
    language: com.example.i18n.AppLanguage,
    onSelectLanguage: (com.example.i18n.AppLanguage) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.telewave_icon_1795372895446),
                        contentDescription = appName,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Column {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = strings.searchChannelsHint,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                LanguageSwitcherChip(
                    currentLanguage = language,
                    onSelectLanguage = onSelectLanguage
                )
            }
        }
    }
}

@Composable
private fun TeleWaveBottomBar(
    currentTab: AppTab,
    favoritesCount: Int,
    playlistsCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    val strings = LocalAppStrings.current
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        val items = listOf(
            NavDest(AppTab.CHANNELS, strings.navChannels, Icons.Filled.LiveTv, Icons.Outlined.LiveTv, 0, "nav_channels"),
            NavDest(AppTab.FAVORITES, strings.navFavorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, favoritesCount, "nav_favorites"),
            NavDest(AppTab.PLAYLISTS, strings.navPlaylists, Icons.Filled.PlaylistPlay, Icons.Outlined.PlaylistPlay, playlistsCount, "nav_playlists"),
            NavDest(AppTab.SETTINGS, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings, 0, "nav_settings")
        )
        items.forEach { dest ->
            val selected = currentTab == dest.tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(dest.tab) },
                modifier = Modifier.testTag(dest.testTag),
                icon = {
                    NavIconWithBadge(
                        icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        label = dest.label,
                        badgeCount = dest.badgeCount,
                        isFavorite = dest.tab == AppTab.FAVORITES && selected
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun TeleWaveNavigationRail(
    currentTab: AppTab,
    favoritesCount: Int,
    playlistsCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    val strings = LocalAppStrings.current
    val items = listOf(
        NavDest(AppTab.CHANNELS, strings.navChannels, Icons.Filled.LiveTv, Icons.Outlined.LiveTv, 0, "nav_rail_channels"),
        NavDest(AppTab.FAVORITES, strings.navFavorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, favoritesCount, "nav_rail_favorites"),
        NavDest(AppTab.PLAYLISTS, strings.navPlaylists, Icons.Filled.PlaylistPlay, Icons.Outlined.PlaylistPlay, playlistsCount, "nav_rail_playlists"),
        NavDest(AppTab.SETTINGS, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings, 0, "nav_rail_settings")
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .navigationBarsPadding()
    ) {
        Spacer(modifier = Modifier.weight(0.15f))
        items.forEach { dest ->
            val selected = currentTab == dest.tab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(dest.tab) },
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .tvFocusHighlight(shape = RoundedCornerShape(16.dp))
                    .testTag(dest.testTag),
                icon = {
                    NavIconWithBadge(
                        icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        label = dest.label,
                        badgeCount = dest.badgeCount,
                        isFavorite = dest.tab == AppTab.FAVORITES && selected
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NavIconWithBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badgeCount: Int,
    isFavorite: Boolean
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = if (isFavorite) CoralAccent else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFavorite) CoralAccent else androidx.compose.ui.graphics.Color.Unspecified
            )
        }
    } else {
        Icon(imageVector = icon, contentDescription = label)
    }
}

private data class NavDest(
    val tab: AppTab,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeCount: Int,
    val testTag: String
)
