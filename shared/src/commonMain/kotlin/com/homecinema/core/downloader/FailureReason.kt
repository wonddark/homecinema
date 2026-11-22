package com.homecinema.core.downloader

import kotlinx.serialization.Serializable

@Serializable
enum class FailureReason { BLOCKED, CONNECTION_LOST, UNAVAILABLE, UNSUPPORTED, GENERIC }

fun mapFailureReason(message: String?): FailureReason {
    val text = message.orEmpty()
    return when {
        text.contains("HTTP Error 403") -> FailureReason.BLOCKED
        listOf("Unable to download", "timed out", "Connection", "Network").any { text.contains(it) } ->
            FailureReason.CONNECTION_LOST
        listOf("Private video", "unavailable", "removed").any { text.contains(it) } -> FailureReason.UNAVAILABLE
        text.contains("Unsupported URL") -> FailureReason.UNSUPPORTED
        else -> FailureReason.GENERIC
    }
}
