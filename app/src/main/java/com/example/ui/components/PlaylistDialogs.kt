package com.example.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaylistEntity
import com.example.data.parser.PlaylistImportValidator
import com.example.data.parser.XtreamCredentials
import com.example.i18n.LocalAppStrings

@Composable
fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAddUrl: (name: String, url: String, epgUrl: String?) -> Unit,
    onImportFile: (name: String, uri: Uri, displayName: String, epgUrl: String?) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var mode by remember { mutableStateOf(0) } // 0 = URL, 1 = Xtream, 2 = Local File
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = resolveFileName(context, uri) ?: "playlist.m3u"
            onImportFile(name, uri, fileName, epgUrl.trim().ifBlank { null })
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = strings.addPlaylist,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        strings.sourceModeUrl,
                        strings.sourceModeXtream,
                        strings.sourceModeFile
                    )
                    modes.forEachIndexed { idx, label ->
                        val selected = mode == idx
                        val pillShape = RoundedCornerShape(12.dp)
                        Surface(
                            shape = pillShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusHighlight(shape = pillShape)
                                .clip(pillShape)
                                .clickable {
                                    mode = idx
                                    showError = false
                                }
                                .testTag("add_playlist_mode_$idx")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text(strings.playlistNameLabel) },
                    placeholder = { Text(strings.playlistNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_playlist_name_input")
                )

                when (mode) {
                    0 -> {
                        OutlinedTextField(
                            value = url,
                            onValueChange = {
                                url = it
                                showError = false
                            },
                            label = { Text(strings.playlistUrlLabel) },
                            placeholder = { Text(strings.playlistUrlPlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_playlist_url_input")
                        )

                        OutlinedTextField(
                            value = epgUrl,
                            onValueChange = { epgUrl = it },
                            label = { Text(strings.playlistEpgUrlLabel) },
                            placeholder = { Text(strings.playlistEpgUrlPlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_playlist_epg_input")
                        )
                    }

                    1 -> {
                        OutlinedTextField(
                            value = xtreamServer,
                            onValueChange = {
                                xtreamServer = it
                                showError = false
                            },
                            label = { Text(strings.xtreamServerLabel) },
                            placeholder = { Text(strings.xtreamServerPlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_xtream_server_input")
                        )

                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = {
                                xtreamUser = it
                                showError = false
                            },
                            label = { Text(strings.xtreamUsernameLabel) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_xtream_user_input")
                        )

                        OutlinedTextField(
                            value = xtreamPass,
                            onValueChange = {
                                xtreamPass = it
                                showError = false
                            },
                            label = { Text(strings.xtreamPasswordLabel) },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_xtream_pass_input")
                        )
                    }

                    else -> {
                        OutlinedTextField(
                            value = epgUrl,
                            onValueChange = { epgUrl = it },
                            label = { Text(strings.playlistEpgUrlLabel) },
                            placeholder = { Text(strings.playlistEpgUrlPlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_playlist_file_epg_input")
                        )

                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "audio/x-mpegurl",
                                        "audio/mpegurl",
                                        "application/vnd.apple.mpegurl",
                                        "application/x-mpegurl",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_m3u_file_button")
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.importM3uFile)
                        }
                    }
                }

                if (showError) {
                    Text(
                        text = strings.invalidPlaylistInput,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (mode != 2) {
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (mode == 0) {
                            val trimmedUrl = url.trim()
                            if (trimmedName.isBlank() ||
                                !com.example.data.parser.PlaylistImportValidator.isValidHttpUrl(trimmedUrl)
                            ) {
                                showError = true
                            } else {
                                onAddUrl(trimmedName, trimmedUrl, epgUrl.trim().ifBlank { null })
                                onDismiss()
                            }
                        } else {
                            val endpoints = com.example.data.parser.PlaylistImportValidator.buildXtreamEndpoints(
                                com.example.data.parser.XtreamCredentials(
                                    serverUrl = xtreamServer,
                                    username = xtreamUser,
                                    password = xtreamPass
                                )
                            )
                            val resolvedName = trimmedName.ifBlank { xtreamUser.trim() }
                            if (endpoints == null || resolvedName.isBlank()) {
                                showError = true
                            } else {
                                onAddUrl(resolvedName, endpoints.playlistUrl, endpoints.epgUrl)
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_playlist_button")
                ) {
                    Text(strings.save)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun EditPlaylistDialog(
    playlist: PlaylistEntity,
    onDismiss: () -> Unit,
    onSave: (newName: String, newUrl: String, newEpgUrl: String?) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember(playlist) { mutableStateOf(playlist.name) }
    var url by remember(playlist) { mutableStateOf(playlist.url) }
    var epgUrl by remember(playlist) { mutableStateOf(playlist.epgUrl.orEmpty()) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = strings.editPlaylist,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text(strings.playlistNameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_playlist_name_input")
                )

                if (!playlist.isLocalFile) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            showError = false
                        },
                        label = { Text(strings.playlistUrlLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_playlist_url_input")
                    )
                } else {
                    Text(
                        text = playlist.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = epgUrl,
                    onValueChange = { epgUrl = it },
                    label = { Text(strings.playlistEpgUrlLabel) },
                    placeholder = { Text(strings.playlistEpgUrlPlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_playlist_epg_input")
                )

                if (showError) {
                    Text(
                        text = strings.invalidPlaylistInput,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedUrl = url.trim()
                    val urlValid = playlist.isLocalFile ||
                        trimmedUrl.startsWith("http://", ignoreCase = true) ||
                        trimmedUrl.startsWith("https://", ignoreCase = true)
                    if (trimmedName.isBlank() || !urlValid) {
                        showError = true
                    } else {
                        onSave(trimmedName, trimmedUrl, epgUrl.trim().ifBlank { null })
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_edit_playlist_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun DeletePlaylistDialog(
    playlist: PlaylistEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = strings.deletePlaylist,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = strings.deletePlaylistConfirm(playlist.name),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_delete_playlist_button")
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

private fun resolveFileName(context: android.content.Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }
}
