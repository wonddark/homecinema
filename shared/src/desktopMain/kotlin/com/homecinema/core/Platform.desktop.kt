package com.homecinema.core

import com.homecinema.core.storage.FileIO
import java.awt.Desktop
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

    override fun write(path: String, text: String) = writeBytes(path, text.encodeToByteArray())

    /** Writes to "$path.tmp" first, then renames it over the target so readers never see a half-written file. */
    override fun writeBytes(path: String, bytes: ByteArray) {
        val target = File(path)
        target.parentFile?.mkdirs()
        val tmp = File("$path.tmp")
        tmp.writeBytes(bytes)
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override fun exists(path: String): Boolean = File(path).exists()

    override fun delete(path: String): Boolean = File(path).delete()

    override fun list(dir: String): List<String> =
        File(dir).listFiles()?.map { it.absolutePath }.orEmpty()
}

actual fun platformFileIO(): FileIO = JvmFileIO()
