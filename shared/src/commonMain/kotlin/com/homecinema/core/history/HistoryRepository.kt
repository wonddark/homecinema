package com.homecinema.core.history

import com.homecinema.core.storage.FileIO
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.domain.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.jvm.Synchronized

/**
 * The download library, persisted in history.json.
 *
 * The queue adds from a background thread while the UI removes and clears on the main thread, so every
 * mutate-and-save runs under this instance's monitor ([Synchronized]): the last write to disk is always the
 * latest list.
 */
class HistoryRepository(
    private val store: JsonFileStore<List<HistoryEntry>>,
    private val io: FileIO,
) {
    private var persisted: List<HistoryEntry> = store.load()
    private val _entries = MutableStateFlow(withMissing(persisted))
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    @Synchronized
    fun add(entry: HistoryEntry) {
        persisted
            .filter { it.filePath == entry.filePath && it.thumbnailPath != entry.thumbnailPath }
            .forEach { deleteThumbnail(it) }
        commit(listOf(entry.copy(fileMissing = false)) + persisted.filter { it.filePath != entry.filePath })
    }

    @Synchronized
    fun remove(id: String) {
        persisted.filter { it.id == id }.forEach { deleteThumbnail(it) }
        commit(persisted.filter { it.id != id })
    }

    @Synchronized
    fun clear() {
        persisted.forEach { deleteThumbnail(it) }
        commit(emptyList())
    }

    /** Re-checks which files are gone from disk; touches the filesystem, so call it off the main thread. */
    @Synchronized
    fun refreshMissing() {
        _entries.value = withMissing(persisted)
    }

    private fun deleteThumbnail(entry: HistoryEntry) {
        entry.thumbnailPath?.let { io.delete(it) }
    }

    private fun commit(list: List<HistoryEntry>) {
        persisted = list
        store.save(list)
        _entries.value = withMissing(list)
    }

    private fun withMissing(list: List<HistoryEntry>): List<HistoryEntry> =
        list.map { it.copy(fileMissing = !io.exists(it.filePath)) }
}
