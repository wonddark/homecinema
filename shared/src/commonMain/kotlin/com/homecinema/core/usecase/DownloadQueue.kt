package com.homecinema.core.usecase

import com.homecinema.domain.DownloadTask
import kotlinx.coroutines.flow.StateFlow

interface DownloadQueue {
    val tasks: StateFlow<List<DownloadTask>>

    fun enqueue(task: DownloadTask)

    fun pause(id: String)

    fun resume(id: String)

    fun retry(id: String)

    fun remove(id: String)
}
