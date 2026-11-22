package com.homecinema.core.downloader

import kotlin.test.Test
import kotlin.test.assertEquals

class FailureReasonTest {
    @Test
    fun `unable to download maps to connection lost`() =
        assertEquals("Connection lost", mapFailureReason("ERROR: Unable to download webpage"))

    @Test
    fun `timed out maps to connection lost`() =
        assertEquals("Connection lost", mapFailureReason("read timed out"))

    @Test
    fun `connection maps to connection lost`() =
        assertEquals("Connection lost", mapFailureReason("Connection reset by peer"))

    @Test
    fun `network maps to connection lost`() =
        assertEquals("Connection lost", mapFailureReason("Network is unreachable"))

    @Test
    fun `private video maps to unavailable`() =
        assertEquals("Unavailable", mapFailureReason("ERROR: Private video"))

    @Test
    fun `unavailable maps to unavailable`() =
        assertEquals("Unavailable", mapFailureReason("Video unavailable"))

    @Test
    fun `removed maps to unavailable`() =
        assertEquals("Unavailable", mapFailureReason("This video has been removed"))

    @Test
    fun `unsupported url maps to unsupported site`() =
        assertEquals("Unsupported site", mapFailureReason("ERROR: Unsupported URL: https://x"))

    @Test
    fun `anything else maps to download failed`() =
        assertEquals("Download failed", mapFailureReason("boom"))

    @Test
    fun `null maps to download failed`() =
        assertEquals("Download failed", mapFailureReason(null))

    @Test
    fun `http 403 maps to blocked by site`() =
        assertEquals(
            "Blocked by site",
            mapFailureReason("ERROR: [PornHub] x: Unable to download webpage: HTTP Error 403: Forbidden"),
        )
}
