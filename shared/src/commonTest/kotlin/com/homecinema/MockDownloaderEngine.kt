package com.homecinema

import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.VideoFormat
import com.homecinema.domain.VideoInfo
import kotlinx.coroutines.CompletableDeferred

/**
 * Each download() call parks on a fresh [CompletableDeferred] gate so tests decide when it ends.
 * pause()/cancel() fail the gate, mimicking the real engine where killing the process makes download() throw.
 */
class MockDownloaderEngine : DownloaderEngine {
    override val ffmpegAvailable: Boolean = true

    private val gates = mutableMapOf<String, CompletableDeferred<String>>()
    val downloadCalls = mutableListOf<DownloadTask>()
    val pausedIds = mutableListOf<String>()
    val cancelledIds = mutableListOf<String>()
    var shutdownCalls = 0
    var onShutdown: () -> Unit = {}

    override suspend fun extractInfo(url: String): VideoInfo =
        VideoInfo(
            id = "mock-id",
            title = "Mock Video Title",
            duration = 120L,
            formats = listOf(VideoFormat("137", "1080p", 100_000_000L)),
        )

    override suspend fun download(
        task: DownloadTask,
        outputDir: String,
        onProgress: (Float) -> Unit,
    ): String {
        downloadCalls += task
        val gate = CompletableDeferred<String>()
        gates[task.id] = gate
        onProgress(0.1f)
        return gate.await()
    }

    fun complete(taskId: String, path: String) {
        gates.getValue(taskId).complete(path)
    }

    fun fail(taskId: String, message: String) {
        gates.getValue(taskId).completeExceptionally(RuntimeException(message))
    }

    override suspend fun pause(taskId: String) {
        pausedIds += taskId
        gates[taskId]?.completeExceptionally(RuntimeException("process killed"))
    }

    override fun shutdown() {
        shutdownCalls++
        onShutdown()
    }

    override suspend fun cancel(taskId: String) {
        cancelledIds += taskId
        gates[taskId]?.completeExceptionally(RuntimeException("process killed"))
    }
}
