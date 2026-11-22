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
    val height: Int? = null,
    val ext: String? = null,
    val abr: Double? = null,
) {
    val hasVideo: Boolean get() = vcodec != null && vcodec != "none"
    val hasAudio: Boolean get() = acodec != null && acodec != "none"
    val bestSize: Long? get() = size ?: filesize_approx
}
