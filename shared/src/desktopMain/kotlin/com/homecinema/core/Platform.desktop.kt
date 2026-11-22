package com.homecinema.core

import com.homecinema.core.storage.FileIO
import java.awt.Desktop
import java.io.File

actual fun platformDownloadDir(): String {
    val dir = File(System.getProperty("user.home"), "Downloads/GrabThatVideo")
    dir.mkdirs()
    return dir.absolutePath
}

actual fun appStorageDir(): String {
    val dir = File(System.getProperty("user.home"), ".grabthatvideo")
    dir.mkdirs()
    return dir.absolutePath
}

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

private class JvmFileIO : FileIO {
    override fun read(path: String): String? {
        val file = File(path)
        return if (file.isFile) file.readText() else null
    }

    override fun write(path: String, text: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    override fun exists(path: String): Boolean = File(path).exists()

    override fun delete(path: String): Boolean = File(path).delete()
}

actual fun platformFileIO(): FileIO = JvmFileIO()
