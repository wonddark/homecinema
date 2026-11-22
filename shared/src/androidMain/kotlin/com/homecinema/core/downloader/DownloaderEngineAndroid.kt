package com.homecinema.core.downloader

import com.homecinema.domain.DownloadTask
import com.homecinema.domain.VideoInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

actual fun createDownloaderEngine(): DownloaderEngine = DownloaderEngineAndroid()

private val FLAG_ONLY = setOf("--continue", "--newline", "--no-playlist", "--no-mtime")

/**
 * YoutubeDL.execute() appends its own options (--no-cache-dir, --ffmpeg-location, …) after ours, so a "--"
 * end-of-options marker cannot be used here; refuse text yt-dlp would parse as an option instead.
 */
private fun requireNotOption(url: String) {
    require(!url.trimStart().startsWith("-")) { "ERROR: not a URL" }
}

class DownloaderEngineAndroid : DownloaderEngine {
    override val ffmpegAvailable: Boolean = true

    private val tasks = ConcurrentHashMap<String, DownloadTask>()
    private val outputDirs = ConcurrentHashMap<String, String>()
    /** Run token per running download; a resumed download replaces it, so a stale finally cannot drop it. */
    private val activeRuns = ConcurrentHashMap<String, Any>()

    override suspend fun extractInfo(url: String): VideoInfo =
        withContext(Dispatchers.IO) {
            requireNotOption(url)
            val request =
                YoutubeDLRequest(url).apply {
                    addOption("--dump-json")
                    addOption("--no-playlist")
                }
            val response = YoutubeDL.getInstance().execute(request)
            parseVideoInfo(response.out, url.hashCode().toString())
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
                YoutubeDLRequest(task.url).apply {
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
                parseFinalPath(response.out.lineSequence())
            } catch (e: Exception) {
                throw RuntimeException(lastErrorLine(e.message.orEmpty()), e)
            } finally {
                activeRuns.remove(task.id, run)
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
