package com.homecinema.ui

import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.core.history.HistoryRepository
import com.homecinema.core.platformFileIO
import com.homecinema.core.settings.AppSettings
import com.homecinema.core.settings.SettingsRepository
import com.homecinema.core.storage.FileIO
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.core.usecase.DownloadQueueManager
import com.homecinema.core.usecase.KtorThumbnailSaver
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.HistoryEntry
import com.homecinema.ui.home.HomeViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The app's object graph: storage, repositories, the download queue and the long-lived [HomeViewModel].
 * Build it once per process; it restores the persisted queue on creation. [close] releases the HTTP client
 * and cancels [scope], stopping every download and view-model coroutine.
 */
class AppGraph(
    val engine: DownloaderEngine,
    storageDir: String,
    val downloadDir: String,
    val scope: CoroutineScope,
) {
    private val io: FileIO = platformFileIO()
    private val httpClient = HttpClient()

    val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }

    val history = HistoryRepository(
        JsonFileStore(io, "$storageDir/history.json", ListSerializer(HistoryEntry.serializer()), emptyList()),
        io,
    )

    val settings = SettingsRepository(
        JsonFileStore(io, "$storageDir/settings.json", AppSettings.serializer(), AppSettings()),
    )

    val queue = DownloadQueueManager(
        engine = engine,
        history = history,
        store = JsonFileStore(io, "$storageDir/queue.json", ListSerializer(DownloadTask.serializer()), emptyList()),
        thumbnails = KtorThumbnailSaver(httpClient, io, "$storageDir/thumbnails"),
        io = io,
        downloadDir = downloadDir,
        scope = scope,
        clock = clock,
    )

    val home = HomeViewModel(engine, queue, scope, ::newId)

    init {
        queue.restore()
    }

    /**
     * Tells the queue to ignore the coming kills (so interrupted downloads restore as PAUSED, not FAILED), kills
     * running downloads (their blocking reads ignore cancellation), then cancels, then frees the client.
     */
    fun close() {
        queue.shutdown()
        engine.shutdown()
        scope.cancel()
        httpClient.close()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newId(): String = Uuid.random().toString()
}
