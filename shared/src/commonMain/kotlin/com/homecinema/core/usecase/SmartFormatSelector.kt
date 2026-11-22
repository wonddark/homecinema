package com.homecinema.core.usecase

import com.homecinema.domain.VideoFormat

enum class FormatPreference { BEST_QUALITY, SMALLEST_SIZE, AUDIO_ONLY }

/**
 * Selects a format from the available list based on a preference.
 */
fun selectFormat(
    formats: List<VideoFormat>,
    preference: FormatPreference,
): VideoFormat? {
    if (formats.isEmpty()) return null

    return when (preference) {
        FormatPreference.BEST_QUALITY -> {
            // Prefer the highest resolution; fall back to first format
            formats.maxByOrNull { fmt ->
                fmt.size ?: 0
            } ?: formats.last()
        }

        FormatPreference.SMALLEST_SIZE -> {
            formats.filter { it.size != null }.minByOrNull { it.size!! }
                ?: formats.firstOrNull { it.resolution.contains("360", ignoreCase = true) }
                ?: formats.last()
        }

        FormatPreference.AUDIO_ONLY -> {
            formats.firstOrNull { it.resolution.lowercase() == "audio" || it.formatId.contains("audio") }
                ?: formats.firstOrNull { it.formatId == "bestaudio" }
                ?: formats.last()
        }
    }
}
