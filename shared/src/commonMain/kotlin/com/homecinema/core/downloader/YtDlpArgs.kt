package com.homecinema.core.downloader

import com.homecinema.domain.DownloadTask
import com.homecinema.domain.MediaKind

/** Options only; the URL is added by the caller. */
fun buildDownloadArgs(
    task: DownloadTask,
    outputDir: String,
    ffmpegAvailable: Boolean,
): List<String> {
    val format = task.format ?: "best"
    val args = mutableListOf<String>()
    if (task.kind == MediaKind.VIDEO && ffmpegAvailable) {
        args += listOf("-f", "$format+bestaudio/$format/best", "--merge-output-format", "mp4")
    } else {
        args += listOf("-f", format)
    }
    args += listOf("-o", outputTemplate(task, outputDir), "--continue", "--newline", "--no-playlist", "--no-mtime")
    return args
}

fun outputTemplate(
    task: DownloadTask,
    outputDir: String,
): String = "$outputDir/%(title)s [%(id)s] ${task.qualityLabel}.%(ext)s"

/** True for a partial-download file (.part / .ytdl) that belongs to [task]. */
fun isPartialFileOf(
    fileName: String,
    task: DownloadTask,
): Boolean {
    val id = task.videoInfo?.id ?: return false
    val isPartial = fileName.endsWith(".part") || fileName.endsWith(".ytdl")
    return isPartial && fileName.contains("[$id] ${task.qualityLabel}")
}

fun parseFinalPath(outputLines: Sequence<String>): String {
    var path = ""
    for (line in outputLines) {
        if (line.startsWith("[download] Destination: ")) {
            path = line.removePrefix("[download] Destination: ").trim()
        }
        if (line.contains("has already been downloaded")) {
            val already =
                line
                    .removePrefix("[download] ")
                    .removeSuffix(" has already been downloaded")
                    .trim()
            if (path.isEmpty()) path = already
        }
        if (line.startsWith("[Merger] Merging formats into ")) {
            path =
                line
                    .removePrefix("[Merger] Merging formats into \"")
                    .trimEnd('"')
                    .trim()
        }
    }
    return path
}

/** The last yt-dlp "ERROR:" line, else the last non-blank line. */
fun lastErrorLine(output: String): String {
    val lines = output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    return lines.lastOrNull { it.startsWith("ERROR") } ?: lines.lastOrNull().orEmpty()
}
