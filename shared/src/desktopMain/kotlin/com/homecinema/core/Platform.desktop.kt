package com.homecinema.core

import java.awt.Desktop
import java.io.File

actual fun platformDownloadDir(): String = "downloads"

actual fun revealInFileBrowser(path: String) {
    val file = File(path)
    if (Desktop.isDesktopSupported()) {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
            desktop.browseFileDirectory(file)
            return
        }
    }
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("mac") -> Runtime.getRuntime().exec(arrayOf("open", "-R", path))
        os.contains("win") -> Runtime.getRuntime().exec(arrayOf("explorer", "/select,$path"))
        else -> Runtime.getRuntime().exec(arrayOf("xdg-open", file.parent ?: path))
    }
}
