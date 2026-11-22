package com.homecinema.core.history

import com.homecinema.domain.HistoryEntry
import com.homecinema.ui.components.formatSize
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class HistoryGroup(val label: String) {
    TODAY("TODAY"),
    YESTERDAY("YESTERDAY"),
    LAST_WEEK("LAST WEEK"),
    EARLIER("EARLIER"),
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun daysAgo(completedAt: Long, now: Long, zone: TimeZone): Int {
    val then = Instant.fromEpochMilliseconds(completedAt).toLocalDateTime(zone).date
    val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date
    return today.toEpochDays() - then.toEpochDays()
}

fun groupOf(completedAt: Long, now: Long, zone: TimeZone): HistoryGroup {
    val days = daysAgo(completedAt, now, zone)
    if (days <= 0) return HistoryGroup.TODAY
    if (days == 1) return HistoryGroup.YESTERDAY
    if (days <= 7) return HistoryGroup.LAST_WEEK
    return HistoryGroup.EARLIER
}

fun historyMeta(e: HistoryEntry): String {
    val parts = mutableListOf(e.ext.uppercase(), e.qualityLabel)
    e.sizeBytes?.let { parts += formatSize(it) }
    return parts.joinToString(" · ")
}

fun sheetTimestamp(completedAt: Long, now: Long, zone: TimeZone): String {
    val dt = Instant.fromEpochMilliseconds(completedAt).toLocalDateTime(zone)
    val time = "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    val days = daysAgo(completedAt, now, zone)
    if (days <= 0) return "Today, $time"
    if (days == 1) return "Yesterday, $time"
    return "${dt.dayOfMonth} ${MONTHS[dt.monthNumber - 1]}, $time"
}
