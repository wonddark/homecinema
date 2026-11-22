package com.homecinema.core.notification

import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationCountsTest {
    private fun task(id: String, state: DownloadState) = DownloadTask(id = id, url = "https://x/$id", state = state)

    @Test
    fun `counts each active state and ignores completed`() {
        val tasks = listOf(
            task("a", DownloadState.DOWNLOADING),
            task("b", DownloadState.QUEUED),
            task("c", DownloadState.PAUSED),
            task("d", DownloadState.COMPLETED),
        )
        assertEquals(NotificationCounts(1, 1, 1), notificationCounts(tasks))
    }
}
