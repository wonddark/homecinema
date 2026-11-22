package com.homecinema.core.history

import com.homecinema.core.platformFileIO
import com.homecinema.core.storage.JsonFileStore
import com.homecinema.domain.HistoryEntry
import com.homecinema.domain.MediaKind
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryRepositoryConcurrencyTest {
    private fun entry(id: String) =
        HistoryEntry(
            id = id, title = id, url = "u", filePath = "/dl/$id.mp4", kind = MediaKind.VIDEO, ext = "mp4",
            qualityLabel = "1080p", sizeBytes = 1, durationSeconds = 1, extractor = null, thumbnailPath = null,
            completedAt = 0,
        )

    @Test
    fun `concurrent adds and removes leave memory and history_json consistent`() {
        val dir = Files.createTempDirectory("history").toFile()
        val pool = Executors.newFixedThreadPool(8)
        try {
            val io = platformFileIO()
            val store = JsonFileStore(io, "${dir.path}/history.json", ListSerializer(HistoryEntry.serializer()), emptyList())
            val repo = HistoryRepository(store, io)
            val start = CountDownLatch(1)
            val errors = java.util.Collections.synchronizedList(mutableListOf<Throwable>())
            val workers = 8
            val perWorker = 40
            repeat(workers) { w ->
                pool.execute {
                    start.await()
                    try {
                        repeat(perWorker) { i ->
                            repo.add(entry("w$w-$i"))
                            if (i % 2 == 1) repo.remove("w$w-${i - 1}")
                        }
                    } catch (t: Throwable) {
                        errors += t
                    }
                }
            }
            start.countDown()
            pool.shutdown()
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))

            assertEquals(emptyList(), errors.map { it.toString() })
            val expected = (0 until workers).flatMap { w -> (1 until perWorker step 2).map { "w$w-$it" } }.toSet()
            assertEquals(expected, repo.entries.value.map { it.id }.toSet())
            assertEquals(expected, store.load().map { it.id }.toSet())
            assertEquals(listOf("history.json"), dir.list()!!.toList(), "temp files left behind")
        } finally {
            pool.shutdownNow()
            dir.deleteRecursively()
        }
    }
}
