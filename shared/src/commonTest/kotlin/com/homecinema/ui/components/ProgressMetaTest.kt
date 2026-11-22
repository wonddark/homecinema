package com.homecinema.ui.components

import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.VideoInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressMetaTest {

    private val mb = 1024L * 1024L

    private fun task(
        state: DownloadState,
        progress: Float = 0.5f,
        extractor: String? = "YouTube",
        quality: String = "1080p",
        size: Long? = 248 * mb,
        error: String? = null,
    ) = DownloadTask(
        id = "t",
        url = "u",
        videoInfo = VideoInfo(id = "v", title = "T", duration = 1, formats = emptyList(), extractor = extractor),
        state = state,
        progress = progress,
        qualityLabel = quality,
        sizeBytes = size,
        errorReason = error,
    )

    @Test
    fun `downloading shows extractor quality and size`() {
        assertEquals("YouTube · 1080p · 248 MB", progressMeta(task(DownloadState.DOWNLOADING)))
    }

    @Test
    fun `downloading at zero progress shows starting`() {
        assertEquals("YouTube · 1080p · Starting…", progressMeta(task(DownloadState.DOWNLOADING, progress = 0f)))
    }

    @Test
    fun `queued ends with queued`() {
        assertEquals("YouTube · 1080p · Queued", progressMeta(task(DownloadState.QUEUED)))
    }

    @Test
    fun `paused starts with paused`() {
        assertEquals("Paused · 4K · 1.1 GB", progressMeta(task(DownloadState.PAUSED, quality = "4K", size = 1126 * mb)))
    }

    @Test
    fun `failed shows the reason`() {
        assertEquals("Failed · Connection lost", progressMeta(task(DownloadState.FAILED, error = "Connection lost")))
    }

    @Test
    fun `blank or missing segments are omitted`() {
        assertEquals("720p", progressMeta(task(DownloadState.DOWNLOADING, extractor = null, quality = "720p", size = null)))
        assertEquals("Queued", progressMeta(task(DownloadState.QUEUED, extractor = " ", quality = "")))
        assertEquals("Paused", progressMeta(task(DownloadState.PAUSED, quality = "", size = null)))
        assertEquals("Failed", progressMeta(task(DownloadState.FAILED, error = null)))
        assertEquals("1080p · Starting…", progressMeta(task(DownloadState.DOWNLOADING, progress = 0f, extractor = null)))
    }
}
