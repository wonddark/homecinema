package com.homecinema.core

import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.homecinema.core.storage.FileIO
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual fun platformDownloadDir(): String =
    try {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    } catch (_: RuntimeException) {
        "downloads"
    }

actual fun revealInFileBrowser(path: String) {
    val file = File(path)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        appContext,
        "${appContext.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContext.startActivity(intent)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } else if (!tmp.renameTo(target)) {
            // java.nio.file needs API 26; rename(2) already replaces atomically, this is the last-resort path.
            target.delete()
            tmp.renameTo(target)
        }
    }

    override fun exists(path: String): Boolean = File(path).exists()

    override fun delete(path: String): Boolean = File(path).delete()

    override fun list(dir: String): List<String> =
        File(dir).listFiles()?.map { it.absolutePath }.orEmpty()
}

actual fun platformFileIO(): FileIO = JvmFileIO()

actual fun appStorageDir(): String = appContext.filesDir.absolutePath
