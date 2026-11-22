package com.homecinema.core.downloader

import android.content.pm.ApplicationInfo
import android.util.Log
import com.homecinema.core.appContext
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.VideoInfo
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

actual fun createDownloaderEngine(): DownloaderEngine = DownloaderEngineAndroid()

private val FLAG_ONLY = setOf("--continue", "--newline", "--no-playlist", "--no-mtime")

private const val TAG = "DownloaderEngine"

/** Media URLs in a fetched info JSON stay valid for hours; reuse it for downloads started within this window. */
private const val INFO_JSON_TTL_MILLIS = 30 * 60 * 1000L

/**
 * YoutubeDL.execute() adds --no-cache-dir unless a cache dir is given, which makes yt-dlp redo YouTube's
 * player download and JS challenge solving (in QuickJS) on every run. Keep yt-dlp's cache like the CLI does.
 */
private fun YoutubeDLRequest.withCache(): YoutubeDLRequest =
    addOption("--cache-dir", File(appContext.cacheDir, "yt-dlp").absolutePath)

/**
 * YoutubeDL.execute() appends its own options (--no-cache-dir, --ffmpeg-location, …) after ours, so a "--"
 * end-of-options marker cannot be used here; refuse text yt-dlp would parse as an option instead.
 */
private fun requireNotOption(url: String) {
    require(!url.trimStart().startsWith("-")) { "ERROR: not a URL" }
}

private fun isDebuggable(): Boolean = appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

/** Logcat truncates an entry at about 4 KB; log long yt-dlp output in pieces so nothing is lost. */
private fun logLong(text: String) {
    text.chunked(3_000).forEach { Log.d(TAG, it) }
}

class DownloaderEngineAndroid : DownloaderEngine {
    override val ffmpegAvailable: Boolean = true

    private val tasks = ConcurrentHashMap<String, DownloadTask>()
    private val outputDirs = ConcurrentHashMap<String, String>()
    /** Run token per running download; a resumed download replaces it, so a stale finally cannot drop it. */
    private val activeRuns = ConcurrentHashMap<String, Any>()
    private val infoJsons = InfoJsonCache(INFO_JSON_TTL_MILLIS)

    override suspend fun extractInfo(url: String): VideoInfo =
        withContext(Dispatchers.IO) {
            requireNotOption(url)
            val request =
                YoutubeDLRequest(url).withCache().apply {
                    addOption("--dump-json")
                    addOption("--no-playlist")
                    // Verbose diagnostics go to stderr, which is logged only on failure; stdout stays pure JSON.
                    if (isDebuggable()) addOption("--verbose")
                }
            val response =
                try {
                    YoutubeDL.getInstance().execute(request)
                } catch (e: YoutubeDLException) {
                    val stderr = e.message.orEmpty()
                    Log.w(TAG, "extractInfo failed for $url: ${lastErrorLine(stderr)}")
                    logLong(stderr)
                    throw RuntimeException(lastErrorLine(e.message.orEmpty()), e)
                }
            Log.i(TAG, "extractInfo took ${response.elapsedTime} ms for $url")
            val info =
                try {
                    parseVideoInfo(response.out, url.hashCode().toString())
                } catch (e: Exception) {
                    Log.w(TAG, "extractInfo could not parse output for $url: ${response.out.take(500)}", e)
                    throw e
                }
            synchronized(infoJsons) { infoJsons.put(url, response.out, System.currentTimeMillis()) }
            info
        }

    private fun infoJsonFile(taskId: String) = File(appContext.cacheDir, "info-json/$taskId.json")

    /** The request for [task]: the fetched info JSON when still fresh (no second extraction), else the URL. */
    private fun downloadRequest(task: DownloadTask): YoutubeDLRequest {
        val json = synchronized(infoJsons) { infoJsons.get(task.url, System.currentTimeMillis()) }
            ?: return YoutubeDLRequest(task.url)
        val file = infoJsonFile(task.id)
        file.parentFile?.mkdirs()
        file.writeText(json)
        return YoutubeDLRequest(emptyList()).addOption("--load-info-json", file.absolutePath)
    }

    override suspend fun download(
        task: DownloadTask,
        outputDir: String,
        onProgress: (Float) -> Unit,
    ): String =
        withContext(Dispatchers.IO) {
            requireNotOption(task.url)
            tasks[task.id] = task
            outputDirs[task.id] = outputDir
            val request =
                downloadRequest(task).withCache().apply {
                    val args = buildDownloadArgs(task, outputDir, ffmpegAvailable)
                    var i = 0
                    while (i < args.size) {
                        val isPair = i + 1 < args.size && !args[i + 1].startsWith("--") && args[i] !in FLAG_ONLY
                        if (isPair) {
                            addOption(args[i], args[i + 1])
                            i += 2
                        } else {
                            addOption(args[i])
                            i += 1
                        }
                    }
                }
            val run = Any()
            activeRuns[task.id] = run
            try {
                val response =
                    YoutubeDL.getInstance().execute(request, task.id) { progress, _, _ ->
                        onProgress(progress / 100f)
                    }
                Log.i(TAG, "download ${task.id} took ${response.elapsedTime} ms")
                parseFinalPath(response.out.lineSequence())
            } catch (e: Exception) {
                Log.w(TAG, "download ${task.id} failed\n${e.message}")
                throw RuntimeException(lastErrorLine(e.message.orEmpty()), e)
            } finally {
                activeRuns.remove(task.id, run)
                infoJsonFile(task.id).delete()
            }
        }

    override suspend fun pause(taskId: String): Unit =
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().destroyProcessById(taskId)
            Unit
        }

    /** Best effort: kills the yt-dlp process of every download still running. */
    override fun shutdown() {
        activeRuns.keys.toList().forEach { id ->
            runCatching { YoutubeDL.getInstance().destroyProcessById(id) }
        }
    }

    override suspend fun cancel(taskId: String): Unit =
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().destroyProcessById(taskId)
            val task = tasks.remove(taskId)
            val dir = outputDirs.remove(taskId)
            if (task != null && dir != null) {
                File(dir).listFiles()?.filter { isPartialFileOf(it.name, task) }?.forEach { it.delete() }
            }
            Unit
        }
}
