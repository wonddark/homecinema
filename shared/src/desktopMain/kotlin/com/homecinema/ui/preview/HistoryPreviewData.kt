package com.homecinema.ui.preview

import com.homecinema.core.history.HistoryGroup
import com.homecinema.core.settings.ThemePreference
import com.homecinema.domain.HistoryEntry
import com.homecinema.domain.MediaKind
import com.homecinema.ui.history.HistoryActions
import com.homecinema.ui.history.HistoryFilter
import com.homecinema.ui.history.HistoryState
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Sample History states matching the design frames (shared by previews and the render check). */
object HistoryPreviewData {
    private const val MB = 1024L * 1024L

    /** 120 characters, to check the 2-line ellipsis keeps the ⋮ button visible. */
    const val LONG_TITLE =
        "The complete beginner's guide to sourdough bread at home: starter, folding, shaping, scoring and baking in a dutch oven!"

    const val CJK_TITLE = "東京の屋台グルメを巡る一日：浅草から新宿まで、地元の人だけが知っている名店と路地裏の味を徹底的に紹介します"

    val actions = HistoryActions(
        onQuery = {},
        onFilter = {},
        onRowClick = {},
        onOpenSheet = {},
        onDismissSheet = {},
        onPlay = {},
        onShare = {},
        onShowInFolder = {},
        onRemove = {},
        onConfirm = {},
        onDismissConfirm = {},
        onToggleMore = {},
        onOpenDownloadsFolder = {},
        onClearHistory = {},
        onTheme = {},
        onSnackbarShown = {},
    )

    val empty = HistoryState(summary = "0 files")

    fun populated(thumbnail: String? = null, missingStreetFood: Boolean = false): HistoryState {
        val today = listOf(
            video("1", "Sunset drive — lofi mix", "1080p", 248 * MB, 12 * 60 + 48, thumbnail, at(0, 18, 42)),
            video("2", "Street food tour in Tokyo", "720p", 86 * MB, 4 * 60 + 12, thumbnail, at(0, 16, 5))
                .copy(fileMissing = missingStreetFood),
            audio("3", "Late night jazz session", "320 kbps", 9_856_614, at(0, 9, 30)),
        )
        val yesterday = listOf(
            video("4", "Mountain trail run POV", "4K", 1_181_116_006, 18 * 60 + 3, thumbnail, at(1, 20, 10)),
            audio("5", "Podcast #142 — Design systems", "192 kbps", 54 * MB, at(1, 8, 0)),
        )
        val lastWeek = listOf(
            video("6", "How to make fresh pasta", "1080p", 312 * MB, 9 * 60 + 27, thumbnail, at(4, 12, 0)),
        )
        return HistoryState(
            counts = mapOf(HistoryFilter.ALL to 14, HistoryFilter.VIDEO to 9, HistoryFilter.AUDIO to 5),
            summary = "14 files · 2.3 GB",
            groups = listOf(
                HistoryGroup.TODAY to today,
                HistoryGroup.YESTERDAY to yesterday,
                HistoryGroup.LAST_WEEK to lastWeek,
            ),
        )
    }

    fun sheetOpen(thumbnail: String? = null): HistoryState {
        val state = populated(thumbnail)
        return state.copy(sheetFor = state.groups.first().second.first())
    }

    fun missingSheetOpen(thumbnail: String? = null): HistoryState {
        val state = populated(thumbnail, missingStreetFood = true)
        return state.copy(sheetFor = state.groups.first().second[1])
    }

    fun menuOpen(thumbnail: String? = null): HistoryState =
        populated(thumbnail).copy(moreMenuOpen = true, theme = ThemePreference.DARK)

    /** Review Focus 5: a 120-character and a CJK title wrap to two lines with an ellipsis. */
    fun longTitles(thumbnail: String? = null): HistoryState =
        HistoryState(
            counts = mapOf(HistoryFilter.ALL to 3, HistoryFilter.VIDEO to 2, HistoryFilter.AUDIO to 1),
            summary = "3 files · 410 MB",
            groups = listOf(
                HistoryGroup.TODAY to listOf(
                    video("l1", LONG_TITLE, "1080p", 248 * MB, 31 * 60, thumbnail, at(0, 10, 0)),
                    video("l2", CJK_TITLE, "720p", 150 * MB, 22 * 60 + 5, thumbnail, at(0, 9, 0)),
                    audio("l3", LONG_TITLE, "128 kbps", 12 * MB, at(0, 8, 0)).copy(fileMissing = true),
                ),
            ),
        )

    /** Epoch millis for [daysAgo] days before today at [hour]:[minute] local time. */
    private fun at(daysAgo: Int, hour: Int, minute: Int): Long {
        val zone = TimeZone.currentSystemDefault()
        val date = Clock.System.now().toLocalDateTime(zone).date.minus(daysAgo, DateTimeUnit.DAY)
        return LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, hour, minute).toInstant(zone).toEpochMilliseconds()
    }

    private fun video(
        id: String,
        title: String,
        quality: String,
        size: Long,
        duration: Long,
        thumbnail: String?,
        completedAt: Long,
    ) = entry(id, title, MediaKind.VIDEO, "mp4", quality, size, duration, thumbnail, completedAt)

    private fun audio(id: String, title: String, bitrate: String, size: Long, completedAt: Long) =
        entry(id, title, MediaKind.AUDIO, "mp3", bitrate, size, 0, null, completedAt)

    private fun entry(
        id: String,
        title: String,
        kind: MediaKind,
        ext: String,
        quality: String,
        size: Long,
        duration: Long,
        thumbnail: String?,
        completedAt: Long,
    ) = HistoryEntry(
        id = id,
        title = title,
        url = "https://example.com/$id",
        filePath = "/downloads/$id.$ext",
        kind = kind,
        ext = ext,
        qualityLabel = quality,
        sizeBytes = size,
        durationSeconds = duration,
        extractor = "YouTube",
        thumbnailPath = thumbnail,
        completedAt = completedAt,
    )
}
