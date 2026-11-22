package com.homecinema.core.downloader

import com.homecinema.domain.DownloadTask
import com.homecinema.domain.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

actual fun createDownloaderEngine(): DownloaderEngine = DownloaderEngineDesktop()

/**
 * Desktop implementation using yt-dlp CLI.
 * Requires `yt-dlp` to be installed and available on PATH.
 */
class DownloaderEngineDesktop : DownloaderEngine {
    private val activeProcesses = ConcurrentHashMap<String, Process>()
    private val tasks = ConcurrentHashMap<String, DownloadTask>()
    private val outputDirs = ConcurrentHashMap<String, String>()

    init {
        // Safety net: the JVM does not kill child processes, so an exit path that skips shutdown() would orphan yt-dlp.
        Runtime.getRuntime().addShutdownHook(Thread(::shutdown, "yt-dlp-shutdown"))
    }

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

    /**
     * stdout carries only the JSON; stderr (warnings such as "version is older than 90 days", and errors) is
     * read separately — merging the two made every fetch fail to parse.
     */
    override suspend fun extractInfo(url: String): VideoInfo =
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder("yt-dlp", "--dump-json", "--no-playlist", url).start()
            try {
                val stderr = async { process.errorStream.bufferedReader().readText() }
                val output = process.inputStream.bufferedReader().readText()
                val exit = process.waitFor()
                val errors = stderr.await()
                if (exit != 0 || output.isBlank()) throw RuntimeException(lastErrorLine(errors))
                parseVideoInfo(output, url.hashCode().toString())
            } finally {
                process.destroy()
            }
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
                listOf("yt-dlp") + buildDownloadArgs(task, outputDir, ffmpegAvailable) + listOf(task.url)
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            activeProcesses[task.id] = process

            val lines = mutableListOf<String>()
            val exit =
                try {
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
                    process.waitFor()
                } finally {
                    // Remove only our own entry: a resumed download may already have registered a new process.
                    activeProcesses.remove(task.id, process)
                    process.destroy()
                }
            if (exit != 0) throw RuntimeException(lastErrorLine(lines.joinToString("\n")))
            onProgress(1f)
            parseFinalPath(lines.asSequence())
        }

    override suspend fun pause(taskId: String): Unit =
        withContext(Dispatchers.IO) {
            activeProcesses.remove(taskId)?.destroy()
            Unit
        }

    /** Asks every yt-dlp to stop, gives each up to 2 s, then force-kills survivors. Idempotent. */
    override fun shutdown() {
        val processes = activeProcesses.values.toList()
        processes.forEach { it.destroy() }
        processes.forEach { process ->
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
        }
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
