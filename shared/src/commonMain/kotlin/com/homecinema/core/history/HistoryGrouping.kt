package com.homecinema.core.history

import com.homecinema.domain.HistoryEntry
import com.homecinema.ui.components.formatSize
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class HistoryGroup { TODAY, YESTERDAY, LAST_WEEK, EARLIER }

enum class StampDay { TODAY, YESTERDAY, DATE }

/** Structured sheet timestamp; the UI layer turns it into localized text. */
data class SheetStamp(val day: StampDay, val dayOfMonth: Int, val monthNumber: Int, val time: String)

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

fun historyMeta(e: HistoryEntry, quality: String = e.qualityLabel): String {
    val parts = mutableListOf(e.ext.uppercase(), quality)
    e.sizeBytes?.let { parts += formatSize(it) }
    return parts.joinToString(" · ")
}

fun sheetTimestamp(completedAt: Long, now: Long, zone: TimeZone): SheetStamp {
    val dt = Instant.fromEpochMilliseconds(completedAt).toLocalDateTime(zone)
    val time = "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    val days = daysAgo(completedAt, now, zone)
    val day = when {
        days <= 0 -> StampDay.TODAY
        days == 1 -> StampDay.YESTERDAY
        else -> StampDay.DATE
    }
    return SheetStamp(day, dt.dayOfMonth, dt.monthNumber, time)
}
