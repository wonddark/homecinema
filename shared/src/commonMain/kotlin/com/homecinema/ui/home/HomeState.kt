package com.homecinema.ui.home

import com.homecinema.core.format.FormatOptions
import com.homecinema.core.format.QualityOption
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.MediaKind
import com.homecinema.domain.VideoInfo

sealed interface FetchState {
    data object Idle : FetchState

    data class Fetching(val url: String) : FetchState

    data class Failed(val url: String) : FetchState

    data class Ready(
        val url: String,
        val info: VideoInfo,
        val options: FormatOptions,
        val kind: MediaKind,
        val selected: QualityOption?,
    ) : FetchState {
        /** False for audio-only sources: the video toggle and quality chips are hidden. */
        val videoAvailable: Boolean =
            options.qualities.isNotEmpty() && !(options.audio != null && info.formats.none { it.hasVideo })
    }
}

data class HomeState(
    val input: String = "",
    val fetch: FetchState = FetchState.Idle,
    val tasks: List<DownloadTask> = emptyList(),
)
