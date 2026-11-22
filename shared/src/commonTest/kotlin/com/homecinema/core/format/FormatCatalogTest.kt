package com.homecinema.core.format

import com.homecinema.domain.VideoFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatCatalogTest {
    private fun muxed(id: String, height: Int, size: Long? = null, ext: String = "mp4") =
        VideoFormat(id, "${height}p", size = size, acodec = "aac", vcodec = "avc1", height = height, ext = ext)

    private fun videoOnly(id: String, height: Int, size: Long? = null, ext: String = "mp4") =
        VideoFormat(id, "${height}p", size = size, acodec = "none", vcodec = "avc1", height = height, ext = ext)

    private fun audioOnly(id: String, abr: Double, size: Long? = null) =
        VideoFormat(id, "audio", size = size, acodec = "mp4a", vcodec = "none", ext = "m4a", abr = abr)

    private fun labels(formats: List<VideoFormat>, ffmpeg: Boolean = true) =
        buildFormatOptions(formats, ffmpeg).qualities.map { it.label }

    @Test
    fun `labels 2160 as 4K and 1440 as 2K`() {
        assertEquals("4K", qualityLabel(2160))
        assertEquals("2K", qualityLabel(1440))
        assertEquals("1080p", qualityLabel(1080))
    }

    @Test
    fun `dedupes heights highest first`() {
        val formats = listOf(videoOnly("a", 1080), videoOnly("b", 720), videoOnly("c", 1080), videoOnly("d", 480))
        assertEquals(listOf("1080p", "720p", "480p"), labels(formats))
    }

    @Test
    fun `picks largest size per height preferring mp4 on ties`() {
        val formats = listOf(
            videoOnly("webm", 1080, 100, "webm"),
            videoOnly("mp4", 1080, 100, "mp4"),
            videoOnly("small", 720, 10),
            videoOnly("big", 720, 20),
        )
        val q = buildFormatOptions(formats, true).qualities
        assertEquals(listOf("mp4", "big"), q.map { it.formatId })
    }

    @Test
    fun `default is highest at or below 1080`() {
        val formats = listOf(videoOnly("a", 2160), videoOnly("b", 1080), videoOnly("c", 720))
        assertEquals("1080p", buildFormatOptions(formats, true).defaultQuality?.label)
    }

    @Test
    fun `default falls back to lowest`() {
        val formats = listOf(videoOnly("a", 2160), videoOnly("b", 1440))
        assertEquals("2K", buildFormatOptions(formats, true).defaultQuality?.label)
    }

    @Test
    fun `audio absent without audio-only format`() {
        assertNull(buildFormatOptions(listOf(muxed("a", 720)), true).audio)
    }

    @Test
    fun `audio picks highest abr`() {
        val formats = listOf(videoOnly("v", 720), audioOnly("low", 50.0), audioOnly("high", 129.4, 999))
        val audio = buildFormatOptions(formats, true).audio
        assertEquals("high", audio?.formatId)
        assertEquals("M4A · Audio", audio?.label)
        assertEquals("m4a", audio?.ext)
        assertEquals(129, audio?.abrKbps)
        assertEquals(999L, audio?.sizeBytes)
    }

    @Test
    fun `without ffmpeg only muxed formats`() {
        val formats = listOf(videoOnly("v", 1080), muxed("m", 360))
        val options = buildFormatOptions(formats, false)
        assertEquals(listOf("360p"), options.qualities.map { it.label })
        assertEquals(false, options.needsFfmpegHint)
    }

    @Test
    fun `without ffmpeg and no muxed gives Best`() {
        val options = buildFormatOptions(listOf(videoOnly("v", 1080)), false)
        assertTrue(options.needsFfmpegHint)
        assertEquals(listOf(QualityOption("Best", 0, "best", null)), options.qualities)
        assertEquals("best", options.defaultQuality?.formatId)
    }

    @Test
    fun `empty formats gives Best`() {
        val options = buildFormatOptions(emptyList(), true)
        val best = QualityOption("Best", 0, "best", null)
        assertEquals(listOf(best), options.qualities)
        assertEquals(best, options.defaultQuality)
        assertNull(options.audio)
        assertEquals(false, options.needsFfmpegHint)
    }
}
