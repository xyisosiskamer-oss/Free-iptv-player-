package com.example.data.parser

import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgramUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CatchupUrlBuilder {

    fun buildArchiveUrl(
        channel: ChannelItem,
        program: EpgProgramUi,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val startSec = program.startTimeMillis / 1000L
        val endSec = program.endTimeMillis / 1000L
        val durationSec = ((program.endTimeMillis - program.startTimeMillis) / 1000L).coerceAtLeast(60L)
        val lutcSec = nowMillis / 1000L
        val offsetSec = ((nowMillis - program.startTimeMillis) / 1000L).coerceAtLeast(0L)

        val base = channel.streamUrl
        val type = channel.catchupType?.trim()?.lowercase()
        val sourceTemplate = channel.catchupSource?.trim()?.takeIf { it.isNotEmpty() }

        return when (type) {
            "append" -> {
                if (sourceTemplate != null) {
                    appendQueryOrSuffix(
                        base,
                        applyPlaceholders(sourceTemplate, startSec, endSec, durationSec, lutcSec, offsetSec)
                    )
                } else {
                    appendShiftQuery(base, startSec, lutcSec)
                }
            }

            "shift" -> appendShiftQuery(base, startSec, lutcSec)

            "flussonic", "flussonic-hls", "flussonic-ts", "fs" -> {
                if (sourceTemplate != null) {
                    val resolved = applyPlaceholders(
                        sourceTemplate, startSec, endSec, durationSec, lutcSec, offsetSec
                    )
                    if (isAbsoluteUrl(resolved)) resolved else appendQueryOrSuffix(base, resolved)
                } else {
                    buildFlussonicArchiveUrl(base, startSec, durationSec, lutcSec)
                }
            }

            "xc" -> {
                if (sourceTemplate != null) {
                    val resolved = applyPlaceholders(
                        sourceTemplate, startSec, endSec, durationSec, lutcSec, offsetSec
                    )
                    if (isAbsoluteUrl(resolved)) resolved else appendQueryOrSuffix(base, resolved)
                } else {
                    buildXtreamArchiveUrl(base, startSec, durationSec, lutcSec)
                }
            }

            else -> {
                if (sourceTemplate != null) {
                    val resolved = applyPlaceholders(
                        sourceTemplate, startSec, endSec, durationSec, lutcSec, offsetSec
                    )
                    if (isAbsoluteUrl(resolved)) resolved else appendQueryOrSuffix(base, resolved)
                } else {
                    appendShiftQuery(base, startSec, lutcSec)
                }
            }
        }
    }

    private fun buildFlussonicArchiveUrl(
        baseUrl: String,
        startSec: Long,
        durationSec: Long,
        lutcSec: Long
    ): String {
        val urlWithoutQuery = baseUrl.substringBefore('?')
        val query = baseUrl.substringAfter('?', "")
        val querySuffix = if (query.isNotEmpty()) "?$query" else ""

        val replaced = when {
            urlWithoutQuery.endsWith("/index.m3u8", ignoreCase = true) ->
                urlWithoutQuery.dropLast("index.m3u8".length) + "archive-$startSec-$durationSec.m3u8$querySuffix"
            urlWithoutQuery.endsWith("/video.m3u8", ignoreCase = true) ->
                urlWithoutQuery.dropLast("video.m3u8".length) + "archive-$startSec-$durationSec.m3u8$querySuffix"
            urlWithoutQuery.endsWith("/mono.m3u8", ignoreCase = true) ->
                urlWithoutQuery.dropLast("mono.m3u8".length) + "archive-$startSec-$durationSec.m3u8$querySuffix"
            urlWithoutQuery.endsWith("/mpegts", ignoreCase = true) ->
                urlWithoutQuery.dropLast("mpegts".length) + "timeshift_abs-$startSec.ts$querySuffix"
            urlWithoutQuery.endsWith(".ts", ignoreCase = true) ->
                appendShiftQuery(baseUrl, startSec, lutcSec)
            else -> appendShiftQuery(baseUrl, startSec, lutcSec)
        }
        return replaced
    }

    private fun buildXtreamArchiveUrl(
        baseUrl: String,
        startSec: Long,
        durationSec: Long,
        lutcSec: Long
    ): String {
        val durationMin = (durationSec / 60L).coerceAtLeast(1L)
        val utcFmt = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formattedTime = utcFmt.format(Date(startSec * 1000L))
        val match = Regex("""^(https?://[^/]+)(?:/live)?/([^/]+)/([^/]+)/([^/?]+).*$""")
            .find(baseUrl)
        if (match != null) {
            val host = match.groupValues[1]
            val user = match.groupValues[2]
            val pass = match.groupValues[3]
            val streamId = match.groupValues[4].substringBefore('.')
            return "$host/timeshift/$user/$pass/$durationMin/$formattedTime/$streamId.ts"
        }
        return appendShiftQuery(baseUrl, startSec, lutcSec)
    }

    private fun appendShiftQuery(baseUrl: String, startSec: Long, lutcSec: Long): String {
        val sep = if (baseUrl.contains('?')) "&" else "?"
        return "$baseUrl${sep}utc=$startSec&lutc=$lutcSec"
    }

    private fun appendQueryOrSuffix(baseUrl: String, suffix: String): String {
        return when {
            suffix.startsWith("?") && baseUrl.contains('?') ->
                "$baseUrl&${suffix.removePrefix("?")}"
            else -> "$baseUrl$suffix"
        }
    }

    private fun isAbsoluteUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true) ||
            url.startsWith("rtsp://", ignoreCase = true)
    }

    private fun applyPlaceholders(
        template: String,
        startSec: Long,
        endSec: Long,
        durationSec: Long,
        lutcSec: Long,
        offsetSec: Long
    ): String {
        val startDate = Date(startSec * 1000L)
        val utc = TimeZone.getTimeZone("UTC")
        fun fmt(pattern: String): String = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = utc
        }.format(startDate)

        val year = fmt("yyyy")
        val month = fmt("MM")
        val day = fmt("dd")
        val hour = fmt("HH")
        val minute = fmt("mm")
        val second = fmt("ss")

        return template
            .replace("\${start}", startSec.toString())
            .replace("{start}", startSec.toString())
            .replace("\${utc}", startSec.toString())
            .replace("{utc}", startSec.toString())
            .replace("\${end}", endSec.toString())
            .replace("{end}", endSec.toString())
            .replace("\${utcend}", endSec.toString())
            .replace("{utcend}", endSec.toString())
            .replace("\${duration}", durationSec.toString())
            .replace("{duration}", durationSec.toString())
            .replace("\${timestamp}", lutcSec.toString())
            .replace("{timestamp}", lutcSec.toString())
            .replace("\${lutc}", lutcSec.toString())
            .replace("{lutc}", lutcSec.toString())
            .replace("\${offset}", offsetSec.toString())
            .replace("{offset}", offsetSec.toString())
            .replace("\${Y}", year)
            .replace("{Y}", year)
            .replace("\${m}", month)
            .replace("{m}", month)
            .replace("\${d}", day)
            .replace("{d}", day)
            .replace("\${H}", hour)
            .replace("{H}", hour)
            .replace("\${M}", minute)
            .replace("{M}", minute)
            .replace("\${S}", second)
            .replace("{S}", second)
    }
}
