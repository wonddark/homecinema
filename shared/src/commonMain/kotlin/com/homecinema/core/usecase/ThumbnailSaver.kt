package com.homecinema.core.usecase

import com.homecinema.core.storage.FileIO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

fun interface ThumbnailSaver {
    /** Saves the image at [url] locally for [taskId]; returns the local path, or null on any failure. */
    suspend fun save(url: String, taskId: String): String?
}

class KtorThumbnailSaver(
    private val client: HttpClient,
    private val io: FileIO,
    private val dir: String,
) : ThumbnailSaver {
    override suspend fun save(url: String, taskId: String): String? =
        try {
            val response = client.get(url)
            if (response.status.isSuccess()) {
                val path = "$dir/$taskId.jpg"
                io.writeBytes(path, response.body<ByteArray>())
                path
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
}
