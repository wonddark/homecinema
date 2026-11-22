package com.homecinema.core

enum class ShareResult { SHARED, PATH_COPIED }

interface PlatformActions {
    fun openFile(path: String)
    fun shareFile(path: String): ShareResult
    fun openFolder(path: String)
    fun revealInFolder(path: String)
    suspend fun deleteFile(path: String): Boolean
}
