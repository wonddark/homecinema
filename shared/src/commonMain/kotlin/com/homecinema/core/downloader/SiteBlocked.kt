package com.homecinema.core.downloader

/** The site refused yt-dlp (HTTP 403) on a page it only serves to browser-like clients; [message] is yt-dlp's error. */
class SiteBlockedException(message: String) : RuntimeException(message)

private const val NO_IMPERSONATION = "no impersonate target is available"

/**
 * True when yt-dlp's stderr shows a 403 from an extractor that wanted browser impersonation it could not do.
 * Retrying will not help: the site rejects this yt-dlp's connection, whatever the network.
 */
fun isSiteBlocked(stderr: String): Boolean = stderr.contains("HTTP Error 403") && stderr.contains(NO_IMPERSONATION)

/** The exception to raise for a failed yt-dlp run: [SiteBlockedException] for a block, else a plain one. */
fun ytDlpFailure(stderr: String): RuntimeException {
    val line = lastErrorLine(stderr)
    return if (isSiteBlocked(stderr)) SiteBlockedException(line) else RuntimeException(line)
}
