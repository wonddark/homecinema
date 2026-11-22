package com.homecinema.core.usecase

import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.core.platformDownloadDir
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadQueueManager(
    private val engine: DownloaderEngine,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    fun enqueue(task: DownloadTask) {
        _tasks.value = _tasks.value + task
    }

    suspend fun startNext() {
        val nextTask = _tasks.value.firstOrNull { it.state == DownloadState.QUEUED } ?: return
        updateTaskState(nextTask.id, DownloadState.DOWNLOADING)
        scope.launch {
            try {
                val outputPath =
                    engine.download(
                        task = nextTask,
                        outputDir = platformDownloadDir(),
                        onProgress = { progress -> updateTaskProgress(nextTask.id, progress) },
                    )
                markTaskCompleted(nextTask.id, outputPath)
            } catch (e: Exception) {
                println(e)
                markTaskFailed(nextTask.id)
            }
        }
    }

    fun remove(taskId: String) {
        _tasks.value = _tasks.value.filter { it.id != taskId }
    }

    fun cancel(taskId: String) {
        scope.launch {
            engine.cancel(taskId)
            updateTaskState(taskId, DownloadState.FAILED)
        }
    }

    fun restart(
        taskId: String,
        formatId: String,
    ) {
        scope.launch {
            engine.cancel(taskId)
            _tasks.value =
                _tasks.value.map { task ->
                    if (task.id == taskId) {
                        task.copy(format = formatId, state = DownloadState.QUEUED, progress = 0f)
                    } else {
                        task
                    }
                }
            startNext()
        }
    }

    private fun updateTaskProgress(
        taskId: String,
        progress: Float,
    ) {
        _tasks.value =
            _tasks.value.map { task ->
                if (task.id == taskId) task.copy(progress = progress) else task
            }
    }

    private fun markTaskCompleted(
        taskId: String,
        outputPath: String,
    ) {
        _tasks.value =
            _tasks.value.map { task ->
                if (task.id == taskId) {
                    task.copy(state = DownloadState.COMPLETED, outputPath = outputPath.ifBlank { null })
                } else {
                    task
                }
            }
    }

    private fun markTaskFailed(taskId: String) {
        updateTaskState(taskId, DownloadState.FAILED)
    }

    private fun updateTaskState(
        taskId: String,
        state: DownloadState,
    ) {
        _tasks.value =
            _tasks.value.map { task ->
                if (task.id == taskId) task.copy(state = state) else task
            }
    }
}
