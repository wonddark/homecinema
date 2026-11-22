package com.homecinema.ui

import com.homecinema.MockDownloaderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
}
