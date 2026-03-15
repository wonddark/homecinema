package com.homecinema.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.core.downloader.createDownloaderEngine
import com.homecinema.core.usecase.DownloadQueueManager
import com.homecinema.domain.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        engine = createDownloaderEngine()
        queueManager = DownloadQueueManager(engine!!, serviceScope)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Ready"))

        queueManager!!.tasks.onEach { tasks ->
            val downloading = tasks.count { it.state == DownloadState.DOWNLOADING }
            val queued = tasks.count { it.state == DownloadState.QUEUED }
            val text = when {
                downloading > 0 -> "Downloading $downloading file(s), $queued queued"
                queued > 0 -> "$queued file(s) queued"
                else -> "No active downloads"
            }
            updateNotification(text)
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        engine = null
        queueManager = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HomeCinema")
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

        var engine: DownloaderEngine? = null
            private set
        var queueManager: DownloadQueueManager? = null
            private set
    }
}
