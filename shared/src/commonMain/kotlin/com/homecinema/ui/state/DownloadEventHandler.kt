package com.homecinema.ui.state

interface DownloadEventHandler {
    fun onUrlChanged(url: String)
    fun onStartDownload(url: String)
    fun onCancelDownload(taskId: String)
    fun onSelectFormat(taskId: String, formatId: String)
    fun onRetryDownload(taskId: String)
    fun onRemoveDownload(taskId: String)
    fun onLocateDownload(taskId: String)
}
