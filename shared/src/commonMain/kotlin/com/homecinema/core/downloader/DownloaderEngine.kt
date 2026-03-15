package com.homecinema.core.downloader

import com.homecinema.domain.VideoInfo

interface DownloaderEngine {
    suspend fun extractInfo(url: String): VideoInfo
    suspend fun download(
        taskId: String,
        url: String,
        formatId: String,
        outputPath: String,
        onProgress: (Float) -> Unit
    ): String
    suspend fun cancel(taskId: String)
}

expect fun createDownloaderEngine(): DownloaderEngine
