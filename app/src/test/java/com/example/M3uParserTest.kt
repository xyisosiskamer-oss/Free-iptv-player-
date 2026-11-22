package com.example

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
    fun parsesXmltvTimestampAndNormalizedChannelName() {
        val ts = XmltvParser.parseXmltvTimestamp("20261122183000 +0300")
        assertTrue(ts > 0L)
        assertEquals("1tvgeorgiahd", XmltvParser.normalizeChannelName("1TV Georgia HD!"))
    }
}
