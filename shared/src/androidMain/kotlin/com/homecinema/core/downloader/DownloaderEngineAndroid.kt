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

class DownloaderEngineAndroid : DownloaderEngine {
    override val ffmpegAvailable: Boolean = true

    private val tasks = ConcurrentHashMap<String, DownloadTask>()
    private val outputDirs = ConcurrentHashMap<String, String>()

    override suspend fun extractInfo(url: String): VideoInfo =
        withContext(Dispatchers.IO) {
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
            try {
                val response =
                    YoutubeDL.getInstance().execute(request, task.id) { progress, _, _ ->
                        onProgress(progress / 100f)
                    }
                parseFinalPath(response.out.lineSequence())
            } catch (e: Exception) {
                throw RuntimeException(lastErrorLine(e.message.orEmpty()), e)
            }
        }

    override suspend fun pause(taskId: String): Unit =
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().destroyProcessById(taskId)
            Unit
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
