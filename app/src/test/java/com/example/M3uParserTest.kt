package com.example

import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgramUi
import com.example.data.parser.CatchupUrlBuilder
import com.example.data.parser.M3uParser
import com.example.data.parser.XmltvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun parsesStandardAndExtendedM3uWithRussianGeorgianAndEpgUrl() {
        val sample = """
            #EXTM3U url-tvg="https://epg.example.com/guide.xml.gz"
            #EXTINF:-1 tvg-id="1tv.ge" tvg-logo="https://cdn.example.com/1tv.png" group-title="Грузия, Новости",საზოგადოებრივი მაუწყებელი
            https://stream.example.com/1tv/index.m3u8
            #EXTINF:-1 tvg-logo="https://cdn.example.com/kino.png"
            #EXTGRP:Кино
            #EXTVLCOPT:http-user-agent=CustomUA/2.0
            #EXTVLCOPT:http-referrer=https://ref.example.com
            http://stream.example.com/kino.ts|User-Agent=InlineUA&Referer=https://inline.example.com
            #EXTINF:0,Первый канал
            https://stream.example.com/pervy.m3u8
        """.trimIndent()

        val result = M3uParser.parsePlaylist(sample)
        assertEquals("https://epg.example.com/guide.xml.gz", result.epgUrl)
        val channels = result.channels
        assertEquals(3, channels.size)

        assertEquals("საზოგადოებრივი მაუწყებელი", channels[0].name)
        assertEquals("Грузия, Новости", channels[0].groupTitle)
        assertEquals("https://cdn.example.com/1tv.png", channels[0].logoUrl)
        assertEquals("1tv.ge", channels[0].tvgId)
        assertEquals("https://stream.example.com/1tv/index.m3u8", channels[0].streamUrl)

        assertEquals("Кино", channels[1].groupTitle)
        assertEquals("http://stream.example.com/kino.ts", channels[1].streamUrl)
        assertEquals("InlineUA", channels[1].userAgent)
        assertEquals("https://inline.example.com", channels[1].referrer)

        assertEquals("Первый канал", channels[2].name)
        assertEquals("", channels[2].groupTitle)
        assertNull(channels[2].logoUrl)
    }

    @Test
    fun parsesCatchupAttributesAndBuildsArchiveUrls() {
        val sample = """
            #EXTM3U catchup="flussonic" catchup-days="5"
            #EXTINF:-1 tvg-id="ch1",News HD
            https://cdn.example.com/ch1/index.m3u8?token=abc
            #EXTINF:-1 catchup="append" catchup-days="3" catchup-source="?utc=${'$'}{start}&dur=${'$'}{duration}",Movies
            https://cdn.example.com/movies/playlist.m3u8
        """.trimIndent()

        val channels = M3uParser.parsePlaylist(sample).channels
        assertEquals(2, channels.size)
        assertEquals("flussonic", channels[0].catchupType)
        assertEquals(5, channels[0].catchupDays)
        assertEquals("append", channels[1].catchupType)
        assertEquals(3, channels[1].catchupDays)

        val program = EpgProgramUi(
            title = "Evening News",
            description = null,
            category = null,
            startTimeMillis = 1_700_000_000_000L,
            endTimeMillis = 1_700_003_600_000L
        )

        val ch0Item = ChannelItem(
            id = 1,
            playlistId = 1,
            playlistName = "Test",
            name = channels[0].name,
            streamUrl = channels[0].streamUrl,
            logoUrl = null,
            groupTitle = "",
            tvgId = channels[0].tvgId,
            userAgent = null,
            referrer = null,
            position = 0,
            isFavorite = false,
            catchupType = channels[0].catchupType,
            catchupDays = channels[0].catchupDays,
            catchupSource = channels[0].catchupSource
        )
        assertEquals(
            "https://cdn.example.com/ch1/archive-1700000000-3600.m3u8?token=abc",
            CatchupUrlBuilder.buildArchiveUrl(ch0Item, program, nowMillis = 1_700_010_000_000L)
        )

        val ch1Item = ch0Item.copy(
            streamUrl = channels[1].streamUrl,
            catchupType = channels[1].catchupType,
            catchupDays = channels[1].catchupDays,
            catchupSource = channels[1].catchupSource
        )
        assertEquals(
            "https://cdn.example.com/movies/playlist.m3u8?utc=1700000000&dur=3600",
            CatchupUrlBuilder.buildArchiveUrl(ch1Item, program, nowMillis = 1_700_010_000_000L)
        )
    }

    @Test
    fun parsesRelativeUrlsAndExtinfHeaderAttributes() {
        val sample = """
            #EXTM3U
            #EXTINF:-1 user-agent="PlayerUA/3.0" referer="https://portal.example.net/",Channel One
            /live/ch1/index.m3u8?token=xyz
            #EXTINF:-1 http-user-agent="AltUA/1.2" http-referrer="https://alt.example.net/",Channel Two
            streams/ch2.m3u8
        """.trimIndent()

        val result = M3uParser.parsePlaylist(sample, baseUrl = "https://cdn.example.net/list/playlist.m3u")
        assertEquals(2, result.channels.size)

        assertEquals("https://cdn.example.net/live/ch1/index.m3u8?token=xyz", result.channels[0].streamUrl)
        assertEquals("PlayerUA/3.0", result.channels[0].userAgent)
        assertEquals("https://portal.example.net/", result.channels[0].referrer)

        assertEquals("https://cdn.example.net/list/streams/ch2.m3u8", result.channels[1].streamUrl)
        assertEquals("AltUA/1.2", result.channels[1].userAgent)
        assertEquals("https://alt.example.net/", result.channels[1].referrer)
    }

    @Test
    fun parsesXmltvTimestampAndNormalizedChannelName() {
        val ts = XmltvParser.parseXmltvTimestamp("20261122183000 +0300")
        assertTrue(ts > 0L)
        assertEquals("1tvgeorgiahd", XmltvParser.normalizeChannelName("1TV Georgia HD!"))
    }
}
