package com.homecinema

import com.homecinema.core.history.HistoryRepository
import com.homecinema.core.storage.FakeFileIO
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.core.usecase.DownloadQueueManager
import com.homecinema.core.usecase.ThumbnailSaver
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.HistoryEntry
import com.homecinema.domain.MediaKind
import com.homecinema.domain.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadQueueManagerTest {
    private val io = FakeFileIO()
    private val engine = MockDownloaderEngine()
    private val queueStore = JsonFileStore(io, "/data/queue.json", ListSerializer(DownloadTask.serializer()), emptyList())
    private val history =
        HistoryRepository(
            JsonFileStore(io, "/data/history.json", ListSerializer(HistoryEntry.serializer()), emptyList()),
            io,
        )

    private fun TestScope.manager(): DownloadQueueManager = manager(backgroundScope)

    private fun manager(scope: CoroutineScope) =
        DownloadQueueManager(
            engine = engine,
            history = history,
            store = queueStore,
            thumbnails = ThumbnailSaver { _, id -> "/data/thumbnails/$id.jpg" },
            io = io,
            downloadDir = "dl",
            scope = scope,
            clock = { 42L },
        )

    private fun task(
        id: String,
        state: DownloadState = DownloadState.QUEUED,
        videoId: String = "v$id",
        format: String? = null,
    ) = DownloadTask(
        id = id,
        url = "https://example.com/$id",
        videoInfo =
            VideoInfo(
                id = videoId,
                title = "Title $id",
                duration = 90L,
                formats = emptyList(),
                thumbnailUrl = "https://img/$id.jpg",
                extractor = "youtube",
            ),
        format = format,
        state = state,
        kind = MediaKind.VIDEO,
        qualityLabel = "1080p",
        sizeBytes = 1234L,
    )

    private fun DownloadQueueManager.stateOf(id: String) = tasks.value.first { it.id == id }.state

    @Test
    fun `runs at most 4 at once`() =
        runTest {
            val m = manager()
            (1..6).forEach { m.enqueue(task("t$it")) }
            runCurrent()
            val states = m.tasks.value.map { it.state }
            assertEquals(4, states.count { it == DownloadState.DOWNLOADING })
            assertEquals(2, states.count { it == DownloadState.QUEUED })
            assertEquals(listOf("t1", "t2", "t3", "t4"), engine.downloadCalls.map { it.id })
        }

    @Test
    fun `completion moves task to history and starts next`() =
        runTest {
            val m = manager()
            (1..5).forEach { m.enqueue(task("t$it")) }
            runCurrent()
            engine.complete("t1", "dl/Title t1 [vt1] 1080p.mp4")
            runCurrent()

            assertEquals(listOf("t2", "t3", "t4", "t5"), m.tasks.value.map { it.id })
            assertEquals(DownloadState.DOWNLOADING, m.stateOf("t5"))
            val entry = history.entries.value.single()
            assertEquals("t1", entry.id)
            assertEquals("Title t1", entry.title)
            assertEquals("https://example.com/t1", entry.url)
            assertEquals("dl/Title t1 [vt1] 1080p.mp4", entry.filePath)
            assertEquals("mp4", entry.ext)
            assertEquals("1080p", entry.qualityLabel)
            assertEquals(1234L, entry.sizeBytes)
            assertEquals(90L, entry.durationSeconds)
            assertEquals("youtube", entry.extractor)
            assertEquals("/data/thumbnails/t1.jpg", entry.thumbnailPath)
            assertEquals(42L, entry.completedAt)
            assertFalse(io.read("/data/queue.json")!!.contains("\"t1\""))
        }

    @Test
    fun `pause keeps task paused not failed`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1"))
            runCurrent()
            m.pause("t1")
            runCurrent()
            assertEquals(DownloadState.PAUSED, m.stateOf("t1"))
            assertNull(m.tasks.value.single().errorReason)
            assertEquals(listOf("t1"), engine.pausedIds)
            assertTrue(io.read("/data/queue.json")!!.contains("PAUSED"))
        }

    @Test
    fun `resume requeues and passes task again`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1"))
            runCurrent()
            m.pause("t1")
            runCurrent()
            m.resume("t1")
            runCurrent()
            assertEquals(DownloadState.DOWNLOADING, m.stateOf("t1"))
            assertEquals(listOf("t1", "t1"), engine.downloadCalls.map { it.id })
        }

    @Test
    fun `retry clears error reason`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1"))
            runCurrent()
            engine.fail("t1", "ERROR: Private video")
            runCurrent()
            assertEquals("Unavailable", m.tasks.value.single().errorReason)
            m.retry("t1")
            runCurrent()
            assertEquals(DownloadState.DOWNLOADING, m.stateOf("t1"))
            assertNull(m.tasks.value.single().errorReason)
        }

    @Test
    fun `failure maps reason`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1"))
            runCurrent()
            engine.fail("t1", "ERROR: Unable to download webpage")
            runCurrent()
            val t = m.tasks.value.single()
            assertEquals(DownloadState.FAILED, t.state)
            assertEquals("Connection lost", t.errorReason)
        }

    @Test
    fun `remove cancels engine`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1"))
            m.enqueue(task("t2"))
            runCurrent()
            m.remove("t1")
            runCurrent()
            assertEquals(listOf("t1"), engine.cancelledIds)
            assertEquals(listOf("t2"), m.tasks.value.map { it.id })
            assertTrue(history.entries.value.isEmpty())
            assertFalse(io.read("/data/queue.json")!!.contains("\"t1\""))
        }

    @Test
    fun `restore turns downloading into paused`() =
        runTest {
            queueStore.save(listOf(task("t1", DownloadState.DOWNLOADING)))
            val m = manager()
            m.restore()
            runCurrent()
            assertEquals(DownloadState.PAUSED, m.stateOf("t1"))
            assertTrue(engine.downloadCalls.isEmpty())
        }

    @Test
    fun `restore keeps part of paused task`() =
        runTest {
            queueStore.save(listOf(task("T", DownloadState.PAUSED, videoId = "abc")))
            io.write("dl/T [abc] 1080p.mp4.part", "partial")
            io.write("dl/Other [zzz] 720p.mp4.part", "partial")
            val m = manager()
            m.restore()
            runCurrent()
            assertTrue(io.exists("dl/T [abc] 1080p.mp4.part"))
            assertFalse(io.exists("dl/Other [zzz] 720p.mp4.part"))
        }

    @Test
    fun `uses selected format id`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1", format = "137"))
            runCurrent()
            assertEquals("137", engine.downloadCalls.single().format)
        }

    @Test
    fun `blank final path marks failed`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1"))
            runCurrent()
            engine.complete("t1", "  ")
            runCurrent()
            val t = m.tasks.value.single()
            assertEquals(DownloadState.FAILED, t.state)
            assertEquals("Download failed", t.errorReason)
            assertTrue(history.entries.value.isEmpty())
        }

    @Test
    fun `sweep keeps foreign part files`() =
        runTest {
            io.write("dl/foreign.zip.part", "x")
            io.write("dl/Some Video.mp4.part", "x")
            io.write("dl/T [zzz] 720p.mp4.part", "x")
            io.write("dl/T [zzz] 720p.f137.mp4.part", "x")
            val m = manager()
            m.restore()
            runCurrent()
            assertTrue(io.exists("dl/foreign.zip.part"))
            assertTrue(io.exists("dl/Some Video.mp4.part"))
            assertFalse(io.exists("dl/T [zzz] 720p.mp4.part"))
            assertFalse(io.exists("dl/T [zzz] 720p.f137.mp4.part"))
        }
    @Test
    fun `shutdown ignores the kill of running downloads so restore pauses them`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1"))
            runCurrent()
            m.shutdown()
            engine.fail("t1", "ERROR: process killed")
            runCurrent()
            assertFalse(io.read("/data/queue.json")!!.contains("FAILED"))
            val restored = manager()
            restored.restore()
            runCurrent()
            assertEquals(DownloadState.PAUSED, restored.stateOf("t1"))
            assertNull(restored.tasks.value.single().errorReason)
        }

    @Test
    fun `enqueue ignores a duplicate of an unfinished task`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1", videoId = "same"))
            m.enqueue(task("t2", videoId = "same"))
            runCurrent()
            assertEquals(listOf("t1"), m.tasks.value.map { it.id })
            assertEquals(listOf("t1"), engine.downloadCalls.map { it.id })
        }

    @Test
    fun `enqueue accepts same video with other quality or kind`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1", videoId = "same"))
            m.enqueue(task("t2", videoId = "same").copy(qualityLabel = "720p"))
            m.enqueue(task("t3", videoId = "same").copy(kind = MediaKind.AUDIO))
            runCurrent()
            assertEquals(listOf("t1", "t2", "t3"), m.tasks.value.map { it.id })
        }

    @Test
    fun `enqueue accepts a duplicate again once the first completed`() =
        runTest {
            val m = manager()
            m.enqueue(task("t1", videoId = "same"))
            runCurrent()
            engine.complete("t1", "dl/Title t1 [same] 1080p.mp4")
            runCurrent()
            m.enqueue(task("t2", videoId = "same"))
            runCurrent()
            assertEquals(listOf("t2"), m.tasks.value.map { it.id })
        }

    @Test
    fun `sweep keeps foreign bracketed part files`() =
        runTest {
            io.write("dl/Movie [x] y.mkv.part", "x")
            io.write("dl/Song [abc] remix.mp3.ytdl", "x")
            io.write("dl/T [zzz] 4K.mp4.part", "x")
            io.write("dl/T [zzz] 128kbps.m4a.part", "x")
            io.write("dl/T [zzz] M4A.m4a.ytdl", "x")
            io.write("dl/T [zzz] Best.f18.mp4.part", "x")
            val m = manager()
            m.restore()
            runCurrent()
            assertTrue(io.exists("dl/Movie [x] y.mkv.part"))
            assertTrue(io.exists("dl/Song [abc] remix.mp3.ytdl"))
            assertFalse(io.exists("dl/T [zzz] 4K.mp4.part"))
            assertFalse(io.exists("dl/T [zzz] 128kbps.m4a.part"))
            assertFalse(io.exists("dl/T [zzz] M4A.m4a.ytdl"))
            assertFalse(io.exists("dl/T [zzz] Best.f18.mp4.part"))
        }
}
