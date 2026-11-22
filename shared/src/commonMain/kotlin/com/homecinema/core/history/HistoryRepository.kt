package com.homecinema.core.history

import com.homecinema.core.storage.FileIO
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.domain.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HistoryRepository(
    private val store: JsonFileStore<List<HistoryEntry>>,
    private val io: FileIO,
) {
    private val persisted = ArrayList(store.load())
    private val _entries = MutableStateFlow(withMissing(persisted))
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    fun add(entry: HistoryEntry) {
        persisted.removeAll { it.filePath == entry.filePath }
        persisted.add(0, entry.copy(fileMissing = false))
        commit()
    }

    fun remove(id: String) {
        val removed = persisted.filter { it.id == id }
        persisted.removeAll(removed)
        removed.forEach { deleteThumbnail(it) }
        commit()
    }

    fun clear() {
        persisted.forEach { deleteThumbnail(it) }
        persisted.clear()
        commit()
    }

    fun refreshMissing() {
        _entries.value = withMissing(persisted)
    }

    private fun deleteThumbnail(entry: HistoryEntry) {
        entry.thumbnailPath?.let { io.delete(it) }
    }

    private fun commit() {
        store.save(persisted.toList())
        _entries.value = withMissing(persisted)
    }

    private fun withMissing(list: List<HistoryEntry>): List<HistoryEntry> =
        list.map { it.copy(fileMissing = !io.exists(it.filePath)) }
}
