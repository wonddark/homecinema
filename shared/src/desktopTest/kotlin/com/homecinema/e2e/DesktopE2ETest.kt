package com.homecinema.e2e

import com.homecinema.core.DesktopPlatformActions
import com.homecinema.core.downloader.DownloaderEngineDesktop
import com.homecinema.core.format.buildFormatOptions
import com.homecinema.core.history.HistoryRepository
import com.homecinema.core.platformFileIO
import com.homecinema.core.settings.AppSettings
import com.homecinema.core.settings.SettingsRepository
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.core.usecase.DownloadQueueManager
import com.homecinema.core.usecase.KtorThumbnailSaver
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.HistoryEntry
import com.homecinema.domain.MediaKind
import com.homecinema.ui.history.ConfirmRequest
import com.homecinema.ui.history.HistoryViewModel
import com.homecinema.ui.home.FetchState
import com.homecinema.ui.home.HomeViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * End-to-end check of the desktop download path against the real yt-dlp, ffmpeg and network.
 * Opt-in: runs only with `-Pe2e=true`. Storage and download folders live under [ROOT] (never ~/Downloads); `-Pe2eDir=<dir>` moves it.
 */
class DesktopE2ETest {
    private val enabled = System.getProperty("e2e") == "true"
    private val engine by lazy { DownloaderEngineDesktop() }
    private val scopes = mutableListOf<CoroutineScope>()
    private val clients = mutableListOf<HttpClient>()

