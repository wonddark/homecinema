package com.homecinema.core.downloader

import com.homecinema.domain.VideoFormat
import com.homecinema.domain.VideoInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

private val parserJson = Json { ignoreUnknownKeys = true }

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

private fun JsonObject.num(key: String): JsonPrimitive? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }

private fun JsonObject.long(key: String): Long? =
    num(key)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }

/** Parses the output of `yt-dlp --dump-json`. Tolerant of missing fields. */
fun parseVideoInfo(json: String, fallbackId: String): VideoInfo {
    val root = parserJson.parseToJsonElement(json).jsonObject
    val formats =
        (root["formats"] as? JsonArray)?.map { parseFormat(it) }.orEmpty()
    val extractorKey = root.str("extractor_key")
    return VideoInfo(
        id = root.str("id") ?: fallbackId,
        title = root.str("title") ?: "Unknown",
        duration = root.long("duration") ?: 0L,
        formats = formats,
        thumbnailUrl = root.str("thumbnail"),
        extractor = if (extractorKey == "Youtube") "YouTube" else extractorKey,
        uploader = root.str("uploader"),
    )
}

private fun parseFormat(element: JsonElement): VideoFormat {
    val obj = element.jsonObject
    val height = obj.num("height")?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }
    return VideoFormat(
        formatId = obj.str("format_id") ?: "unknown",
        resolution = obj.str("resolution") ?: height?.let { "${it}p" } ?: "audio",
        size = obj.long("filesize"),
        acodec = obj.str("acodec"),
        vcodec = obj.str("vcodec"),
        filesize_approx = obj.long("filesize_approx"),
        height = height,
        ext = obj.str("ext"),
        abr = obj.num("abr")?.doubleOrNull,
    )
}
