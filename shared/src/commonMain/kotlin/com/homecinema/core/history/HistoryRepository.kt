package com.homecinema.core.history

import com.homecinema.core.storage.FileIO
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.domain.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/** The persisted list is copy-on-write (StateFlow getAndUpdate) so concurrent callers never corrupt it. */
class HistoryRepository(
    private val store: JsonFileStore<List<HistoryEntry>>,
    private val io: FileIO,
) {
    private val persisted = MutableStateFlow(store.load())
    private val _entries = MutableStateFlow(withMissing(persisted.value))
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    fun add(entry: HistoryEntry) {
        val fresh = entry.copy(fileMissing = false)
        val before = persisted.getAndUpdate { list -> listOf(fresh) + list.filter { it.filePath != entry.filePath } }
        before
            .filter { it.filePath == entry.filePath && it.thumbnailPath != entry.thumbnailPath }
            .forEach { deleteThumbnail(it) }
        commit()
    }

    fun remove(id: String) {
        val before = persisted.getAndUpdate { list -> list.filter { it.id != id } }
        before.filter { it.id == id }.forEach { deleteThumbnail(it) }
        commit()
    }

    fun clear() {
        persisted.getAndUpdate { emptyList() }.forEach { deleteThumbnail(it) }
        commit()
    }

    fun refreshMissing() {
        _entries.value = withMissing(persisted.value)
    }

    private fun deleteThumbnail(entry: HistoryEntry) {
        entry.thumbnailPath?.let { io.delete(it) }
    }

    private fun commit() {
        val current = persisted.value
        store.save(current)
        _entries.value = withMissing(current)
    }

    private fun withMissing(list: List<HistoryEntry>): List<HistoryEntry> =
        list.map { it.copy(fileMissing = !io.exists(it.filePath)) }
}