    /** The real pieces AppGraph wires, rooted at [dir]; building a second one from the same dir simulates a restart. */
    private inner class Graph(dir: File) {
        val storage = File(dir, "storage").apply { mkdirs() }.path
        val downloads = File(dir, "downloads").apply { mkdirs() }.path
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scopes += it }
        private val io = platformFileIO()
        private val client = HttpClient().also { clients += it }
        val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }
        val historyStore = JsonFileStore(io, "$storage/history.json", ListSerializer(HistoryEntry.serializer()), emptyList())
        val history = HistoryRepository(historyStore, io)
        val settings = SettingsRepository(JsonFileStore(io, "$storage/settings.json", AppSettings.serializer(), AppSettings()))
        val queue = DownloadQueueManager(
            engine = engine,
            history = history,
            store = JsonFileStore(io, "$storage/queue.json", ListSerializer(DownloadTask.serializer()), emptyList()),
            thumbnails = KtorThumbnailSaver(client, io, "$storage/thumbnails"),
            io = io,
            downloadDir = downloads,
            scope = scope,
            clock = clock,
        ).also { it.restore() }
        val home = HomeViewModel(engine, queue, scope) { UUID.randomUUID().toString() }

        fun historyVm() = HistoryViewModel(
            history, settings, DesktopPlatformActions, downloads, scope, clock, TimeZone.currentSystemDefault(),
        )
    }

    private fun freshDir(name: String) = File(ROOT, name).apply { deleteRecursively(); mkdirs() }

    @AfterTest
    fun tearDown() {
        if (!enabled) return
        engine.shutdown()
        scopes.forEach { it.cancel() }
        clients.forEach { it.close() }
    }

    @Test
    fun `1 extract info builds video qualities and an audio option`() {
        if (!enabled) return
        runBlocking {
            val info = withTimeout(2.minutes) { engine.extractInfo(ZOO) }
            val options = buildFormatOptions(info.formats, engine.ffmpegAvailable)
            log("1: title='${info.title}' id=${info.id} ffmpeg=${engine.ffmpegAvailable}")
            log("1: qualities=${options.qualities.map { it.label }} default=${options.defaultQuality?.label} audio=${options.audio}")
            assertEquals(ZOO_ID, info.id)
            assertTrue(engine.ffmpegAvailable)
            assertTrue(options.qualities.isNotEmpty() && options.qualities.none { it.label == "Best" })
            assertNotNull(options.audio)
        }
    }

    @Test
    fun `2 and 6 lowest quality video downloads with audio, then a renamed file is flagged missing`() {
        if (!enabled) return
        val g = Graph(freshDir("video"))
        runBlocking {
            val ready = fetchReady(g.home, ZOO)
            val lowest = ready.options.qualities.last()
            g.home.onSelectQuality(lowest)
            g.home.onDownload()
            val entry = awaitHistory(g, 5.minutes)
            val file = File(entry.filePath)
            log("2: file=${file.name} size=${file.length()} quality=${entry.qualityLabel} kind=${entry.kind}")
            assertEquals(lowest.label, entry.qualityLabel)
            assertTrue(
                Regex("""^.+ \[$ZOO_ID] ${Regex.escape(lowest.label)}\.mp4$""").matches(file.name),
                "unexpected name ${file.name}",
            )
            assertTrue(file.isFile)
            val streams = ffprobeStreams(file)
            log("2: ffprobe streams=$streams")
            assertTrue("video" in streams && "audio" in streams, "streams=$streams")
            assertEquals(MediaKind.VIDEO, entry.kind)
            val thumb = assertNotNull(entry.thumbnailPath, "no thumbnail saved")
            log("2: thumbnail=$thumb size=${File(thumb).length()}")
            assertTrue(File(thumb).length() > 0)
            assertTrue(g.queue.tasks.value.isEmpty())

            // 6: rename the file on disk; a reload (new repository from the same store) flags it, a refresh does too.
            val renamed = File(file.parentFile, "renamed-${file.name}")
            assertTrue(file.renameTo(renamed))
            val reloaded = HistoryRepository(g.historyStore, platformFileIO())
            log("6: reloaded fileMissing=${reloaded.entries.value.map { it.fileMissing }}")
            assertTrue(reloaded.entries.value.single().fileMissing)

            // History VM on the live repository: refresh on open flags it; Remove on a missing row needs no confirm.
            // (state is collected on Dispatchers.Default, so wait for it rather than reading .value.)
            val vm = g.historyVm()
            val missing = withTimeout(5.seconds) {
                vm.state.first { s -> s.groups.flatMap { it.second }.singleOrNull()?.fileMissing == true }
            }.groups.flatMap { it.second }.single()
            vm.onOpenSheet(missing)
            vm.onRemove()
            withTimeout(5.seconds) { vm.state.first { it.groups.isEmpty() } }
            assertEquals(null, vm.state.value.confirm)
            assertTrue(g.history.entries.value.isEmpty())
            assertFalse(File(thumb).exists(), "thumbnail not deleted on remove")
            assertTrue(renamed.exists(), "remove of a missing entry must not touch other files")
            log("6: missing entry removed without confirm; thumbnail deleted")
        }
    }

    @Test
    fun `3 audio download is audio only`() {
        if (!enabled) return
        val g = Graph(freshDir("audio"))
        runBlocking {
            fetchReady(g.home, ZOO)
            g.home.onSelectKind(MediaKind.AUDIO)
            g.home.onDownload()
            val entry = awaitHistory(g, 5.minutes)
            val file = File(entry.filePath)
            val streams = ffprobeStreams(file)
            log("3: file=${file.name} kind=${entry.kind} ext=${entry.ext} streams=$streams")
            assertEquals(MediaKind.AUDIO, entry.kind)
            assertEquals(listOf("audio"), streams)

            // Remove on a present file asks first, then deletes it via DesktopPlatformActions.deleteFile.
            val vm = g.historyVm()
            vm.onOpenSheet(entry)
            vm.onRemove()
            val asked = withTimeout(5.seconds) { vm.state.first { it.confirm != null } }
            assertIs<ConfirmRequest.DeleteFile>(asked.confirm)
            vm.onConfirm()
            withTimeout(10.seconds) { g.history.entries.first { it.isEmpty() } }
            assertFalse(file.exists(), "file not deleted")
            log("3: confirm + delete removed file and entry")
        }
    }

    @Test
    fun `4 pause survives a restart and resume completes`() {
        if (!enabled) return
        val dir = freshDir("resume")
        val first = Graph(dir)
        val id = runBlocking {
            val ready = fetchReady(first.home, BUNNY)
            val mid = ready.options.qualities.firstOrNull { it.height == 480 }
                ?: ready.options.qualities[ready.options.qualities.size / 2]
            log("4: qualities=${ready.options.qualities.map { "${it.label}:${it.sizeBytes}" }} picked=${mid.label}")
            first.home.onSelectQuality(mid)
            first.home.onDownload()
            val running = withTimeout(3.minutes) {
                first.queue.tasks.first { tasks -> tasks.any { it.state == DownloadState.DOWNLOADING && it.progress > 0.02f } }
            }.single()
            log("4: progress before pause=${running.progress}")
            first.home.onPause(running.id)
            val paused = withTimeout(10.seconds) {
                first.queue.tasks.first { tasks -> tasks.single().state == DownloadState.PAUSED }
            }.single()
            delay(2_000) // let the killed process exit
            assertTrue(partFiles(first.downloads).isNotEmpty(), "no .part after pause: ${File(first.downloads).list()?.toList()}")
            log("4: paused; partials=${partFiles(first.downloads)}")
            paused.id
        }
        // Simulate quitting the app: stop everything belonging to the first graph.
        first.scope.cancel()

        val second = Graph(dir)
        runBlocking {
            val restored = second.queue.tasks.value.single()
            log("4: after restart state=${restored.state} progress=${restored.progress}; partials=${partFiles(second.downloads)}")
            assertEquals(id, restored.id)
            assertEquals(DownloadState.PAUSED, restored.state)
            assertTrue(partFiles(second.downloads).isNotEmpty(), ".part swept on restart")
            second.home.onResume(id)
            val entry = awaitHistory(second, 10.minutes)
            val file = File(entry.filePath)
            val streams = ffprobeStreams(file)
            log("4: resumed -> file=${file.name} size=${file.length()} streams=$streams")
            assertTrue("video" in streams && "audio" in streams)
            assertTrue(partFiles(second.downloads).isEmpty())
        }
    }

    @Test
    fun `5 invalid link fails to fetch`() {
        if (!enabled) return
        val g = Graph(freshDir("invalid"))
        runBlocking {
            val e = assertFailsWith<Exception> { withTimeout(2.minutes) { engine.extractInfo(INVALID) } }
            log("5: extractInfo threw ${e::class.simpleName}: ${e.message}")
            g.home.onInputChange(INVALID)
            g.home.onSubmit()
            val failed = withTimeout(2.minutes) { g.home.state.first { it.fetch is FetchState.Failed } }
            log("5: HomeViewModel fetch=${failed.fetch}")
            g.home.onRetryFetch()
            assertIs<FetchState.Fetching>(g.home.state.value.fetch)
            withTimeout(2.minutes) { g.home.state.first { it.fetch is FetchState.Failed } }
            log("5: Try again re-fetched and failed again")
        }
    }

    private suspend fun fetchReady(home: HomeViewModel, url: String): FetchState.Ready {
        home.onInputChange(url)
        home.onSubmit()
        val state = withTimeout(2.minutes) { home.state.first { it.fetch !is FetchState.Fetching } }
        return assertIs<FetchState.Ready>(state.fetch, "fetch did not become Ready: ${state.fetch}")
    }

    private suspend fun awaitHistory(g: Graph, timeout: kotlin.time.Duration): HistoryEntry =
        withTimeout(timeout) {
            while (g.history.entries.value.isEmpty()) {
                g.queue.tasks.value.firstOrNull { it.state == DownloadState.FAILED }?.let {
                    error("download failed: ${it.failure}")
                }
                delay(500)
            }
            g.history.entries.value.single()
        }

    private fun partFiles(dir: String) = File(dir).list().orEmpty().filter { it.endsWith(".part") }

    private fun ffprobeStreams(file: File): List<String> {
        val p = ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "stream=codec_type", "-of", "csv=p=0", file.path)
            .redirectErrorStream(true)
            .start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        return out.lines().map { it.trim().trimEnd(',') }.filter { it.isNotEmpty() }
    }

    private fun log(line: String) = println("[E2E] $line")

    private companion object {
        /** `-Pe2eDir=<dir>` overrides the default `<java.io.tmpdir>/grabthatvideo-e2e`. */
        val ROOT: String =
            System.getProperty("e2eDir") ?: (System.getProperty("java.io.tmpdir") + "/grabthatvideo-e2e")
        const val ZOO = "https://www.youtube.com/watch?v=jNQXAC9IVRw"
        const val ZOO_ID = "jNQXAC9IVRw"
        const val BUNNY = "https://www.youtube.com/watch?v=aqz-KE-bpKQ"
        const val INVALID = "https://vimeo.com/private/88213"
    }
}
