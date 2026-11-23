package com.example.data.parser

data class ParsedChannel(
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupTitle: String,
    val tvgId: String?,
    val userAgent: String?,
    val referrer: String?,
    val position: Int,
    val catchupType: String? = null,
    val catchupDays: Int = 0,
    val catchupSource: String? = null
)

data class M3uParseResult(
    val channels: List<ParsedChannel>,
    val epgUrl: String?
)

object M3uParser {
    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s,]*))""")

    fun parse(rawContent: String): List<ParsedChannel> = parsePlaylist(rawContent).channels

    fun parsePlaylist(rawContent: String, baseUrl: String? = null): M3uParseResult {
        val cleaned = rawContent.removePrefix("\uFEFF")
        val lines = cleaned.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (lines.isEmpty()) return M3uParseResult(emptyList(), null)

        val channels = mutableListOf<ParsedChannel>()
        var discoveredEpgUrl: String? = null
        var defaultCatchupType: String? = null
        var defaultCatchupDays = 0
        var defaultCatchupSource: String? = null

        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String = ""
        var pendingTvgId: String? = null
        var pendingUserAgent: String? = null
        var pendingReferrer: String? = null
        var pendingCatchupType: String? = null
        var pendingCatchupDays = 0
        var pendingCatchupSource: String? = null
        var hasMetadataForNextUrl = false

        for (line in lines) {
            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    val attrs = parseAttributes(line)
                    val epgCandidate = attrs["url-tvg"]
                        ?: attrs["x-tvg-url"]
                        ?: attrs["tvg-url"]
                    if (!epgCandidate.isNullOrBlank()) {
                        // Some playlists list comma-separated EPG URLs; take the first valid URL
                        discoveredEpgUrl = epgCandidate.split(',', ';')
                            .map { it.trim() }
                            .firstOrNull {
                                it.startsWith("http://", ignoreCase = true) ||
                                    it.startsWith("https://", ignoreCase = true)
                            }
                    }
                    defaultCatchupType = (attrs["catchup"] ?: attrs["catchup-type"])?.takeIf { it.isNotBlank() }
                    defaultCatchupDays = (attrs["catchup-days"] ?: attrs["tvg-rec"] ?: attrs["timeshift"])
                        ?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    defaultCatchupSource = attrs["catchup-source"]?.takeIf { it.isNotBlank() }
                }

                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val commaIndex = findCommaOutsideQuotes(line)
                    val headerPart = if (commaIndex >= 0) line.substring(0, commaIndex) else line
                    val titlePart = if (commaIndex >= 0) line.substring(commaIndex + 1).trim() else ""

                    val attrs = parseAttributes(headerPart)

                    pendingTvgId = attrs["tvg-id"]?.takeIf { it.isNotBlank() }
                    pendingLogo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                        ?: attrs["logo"]?.takeIf { it.isNotBlank() }
                    pendingGroup = attrs["group-title"]?.trim().orEmpty()
                    val tvgName = attrs["tvg-name"]?.takeIf { it.isNotBlank() }
                    pendingName = titlePart.ifBlank { tvgName } ?: "TV Channel"
                    pendingUserAgent = (attrs["user-agent"] ?: attrs["http-user-agent"])?.takeIf { it.isNotBlank() }
                        ?: pendingUserAgent
                    pendingReferrer = (attrs["referer"] ?: attrs["referrer"] ?: attrs["http-referrer"])
                        ?.takeIf { it.isNotBlank() } ?: pendingReferrer

                    pendingCatchupType = (attrs["catchup"] ?: attrs["catchup-type"])
                        ?.takeIf { it.isNotBlank() } ?: defaultCatchupType
                    pendingCatchupDays = (attrs["catchup-days"] ?: attrs["tvg-rec"] ?: attrs["timeshift"])
                        ?.toIntOrNull()?.coerceAtLeast(0) ?: defaultCatchupDays
                    pendingCatchupSource = attrs["catchup-source"]?.takeIf { it.isNotBlank() }
                        ?: defaultCatchupSource

                    hasMetadataForNextUrl = true
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    val grp = line.substringAfter(":", "").trim()
                    if (grp.isNotEmpty()) {
                        pendingGroup = grp
                    }
                }

                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val opt = line.substringAfter(":", "").trim()
                    when {
                        opt.startsWith("http-user-agent=", ignoreCase = true) -> {
                            pendingUserAgent = opt.substringAfter("=").trim().takeIf { it.isNotBlank() }
                        }
                        opt.startsWith("http-referrer=", ignoreCase = true) -> {
                            pendingReferrer = opt.substringAfter("=").trim().takeIf { it.isNotBlank() }
                        }
                    }
                }

                line.startsWith("#") -> Unit

                looksLikeStreamUrl(line) || (hasMetadataForNextUrl && isResolvableRelativeUrl(line, baseUrl)) -> {
                    val resolvedLine = resolveUrlIfRelative(line, baseUrl)
                    val (cleanUrl, inlineUa, inlineRef) = splitInlineHeaders(resolvedLine)
                    val finalName = pendingName?.takeIf { it.isNotBlank() }
                        ?: deriveNameFromUrl(cleanUrl, channels.size + 1)
                    channels.add(
                        ParsedChannel(
                            name = finalName,
                            streamUrl = cleanUrl,
                            logoUrl = pendingLogo,
                            groupTitle = pendingGroup,
                            tvgId = pendingTvgId,
                            userAgent = inlineUa ?: pendingUserAgent,
                            referrer = inlineRef ?: pendingReferrer,
                            position = channels.size,
                            catchupType = pendingCatchupType ?: defaultCatchupType,
                            catchupDays = if (hasMetadataForNextUrl) pendingCatchupDays else defaultCatchupDays,
                            catchupSource = pendingCatchupSource ?: defaultCatchupSource
                        )
                    )
                    pendingName = null
                    pendingLogo = null
                    pendingGroup = ""
                    pendingTvgId = null
                    pendingUserAgent = null
                    pendingReferrer = null
                    pendingCatchupType = null
                    pendingCatchupDays = 0
                    pendingCatchupSource = null
                    hasMetadataForNextUrl = false
                }

                hasMetadataForNextUrl -> {
                    hasMetadataForNextUrl = false
                }
            }
        }

        return M3uParseResult(channels = channels, epgUrl = discoveredEpgUrl)
    }

    private fun parseAttributes(headerPart: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        attrRegex.findAll(headerPart).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2].ifEmpty {
                match.groupValues[3].ifEmpty { match.groupValues[4] }
            }.trim()
            attrs[key] = value
        }
        return attrs
    }

    private fun findCommaOutsideQuotes(line: String): Int {
        var inDoubleQuotes = false
        var inSingleQuotes = false
        for (i in line.indices) {
            when (line[i]) {
                '"' -> if (!inSingleQuotes) inDoubleQuotes = !inDoubleQuotes
                '\'' -> if (!inDoubleQuotes) inSingleQuotes = !inSingleQuotes
                ',' -> if (!inDoubleQuotes && !inSingleQuotes) return i
            }
        }
        return -1
    }

    private fun looksLikeStreamUrl(line: String): Boolean {
        val lower = line.lowercase()
        return lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("rtsp://") ||
            lower.startsWith("rtmp://") ||
            lower.startsWith("rtp://") ||
            lower.startsWith("udp://") ||
            lower.startsWith("mms://")
    }

    private fun isResolvableRelativeUrl(line: String, baseUrl: String?): Boolean {
        if (baseUrl.isNullOrBlank()) return false
        if (line.startsWith("#") || line.contains(' ')) return false
        return line.startsWith("/") || line.endsWith(".m3u8", ignoreCase = true) ||
            line.endsWith(".ts", ignoreCase = true) || line.contains(".m3u8?")
    }

    private fun resolveUrlIfRelative(line: String, baseUrl: String?): String {
        if (looksLikeStreamUrl(line) || baseUrl.isNullOrBlank()) return line
        return runCatching {
            java.net.URI(baseUrl).resolve(line).toString()
        }.getOrDefault(line)
    }

    private fun splitInlineHeaders(rawUrl: String): Triple<String, String?, String?> {
        val pipeIdx = rawUrl.indexOf('|')
        if (pipeIdx <= 0) return Triple(rawUrl, null, null)
        val url = rawUrl.substring(0, pipeIdx).trim()
        val params = rawUrl.substring(pipeIdx + 1)
        var ua: String? = null
        var ref: String? = null
        params.split('&').forEach { pair ->
            val key = pair.substringBefore('=').trim().lowercase()
            val value = pair.substringAfter('=', "").trim()
            when (key) {
                "user-agent" -> if (value.isNotBlank()) ua = value
                "referer", "referrer" -> if (value.isNotBlank()) ref = value
            }
        }
        return Triple(url, ua, ref)
    }

    private fun deriveNameFromUrl(url: String, fallbackIndex: Int): String {
        val lastSegment = url.substringBefore('?').substringAfterLast('/').trim()
        return if (lastSegment.isNotBlank() && lastSegment.length <= 32) {
            lastSegment
        } else {
            "Channel $fallbackIndex"
        }
    }
}
