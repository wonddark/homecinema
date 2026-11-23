package com.homecinema.core.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class JsonFileStore<T>(
    private val io: FileIO,
    private val path: String,
    private val serializer: KSerializer<T>,
    private val default: T,
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }

    fun load(): T {
        val text = io.read(path) ?: return default
        return try {
            json.decodeFromString(serializer, text)
        } catch (_: Exception) {
            default
        }
    }

    fun save(value: T) {
        io.write(path, json.encodeToString(serializer, value))
    }
}
