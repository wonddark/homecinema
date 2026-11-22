package com.homecinema.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class HistoryEntry(
    val id: String,
    val title: String,
    val url: String,
    val filePath: String,
    val kind: MediaKind,
    val ext: String,
    val qualityLabel: String,
    val sizeBytes: Long?,
    val durationSeconds: Long,
    val extractor: String?,
    val thumbnailPath: String?,
    val completedAt: Long,
    @Transient val fileMissing: Boolean = false,
)
