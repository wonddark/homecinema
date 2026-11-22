package com.homecinema.ui.home

import com.homecinema.MockDownloaderEngine
import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.core.usecase.DownloadQueue
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.MediaKind
import com.homecinema.domain.VideoFormat
import com.homecinema.domain.VideoInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeQueue : DownloadQueue {
    override val tasks: StateFlow<List<DownloadTask>> = MutableStateFlow(emptyList())
    val enqueued = mutableListOf<DownloadTask>()
    val calls = mutableListOf<String>()

    override fun enqueue(task: DownloadTask) {
        enqueued += task
    }

    override fun pause(id: String) {
        calls += "pause:$id"
    }

    override fun resume(id: String) {
        calls += "resume:$id"
    }

    override fun retry(id: String) {
        calls += "retry:$id"
    }

    override fun remove(id: String) {
        calls += "remove:$id"
    }
}

private class ScriptedEngine(private val handler: suspend (String) -> VideoInfo) : DownloaderEngine {
    private val delegate = MockDownloaderEngine()
    var calls = 0
    override val ffmpegAvailable = true

    override suspend fun extractInfo(url: String): VideoInfo {
        calls++
        return handler(url)
    }

    override suspend fun download(task: DownloadTask, outputDir: String, onProgress: (Float) -> Unit) =
        delegate.download(task, outputDir, onProgress)

    override suspend fun pause(taskId: String) = delegate.pause(taskId)

    override suspend fun cancel(taskId: String) = delegate.cancel(taskId)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val video = VideoInfo(
        "v1",
        "Title",
        10,
        listOf(
            VideoFormat("137", "1080p", 100L, acodec = "none", vcodec = "avc1", height = 1080, ext = "mp4"),
            VideoFormat("140", "audio", 5L, acodec = "mp4a", vcodec = "none", ext = "m4a", abr = 128.0),
        ),
    )

    private fun TestScope.vm(queue: FakeQueue = FakeQueue(), handler: suspend (String) -> VideoInfo = { video }): Pair<HomeViewModel, ScriptedEngine> {
        val engine = ScriptedEngine(handler)
        return HomeViewModel(engine, queue, backgroundScope, { "id-1" }) to engine
    }

    @Test
    fun `submit goes fetching then ready`() = runTest {
        val gate = CompletableDeferred<VideoInfo>()
        val (vm, _) = vm { gate.await() }
        vm.onInputChange(" https://x.com/v ")
        vm.onSubmit()
        runCurrent()
        assertEquals(FetchState.Fetching("https://x.com/v"), vm.state.value.fetch)
        gate.complete(video)
        runCurrent()
        val ready = assertIs<FetchState.Ready>(vm.state.value.fetch)
        assertEquals(MediaKind.VIDEO, ready.kind)
        assertEquals("1080p", ready.selected?.label)
        assertTrue(ready.videoAvailable)
    }

    @Test
    fun `extract failure goes failed`() = runTest {
        val (vm, _) = vm { error("boom") }
        vm.onPaste("https://x.com/v")
        runCurrent()
        assertEquals(FetchState.Failed("https://x.com/v"), vm.state.value.fetch)
    }

    @Test
    fun `retry fetch reuses url`() = runTest {
        var fail = true
        val (vm, engine) = vm { if (fail) error("boom") else video }
        vm.onPaste("https://x.com/v")
        runCurrent()
        fail = false
        vm.onRetryFetch()
        runCurrent()
        assertIs<FetchState.Ready>(vm.state.value.fetch)
        assertEquals(2, engine.calls)
        vm.onRetryFetch()
        runCurrent()
        assertEquals(2, engine.calls)
    }

    @Test
    fun `clear during fetch returns idle and ignores late result`() = runTest {
        val gate = CompletableDeferred<VideoInfo>()
        val (vm, _) = vm { gate.await() }
        vm.onPaste("https://x.com/v")
        runCurrent()
        vm.onClear()
        gate.complete(video)
        runCurrent()
        assertEquals(HomeState(), vm.state.value)
    }

    @Test
    fun `paste empty clipboard does nothing`() = runTest {
        val (vm, engine) = vm()
        vm.onPaste(null)
        vm.onPaste("  ")
        runCurrent()
        assertEquals(HomeState(), vm.state.value)
        assertEquals(0, engine.calls)
    }

    @Test
    fun `paste text fills input and fetches`() = runTest {
        val (vm, _) = vm()
        vm.onPaste("  https://x.com/v \n")
        runCurrent()
        assertEquals("https://x.com/v", vm.state.value.input)
        assertIs<FetchState.Ready>(vm.state.value.fetch)
    }

    @Test
    fun `download enqueues selected format and resets`() = runTest {
        val queue = FakeQueue()
        val (vm, _) = vm(queue)
        vm.onPaste("https://x.com/v")
        runCurrent()
        vm.onDownload()
        val task = queue.enqueued.single()
        assertEquals("id-1", task.id)
        assertEquals("137", task.format)
        assertEquals("1080p", task.qualityLabel)
        assertEquals(MediaKind.VIDEO, task.kind)
        assertEquals(100L, task.sizeBytes)
        assertEquals(FetchState.Idle, vm.state.value.fetch)
        assertEquals("", vm.state.value.input)
    }

    @Test
    fun `audio download uses abr label`() = runTest {
        val queue = FakeQueue()
        val (vm, _) = vm(queue)
        vm.onPaste("https://x.com/v")
        runCurrent()
        vm.onSelectKind(MediaKind.AUDIO)
        vm.onDownload()
        val task = queue.enqueued.single()
        assertEquals("140", task.format)
        assertEquals("128kbps", task.qualityLabel)
        assertEquals(MediaKind.AUDIO, task.kind)
    }

    @Test
    fun `audio only source defaults to audio kind`() = runTest {
        val audioOnly = VideoInfo("a", "T", 1, listOf(VideoFormat("140", "audio", 5L, acodec = "mp4a", vcodec = "none", ext = "m4a", abr = 128.0)))
        val (vm, _) = vm { audioOnly }
        vm.onPaste("https://x.com/a")
        runCurrent()
        val ready = assertIs<FetchState.Ready>(vm.state.value.fetch)
        assertEquals(MediaKind.AUDIO, ready.kind)
        assertFalse(ready.videoAvailable)
    }

    @Test
    fun `queue actions delegate and tasks mirror queue`() = runTest {
        val queue = FakeQueue()
        val (vm, _) = vm(queue)
        vm.onPause("a"); vm.onResume("b"); vm.onRetry("c"); vm.onRemove("d")
        assertEquals(listOf("pause:a", "resume:b", "retry:c", "remove:d"), queue.calls)
    }
}
