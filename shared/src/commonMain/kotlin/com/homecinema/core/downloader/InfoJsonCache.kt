package com.homecinema.core.downloader

/**
 * The `--dump-json` output of the last few fetches, keyed by URL, so a download started soon after can hand it
 * to yt-dlp with `--load-info-json` instead of extracting the page a second time.
 *
 * Media URLs inside the JSON expire (YouTube's after about six hours), hence the [ttlMillis]. Not thread-safe.
 */
class InfoJsonCache(
    private val ttlMillis: Long,
    private val maxEntries: Int = 16,
) {
    private class Entry(val json: String, val storedAt: Long)

    private val entries = LinkedHashMap<String, Entry>()

    fun put(
        url: String,
        json: String,
        nowMillis: Long,
    ) {
        entries.remove(url)
        entries[url] = Entry(json, nowMillis)
        while (entries.size > maxEntries) entries.remove(entries.keys.first())
    }

    fun get(
        url: String,
        nowMillis: Long,
    ): String? {
        val entry = entries[url] ?: return null
        if (nowMillis - entry.storedAt >= ttlMillis) {
            entries.remove(url)
            return null
        }
        return entry.json
    }
}
