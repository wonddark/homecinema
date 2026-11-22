package com.homecinema.ui.history

import com.homecinema.core.PlatformActions
import com.homecinema.core.ShareResult
import com.homecinema.core.history.HistoryGroup
import com.homecinema.core.history.HistoryRepository
import com.homecinema.core.history.groupOf
import com.homecinema.core.settings.SettingsRepository
import com.homecinema.core.settings.ThemePreference
import com.homecinema.domain.HistoryEntry
import com.homecinema.domain.MediaKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.coroutines.CoroutineContext

class HistoryViewModel(
    private val history: HistoryRepository,
    private val settings: SettingsRepository,
    private val actions: PlatformActions,
    private val downloadDir: String,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val zone: TimeZone,
    private val refreshContext: CoroutineContext = Dispatchers.Default,
) {
    private data class Ui(
        val query: String = "",
        val filter: HistoryFilter = HistoryFilter.ALL,
        val sheetId: String? = null,
        val moreMenuOpen: Boolean = false,
        val confirm: ConfirmRequest? = null,
        val snackbar: HistorySnackbar? = null,
    )

    private val ui = MutableStateFlow(Ui())

    val state: StateFlow<HistoryState> =
        combine(history.entries, settings.settings, ui) { entries, appSettings, u ->
            build(entries, appSettings.theme, u)
        }.stateIn(scope, SharingStarted.Eagerly, build(history.entries.value, settings.settings.value.theme, ui.value))

    init {
        refreshMissing()
    }

    private fun build(entries: List<HistoryEntry>, theme: ThemePreference, u: Ui): HistoryState {
        val q = u.query.trim()
        val searched = if (q.isEmpty()) entries else entries.filter { it.title.contains(q, ignoreCase = true) }
        val filtered = when (u.filter) {
            HistoryFilter.ALL -> searched
            HistoryFilter.VIDEO -> searched.filter { it.kind == MediaKind.VIDEO }
            HistoryFilter.AUDIO -> searched.filter { it.kind == MediaKind.AUDIO }
        }
        val counts = mapOf(
            HistoryFilter.ALL to searched.size,
            HistoryFilter.VIDEO to searched.count { it.kind == MediaKind.VIDEO },
            HistoryFilter.AUDIO to searched.count { it.kind == MediaKind.AUDIO },
        )
        val now = clock()
        val byGroup = filtered.sortedByDescending { it.completedAt }.groupBy { groupOf(it.completedAt, now, zone) }
        val groups = HistoryGroup.entries.mapNotNull { g -> byGroup[g]?.let { g to it } }
        return HistoryState(
            query = u.query,
            filter = u.filter,
            counts = counts,
            summaryCount = filtered.size,
            summaryBytes = filtered.mapNotNull { it.sizeBytes }.takeIf { it.isNotEmpty() }?.sum(),
            groups = groups,
            sheetFor = u.sheetId?.let { id -> entries.firstOrNull { it.id == id } },
            moreMenuOpen = u.moreMenuOpen,
            confirm = u.confirm,
            theme = theme,
            snackbar = u.snackbar,
        )
    }

    private fun sheetEntry(): HistoryEntry? =
        ui.value.sheetId?.let { id -> history.entries.value.firstOrNull { it.id == id } }

    /** Call each time History comes on screen: files may have been moved or deleted since the last check. */
    fun onShown() = refreshMissing()

    /** Stats every file, so it runs on [refreshContext] (a background dispatcher by default), not the caller's thread. */
    private fun refreshMissing() {
        scope.launch(refreshContext) { history.refreshMissing() }
    }

    fun onQuery(q: String) = ui.update { it.copy(query = q) }

    fun onFilter(f: HistoryFilter) = ui.update { it.copy(filter = f) }

    fun onRowClick(e: HistoryEntry) {
        if (e.fileMissing) onOpenSheet(e) else actions.openFile(e.filePath)
    }

    fun onOpenSheet(e: HistoryEntry) = ui.update { it.copy(sheetId = e.id) }

    fun onDismissSheet() = ui.update { it.copy(sheetId = null) }

    fun onPlay() {
        val e = sheetEntry()?.takeUnless { it.fileMissing } ?: return
        actions.openFile(e.filePath)
    }

    fun onShare() {
        val e = sheetEntry()?.takeUnless { it.fileMissing } ?: return
        if (actions.shareFile(e.filePath) == ShareResult.PATH_COPIED) {
            ui.update { it.copy(snackbar = HistorySnackbar.PATH_COPIED) }
        }
    }

    fun onShowInFolder() {
        val e = sheetEntry()?.takeUnless { it.fileMissing } ?: return
        actions.revealInFolder(e.filePath)
    }

    fun onRemove() {
        val e = sheetEntry() ?: return
        if (e.fileMissing) {
            history.remove(e.id)
            ui.update { it.copy(sheetId = null) }
        } else {
            ui.update { it.copy(confirm = ConfirmRequest.DeleteFile(e)) }
        }
    }

    fun onConfirm() {
        val request = ui.value.confirm ?: return
        ui.update { it.copy(confirm = null) }
        when (request) {
            ConfirmRequest.ClearHistory -> history.clear()
            is ConfirmRequest.DeleteFile -> scope.launch {
                if (actions.deleteFile(request.entry.filePath)) {
                    history.remove(request.entry.id)
                    ui.update { it.copy(sheetId = null) }
                } else {
                    ui.update { it.copy(snackbar = HistorySnackbar.DELETE_FAILED) }
                }
            }
        }
    }

    fun onDismissConfirm() = ui.update { it.copy(confirm = null) }

    fun onToggleMore() = ui.update { it.copy(moreMenuOpen = !it.moreMenuOpen) }

    fun onOpenDownloadsFolder() {
        actions.openFolder(downloadDir)
        ui.update { it.copy(moreMenuOpen = false) }
    }

    fun onClearHistory() = ui.update { it.copy(moreMenuOpen = false, confirm = ConfirmRequest.ClearHistory) }

    fun onTheme(t: ThemePreference) {
        settings.setTheme(t)
        ui.update { it.copy(moreMenuOpen = false) }
    }

    fun onSnackbarShown() = ui.update { it.copy(snackbar = null) }
}
