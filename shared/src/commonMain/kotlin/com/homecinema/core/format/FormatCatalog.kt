package com.homecinema.core.format

import com.homecinema.domain.VideoFormat
import kotlin.math.roundToInt

data class QualityOption(val label: String, val height: Int, val formatId: String, val sizeBytes: Long?)

data class AudioOption(
    val formatId: String,
    val ext: String,
    val label: String,
    val abrKbps: Int?,
    val sizeBytes: Long?,
)

data class FormatOptions(
    val qualities: List<QualityOption>,
    val audio: AudioOption?,
    val defaultQuality: QualityOption?,
    val needsFfmpegHint: Boolean,
)

private const val DEFAULT_MAX_HEIGHT = 1080
/** Persisted label of the "best" option; a filename/resume/dedupe key, so never localize the stored value. */
const val BEST_QUALITY_LABEL = "Best"
private val BEST_OPTION = QualityOption(BEST_QUALITY_LABEL, 0, "best", null)

fun qualityLabel(height: Int): String =
    when (height) {
        2160 -> "4K"
        1440 -> "2K"
        else -> "${height}p"
    }

fun buildFormatOptions(formats: List<VideoFormat>, ffmpegAvailable: Boolean): FormatOptions {
    if (formats.isEmpty()) {
        return FormatOptions(listOf(BEST_OPTION), null, BEST_OPTION, needsFfmpegHint = false)
    }

    val candidates = formats.filter { it.hasVideo && (ffmpegAvailable || it.hasAudio) && (it.height ?: 0) > 0 }
    val qualities = candidates
        .groupBy { it.height!! }
        .map { (height, group) ->
            val chosen = group.maxWith(
                compareBy<VideoFormat> { it.bestSize ?: 0L }.thenBy { if (it.ext == "mp4") 1 else 0 },
            )
            QualityOption(qualityLabel(height), height, chosen.formatId, chosen.bestSize)
        }
        .sortedByDescending { it.height }

    val audio = formats
        .filter { it.hasAudio && !it.hasVideo }
        .maxByOrNull { it.abr ?: 0.0 }
        ?.let { fmt ->
            val ext = fmt.ext ?: "audio"
            AudioOption(fmt.formatId, ext, "${ext.uppercase()} · Audio", fmt.abr?.roundToInt(), fmt.bestSize)
        }

    if (qualities.isEmpty()) {
        return FormatOptions(listOf(BEST_OPTION), audio, BEST_OPTION, needsFfmpegHint = !ffmpegAvailable)
    }

    val default = qualities.firstOrNull { it.height <= DEFAULT_MAX_HEIGHT } ?: qualities.last()
    return FormatOptions(qualities, audio, default, needsFfmpegHint = false)
}
