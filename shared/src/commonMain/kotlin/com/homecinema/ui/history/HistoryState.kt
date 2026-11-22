package com.homecinema.ui.history

import com.homecinema.core.history.HistoryGroup
import com.homecinema.core.settings.ThemePreference
import com.homecinema.domain.HistoryEntry

enum class HistoryFilter { ALL, VIDEO, AUDIO }

enum class HistorySnackbar { PATH_COPIED, DELETE_FAILED }

sealed interface ConfirmRequest {
    data class DeleteFile(val entry: HistoryEntry) : ConfirmRequest
    data object ClearHistory : ConfirmRequest
}

data class HistoryState(
    val query: String = "",
    val filter: HistoryFilter = HistoryFilter.ALL,
    val counts: Map<HistoryFilter, Int> = HistoryFilter.entries.associateWith { 0 },
    val summaryCount: Int = 0,
    val summaryBytes: Long? = null,
    val groups: List<Pair<HistoryGroup, List<HistoryEntry>>> = emptyList(),
    val sheetFor: HistoryEntry? = null,
    val moreMenuOpen: Boolean = false,
    val confirm: ConfirmRequest? = null,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val snackbar: HistorySnackbar? = null,
)
