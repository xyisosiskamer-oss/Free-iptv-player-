package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgramUi
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldLive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChannelGridCard(
    channel: ChannelItem,
    indexNumber: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val shape = RoundedCornerShape(18.dp)
    val categoryLabel = channel.groupTitle.ifBlank { strings.generalCategory }
    val nowMillis = remember(channel.currentProgram) { System.currentTimeMillis() }

    Surface(
        modifier = modifier
            .tvFocusHighlight(
                shape = shape,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                }
            )
            .clip(shape)
            .clickable(onClick = onPlay)
            .testTag("channel_card_${indexNumber}"),
        shape = shape,
        color = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        tonalElevation = if (isPlaying) 6.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    ChannelLogo(
                        name = channel.name,
                        logoUrl = channel.logoUrl,
                        size = 54.dp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LiveStatusPill(
                            label = if (isPlaying) strings.nowPlaying else strings.liveBadge,
                            isPlaying = isPlaying
                        )
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("favorite_toggle_${indexNumber}")
                        ) {
                            Icon(
                                imageVector = if (channel.isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = if (channel.isFavorite) {
                                    strings.removeFromFavorites
                                } else {
                                    strings.addToFavorites
                                },
                                tint = if (channel.isFavorite) {
                                    CoralAccent
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(44.dp)
                )

                val currentEpg = channel.currentProgram
                if (currentEpg != null) {
                    EpgCompactSnippet(program = currentEpg, nowMillis = nowMillis)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "#${indexNumber.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelListRow(
    channel: ChannelItem,
    indexNumber: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val shape = RoundedCornerShape(16.dp)
    val categoryLabel = channel.groupTitle.ifBlank { strings.generalCategory }
    val nowMillis = remember(channel.currentProgram) { System.currentTimeMillis() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusHighlight(
                shape = shape,
                focusedScale = 1.015f,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                }
            )
            .clip(shape)
            .clickable(onClick = onPlay)
            .testTag("channel_row_${indexNumber}"),
        shape = shape,
        color = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = indexNumber.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(34.dp)
            )

            ChannelLogo(
                name = channel.name,
                logoUrl = channel.logoUrl,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val currentEpg = channel.currentProgram
                if (currentEpg != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatEpgTimeRange(currentEpg)} • ${currentEpg.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { currentEpg.progressAt(nowMillis) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (channel.playlistName.isNotBlank()) {
                            Text(
                                text = "• ${channel.playlistName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (isPlaying) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = strings.nowPlaying,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("favorite_toggle_${indexNumber}")
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = if (channel.isFavorite) {
                        strings.removeFromFavorites
                    } else {
                        strings.addToFavorites
                    },
                    tint = if (channel.isFavorite) {
                        CoralAccent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun EpgCompactSnippet(program: EpgProgramUi, nowMillis: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${formatEpgTimeRange(program)} • ${program.title}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LinearProgressIndicator(
            progress = { program.progressAt(nowMillis) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

fun formatEpgTimeRange(program: EpgProgramUi): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${fmt.format(Date(program.startTimeMillis))}–${fmt.format(Date(program.endTimeMillis))}"
}

@Composable
private fun LiveStatusPill(label: String, isPlaying: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isPlaying) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            EmeraldLive.copy(alpha = 0.14f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) MaterialTheme.colorScheme.primary else EmeraldLive)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else EmeraldLive,
                maxLines = 1
            )
        }
    }
}
