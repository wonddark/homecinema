package com.homecinema.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

/** Desktop actions: AWT `Desktop` for opening, the system clipboard for "share". Failures are swallowed. */
private object DesktopPlatformActions : PlatformActions {
    override fun openFile(path: String) = open(File(path))

    override fun shareFile(path: String): ShareResult {
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(path), null)
        } catch (_: Exception) {
            // Headless or clipboard busy: nothing useful to report beyond the snackbar.
        }
        return ShareResult.PATH_COPIED
    }

    override fun openFolder(path: String) = open(File(path))

    override fun revealInFolder(path: String) {
        try {
            revealInFileBrowser(path)
        } catch (_: Exception) {
            // No file browser available.
        }
    }

    override suspend fun deleteFile(path: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                File(path).delete()
            } catch (_: SecurityException) {
                false
            }
        }

    private fun open(file: File) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file)
            }
        } catch (_: Exception) {
            // Missing file, no associated app, or headless.
        }
    }
}

@Composable
actual fun rememberPlatformActions(): PlatformActions = remember { DesktopPlatformActions }
