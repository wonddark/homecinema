package com.homecinema.domain

import kotlinx.serialization.Serializable

@Serializable
data class DownloadTask(
    val id: String,
    val url: String,
    val videoInfo: VideoInfo? = null,
    val format: String? = null,
    val state: DownloadState = DownloadState.QUEUED,
    val progress: Float = 0f,
    val outputPath: String? = null
)
