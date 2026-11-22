package com.homecinema

import com.homecinema.core.downloader.parseVideoInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoInfoParserTest {
    private val youtubeDump =
        """
        {"id":"abc","title":"T","duration":120.5,"thumbnail":"http://t/x.jpg",
         "extractor_key":"Youtube","uploader":"Up",
         "formats":[
          {"format_id":"137","height":1080,"ext":"mp4","vcodec":"avc1","acodec":"none","filesize":100},
          {"format_id":"140","ext":"m4a","vcodec":"none","acodec":"mp4a","abr":129.5}
         ]}
        """.trimIndent()

    @Test
    fun `parseVideoInfo maps youtube dump`() {
        val info = parseVideoInfo(youtubeDump, "fb")
        assertEquals("abc", info.id)
        assertEquals(120L, info.duration)
        assertEquals("http://t/x.jpg", info.thumbnailUrl)
        assertEquals("YouTube", info.extractor)
        assertEquals("Up", info.uploader)
        assertTrue(info.formats[0].hasVideo)
        assertFalse(info.formats[0].hasAudio)
        assertEquals(1080, info.formats[0].height)
        assertEquals(100L, info.formats[0].bestSize)
        assertEquals(129.5, info.formats[1].abr)
        assertTrue(info.formats[1].hasAudio)
    }

    @Test
    fun `parseVideoInfo tolerates missing fields`() {
        val info = parseVideoInfo("""{"title":"x"}""", "fb")
        assertEquals("fb", info.id)
        assertTrue(info.formats.isEmpty())
        assertEquals(0L, info.duration)
    }
}
