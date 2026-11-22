package com.example

import com.example.data.parser.PlaylistImportValidator
import com.example.data.parser.PlaylistValidationResult
import com.example.data.parser.XtreamCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportValidatorTest {

    @Test
    fun buildsXtreamEndpointsWithNormalizationAndEncoding() {
        val endpoints = PlaylistImportValidator.buildXtreamEndpoints(
            XtreamCredentials(
                serverUrl = "tv.example.com:8080/",
                username = "user+1",
                password = "p@ss word"
            )
        )
        assertNotNull(endpoints)
        assertEquals(
            "http://tv.example.com:8080/get.php?username=user%2B1&password=p%40ss%20word&type=m3u_plus&output=ts",
            endpoints!!.playlistUrl
        )
        assertEquals(
            "http://tv.example.com:8080/xmltv.php?username=user%2B1&password=p%40ss%20word",
            endpoints.epgUrl
        )

        assertNull(
            PlaylistImportValidator.buildXtreamEndpoints(
                XtreamCredentials("", "u", "p")
            )
        )
    }

    @Test
    fun validatesM3uContentAndComputesStableHash() {
        val m3u = """
            #EXTM3U url-tvg="https://epg.example.com/guide.xml"
            #EXTINF:-1,Первый канал
            https://stream.example.com/live/1.m3u8
        """.trimIndent()

        val result = PlaylistImportValidator.validateM3uContent(m3u)
        assertTrue(result is PlaylistValidationResult.Valid)
        val valid = result as PlaylistValidationResult.Valid
        assertEquals(1, valid.parseResult.channels.size)
        assertEquals("https://epg.example.com/guide.xml", valid.parseResult.epgUrl)
        assertEquals(64, valid.contentHash.length)

        val invalid = PlaylistImportValidator.validateM3uContent("not a playlist")
        assertTrue(invalid is PlaylistValidationResult.Invalid)
    }

    @Test
    fun validatesUrlsAndExtractsSharedLinks() {
        assertTrue(PlaylistImportValidator.isValidHttpUrl("https://m3u.work/ofRjF2G"))
        assertFalse(PlaylistImportValidator.isValidHttpUrl("ftp://bad.example.com/list.m3u"))
        assertFalse(PlaylistImportValidator.isValidHttpUrl("https://has space.com/list.m3u"))

        val extracted = PlaylistImportValidator.extractFirstHttpUrl(
            "Вот мой плейлист:\nhttps://cdn.example.com/channels/my_tv.m3u8?token=xyz\nПриятного просмотра"
        )
        assertEquals("https://cdn.example.com/channels/my_tv.m3u8?token=xyz", extracted)

        assertEquals(
            "sport_vip",
            PlaylistImportValidator.suggestNameFromDisplayName("content://media/external/sport_vip.m3u8?dl=1")
        )
    }
}
