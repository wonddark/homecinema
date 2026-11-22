package com.homecinema

import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.VideoFormat
import com.homecinema.domain.VideoInfo

class MockDownloaderEngine(
    private val shouldFail: Boolean = false,
    private val progressSteps: List<Float> = listOf(0.25f, 0.5f, 0.75f, 1.0f),
    private val mockOutputPath: String = "mock/downloads/Mock Video Title.mp4"
) : DownloaderEngine {

    override val ffmpegAvailable: Boolean = true

    var cancelledTaskId: String? = null
    var pausedTaskId: String? = null

    override suspend fun extractInfo(url: String): VideoInfo {
        if (shouldFail) throw RuntimeException("Mock extraction failure")
        return VideoInfo(
            id = "mock-id",
            title = "Mock Video Title",
            duration = 120L,
            formats = listOf(
                VideoFormat("best", "best", null),
                VideoFormat("137", "1080p", 100_000_000L),
                VideoFormat("136", "720p", 50_000_000L)
            )
        )
    }

    override suspend fun download(
        task: DownloadTask,
        outputDir: String,
        onProgress: (Float) -> Unit
    ): String {
        if (shouldFail) throw RuntimeException("Mock download failure")
        for (step in progressSteps) {
            onProgress(step)
        }
        return mockOutputPath
    }

    override suspend fun pause(taskId: String) {
        pausedTaskId = taskId
    }

    override suspend fun cancel(taskId: String) {
        cancelledTaskId = taskId
    }
}
