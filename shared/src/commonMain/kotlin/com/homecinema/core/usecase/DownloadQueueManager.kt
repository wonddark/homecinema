package com.homecinema.core.usecase

import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.core.downloader.isOwnPartialFile
import com.homecinema.core.downloader.isPartialFileOf
import com.homecinema.core.downloader.mapFailureReason
import com.homecinema.core.history.HistoryRepository
import com.homecinema.core.storage.FileIO
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.HistoryEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persistent download queue running up to [maxConcurrent] downloads at once.
 *
 * Every mutation of the task list (and every call into [history]) happens under [mutex]. Public calls lock
 * with an UNDISPATCHED launch: uncontended they apply synchronously, contended they queue FIFO on the mutex,
 * so calls keep their order.
 *
 * Each started download gets a run token in [activeRuns]. pause/remove drop the token, so when the killed
 * process makes engine.download() throw, the outcome is recognised as stale and never marks the task FAILED.
 */
class DownloadQueueManager(
    private val engine: DownloaderEngine,
    private val history: HistoryRepository,
    private val store: JsonFileStore<List<DownloadTask>>,
    private val thumbnails: ThumbnailSaver,
    private val io: FileIO,
    private val downloadDir: String,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val maxConcurrent: Int = 4,
) {
    private val mutex = Mutex()
    private val activeRuns = mutableMapOf<String, Any>()
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())

    /** Non-completed tasks in insertion order. */
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    /** Loads queue.json, turns DOWNLOADING into PAUSED, deletes our own orphan partial files, then starts queued tasks. */
    fun restore() =
        locked {
            _tasks.value =
                store
                    .load()
                    .filter { it.state != DownloadState.COMPLETED }
                    .map { if (it.state == DownloadState.DOWNLOADING) it.copy(state = DownloadState.PAUSED) else it }
            persist()
            sweepOrphanPartials()
            pump()
        }

    fun enqueue(task: DownloadTask) =
        locked {
            if (_tasks.value.none { it.id == task.id }) {
                _tasks.value += task.copy(state = DownloadState.QUEUED, addedAt = clock())
                persist()
                pump()
            }
        }

    fun pause(id: String) =
        locked {
            val task = find(id) ?: return@locked
            if (task.state != DownloadState.DOWNLOADING && task.state != DownloadState.QUEUED) return@locked
            val wasRunning = activeRuns.remove(id) != null
            transition(id) { it.copy(state = DownloadState.PAUSED) }
            if (wasRunning) scope.launch { engine.pause(id) }
            pump()
        }

    fun resume(id: String) = requeue(id, DownloadState.PAUSED)

    fun retry(id: String) = requeue(id, DownloadState.FAILED)

    fun remove(id: String) =
        locked {
            if (find(id) == null) return@locked
            activeRuns.remove(id)
            _tasks.value = _tasks.value.filter { it.id != id }
            persist()
            scope.launch { engine.cancel(id) }
            pump()
        }

    private fun requeue(
        id: String,
        from: DownloadState,
    ) = locked {
        if (find(id)?.state != from) return@locked
        transition(id) { it.copy(state = DownloadState.QUEUED, errorReason = null) }
        pump()
    }

    // --- Everything below runs under [mutex]. ---

    private fun pump() {
        var running = _tasks.value.count { it.state == DownloadState.DOWNLOADING }
        while (running < maxConcurrent) {
            val next = _tasks.value.firstOrNull { it.state == DownloadState.QUEUED } ?: return
            start(next)
            running++
        }
    }

    private fun start(task: DownloadTask) {
        val token = Any()
        activeRuns[task.id] = token
        transition(task.id) { it.copy(state = DownloadState.DOWNLOADING) }
        val started = find(task.id) ?: return
        scope.launch { run(started, token) }
    }

    private suspend fun run(
        task: DownloadTask,
        token: Any,
    ) {
        val path =
            try {
                engine.download(task, downloadDir) { progress -> onProgress(task.id, token, progress) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(task.id, token, mapFailureReason(e.message))
                return
            }
        // A blank path means the file cannot be located, so it must not reach history.
        if (path.isBlank()) {
            fail(task.id, token, mapFailureReason(null))
            return
        }
        val thumbnailPath = task.videoInfo?.thumbnailUrl?.let { thumbnails.save(it, task.id) }
        mutex.withLock {
            if (activeRuns[task.id] !== token) return
            activeRuns.remove(task.id)
            history.add(historyEntry(task, path, thumbnailPath))
            _tasks.value = _tasks.value.filter { it.id != task.id }
            persist()
            pump()
        }
    }

    private suspend fun fail(
        id: String,
        token: Any,
        reason: String,
    ) = mutex.withLock {
        if (activeRuns[id] !== token) return@withLock // paused or removed: the kill is not a failure
        activeRuns.remove(id)
        transition(id) { it.copy(state = DownloadState.FAILED, errorReason = reason) }
        pump()
    }

    /** Progress only updates memory; queue.json is written on state transitions. */
    private fun onProgress(
        id: String,
        token: Any,
        progress: Float,
    ) = locked {
        if (activeRuns[id] === token) {
            _tasks.value = _tasks.value.map { if (it.id == id) it.copy(progress = progress) else it }
        }
    }

    private fun historyEntry(
        task: DownloadTask,
        path: String,
        thumbnailPath: String?,
    ) = HistoryEntry(
        id = task.id,
        title = task.videoInfo?.title ?: path.substringAfterLast('/'),
        url = task.url,
        filePath = path,
        kind = task.kind,
        ext = path.substringAfterLast('.', ""),
        qualityLabel = task.qualityLabel,
        sizeBytes = task.sizeBytes,
        durationSeconds = task.videoInfo?.duration ?: 0L,
        extractor = task.videoInfo?.extractor,
        thumbnailPath = thumbnailPath,
        completedAt = clock(),
    )

    private fun sweepOrphanPartials() {
        val tasks = _tasks.value
        io
            .list(downloadDir)
            .map { it.substringAfterLast('/').substringAfterLast('\\') to it }
            .filter { (name, _) -> isOwnPartialFile(name) }
            .filter { (name, _) -> tasks.none { isPartialFileOf(name, it) } }
            .forEach { (_, path) -> io.delete(path) }
    }

    private fun find(id: String) = _tasks.value.firstOrNull { it.id == id }

    private fun transition(
        id: String,
        change: (DownloadTask) -> DownloadTask,
    ) {
        _tasks.value = _tasks.value.map { if (it.id == id) change(it) else it }
        persist()
    }

    private fun persist() = store.save(_tasks.value)

    private fun locked(block: () -> Unit) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { mutex.withLock { block() } }
    }
}
