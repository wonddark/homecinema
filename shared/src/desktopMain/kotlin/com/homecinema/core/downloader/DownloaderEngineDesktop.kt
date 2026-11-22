package com.homecinema.core.downloader

import com.homecinema.domain.DownloadTask
import com.homecinema.domain.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

actual fun createDownloaderEngine(): DownloaderEngine = DownloaderEngineDesktop()

/**
 * Desktop implementation using yt-dlp CLI.
 * Requires `yt-dlp` to be installed and available on PATH.
 */
class DownloaderEngineDesktop : DownloaderEngine {
    private val activeProcesses = ConcurrentHashMap<String, Process>()
    private val tasks = ConcurrentHashMap<String, DownloadTask>()
    private val outputDirs = ConcurrentHashMap<String, String>()

    override val ffmpegAvailable: Boolean by lazy {
        try {
            ProcessBuilder("ffmpeg", "-version")
                .redirectErrorStream(true)
                .start()
                .also { it.inputStream.readBytes() }
                .waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun extractInfo(url: String): VideoInfo =
        withContext(Dispatchers.IO) {
            val process =
                ProcessBuilder("yt-dlp", "--dump-json", "--no-playlist", url)
                    .redirectErrorStream(true)
                    .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            parseVideoInfo(output, url.hashCode().toString())
        }

    override suspend fun download(
        task: DownloadTask,
        outputDir: String,
        onProgress: (Float) -> Unit,
    ): String =
        withContext(Dispatchers.IO) {
            tasks[task.id] = task
            outputDirs[task.id] = outputDir
            val command =
                listOf("yt-dlp") + buildDownloadArgs(task, outputDir, ffmpegAvailable) + listOf("--newline", task.url)
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            activeProcesses[task.id] = process

            val lines = mutableListOf<String>()
            process.inputStream.bufferedReader().useLines { seq ->
                for (line in seq) {
                    lines += line
                    if (line.startsWith("[download]")) {
                        Regex("""(\d+\.?\d*)%""").find(line)?.groupValues?.get(1)?.toFloatOrNull()?.let {
                            onProgress(it / 100f)
                        }
                    }
                }
            }
            val exit = process.waitFor()
            activeProcesses.remove(task.id)
            if (exit != 0) throw RuntimeException(lastErrorLine(lines.joinToString("\n")))
            onProgress(1f)
            parseFinalPath(lines.asSequence())
        }

    override suspend fun pause(taskId: String): Unit =
        withContext(Dispatchers.IO) {
            activeProcesses.remove(taskId)?.destroy()
            Unit
        }

    override suspend fun cancel(taskId: String): Unit =
        withContext(Dispatchers.IO) {
            activeProcesses.remove(taskId)?.destroy()
            val task = tasks.remove(taskId)
            val dir = outputDirs.remove(taskId)
            if (task != null && dir != null) {
                File(dir).listFiles()?.filter { isPartialFileOf(it.name, task) }?.forEach { it.delete() }
            }
            Unit
        }
}
