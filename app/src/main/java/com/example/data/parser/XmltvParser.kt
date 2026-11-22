package com.example.data.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

data class ParsedEpgAlias(
    val channelId: String,
    val displayNameNormalized: String
)

data class ParsedEpgProgram(
    val channelId: String,
    val title: String,
    val description: String?,
    val category: String?,
    val startTimeMillis: Long,
    val endTimeMillis: Long
)

data class XmltvParseResult(
    val aliases: List<ParsedEpgAlias>,
    val programs: List<ParsedEpgProgram>
)

object XmltvParser {

    fun parse(
        inputStream: InputStream,
        windowStartMillis: Long = System.currentTimeMillis() - 12L * 60 * 60 * 1000,
        windowEndMillis: Long = System.currentTimeMillis() + 48L * 60 * 60 * 1000
    ): XmltvParseResult {
        val buffered = BufferedInputStream(inputStream, 16 * 1024)
        buffered.mark(4)
        val magic = ByteArray(2)
        val readBytes = buffered.read(magic)
        buffered.reset()

        val isGzip = readBytes == 2 &&
            (magic[0].toInt() and 0xFF) == 0x1F &&
            (magic[1].toInt() and 0xFF) == 0x8B

        val xmlStream: InputStream = if (isGzip) GZIPInputStream(buffered) else buffered
        return parseXmlStream(xmlStream, windowStartMillis, windowEndMillis)
    }

    private fun parseXmlStream(
        stream: InputStream,
        windowStartMillis: Long,
        windowEndMillis: Long
    ): XmltvParseResult {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(stream, null)
        }

        val aliases = mutableListOf<ParsedEpgAlias>()
        val programs = mutableListOf<ParsedEpgProgram>()

        var currentChannelId: String? = null
        var progChannelId: String? = null
        var progStart: Long = 0L
        var progStop: Long = 0L
        var progTitle: String? = null
        var progDesc: String? = null
        var progCategory: String? = null
        var insideProgramme = false
        var insideChannel = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "channel" -> {
                            currentChannelId = parser.getAttributeValue(null, "id")?.trim()
                            insideChannel = !currentChannelId.isNullOrBlank()
                        }
                        "display-name" -> {
                            if (insideChannel && currentChannelId != null) {
                                val text = parser.nextText().trim()
                                val normalized = normalizeChannelName(text)
                                if (normalized.isNotEmpty()) {
                                    aliases.add(ParsedEpgAlias(currentChannelId.lowercase(), normalized))
                                }
                            }
                        }
                        "programme" -> {
                            val ch = parser.getAttributeValue(null, "channel")?.trim()
                            val startRaw = parser.getAttributeValue(null, "start")
                            val stopRaw = parser.getAttributeValue(null, "stop")
                            val startMs = parseXmltvTimestamp(startRaw)
                            val stopMs = parseXmltvTimestamp(stopRaw)
                            if (!ch.isNullOrBlank() && startMs > 0L && stopMs > startMs &&
                                stopMs >= windowStartMillis && startMs <= windowEndMillis
                            ) {
                                progChannelId = ch.lowercase()
                                progStart = startMs
                                progStop = stopMs
                                progTitle = null
                                progDesc = null
                                progCategory = null
                                insideProgramme = true
                            } else {
                                insideProgramme = false
                            }
                        }
                        "title" -> {
                            if (insideProgramme && progTitle == null) {
                                progTitle = parser.nextText().trim().takeIf { it.isNotBlank() }
                            }
                        }
                        "desc" -> {
                            if (insideProgramme && progDesc == null) {
                                progDesc = parser.nextText().trim().takeIf { it.isNotBlank() }
                            }
                        }
                        "category" -> {
                            if (insideProgramme && progCategory == null) {
                                progCategory = parser.nextText().trim().takeIf { it.isNotBlank() }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name.lowercase()) {
                        "channel" -> {
                            insideChannel = false
                            currentChannelId = null
                        }
                        "programme" -> {
                            if (insideProgramme && progChannelId != null && !progTitle.isNullOrBlank()) {
                                programs.add(
                                    ParsedEpgProgram(
                                        channelId = progChannelId,
                                        title = progTitle!!,
                                        description = progDesc,
                                        category = progCategory,
                                        startTimeMillis = progStart,
                                        endTimeMillis = progStop
                                    )
                                )
                            }
                            insideProgramme = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return XmltvParseResult(aliases = aliases, programs = programs)
    }

    fun normalizeChannelName(name: String): String {
        return name.lowercase()
            .replace(Regex("""[^\p{L}\p{N}]"""), "")
    }

    fun parseXmltvTimestamp(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val trimmed = raw.trim()
        val withTz = trimmed.replace(Regex("""\s+UTC$""", RegexOption.IGNORE_CASE), " +0000")

        val formats = listOf(
            "yyyyMMddHHmmss Z",
            "yyyyMMddHHmmssZ",
            "yyyyMMddHHmmss"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    if (!pattern.contains("Z")) {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                }
                val parsed = sdf.parse(withTz)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return 0L
    }
}
