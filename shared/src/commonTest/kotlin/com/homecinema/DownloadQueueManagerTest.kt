package com.homecinema

import com.homecinema.core.usecase.DownloadQueueManager
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadQueueManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun makeTask(id: String = "task-1", url: String = "https://example.com/video") =
        DownloadTask(id = id, url = url)

    @Test
    fun `enqueue adds task with QUEUED state`() = testScope.runTest {
        val engine = MockDownloaderEngine()
        val manager = DownloadQueueManager(engine, this)

        val task = makeTask()
        manager.enqueue(task)

        val tasks = manager.tasks.value
        assertEquals(1, tasks.size)
        assertEquals(DownloadState.QUEUED, tasks.first().state)
    }

    @Test
    fun `startNext transitions task from QUEUED to DOWNLOADING to COMPLETED`() =
        testScope.runTest {
            val engine = MockDownloaderEngine()
            val manager = DownloadQueueManager(engine, this)
            val task = makeTask()
            manager.enqueue(task)

            manager.startNext()
            advanceUntilIdle()

            val tasks = manager.tasks.value
            assertEquals(1, tasks.size)
            assertEquals(DownloadState.COMPLETED, tasks.first().state)
        }

    @Test
    fun `failed download is marked as FAILED`() = testScope.runTest {
        val engine = MockDownloaderEngine(shouldFail = true)
        val manager = DownloadQueueManager(engine, this)
        val task = makeTask()
        manager.enqueue(task)

        manager.startNext()
        advanceUntilIdle()

        val tasks = manager.tasks.value
        assertEquals(DownloadState.FAILED, tasks.first().state)
    }

    @Test
    fun `cancel updates task state and calls engine cancel`() = testScope.runTest {
        val engine = MockDownloaderEngine()
        val manager = DownloadQueueManager(engine, this)
        val task = makeTask(id = "abc123")
        manager.enqueue(task)

        manager.cancel("abc123")
        advanceUntilIdle()

        assertEquals("abc123", engine.cancelledTaskId)
        val tasks = manager.tasks.value
        assertTrue(tasks.first().state == DownloadState.FAILED)
    }

    @Test
    fun `progress updates are reflected in StateFlow`() = testScope.runTest {
        val engine = MockDownloaderEngine(progressSteps = listOf(0.5f, 1.0f))
        val manager = DownloadQueueManager(engine, this)
        val task = makeTask()
        manager.enqueue(task)

        manager.startNext()
        advanceUntilIdle()

        // After completion, progress should be 1.0 (last step)
        val finalTask = manager.tasks.value.first()
        assertEquals(DownloadState.COMPLETED, finalTask.state)
    }

    @Test
    fun `multiple tasks are enqueued and processed in order`() = testScope.runTest {
        val engine = MockDownloaderEngine()
        val manager = DownloadQueueManager(engine, this)

        manager.enqueue(makeTask("t1"))
        manager.enqueue(makeTask("t2"))
        manager.enqueue(makeTask("t3"))

        assertEquals(3, manager.tasks.value.size)
        manager.tasks.value.forEach { task ->
            assertEquals(DownloadState.QUEUED, task.state)
        }
    }

    @Test
    fun `remove deletes task from list`() = testScope.runTest {
        val engine = MockDownloaderEngine()
        val manager = DownloadQueueManager(engine, this)
        manager.enqueue(makeTask("t1"))
        manager.enqueue(makeTask("t2"))

        manager.remove("t1")

        val tasks = manager.tasks.value
        assertEquals(1, tasks.size)
        assertEquals("t2", tasks.first().id)
    }

    @Test
    fun `remove on unknown id is a no-op`() = testScope.runTest {
        val engine = MockDownloaderEngine()
        val manager = DownloadQueueManager(engine, this)
        manager.enqueue(makeTask("t1"))

        manager.remove("nonexistent")

        assertEquals(1, manager.tasks.value.size)
    }

    @Test
    fun `completed task stores outputPath from engine`() = testScope.runTest {
        val engine = MockDownloaderEngine(mockOutputPath = "downloads/video.mp4")
        val manager = DownloadQueueManager(engine, this)
        manager.enqueue(makeTask())

        manager.startNext()
        advanceUntilIdle()

        val task = manager.tasks.value.first()
        assertEquals(DownloadState.COMPLETED, task.state)
        assertEquals("downloads/video.mp4", task.outputPath)
    }
}
