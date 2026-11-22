package com.homecinema.android

import com.homecinema.core.appStorageDir
import com.homecinema.core.downloader.createDownloaderEngine
import com.homecinema.core.platformDownloadDir
import com.homecinema.ui.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The process-wide [AppGraph]. Whichever of [DownloadService] or [MainActivity] asks first creates it,
 * so the activity never races the service's onCreate; [DownloadService.onDestroy] releases it.
 */
object AppGraphHolder {
    private var graph: AppGraph? = null

    @Synchronized
    fun getOrCreate(): AppGraph =
        graph ?: AppGraph(
            engine = createDownloaderEngine(),
            storageDir = appStorageDir(),
            downloadDir = platformDownloadDir(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        ).also { graph = it }

    @Synchronized
    fun release() {
        graph?.close()
        graph = null
    }
}
