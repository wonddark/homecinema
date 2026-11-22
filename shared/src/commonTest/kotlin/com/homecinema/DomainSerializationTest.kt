package com.homecinema

import com.homecinema.domain.DownloadTask
import com.homecinema.domain.MediaKind
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DomainSerializationTest {
    @Test
    fun `DownloadTask decodes legacy json`() {
        val task = Json.decodeFromString<DownloadTask>("""{"id":"a","url":"u","state":"QUEUED"}""")
        assertEquals(MediaKind.VIDEO, task.kind)
        assertEquals(0L, task.addedAt)
    }
}
