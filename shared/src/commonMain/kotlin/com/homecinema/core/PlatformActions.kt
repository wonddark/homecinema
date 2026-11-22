package com.homecinema.core

import androidx.compose.runtime.Composable

enum class ShareResult { SHARED, PATH_COPIED }

interface PlatformActions {
    fun openFile(path: String)
    fun shareFile(path: String): ShareResult
    fun openFolder(path: String)
    fun revealInFolder(path: String)
    suspend fun deleteFile(path: String): Boolean
}

/** The platform's [PlatformActions], remembered for the composition (Android binds an activity-result launcher). */
@Composable
expect fun rememberPlatformActions(): PlatformActions
