package com.homecinema.ui.components

import kotlin.math.roundToLong

private const val KB = 1024L
private const val MB = KB * 1024L
private const val GB = MB * 1024L

/** Human-readable byte size, base 1024: "512 KB", "9.4 MB", "86 MB", "1.1 GB". */
fun formatSize(bytes: Long): String =
    when {
        bytes < MB -> "${bytes / KB} KB"
        bytes < 10 * MB -> "${oneDecimal(bytes.toDouble() / MB)} MB"
        bytes < GB -> "${(bytes.toDouble() / MB).roundToLong()} MB"
        else -> "${oneDecimal(bytes.toDouble() / GB)} GB"
    }

/** Duration as "m:ss" under an hour, otherwise "h:mm:ss". */
fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    if (hours > 0) return "$hours:${twoDigits(minutes)}:${twoDigits(secs)}"
    return "$minutes:${twoDigits(secs)}"
}

private fun oneDecimal(value: Double): String {
    val tenths = (value * 10).roundToLong()
    return "${tenths / 10}.${tenths % 10}"
}

private fun twoDigits(value: Long): String = value.toString().padStart(2, '0')
