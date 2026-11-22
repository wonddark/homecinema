package com.homecinema.core

import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.homecinema.core.storage.FileIO
import java.io.File

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

    override fun write(path: String, text: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    override fun exists(path: String): Boolean = File(path).exists()

    override fun delete(path: String): Boolean = File(path).delete()
}

actual fun platformFileIO(): FileIO = JvmFileIO()

actual fun appStorageDir(): String = appContext.filesDir.absolutePath
