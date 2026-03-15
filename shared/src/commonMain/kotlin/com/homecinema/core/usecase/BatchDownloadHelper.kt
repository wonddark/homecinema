package com.homecinema.core.usecase

import com.homecinema.domain.DownloadTask
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Parses a multi-line or comma-separated string of URLs into individual DownloadTask objects.
 */
@OptIn(ExperimentalUuidApi::class)
fun parseBatchUrls(input: String, defaultFormat: String? = null): List<DownloadTask> {
    return input
        .split("\n", ",")
        .map { it.trim() }
        .filter { it.isNotEmpty() && (it.startsWith("http://") || it.startsWith("https://")) }
        .map { url ->
            DownloadTask(
                id = Uuid.random().toString(),
                url = url,
                format = defaultFormat
            )
        }
}
