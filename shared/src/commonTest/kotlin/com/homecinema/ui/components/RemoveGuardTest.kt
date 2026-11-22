package com.homecinema.ui.components

import com.homecinema.domain.DownloadState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class RemoveGuardTest {
    @Test
    fun `remove is ignored for 600 ms after the card turns paused or failed`() {
        listOf(DownloadState.PAUSED, DownloadState.FAILED).forEach { state ->
            assertFalse(removeAllowed(state, 0.milliseconds), "$state at 0")
            assertFalse(removeAllowed(state, 599.milliseconds), "$state at 599")
            assertTrue(removeAllowed(state, 600.milliseconds), "$state at 600")
        }
    }

    @Test
    fun `queued remove is never delayed`() {
        assertTrue(removeAllowed(DownloadState.QUEUED, 0.milliseconds))
    }
}
