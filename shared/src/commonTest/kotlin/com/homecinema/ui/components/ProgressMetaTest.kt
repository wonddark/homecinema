package com.homecinema.ui.components

import com.homecinema.core.downloader.FailureReason
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.VideoInfo
import com.homecinema.ui.text.ProgressLabels
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressMetaTest {

    private val en = ProgressLabels("Queued", "Paused", "Failed", "Starting…", "Connection lost")

    private val mb = 1024L * 1024L

    private fun task(
        state: DownloadState,
        progress: Float = 0.5f,
        extractor: String? = "YouTube",
        quality: String = "1080p",
        size: Long? = 248 * mb,
        failure: FailureReason? = null,
    ) = DownloadTask(
        id = "t",
        url = "u",
        videoInfo = VideoInfo(id = "v", title = "T", duration = 1, formats = emptyList(), extractor = extractor),
        state = state,
        progress = progress,
        qualityLabel = quality,
        sizeBytes = size,
        failure = failure,
    )

    @Test
    fun `downloading shows extractor quality and size`() {
        assertEquals("YouTube · 1080p · 248 MB", progressMeta(task(DownloadState.DOWNLOADING), en))
    }

    @Test
    fun `downloading at zero progress shows starting`() {
        assertEquals("YouTube · 1080p · Starting…", progressMeta(task(DownloadState.DOWNLOADING, progress = 0f), en))
    }

    @Test
    fun `queued ends with queued`() {
        assertEquals("YouTube · 1080p · Queued", progressMeta(task(DownloadState.QUEUED), en))
    }

    @Test
    fun `paused starts with paused`() {
        assertEquals("Paused · 4K · 1.1 GB", progressMeta(task(DownloadState.PAUSED, quality = "4K", size = 1126 * mb), en))
    }

    @Test
    fun `failed shows the reason`() {
        val failed = task(DownloadState.FAILED, failure = FailureReason.CONNECTION_LOST)
        assertEquals("Failed · Connection lost", progressMeta(failed, en))
    }

    @Test
    fun `failed uses provided failure text`() =
        assertEquals(
            "Error · Se perdió la conexión",
            progressMeta(task(DownloadState.FAILED), en.copy(failed = "Error", failure = "Se perdió la conexión")),
        )

    @Test
    fun `blank or missing segments are omitted`() {
        assertEquals("720p", progressMeta(task(DownloadState.DOWNLOADING, extractor = null, quality = "720p", size = null), en))
        assertEquals("Queued", progressMeta(task(DownloadState.QUEUED, extractor = " ", quality = ""), en))
        assertEquals("Paused", progressMeta(task(DownloadState.PAUSED, quality = "", size = null), en))
        assertEquals("Failed", progressMeta(task(DownloadState.FAILED, failure = null), en.copy(failure = "")))
        assertEquals("1080p · Starting…", progressMeta(task(DownloadState.DOWNLOADING, progress = 0f, extractor = null), en))
    }
}
