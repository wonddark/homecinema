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
    val outputPath: String? = null,
    val kind: MediaKind = MediaKind.VIDEO,
    val qualityLabel: String = "",
    val sizeBytes: Long? = null,
    val addedAt: Long = 0L,
    val errorReason: String? = null,
)
