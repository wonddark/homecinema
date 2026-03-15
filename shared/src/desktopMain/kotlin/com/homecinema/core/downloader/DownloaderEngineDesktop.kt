package com.homecinema.core.downloader

import com.homecinema.domain.VideoFormat
import com.homecinema.domain.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

actual fun createDownloaderEngine(): DownloaderEngine = DownloaderEngineDesktop()

/**
 * Desktop implementation using yt-dlp CLI.
 * Requires `yt-dlp` to be installed and available on PATH.
 */
class DownloaderEngineDesktop : DownloaderEngine {

    private val activeProcesses = mutableMapOf<String, Process>()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun extractInfo(url: String): VideoInfo = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("yt-dlp", "--dump-json", "--no-playlist", url)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        val jsonObj = json.parseToJsonElement(output).jsonObject
        val id = jsonObj["id"]?.jsonPrimitive?.content ?: url.hashCode().toString()
        val title = jsonObj["title"]?.jsonPrimitive?.content ?: "Unknown"
        val duration = jsonObj["duration"]?.jsonPrimitive?.longOrNull ?: 0L
        val formats = jsonObj["formats"]?.jsonArray?.map { fmt ->
            val fmtObj = fmt.jsonObject
            VideoFormat(
                formatId = fmtObj["format_id"]?.jsonPrimitive?.content ?: "unknown",
                resolution = fmtObj["resolution"]?.jsonPrimitive?.content
                    ?: fmtObj["height"]?.jsonPrimitive?.content?.let { "${it}p" }
                    ?: "audio",
                size = fmtObj["filesize"]?.jsonPrimitive?.longOrNull
            )
        } ?: listOf(VideoFormat("best", "best", null))

        VideoInfo(id = id, title = title, duration = duration, formats = formats)
    }

    override suspend fun download(
        taskId: String,
        url: String,
        formatId: String,
        outputPath: String,
        onProgress: (Float) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(
            "yt-dlp",
            "-f", formatId,
            "-o", "$outputPath/%(title)s.%(ext)s",
            "--newline",
            url
        )
            .redirectErrorStream(true)
            .start()

        var capturedPath = ""

        // Track the process for cancellation support
        activeProcesses[taskId] = process

        process.inputStream.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.startsWith("[download]")) {
                    val match = Regex("""(\d+\.?\d*)%""").find(line)
                    match?.groupValues?.get(1)?.toFloatOrNull()?.let { pct ->
                        onProgress(pct / 100f)
                    }
                    if (line.startsWith("[download] Destination: ")) {
                        capturedPath = line.removePrefix("[download] Destination: ").trim()
                    }
                    // Handle already-downloaded: "[download] /path has already been downloaded"
                    if (line.contains("has already been downloaded")) {
                        val alreadyPath = line
                            .removePrefix("[download] ")
                            .removeSuffix(" has already been downloaded")
                            .trim()
                        if (capturedPath.isEmpty()) capturedPath = alreadyPath
                    }
                }
                if (line.startsWith("[Merger] Merging formats into ")) {
                    // Merger output supersedes the destination (final merged file)
                    capturedPath = line
                        .removePrefix("[Merger] Merging formats into \"")
                        .trimEnd('"')
                        .trim()
                }
            }
        }
        process.waitFor()
        activeProcesses.remove(taskId)
        onProgress(1f)
        capturedPath
    }

    override suspend fun cancel(taskId: String) = withContext(Dispatchers.IO) {
        activeProcesses[taskId]?.destroy()
        activeProcesses.remove(taskId)
        Unit
    }
}
