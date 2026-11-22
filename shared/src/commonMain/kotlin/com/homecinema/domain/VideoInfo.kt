package com.homecinema.domain

import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    val id: String,
    val title: String,
    val duration: Long,
    val formats: List<VideoFormat>,
    val thumbnailUrl: String? = null,
    val extractor: String? = null,
    val uploader: String? = null,
)
