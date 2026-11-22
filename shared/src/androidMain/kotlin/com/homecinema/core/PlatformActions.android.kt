package com.homecinema.core

import android.app.Activity
import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * The outstanding MediaStore delete request. Process-wide so a launcher registered again after a
 * configuration change still completes the deletion that is waiting on it.
 */
private val pendingDelete = AtomicReference<CompletableDeferred<Boolean>?>(null)

/** The launcher of the composition currently on screen; replaced on every recreation. */
private val currentLauncher = AtomicReference<ActivityResultLauncher<IntentSenderRequest>?>(null)

/**
 * Returns the process-wide Android [PlatformActions]. The instance is stable, so callers (e.g. an
 * app-level view-model surviving activity recreation) may hold it long-term: each composition
 * registers its activity-result launcher into a shared holder that the instance reads at use time.
 */
@Composable
actual fun rememberPlatformActions(): PlatformActions {
    AndroidPlatformActions.bind(LocalContext.current.applicationContext)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        pendingDelete.getAndSet(null)?.complete(result.resultCode == Activity.RESULT_OK)
    }
    DisposableEffect(launcher) {
        currentLauncher.set(launcher)
        onDispose { currentLauncher.compareAndSet(launcher, null) }
    }
    return AndroidPlatformActions
}

private object AndroidPlatformActions : PlatformActions {
    private val contextRef = AtomicReference<Context?>(null)

    private val context: Context
        get() = contextRef.get() ?: appContext

    fun bind(applicationContext: Context) {
        contextRef.compareAndSet(null, applicationContext)
    }

    override fun openFile(path: String) {
        val file = File(path)
        start {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri(file), mimeOf(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    override fun shareFile(path: String): ShareResult {
        val file = File(path)
        start {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mimeOf(file)
                putExtra(Intent.EXTRA_STREAM, contentUri(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ShareResult.SHARED
    }

    override fun openFolder(path: String) {
        val opened = start {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderDocumentUri(File(path)), DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        if (!opened) {
            start { Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        }
    }

    override fun revealInFolder(path: String) = openFolder(File(path).parent ?: path)

    override suspend fun deleteFile(path: String): Boolean {
        val file = File(path)
        val deleted = withContext(Dispatchers.IO) { runCatching { file.delete() }.getOrDefault(false) }
        if (deleted) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return requestMediaStoreDelete(file)
    }

    /** Asks the user to approve deleting a file this app doesn't own (scoped storage, API 30+). */
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun requestMediaStoreDelete(file: File): Boolean {
        val uri = withContext(Dispatchers.IO) { findMediaStoreUri(file) } ?: return false
        val result = CompletableDeferred<Boolean>()
        pendingDelete.getAndSet(result)?.complete(false)
        return try {
            val sender = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender
            val launcher = currentLauncher.get() ?: return false
            withContext(Dispatchers.Main) { launcher.launch(IntentSenderRequest.Builder(sender).build()) }
            result.await()
        } catch (_: RuntimeException) {
            false
        } finally {
            pendingDelete.compareAndSet(result, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun findMediaStoreUri(file: File): Uri? {
        val collections = listOf(
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL),
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
        )
        for (collection in collections) {
            val id = queryId(collection, "${MediaStore.MediaColumns.DATA} = ?", arrayOf(file.absolutePath))
                ?: relativePath(file)?.let { rel ->
                    queryId(
                        collection,
                        "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                        arrayOf(rel, file.name),
                    )
                }
            if (id != null) return ContentUris.withAppendedId(collection, id)
        }
        return null
    }

    private fun queryId(collection: Uri, selection: String, args: Array<String>): Long? =
        try {
            context.contentResolver
                .query(collection, arrayOf(MediaStore.MediaColumns._ID), selection, args, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
        } catch (_: RuntimeException) {
            null
        }

    /** "Download/GrabThatVideo/" for a file under primary external storage, else null. */
    private fun relativePath(file: File): String? {
        val parent = file.parentFile ?: return null
        val rel = pathUnderExternalRoot(parent) ?: return null
        return "$rel/"
    }

    /** The externalstorage document for [dir]; the public Downloads folder when [dir] isn't on primary storage. */
    private fun folderDocumentUri(dir: File): Uri {
        val rel = pathUnderExternalRoot(dir) ?: Environment.DIRECTORY_DOWNLOADS
        return DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, "primary:$rel")
    }

    private fun pathUnderExternalRoot(file: File): String? {
        val root = runCatching { Environment.getExternalStorageDirectory().absolutePath }.getOrNull() ?: return null
        val path = file.absolutePath
        if (!path.startsWith("$root/")) return null
        return path.removePrefix("$root/").trim('/')
    }

    private fun contentUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    private fun mimeOf(file: File): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"

    /** Starts the built intent; false when nothing can handle it (or the URI can't be shared). */
    private inline fun start(build: () -> Intent): Boolean =
        try {
            context.startActivity(build())
            true
        } catch (_: RuntimeException) {
            // ActivityNotFoundException, or FileProvider rejecting a path outside file_paths.xml.
            false
        }

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
}
