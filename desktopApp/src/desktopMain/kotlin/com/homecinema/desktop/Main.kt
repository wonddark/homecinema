package com.homecinema.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.homecinema.core.downloader.createDownloaderEngine
import com.homecinema.core.usecase.DownloadQueueManager
import com.homecinema.core.usecase.DownloadViewModel
import com.homecinema.ui.DownloadScreen
import com.homecinema.ui.theme.AppTheme
import kotlin.system.exitProcess

fun main() {
    application {
    val engine = createDownloaderEngine()
    val viewModel = DownloadViewModel(engine = engine, queueManager = DownloadQueueManager(engine))
        Window(
            onCloseRequest = ::exitApplication,
            title = "HomeCinema"
        ) {
            AppTheme {
                DownloadScreen(viewModel = viewModel)
            }
        }
    }
    exitProcess(0)
}
