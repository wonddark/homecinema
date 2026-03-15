package com.homecinema.domain

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadState {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}
