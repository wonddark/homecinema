package com.homecinema.core.downloader

fun mapFailureReason(message: String?): String {
    val text = message.orEmpty()
    return when {
        text.contains("HTTP Error 403") -> "Blocked by site"
        listOf("Unable to download", "timed out", "Connection", "Network").any { text.contains(it) } ->
            "Connection lost"
        listOf("Private video", "unavailable", "removed").any { text.contains(it) } -> "Unavailable"
        text.contains("Unsupported URL") -> "Unsupported site"
        else -> "Download failed"
    }
}
