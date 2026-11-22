package com.homecinema.core.history

import com.homecinema.core.storage.FakeFileIO
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.domain.HistoryEntry
import com.homecinema.domain.MediaKind
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryRepositoryTest {
    private val io = FakeFileIO()
    private val path = "/data/history.json"

    private fun repo() = HistoryRepository(
        JsonFileStore(io, path, ListSerializer(HistoryEntry.serializer()), emptyList()),
        io,
    )

    private fun entry(id: String, filePath: String = "/dl/$id.mp4", thumb: String? = "/thumbs/$id.jpg") =
        HistoryEntry(
            id = id, title = "t$id", url = "u", filePath = filePath, kind = MediaKind.VIDEO,
            ext = "mp4", qualityLabel = "1080p", sizeBytes = 1L, durationSeconds = 1L,
            extractor = null, thumbnailPath = thumb, completedAt = 0L,
        ).also { io.write(filePath, "media"); thumb?.let { t -> io.write(t, "img") } }

    @Test
    fun `add persists newest first`() {
        val r = repo()
        r.add(entry("a"))
        r.add(entry("b"))
        assertEquals(listOf("b", "a"), r.entries.value.map { it.id })
        assertEquals(listOf("b", "a"), repo().entries.value.map { it.id })
    }

    @Test
    fun `add replaces entry with same filePath`() {
        val r = repo()
        r.add(entry("a", filePath = "/dl/x.mp4"))
        r.add(entry("b", filePath = "/dl/x.mp4"))
        assertEquals(listOf("b"), r.entries.value.map { it.id })
    }

    @Test
    fun `missing file is flagged not dropped`() {
        val r = repo()
        r.add(entry("a"))
        io.delete("/dl/a.mp4")
        val reloaded = repo()
        assertEquals(1, reloaded.entries.value.size)
        assertTrue(reloaded.entries.value[0].fileMissing)
        io.write("/dl/a.mp4", "media")
        reloaded.refreshMissing()
        assertFalse(reloaded.entries.value[0].fileMissing)
    }

    @Test
    fun `remove deletes thumbnail but not media`() {
        val r = repo()
        r.add(entry("a"))
        r.remove("a")
        assertTrue(r.entries.value.isEmpty())
        assertFalse(io.exists("/thumbs/a.jpg"))
        assertTrue(io.exists("/dl/a.mp4"))
    }

    @Test
    fun `clear keeps media files`() {
        val r = repo()
        r.add(entry("a"))
        r.add(entry("b"))
        r.clear()
        assertTrue(r.entries.value.isEmpty())
        assertFalse(io.exists("/thumbs/a.jpg"))
        assertFalse(io.exists("/thumbs/b.jpg"))
        assertTrue(io.exists("/dl/a.mp4"))
        assertTrue(io.exists("/dl/b.mp4"))
    }

    @Test
    fun `corrupt json loads empty`() {
        io.write(path, "{not json")
        assertTrue(repo().entries.value.isEmpty())
    }
}
