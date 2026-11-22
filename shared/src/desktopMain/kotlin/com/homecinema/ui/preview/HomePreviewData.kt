package com.homecinema.ui.preview

import com.homecinema.core.downloader.FailureReason
import com.homecinema.core.format.AudioOption
import com.homecinema.core.format.FormatOptions
import com.homecinema.core.format.QualityOption
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.MediaKind
import com.homecinema.domain.VideoFormat
import com.homecinema.domain.VideoInfo
import com.homecinema.ui.home.FetchState
import com.homecinema.ui.home.HomeActions
import com.homecinema.ui.home.HomeState

/** Sample Home states matching the design frames (shared by previews and the render check). */
object HomePreviewData {
    private const val MB = 1024L * 1024L

    val actions = HomeActions(
        onInputChange = {},
        onSubmit = {},
        onPaste = {},
        onClear = {},
        onRetryFetch = {},
        onSelectKind = {},
        onSelectQuality = {},
        onDownload = {},
        onPause = {},
        onResume = {},
        onRetry = {},
        onRemove = {},
    )

    val empty = HomeState()

    val fetching = HomeState(input = "youtu.be/x8Kq2Lm", fetch = FetchState.Fetching("youtu.be/x8Kq2Lm"))

    val failed = HomeState(input = "vimeo.com/private/88213", fetch = FetchState.Failed("vimeo.com/private/88213"))

    fun ready(thumbnail: String? = null): HomeState {
        val qualities = listOf(
            QualityOption("1080p", 1080, "137", 248 * MB),
            QualityOption("720p", 720, "136", 120 * MB),
            QualityOption("480p", 480, "135", 60 * MB),
            QualityOption("4K", 2160, "313", 1126 * MB),
        )
        val info = VideoInfo(
            id = "x8Kq2Lm",
            title = "Sunset drive — lofi mix",
            duration = 12 * 60 + 48,
            formats = listOf(VideoFormat("137", "1920x1080", vcodec = "avc1", acodec = "none", height = 1080)),
            thumbnailUrl = thumbnail,
            extractor = "YouTube",
            uploader = "Lofi Records",
        )
        val options = FormatOptions(
            qualities = qualities,
            audio = AudioOption("140", "mp3", "MP3 · Audio", 128, 12 * MB),
            defaultQuality = qualities.first(),
            needsFfmpegHint = false,
        )
        return HomeState(
            input = "youtu.be/x8Kq2Lm",
            fetch = FetchState.Ready("youtu.be/x8Kq2Lm", info, options, MediaKind.VIDEO, qualities.first()),
        )
    }

    fun downloading(thumbnail: String? = null): HomeState =
        HomeState(
            tasks = listOf(
                task("1", "Sunset drive — lofi mix", "YouTube", "1080p", 248 * MB, DownloadState.DOWNLOADING, 0f, thumbnail),
                task("2", "Street food tour in Tokyo", "Instagram", "720p", 86 * MB, DownloadState.DOWNLOADING, 0.28f, thumbnail),
                task("3", "Mountain trail run POV", "YouTube", "4K", 1126 * MB, DownloadState.PAUSED, 0.41f, thumbnail),
                task("4", "How to make fresh pasta", "YouTube", "720p", 40 * MB, DownloadState.FAILED, 0.12f, thumbnail)
                    .copy(failure = FailureReason.CONNECTION_LOST),
                task("5", "Rainy night jazz", "YouTube", "MP3", 9 * MB, DownloadState.QUEUED, 0f, null)
                    .copy(kind = MediaKind.AUDIO),
            ),
        )

    private fun task(
        id: String,
        title: String,
        extractor: String,
        quality: String,
        size: Long,
        state: DownloadState,
        progress: Float,
        thumbnail: String?,
    ) = DownloadTask(
        id = id,
        url = "https://example.com/$id",
        videoInfo = VideoInfo(id, title, 300, emptyList(), thumbnailUrl = thumbnail, extractor = extractor),
        state = state,
        progress = progress,
        qualityLabel = quality,
        sizeBytes = size,
    )
}
