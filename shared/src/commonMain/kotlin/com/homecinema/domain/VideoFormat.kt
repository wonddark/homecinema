package com.homecinema.domain

import kotlinx.serialization.Serializable

@Serializable
data class VideoFormat(
    val formatId: String,
    val resolution: String,
    val size: Long? = null,
    val acodec: String? = null,
    val vcodec: String? = null,
    val filesize_approx: Long? = null,
)
