package com.homecinema.core.notification

import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.notif_channel_name
import homecinema.shared.generated.resources.notif_downloading
import homecinema.shared.generated.resources.notif_idle
import homecinema.shared.generated.resources.notif_paused
import homecinema.shared.generated.resources.notif_queued
import org.jetbrains.compose.resources.getString

data class NotificationCounts(val downloading: Int, val queued: Int, val paused: Int)

fun notificationCounts(tasks: List<DownloadTask>): NotificationCounts =
    NotificationCounts(
        downloading = tasks.count { it.state == DownloadState.DOWNLOADING },
        queued = tasks.count { it.state == DownloadState.QUEUED },
        paused = tasks.count { it.state == DownloadState.PAUSED },
    )

/** e.g. "Downloading 2 · 1 queued · 1 paused", or the idle text when nothing is active. */
suspend fun notificationText(counts: NotificationCounts): String {
    val parts = buildList {
        if (counts.downloading > 0) add(getString(Res.string.notif_downloading, counts.downloading))
        if (counts.queued > 0) add(getString(Res.string.notif_queued, counts.queued))
        if (counts.paused > 0) add(getString(Res.string.notif_paused, counts.paused))
    }
    if (parts.isEmpty()) return getString(Res.string.notif_idle)
    return parts.joinToString(" · ")
}

suspend fun downloadsChannelName(): String = getString(Res.string.notif_channel_name)
