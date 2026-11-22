package com.homecinema.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.homecinema.core.appStorageDir
import com.homecinema.core.downloader.createDownloaderEngine
import com.homecinema.core.platformDownloadDir
import com.homecinema.ui.App
import com.homecinema.ui.AppGraph
import com.homecinema.ui.DesktopBackDispatcher
import com.homecinema.ui.LocalDesktopBackDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.awt.Dimension
import kotlin.system.exitProcess

fun main() {
    val graph = AppGraph(
        engine = createDownloaderEngine(),
        storageDir = appStorageDir(),
        downloadDir = platformDownloadDir(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )
    application {
        val backDispatcher = remember { DesktopBackDispatcher() }
        Window(
            onCloseRequest = ::exitApplication,
            title = "GrabThatVideo",
            state = rememberWindowState(size = DpSize(420.dp, 860.dp)),
            onPreviewKeyEvent = { event ->
                event.key == Key.Escape && event.type == KeyEventType.KeyDown && backDispatcher.onBack()
            },
        ) {
            LaunchedEffect(Unit) { window.minimumSize = Dimension(360, 600) }
            CompositionLocalProvider(LocalDesktopBackDispatcher provides backDispatcher) {
                App(graph)
            }
        }
    }
    graph.close()
    exitProcess(0)
}
