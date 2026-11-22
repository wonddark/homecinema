package com.homecinema.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.ui.AppGraph
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/** Foreground service that owns the [AppGraph] (and so the download queue) for the process lifetime. */
class DownloadService : Service() {

    override fun onCreate() {
        super.onCreate()
        val graph = AppGraphHolder.getOrCreate()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(notificationText(graph.queue.tasks.value)))

        graph.queue.tasks
            .map(::notificationText)
            .distinctUntilChanged()
            .onEach(::updateNotification)
            .launchIn(graph.scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        AppGraphHolder.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GrabThatVideo")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        private const val CHANNEL_ID = "homecinema_downloads"
        private const val NOTIFICATION_ID = 1

        /** The single app graph, shared with [MainActivity]. */
        val graph: AppGraph get() = AppGraphHolder.getOrCreate()
    }
}

/** e.g. "Downloading 2 · 1 queued · 1 paused", or "No active downloads". */
private fun notificationText(tasks: List<DownloadTask>): String {
    val downloading = tasks.count { it.state == DownloadState.DOWNLOADING }
    val queued = tasks.count { it.state == DownloadState.QUEUED }
    val paused = tasks.count { it.state == DownloadState.PAUSED }
    val parts = buildList {
        if (downloading > 0) add("Downloading $downloading")
        if (queued > 0) add("$queued queued")
        if (paused > 0) add("$paused paused")
    }
    return parts.joinToString(" · ").ifEmpty { "No active downloads" }
}
