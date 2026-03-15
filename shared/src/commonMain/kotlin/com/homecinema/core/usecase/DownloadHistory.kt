package com.homecinema.core.usecase

import com.homecinema.domain.DownloadTask
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Simple in-memory history with serialization support.
 * Platform modules can persist the JSON string to local storage.
 */
class DownloadHistory {
    private val _entries = mutableListOf<DownloadTask>()
    val entries: List<DownloadTask> get() = _entries.toList()

    fun add(task: DownloadTask) {
        _entries.removeAll { it.id == task.id }
        _entries.add(0, task)
        if (_entries.size > 100) _entries.removeAt(_entries.lastIndex)
    }

    fun clear() = _entries.clear()

    fun toJson(): String = Json.encodeToString(_entries)

    fun loadFromJson(json: String) {
        _entries.clear()
        _entries.addAll(Json.decodeFromString(json))
    }
}
