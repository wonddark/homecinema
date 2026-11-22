package com.homecinema.core.usecase

import com.homecinema.core.platformFileIO
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class KtorThumbnailSaverTest {
    @Test
    fun `a server that never answers gives up with null`() {
        val dir = Files.createTempDirectory("thumbs").toFile()
        val accepted = mutableListOf<Socket>()
        ServerSocket(0).use { server ->
            // Accepts the connection and never writes a byte back.
            thread(isDaemon = true) { runCatching { while (true) accepted += server.accept() } }
            HttpClient().use { client ->
                val saver = KtorThumbnailSaver(client, platformFileIO(), dir.path)
                // Virtual time: the 10 s timeout elapses instantly; without one the request hangs until runTest gives up.
                runTest(timeout = 20.seconds) {
                    assertNull(saver.save("http://127.0.0.1:${server.localPort}/t.jpg", "t1"))
                }
            }
        }
        accepted.forEach { runCatching { it.close() } }
        dir.deleteRecursively()
    }
}
