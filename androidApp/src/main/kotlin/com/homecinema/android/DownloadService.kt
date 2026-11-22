package com.homecinema.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.homecinema.core.notification.downloadsChannelName
import com.homecinema.core.notification.notificationCounts
import com.homecinema.core.notification.notificationText
import com.homecinema.ui.AppGraph
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

/** Foreground service that owns the [AppGraph] (and so the download queue) for the process lifetime. */
class DownloadService : Service() {

    override fun onCreate() {
        super.onCreate()
        val graph = AppGraphHolder.getOrCreate()

        // startForeground must run synchronously, so resolve the (small, bundled) strings blocking.
        val (channelName, initialText) = runBlocking {
            downloadsChannelName() to notificationText(notificationCounts(graph.queue.tasks.value))
        }
        createNotificationChannel(channelName)
        startForeground(NOTIFICATION_ID, buildNotification(initialText))

        graph.queue.tasks
            .map { notificationText(notificationCounts(it)) }
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

    private fun createNotificationChannel(name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
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
