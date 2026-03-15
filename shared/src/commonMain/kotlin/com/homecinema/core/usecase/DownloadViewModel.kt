package com.homecinema.core.usecase

import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.ui.state.DownloadEventHandler
import com.homecinema.ui.state.DownloadUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import com.homecinema.core.revealInFileBrowser
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class DownloadViewModel(
    private val engine: DownloaderEngine,
    private val queueManager: DownloadQueueManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : DownloadEventHandler {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    init {
        queueManager.tasks
            .onEach { tasks ->
                _uiState.update { state ->
                    state.copy(
                        tasks = tasks,
                        isDownloading = tasks.any { it.state == DownloadState.DOWNLOADING }
                    )
                }
            }
            .launchIn(scope)
    }

    override fun onUrlChanged(url: String) {
        _uiState.update { it.copy(urlInput = url, errorMessage = null) }
    }

    override fun onStartDownload(url: String) {
        if (url.isBlank()) return
        scope.launch {
            _uiState.update { it.copy(isLoadingInfo = true, errorMessage = null) }
            try {
                val videoInfo = engine.extractInfo(url)
                val task = DownloadTask(
                    id = Uuid.random().toString(),
                    url = url,
                    videoInfo = videoInfo,
                    format = selectFormat(videoInfo.formats, FormatPreference.BEST_QUALITY)?.formatId
                )
                queueManager.enqueue(task)
                _uiState.update { it.copy(urlInput = "", isLoadingInfo = false) }
                queueManager.startNext()
            } catch (e: Exception) {
                println(e.message)
                _uiState.update {
                    it.copy(isLoadingInfo = false, errorMessage = "Failed: ${e.message}")
                }
            }
        }
    }

    override fun onCancelDownload(taskId: String) {
        queueManager.cancel(taskId)
    }

    override fun onSelectFormat(taskId: String, formatId: String) {
        queueManager.restart(taskId, formatId)
    }

    override fun onRetryDownload(taskId: String) {
        scope.launch {
            val task = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return@launch
            val retryTask = task.copy(state = DownloadState.QUEUED, progress = 0f, outputPath = null)
            queueManager.remove(task.id)
            queueManager.enqueue(retryTask)
            queueManager.startNext()
        }
    }

    override fun onRemoveDownload(taskId: String) {
        queueManager.remove(taskId)
    }

    override fun onLocateDownload(taskId: String) {
        val path = _uiState.value.tasks.firstOrNull { it.id == taskId }?.outputPath
        if (!path.isNullOrBlank()) {
            revealInFileBrowser(path)
        }
    }
}
