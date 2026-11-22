package com.homecinema.core.downloader

import com.homecinema.domain.DownloadTask
import com.homecinema.domain.VideoInfo

interface DownloaderEngine {
    val ffmpegAvailable: Boolean

    suspend fun extractInfo(url: String): VideoInfo

    /** Returns the final file path. Throws with yt-dlp's last error line on failure. */
    suspend fun download(
        task: DownloadTask,
        outputDir: String,
        onProgress: (Float) -> Unit,
    ): String

    /** Kills the process but keeps the .part files so the download can resume. */
    suspend fun pause(taskId: String)

    /** Kills the process and deletes the task's partial files. */
    suspend fun cancel(taskId: String)
}

expect fun createDownloaderEngine(): DownloaderEngine
