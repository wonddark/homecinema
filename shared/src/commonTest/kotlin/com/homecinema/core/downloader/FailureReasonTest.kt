package com.homecinema.core.downloader

import kotlin.test.Test
import kotlin.test.assertEquals

class FailureReasonTest {
    @Test
    fun `unable to download maps to connection lost`() =
        assertEquals(FailureReason.CONNECTION_LOST, mapFailureReason("ERROR: Unable to download webpage"))

    @Test
    fun `timed out maps to connection lost`() =
        assertEquals(FailureReason.CONNECTION_LOST, mapFailureReason("read timed out"))

    @Test
    fun `connection maps to connection lost`() =
        assertEquals(FailureReason.CONNECTION_LOST, mapFailureReason("Connection reset by peer"))

    @Test
    fun `network maps to connection lost`() =
        assertEquals(FailureReason.CONNECTION_LOST, mapFailureReason("Network is unreachable"))

    @Test
    fun `private video maps to unavailable`() =
        assertEquals(FailureReason.UNAVAILABLE, mapFailureReason("ERROR: Private video"))

    @Test
    fun `unavailable maps to unavailable`() =
        assertEquals(FailureReason.UNAVAILABLE, mapFailureReason("Video unavailable"))

    @Test
    fun `removed maps to unavailable`() =
        assertEquals(FailureReason.UNAVAILABLE, mapFailureReason("This video has been removed"))

    @Test
    fun `unsupported url maps to unsupported site`() =
        assertEquals(FailureReason.UNSUPPORTED, mapFailureReason("ERROR: Unsupported URL: https://x"))

    @Test
    fun `anything else maps to download failed`() =
        assertEquals(FailureReason.GENERIC, mapFailureReason("boom"))

    @Test
    fun `null maps to download failed`() =
        assertEquals(FailureReason.GENERIC, mapFailureReason(null))

    @Test
    fun `http 403 maps to blocked by site`() =
        assertEquals(
            FailureReason.BLOCKED,
            mapFailureReason("ERROR: [PornHub] x: Unable to download webpage: HTTP Error 403: Forbidden"),
        )
}
