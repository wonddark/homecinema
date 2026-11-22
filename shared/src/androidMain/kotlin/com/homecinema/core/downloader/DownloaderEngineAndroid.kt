package com.homecinema.core.downloader

import com.homecinema.domain.VideoFormat
import com.homecinema.domain.VideoInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

actual fun createDownloaderEngine(): DownloaderEngine = DownloaderEngineAndroid()

class DownloaderEngineAndroid : DownloaderEngine {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun extractInfo(url: String): VideoInfo =
        withContext(Dispatchers.IO) {
            val request =
                YoutubeDLRequest(url).apply {
                    addOption("--dump-json")
                    addOption("--no-playlist")
                }
            val response = YoutubeDL.getInstance().execute(request)
            val jsonObj = json.parseToJsonElement(response.out).jsonObject

            val id = jsonObj["id"]?.jsonPrimitive?.content ?: url.hashCode().toString()
            val title = jsonObj["title"]?.jsonPrimitive?.content ?: "Unknown"
            val duration = jsonObj["duration"]?.jsonPrimitive?.longOrNull ?: 0L
            val formats =
                jsonObj["formats"]?.jsonArray?.map { fmt ->
                    val fmtObj = fmt.jsonObject
                    VideoFormat(
                        formatId = fmtObj["format_id"]?.jsonPrimitive?.content ?: "unknown",
                        resolution =
                            fmtObj["resolution"]?.jsonPrimitive?.content
                                ?: fmtObj["height"]?.jsonPrimitive?.content?.let { "${it}p" }
                                ?: "audio",
                        size = fmtObj["filesize"]?.jsonPrimitive?.longOrNull,
                    )
                } ?: listOf(VideoFormat("best", "best", null))

            VideoInfo(id = id, title = title, duration = duration, formats = formats)
        }

    override suspend fun download(
        taskId: String,
        url: String,
        formatId: String,
        outputPath: String,
        onProgress: (Float) -> Unit,
    ): String =
        withContext(Dispatchers.IO) {
            val request =
                YoutubeDLRequest(url).apply {
                    addOption("-f", formatId)
                    addOption("-o", "$outputPath/%(title)s.%(ext)s")
                    addOption("--no-mtime")
                }
            val response = YoutubeDL.getInstance().execute(request, taskId) { progress, _, _ ->
                onProgress(progress / 100f)
            }
            var capturedPath = ""
            response.out.lineSequence().forEach { line ->
                if (line.startsWith("[download] Destination: ")) {
                    capturedPath = line.removePrefix("[download] Destination: ").trim()
                }
                if (line.contains("has already been downloaded")) {
                    val alreadyPath = line
                        .removePrefix("[download] ")
                        .removeSuffix(" has already been downloaded")
                        .trim()
                    if (capturedPath.isEmpty()) capturedPath = alreadyPath
                }
                if (line.startsWith("[Merger] Merging formats into ")) {
                    capturedPath = line
                        .removePrefix("[Merger] Merging formats into \"")
                        .trimEnd('"')
                        .trim()
                }
            }
            capturedPath
        }

    override suspend fun cancel(taskId: String): Unit =
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().destroyProcessById(taskId)
            Unit
        }
}
