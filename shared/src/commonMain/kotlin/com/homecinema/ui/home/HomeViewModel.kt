package com.homecinema.ui.home

import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.core.downloader.SiteBlockedException
import com.homecinema.core.format.QualityOption
import com.homecinema.core.format.buildFormatOptions
import com.homecinema.core.usecase.DownloadQueue
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.MediaKind
import com.homecinema.domain.VideoInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val engine: DownloaderEngine,
    private val queue: DownloadQueue,
    private val scope: CoroutineScope,
    private val idGen: () -> String,
) {
    private val _state = MutableStateFlow(HomeState(tasks = queue.tasks.value))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var fetchJob: Job? = null
    private var fetchToken = 0

    init {
        scope.launch { queue.tasks.collect { tasks -> _state.update { it.copy(tasks = tasks) } } }
    }

    fun onInputChange(text: String) = _state.update { it.copy(input = text) }

    fun onSubmit() {
        val url = _state.value.input.trim()
        if (url.isEmpty()) return
        fetch(url)
    }

    fun onPaste(clipboard: String?) {
        if (clipboard.isNullOrBlank()) return
        val url = clipboard.trim()
        _state.update { it.copy(input = url) }
        fetch(url)
    }

    fun onClear() {
        fetchJob?.cancel()
        fetchJob = null
        fetchToken++
        _state.update { it.copy(input = "", fetch = FetchState.Idle) }
    }

    fun onRetryFetch() {
        val failed = _state.value.fetch as? FetchState.Failed ?: return
        fetch(failed.url)
    }

    fun onSelectKind(kind: MediaKind) = updateReady { ready ->
        when {
            kind == MediaKind.VIDEO && !ready.videoAvailable -> ready
            kind == MediaKind.AUDIO && ready.options.audio == null -> ready
            else -> ready.copy(kind = kind)
        }
    }

    fun onSelectQuality(q: QualityOption) = updateReady { it.copy(selected = q) }

    fun onDownload() {
        val ready = _state.value.fetch as? FetchState.Ready ?: return
        val task = buildTask(ready) ?: return
        queue.enqueue(task)
        _state.update { it.copy(input = "", fetch = FetchState.Idle) }
    }

    fun onPause(id: String) = queue.pause(id)

    fun onResume(id: String) = queue.resume(id)

    fun onRetry(id: String) = queue.retry(id)

    fun onRemove(id: String) = queue.remove(id)

    private fun buildTask(ready: FetchState.Ready): DownloadTask? {
        val base = DownloadTask(id = idGen(), url = ready.url, videoInfo = ready.info, kind = ready.kind)
        if (ready.kind == MediaKind.AUDIO) {
            val audio = ready.options.audio ?: return null
            return base.copy(
                format = audio.formatId,
                qualityLabel = audio.abrKbps?.let { "${it}kbps" } ?: audio.ext.uppercase(),
                sizeBytes = audio.sizeBytes,
            )
        }
        val quality = ready.selected ?: return null
        return base.copy(format = quality.formatId, qualityLabel = quality.label, sizeBytes = quality.sizeBytes)
    }

    private fun updateReady(transform: (FetchState.Ready) -> FetchState.Ready) {
        _state.update { s ->
            val ready = s.fetch as? FetchState.Ready ?: return@update s
            s.copy(fetch = transform(ready))
        }
    }

    private fun fetch(url: String) {
        fetchJob?.cancel()
        val token = ++fetchToken
        // yt-dlp would read it as an option (e.g. --exec), not a URL; never pass it on.
        if (url.startsWith("-")) {
            fetchJob = null
            _state.update { it.copy(input = url, fetch = FetchState.Failed(url)) }
            return
        }
        _state.update { it.copy(input = url, fetch = FetchState.Fetching(url)) }
        fetchJob = scope.launch {
            val result = try {
                Result.success(engine.extractInfo(url))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (token != fetchToken) return@launch
            _state.update { it.copy(fetch = result.fold({ info -> ready(url, info) }, { e -> FetchState.Failed(url, fetchFailure(e)) })) }
        }
    }

    private fun fetchFailure(e: Throwable): FetchFailure =
        if (e is SiteBlockedException) FetchFailure.SiteBlocked else FetchFailure.Generic

    private fun ready(url: String, info: VideoInfo): FetchState.Ready {
        val options = buildFormatOptions(info.formats, engine.ffmpegAvailable)
        val placeholder = FetchState.Ready(url, info, options, MediaKind.VIDEO, options.defaultQuality)
        if (placeholder.videoAvailable) return placeholder
        return placeholder.copy(kind = MediaKind.AUDIO)
    }
}
