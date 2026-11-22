package com.homecinema.ui

import com.homecinema.MockDownloaderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppGraphTest {
    @Test
    fun closeShutsDownTheEngineThenCancelsTheScope() {
        val dir = Files.createTempDirectory("appgraph").toFile()
        try {
            val engine = MockDownloaderEngine()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            var scopeActiveAtShutdown: Boolean? = null
            engine.onShutdown = { scopeActiveAtShutdown = scope.isActive }
            val graph = AppGraph(engine, dir.path, dir.path, scope)

            graph.close()

            assertEquals(1, engine.shutdownCalls)
            assertEquals(true, scopeActiveAtShutdown)
            assertFalse(scope.isActive)
        } finally {
            dir.deleteRecursively()
        }
    }
    @Test
    fun closeDuringDownloadKeepsTheTaskResumable() {
        val dir = Files.createTempDirectory("appgraph").toFile()
        try {
            val engine = MockDownloaderEngine()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val graph = AppGraph(engine, dir.path, dir.path, scope)
            graph.queue.enqueue(DownloadTask(id = "t1", url = "https://x/v"))
            runBlocking { withTimeout(5_000) { graph.queue.tasks.first { it.singleOrNull()?.state == DownloadState.DOWNLOADING } } }
            // Like the desktop engine: killing yt-dlp makes download() throw while shutdown waits for the exit.
            engine.onShutdown = {
                engine.fail("t1", "ERROR: killed")
                Thread.sleep(300)
            }

            graph.close()

            val queueJson = File(dir, "queue.json").readText()
            assertFalse(queueJson.contains("FAILED"), queueJson)
            assertTrue(queueJson.contains("DOWNLOADING"), queueJson)
        } finally {
            dir.deleteRecursively()
        }
    }
}
