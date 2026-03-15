package com.homecinema.ui.state

import com.homecinema.domain.DownloadTask

data class DownloadUiState(
    val tasks: List<DownloadTask> = emptyList(),
    val isDownloading: Boolean = false,
    val selectedFormat: String? = null,
    val urlInput: String = "",
    val isLoadingInfo: Boolean = false,
    val errorMessage: String? = null
)
