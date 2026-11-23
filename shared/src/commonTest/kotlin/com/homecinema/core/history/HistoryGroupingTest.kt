package com.homecinema.core.history

import com.homecinema.domain.HistoryEntry
import com.homecinema.domain.MediaKind
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryGroupingTest {
    private val zone = TimeZone.of("Europe/Madrid")

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        LocalDateTime(y, mo, d, h, mi).toInstant(zone).toEpochMilliseconds()

    private val now = at(2026, 11, 21, 10, 0)

    @Test
    fun `grouping respects calendar days across midnight`() {
        assertEquals(HistoryGroup.YESTERDAY, groupOf(at(2026, 11, 20, 23, 59), now, zone))
        assertEquals(HistoryGroup.TODAY, groupOf(at(2026, 11, 21, 0, 1), now, zone))
    }

    @Test
    fun `two to seven days is last week and eight days is earlier`() {
        assertEquals(HistoryGroup.LAST_WEEK, groupOf(at(2026, 11, 19, 12, 0), now, zone))
        assertEquals(HistoryGroup.LAST_WEEK, groupOf(at(2026, 11, 14, 12, 0), now, zone))
        assertEquals(HistoryGroup.EARLIER, groupOf(at(2026, 11, 13, 12, 0), now, zone))
    }

    private fun entry(kind: MediaKind, ext: String, quality: String, size: Long?) = HistoryEntry(
        id = "i", title = "t", url = "u", filePath = "/p", kind = kind, ext = ext, qualityLabel = quality,
        sizeBytes = size, durationSeconds = 1, extractor = null, thumbnailPath = null, completedAt = 0,
    )

    @Test
    fun `meta formats video and audio`() {
        assertEquals("MP4 · 1080p · 248 MB", historyMeta(entry(MediaKind.VIDEO, "mp4", "1080p", 248L * 1024 * 1024)))
        assertEquals("M4A · 128kbps · 9.4 MB", historyMeta(entry(MediaKind.AUDIO, "m4a", "128kbps", (9.4 * 1024 * 1024).toLong())))
    }

    @Test
    fun `meta uses provided quality text not the stored label`() {
        assertEquals("MP4 · Mejor", historyMeta(entry(MediaKind.VIDEO, "mp4", "Best", null), quality = "Mejor"))
    }

    @Test
    fun `meta omits unknown size`() {
        assertEquals("MP4 · 720p", historyMeta(entry(MediaKind.VIDEO, "mp4", "720p", null)))
    }

    @Test
    fun `sheet timestamp variants`() {
        assertEquals(SheetStamp(StampDay.TODAY, 21, 11, "18:42"), sheetTimestamp(at(2026, 11, 21, 18, 42), now, zone))
        assertEquals(SheetStamp(StampDay.YESTERDAY, 20, 11, "09:05"), sheetTimestamp(at(2026, 11, 20, 9, 5), now, zone))
        assertEquals(SheetStamp(StampDay.DATE, 12, 11, "18:42"), sheetTimestamp(at(2026, 11, 12, 18, 42), now, zone))
    }
}
