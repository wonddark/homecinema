package com.homecinema.core.downloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InfoJsonCacheTest {
    private val ttl = 1_000L

    @Test
    fun `returns the json stored for the url while fresh`() {
        val cache = InfoJsonCache(ttl)
        cache.put("https://a", "{\"id\":\"a\"}", nowMillis = 0)
        assertEquals("{\"id\":\"a\"}", cache.get("https://a", nowMillis = ttl - 1))
    }

    @Test
    fun `returns null for an unknown url`() {
        assertNull(InfoJsonCache(ttl).get("https://a", nowMillis = 0))
    }

    @Test
    fun `expires entries once the ttl has passed`() {
        val cache = InfoJsonCache(ttl)
        cache.put("https://a", "{}", nowMillis = 0)
        assertNull(cache.get("https://a", nowMillis = ttl))
    }

    @Test
    fun `a later put replaces the earlier json`() {
        val cache = InfoJsonCache(ttl)
        cache.put("https://a", "{\"v\":1}", nowMillis = 0)
        cache.put("https://a", "{\"v\":2}", nowMillis = 10)
        assertEquals("{\"v\":2}", cache.get("https://a", nowMillis = 20))
    }

    @Test
    fun `keeps only the newest entries`() {
        val cache = InfoJsonCache(ttl, maxEntries = 2)
        cache.put("https://a", "a", nowMillis = 0)
        cache.put("https://b", "b", nowMillis = 1)
        cache.put("https://c", "c", nowMillis = 2)
        assertNull(cache.get("https://a", nowMillis = 3))
        assertEquals("c", cache.get("https://c", nowMillis = 3))
    }
}
