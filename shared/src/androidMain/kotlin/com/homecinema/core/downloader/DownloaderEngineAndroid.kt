package com.homecinema.core.downloader

import com.homecinema.domain.VideoInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun createDownloaderEngine(): DownloaderEngine = DownloaderEngineAndroid()

class DownloaderEngineAndroid : DownloaderEngine {
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
