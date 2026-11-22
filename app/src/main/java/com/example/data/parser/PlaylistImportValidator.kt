package com.example.data.parser

import java.security.MessageDigest

data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)

data class XtreamEndpoints(
    val playlistUrl: String,
    val epgUrl: String
)

sealed interface PlaylistValidationResult {
    data class Valid(
        val parseResult: M3uParseResult,
        val contentHash: String
    ) : PlaylistValidationResult

    data class Invalid(val reason: String) : PlaylistValidationResult
}

object PlaylistImportValidator {
    const val MAX_LOCAL_M3U_BYTES: Long = 15L * 1024L * 1024L // 15 MB safety cap

    fun isValidHttpUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        return (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)) &&
            trimmed.length > 10 &&
            !trimmed.any { it.isWhitespace() }
    }

    fun buildXtreamEndpoints(credentials: XtreamCredentials): XtreamEndpoints? {
        val rawServer = credentials.serverUrl.trim().trimEnd('/')
        val user = credentials.username.trim()
        val pass = credentials.password.trim()
        if (rawServer.isBlank() || user.isBlank() || pass.isBlank()) return null

        val normalizedServer = if (
            rawServer.startsWith("http://", ignoreCase = true) ||
            rawServer.startsWith("https://", ignoreCase = true)
        ) {
            rawServer
        } else {
            "http://$rawServer"
        }

        if (!isValidHttpUrl(normalizedServer)) return null

        val encodedUser = encodeQueryComponent(user)
        val encodedPass = encodeQueryComponent(pass)

        return XtreamEndpoints(
            playlistUrl = "$normalizedServer/get.php?username=$encodedUser&password=$encodedPass&type=m3u_plus&output=ts",
            epgUrl = "$normalizedServer/xmltv.php?username=$encodedUser&password=$encodedPass"
        )
    }

    fun validateM3uContent(rawContent: String): PlaylistValidationResult {
        if (rawContent.isBlank()) {
            return PlaylistValidationResult.Invalid("Empty playlist content")
        }
        val parseResult = M3uParser.parsePlaylist(rawContent)
        if (parseResult.channels.isEmpty()) {
            return PlaylistValidationResult.Invalid("No valid channels found in M3U")
        }
        val hash = sha256(rawContent)
        return PlaylistValidationResult.Valid(parseResult = parseResult, contentHash = hash)
    }

    fun extractFirstHttpUrl(rawText: String): String? {
        val candidate = rawText.lineSequence()
            .map { it.trim() }
            .flatMap { it.split(' ', '\t').asSequence() }
            .firstOrNull { isValidHttpUrl(it) }
        return candidate
    }

    fun suggestNameFromDisplayName(displayName: String, fallback: String = "Imported Playlist"): String {
        val clean = displayName
            .substringAfterLast('/')
            .substringBefore('?')
            .substringBeforeLast('.')
            .trim()
        return clean.ifBlank { fallback }
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun encodeQueryComponent(value: String): String {
        val out = StringBuilder(value.length)
        for (ch in value) {
            when {
                ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '.' || ch == '~' -> out.append(ch)
                else -> {
                    for (b in ch.toString().toByteArray(Charsets.UTF_8)) {
                        out.append('%')
                        out.append("%02X".format(b))
                    }
                }
            }
        }
        return out.toString()
    }
}
