package com.homecinema.core.downloader

import com.homecinema.domain.DownloadTask
import com.homecinema.domain.MediaKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YtDlpArgsTest {
    private fun task(
        format: String,
        kind: MediaKind = MediaKind.VIDEO,
    ) = DownloadTask(id = "t1", url = "https://x/v", format = format, kind = kind, qualityLabel = "1080p")

    private fun List<String>.hasPair(flag: String, value: String) =
        indexOf(flag).let { it >= 0 && getOrNull(it + 1) == value }

    @Test
    fun `video args merge with bestaudio`() {
        val args = buildDownloadArgs(task("137"), "/dl", ffmpegAvailable = true)
        assertTrue(args.hasPair("-f", "137+bestaudio/137/best"))
        assertTrue(args.hasPair("--merge-output-format", "mp4"))
        assertTrue("--continue" in args)
        assertTrue("--no-playlist" in args)
    }

    @Test
    fun `video args without ffmpeg use format as is`() {
        val args = buildDownloadArgs(task("18"), "/dl", ffmpegAvailable = false)
        assertTrue(args.hasPair("-f", "18"))
        assertFalse("--merge-output-format" in args)
    }

    @Test
    fun `audio args have no merge`() {
        val args = buildDownloadArgs(task("140", MediaKind.AUDIO), "/dl", ffmpegAvailable = true)
        assertTrue(args.hasPair("-f", "140"))
        assertFalse("--merge-output-format" in args)
    }

    @Test
    fun `args include output template`() {
        val args = buildDownloadArgs(task("18"), "/dl", ffmpegAvailable = false)
        assertTrue(args.hasPair("-o", "/dl/%(title)s [%(id)s] 1080p.%(ext)s"))
    }

    @Test
    fun `output template includes id and quality`() {
        assertEquals("/dl/%(title)s [%(id)s] 1080p.%(ext)s", outputTemplate(task("137"), "/dl"))
    }

    @Test
    fun `parseFinalPath prefers merger line`() {
        val lines = sequenceOf(
            "[download] Destination: /dl/a.f137.mp4",
            "[download] Destination: /dl/a.f140.m4a",
            "[Merger] Merging formats into \"/dl/a.mp4\"",
        )
        assertEquals("/dl/a.mp4", parseFinalPath(lines))
    }

    @Test
    fun `parseFinalPath handles already downloaded`() {
        val lines = sequenceOf("[download] /dl/a.mp4 has already been downloaded")
        assertEquals("/dl/a.mp4", parseFinalPath(lines))
    }
}
