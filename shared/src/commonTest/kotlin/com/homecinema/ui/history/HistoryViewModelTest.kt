package com.homecinema.ui.history

import com.homecinema.core.PlatformActions
import com.homecinema.core.ShareResult
import com.homecinema.core.history.HistoryGroup
import com.homecinema.core.history.HistoryRepository
import com.homecinema.core.settings.AppSettings
import com.homecinema.core.settings.SettingsRepository
import com.homecinema.core.settings.ThemePreference
import com.homecinema.core.storage.FakeFileIO
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.domain.HistoryEntry
import com.homecinema.domain.MediaKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.builtins.ListSerializer
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeActions : PlatformActions {
    val calls = mutableListOf<String>()
    var shareResult = ShareResult.SHARED
    var deleteResult = true
    override fun openFile(path: String) { calls += "open:$path" }
    override fun shareFile(path: String): ShareResult { calls += "share:$path"; return shareResult }
    override fun openFolder(path: String) { calls += "folder:$path" }
    override fun revealInFolder(path: String) { calls += "reveal:$path" }
    override suspend fun deleteFile(path: String): Boolean { calls += "delete:$path"; return deleteResult }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val io = FakeFileIO()
    private val zone = TimeZone.of("Europe/Madrid")
    private val mb = 1024L * 1024
    private val now = LocalDateTime(2026, 11, 21, 12, 0).toInstant(zone).toEpochMilliseconds()

    private fun repo() = HistoryRepository(
        JsonFileStore(io, "/data/history.json", ListSerializer(HistoryEntry.serializer()), emptyList()), io,
    )

    private fun settings() = SettingsRepository(JsonFileStore(io, "/data/settings.json", AppSettings.serializer(), AppSettings()))

    private fun entry(id: String, title: String, kind: MediaKind = MediaKind.VIDEO, size: Long? = 10 * mb, ageMs: Long = 0, exists: Boolean = true) =
        HistoryEntry(
            id = id, title = title, url = "u", filePath = "/dl/$id", kind = kind,
            ext = if (kind == MediaKind.VIDEO) "mp4" else "m4a", qualityLabel = "q", sizeBytes = size,
            durationSeconds = 1, extractor = null, thumbnailPath = null, completedAt = now - ageMs,
        ).also { if (exists) io.write(it.filePath, "x") }

    private class Ctx(val vm: HistoryViewModel, val repo: HistoryRepository, val actions: FakeActions, val settings: SettingsRepository)

    private fun TestScope.ctx(vararg entries: HistoryEntry): Ctx {
        val r = repo()
        entries.reversed().forEach { r.add(it) }
        val a = FakeActions()
        val s = settings()
        val vm = HistoryViewModel(r, s, a, "/dl", backgroundScope, { now }, zone, refreshContext = EmptyCoroutineContext)
        runCurrent()
        return Ctx(vm, r, a, s)
    }

    @Test
    fun `counts follow search`() = runTest {
        val c = ctx(entry("1", "Pasta recipe"), entry("2", "Other song", MediaKind.AUDIO))
        c.vm.onQuery("pasta"); runCurrent()
        val counts = c.vm.state.value.counts
        assertEquals(1, counts[HistoryFilter.ALL])
        assertEquals(1, counts[HistoryFilter.VIDEO])
        assertEquals(0, counts[HistoryFilter.AUDIO])
    }

    @Test
    fun `groups are ordered and empty ones omitted`() = runTest {
        val day = 24 * 3600_000L
        val c = ctx(entry("1", "a"), entry("2", "b", ageMs = 20 * day), entry("3", "c", ageMs = day))
        assertEquals(listOf(HistoryGroup.TODAY, HistoryGroup.YESTERDAY, HistoryGroup.EARLIER), c.vm.state.value.groups.map { it.first })
    }

    @Test
    fun `summary sums known sizes only`() = runTest {
        val c = ctx(entry("1", "a", size = 100 * mb), entry("2", "b", size = null), entry("3", "c", size = 50 * mb))
        assertEquals("3 files · 150 MB", c.vm.state.value.summary)
        c.vm.onQuery("a"); runCurrent()
        assertEquals("1 file · 100 MB", c.vm.state.value.summary)
    }

    @Test
    fun `summary without known sizes shows count only`() = runTest {
        val c = ctx(entry("1", "a", size = null), entry("2", "b", size = null))
        assertEquals("2 files", c.vm.state.value.summary)
    }

    @Test
    fun `missing row click opens sheet and present row plays`() = runTest {
        val missing = entry("1", "gone", exists = false)
        val ok = entry("2", "ok")
        val c = ctx(missing, ok)
        val rows = c.vm.state.value.groups.flatMap { it.second }
        c.vm.onRowClick(rows.first { it.id == "1" }); runCurrent()
        assertEquals("1", c.vm.state.value.sheetFor?.id)
        assertTrue(c.actions.calls.isEmpty())
        c.vm.onRowClick(rows.first { it.id == "2" })
        assertEquals(listOf("open:/dl/2"), c.actions.calls)
    }

    @Test
    fun `remove missing skips confirm and file delete`() = runTest {
        val c = ctx(entry("1", "gone", exists = false))
        c.vm.onOpenSheet(c.repo.entries.value.first()); runCurrent()
        c.vm.onRemove(); runCurrent()
        assertNull(c.vm.state.value.confirm)
        assertTrue(c.repo.entries.value.isEmpty())
        assertTrue(c.actions.calls.none { it.startsWith("delete:") })
        assertNull(c.vm.state.value.sheetFor)
    }

    @Test
    fun `confirm delete removes file and entry`() = runTest {
        val c = ctx(entry("1", "a"))
        c.vm.onOpenSheet(c.repo.entries.value.first()); runCurrent()
        c.vm.onRemove(); runCurrent()
        assertIs<ConfirmRequest.DeleteFile>(c.vm.state.value.confirm)
        c.vm.onConfirm(); runCurrent()
        assertEquals(listOf("delete:/dl/1"), c.actions.calls)
        assertTrue(c.repo.entries.value.isEmpty())
        assertNull(c.vm.state.value.sheetFor)
    }

    @Test
    fun `failed delete keeps entry and shows snackbar`() = runTest {
        val c = ctx(entry("1", "a"))
        c.actions.deleteResult = false
        c.vm.onOpenSheet(c.repo.entries.value.first()); runCurrent()
        c.vm.onRemove(); c.vm.onConfirm(); runCurrent()
        assertEquals(1, c.repo.entries.value.size)
        assertEquals("Couldn't delete file", c.vm.state.value.snackbar)
        c.vm.onSnackbarShown(); runCurrent()
        assertNull(c.vm.state.value.snackbar)
    }

    @Test
    fun `share reports copied path and actions no-op when file missing`() = runTest {
        val c = ctx(entry("1", "a"), entry("2", "gone", exists = false))
        c.actions.shareResult = ShareResult.PATH_COPIED
        c.vm.onOpenSheet(c.repo.entries.value.first { it.id == "1" }); runCurrent()
        c.vm.onShare(); runCurrent()
        assertEquals("Path copied", c.vm.state.value.snackbar)
        c.actions.calls.clear()
        c.vm.onOpenSheet(c.repo.entries.value.first { it.id == "2" }); runCurrent()
        c.vm.onPlay(); c.vm.onShare(); c.vm.onShowInFolder()
        assertTrue(c.actions.calls.isEmpty())
    }

    @Test
    fun `clear history confirm clears and theme closes menu`() = runTest {
        val c = ctx(entry("1", "a"))
        c.vm.onToggleMore(); runCurrent()
        assertTrue(c.vm.state.value.moreMenuOpen)
        c.vm.onClearHistory(); runCurrent()
        assertEquals(ConfirmRequest.ClearHistory, c.vm.state.value.confirm)
        assertTrue(!c.vm.state.value.moreMenuOpen)
        c.vm.onConfirm(); runCurrent()
        assertTrue(c.repo.entries.value.isEmpty())
        c.vm.onToggleMore(); c.vm.onTheme(ThemePreference.OCEAN); runCurrent()
        assertEquals(ThemePreference.OCEAN, c.vm.state.value.theme)
        assertTrue(!c.vm.state.value.moreMenuOpen)
        c.vm.onOpenDownloadsFolder()
        assertEquals(listOf("folder:/dl"), c.actions.calls)
    }
    @Test
    fun `opening history re-checks missing files`() = runTest {
        val c = ctx(entry("1", "a"))
        assertFalse(c.vm.state.value.groups.single().second.single().fileMissing)
        io.delete("/dl/1")
        c.vm.onShown(); runCurrent()
        assertTrue(c.vm.state.value.groups.single().second.single().fileMissing)
        io.write("/dl/1", "x")
        c.vm.onShown(); runCurrent()
        assertFalse(c.vm.state.value.groups.single().second.single().fileMissing)
    }
}
