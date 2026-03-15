# Download Item Actions Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add retry, remove, and locate actions to download item cards, with state-conditional visibility.

**Architecture:** Changes flow outward from domain → engine → queue manager → view model → UI. The engine's `download()` return type changes from `Unit` to `String` (the output file path), which requires updating the interface and all implementations atomically. Platform-specific file-browser opening is added via a new `expect/actual` in `Platform.kt`. UI buttons are rendered conditionally by state in both `VideoItem` actuals.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, yt-dlp CLI (desktop), youtubedl-android (Android), kotlinx.coroutines, kotlinx.serialization, kotlin.test

---

## Chunk 1: Domain + Engine

### Task 1: Add `outputPath` to `DownloadTask`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/domain/DownloadTask.kt`

- [ ] **Step 1: Add the field**

```kotlin
@Serializable
data class DownloadTask(
    val id: String,
    val url: String,
    val videoInfo: VideoInfo? = null,
    val format: String? = null,
    val state: DownloadState = DownloadState.QUEUED,
    val progress: Float = 0f,
    val outputPath: String? = null
)
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew :shared:compileKotlinJvm :shared:compileTestKotlinJvm
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/homecinema/domain/DownloadTask.kt
git commit -m "feat: add outputPath field to DownloadTask"
```

---

### Task 2: Change `DownloaderEngine.download()` return type + update all implementations atomically

This task must update the interface AND all three implementors in one commit — the build will not compile until all are consistent. `DownloadQueueManager.startNext()` will continue to compile after this change because Kotlin does not require capturing a return value; the path wiring is deferred to Task 3.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/core/downloader/DownloaderEngine.kt`
- Modify: `shared/src/desktopMain/kotlin/com/homecinema/core/downloader/DownloaderEngineDesktop.kt`
- Modify: `shared/src/androidMain/kotlin/com/homecinema/core/downloader/DownloaderEngineAndroid.kt`
- Modify: `shared/src/commonTest/kotlin/com/homecinema/MockDownloaderEngine.kt`

- [ ] **Step 1: Update the interface**

```kotlin
// shared/src/commonMain/kotlin/com/homecinema/core/downloader/DownloaderEngine.kt
interface DownloaderEngine {
    suspend fun extractInfo(url: String): VideoInfo
    suspend fun download(
        taskId: String,
        url: String,
        formatId: String,
        outputPath: String,
        onProgress: (Float) -> Unit
    ): String
    suspend fun cancel(taskId: String)
}

expect fun createDownloaderEngine(): DownloaderEngine
```

- [ ] **Step 2: Update `DownloaderEngineDesktop` to parse the output path and return it**

Parse yt-dlp stdout for the destination path. For single-stream downloads, yt-dlp prints `[download] Destination: <path>`. For merged audio+video, `[Merger] Merging formats into "<path>"`. The merger line takes precedence since it represents the final file.

```kotlin
// shared/src/desktopMain/kotlin/com/homecinema/core/downloader/DownloaderEngineDesktop.kt

override suspend fun download(
    taskId: String,
    url: String,
    formatId: String,
    outputPath: String,
    onProgress: (Float) -> Unit
): String = withContext(Dispatchers.IO) {
    val process = ProcessBuilder(
        "yt-dlp",
        "-f", formatId,
        "-o", "$outputPath/%(title)s.%(ext)s",
        "--newline",
        url
    )
        .redirectErrorStream(true)
        .start()

    var capturedPath = ""

    // Track the process for cancellation support
    activeProcesses[taskId] = process

    process.inputStream.bufferedReader().useLines { lines ->
        for (line in lines) {
            if (line.startsWith("[download]")) {
                val match = Regex("""(\d+\.?\d*)%""").find(line)
                match?.groupValues?.get(1)?.toFloatOrNull()?.let { pct ->
                    onProgress(pct / 100f)
                }
                if (line.startsWith("[download] Destination: ")) {
                    capturedPath = line.removePrefix("[download] Destination: ").trim()
                }
                // Handle already-downloaded: "[download] /path has already been downloaded"
                if (line.contains("has already been downloaded")) {
                    val alreadyPath = line
                        .removePrefix("[download] ")
                        .removeSuffix(" has already been downloaded")
                        .trim()
                    if (capturedPath.isEmpty()) capturedPath = alreadyPath
                }
            }
            if (line.startsWith("[Merger] Merging formats into ")) {
                // Merger output supersedes the destination (final merged file)
                capturedPath = line
                    .removePrefix("[Merger] Merging formats into \"")
                    .trimEnd('"')
                    .trim()
            }
        }
    }
    process.waitFor()
    activeProcesses.remove(taskId)
    onProgress(1f)
    capturedPath
}
```

- [ ] **Step 3: Update `DownloaderEngineAndroid` to return empty string**

Android's `YoutubeDL.execute()` does not expose the final output filename. Return `""` — the `onLocateDownload` handler will no-op when `outputPath` is blank.

```kotlin
// In DownloaderEngineAndroid — change the download() function:
override suspend fun download(
    taskId: String,
    url: String,
    formatId: String,
    outputPath: String,
    onProgress: (Float) -> Unit,
): String = withContext(Dispatchers.IO) {
    val request =
        YoutubeDLRequest(url).apply {
            addOption("-f", formatId)
            addOption("-o", "$outputPath/%(title)s.%(ext)s")
            addOption("--no-mtime")
        }
    YoutubeDL.getInstance().execute(request, taskId) { progress, _, _ ->
        onProgress(progress / 100f)
    }
    ""
}
```

- [ ] **Step 4: Fix `MockDownloaderEngine` — add missing `taskId` param and return `String`**

The mock was missing `taskId` (pre-existing mismatch with the interface). Fix both issues now.

```kotlin
// shared/src/commonTest/kotlin/com/homecinema/MockDownloaderEngine.kt
package com.homecinema

import com.homecinema.core.downloader.DownloaderEngine
import com.homecinema.domain.VideoFormat
import com.homecinema.domain.VideoInfo

class MockDownloaderEngine(
    private val shouldFail: Boolean = false,
    private val progressSteps: List<Float> = listOf(0.25f, 0.5f, 0.75f, 1.0f),
    private val mockOutputPath: String = "mock/downloads/Mock Video Title.mp4"
) : DownloaderEngine {

    var cancelledTaskId: String? = null

    override suspend fun extractInfo(url: String): VideoInfo {
        if (shouldFail) throw RuntimeException("Mock extraction failure")
        return VideoInfo(
            id = "mock-id",
            title = "Mock Video Title",
            duration = 120L,
            formats = listOf(
                VideoFormat("best", "best", null),
                VideoFormat("137", "1080p", 100_000_000L),
                VideoFormat("136", "720p", 50_000_000L)
            )
        )
    }

    override suspend fun download(
        taskId: String,
        url: String,
        formatId: String,
        outputPath: String,
        onProgress: (Float) -> Unit
    ): String {
        if (shouldFail) throw RuntimeException("Mock download failure")
        for (step in progressSteps) {
            onProgress(step)
        }
        return mockOutputPath
    }

    override suspend fun cancel(taskId: String) {
        cancelledTaskId = taskId
    }
}
```

- [ ] **Step 5: Verify the build compiles**

```bash
./gradlew :shared:compileKotlinJvm :shared:compileTestKotlinJvm
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Run existing tests to confirm nothing regressed**

```bash
./gradlew :shared:jvmTest
```
Expected: All tests pass.

- [ ] **Step 7: Commit**

```bash
git add \
  shared/src/commonMain/kotlin/com/homecinema/core/downloader/DownloaderEngine.kt \
  shared/src/desktopMain/kotlin/com/homecinema/core/downloader/DownloaderEngineDesktop.kt \
  shared/src/androidMain/kotlin/com/homecinema/core/downloader/DownloaderEngineAndroid.kt \
  shared/src/commonTest/kotlin/com/homecinema/MockDownloaderEngine.kt
git commit -m "feat: change download() return type to String for output path capture"
```

---

## Chunk 2: Queue Manager + ViewModel

### Task 3: Update `DownloadQueueManager` — add `remove()` and store `outputPath`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/core/usecase/DownloadQueueManager.kt`
- Modify: `shared/src/commonTest/kotlin/com/homecinema/DownloadQueueManagerTest.kt`

- [ ] **Step 1: Write failing tests for `remove()` and `outputPath` storage**

Add to `DownloadQueueManagerTest`:

```kotlin
@Test
fun `remove deletes task from list`() = testScope.runTest {
    val engine = MockDownloaderEngine()
    val manager = DownloadQueueManager(engine, this)
    manager.enqueue(makeTask("t1"))
    manager.enqueue(makeTask("t2"))

    manager.remove("t1")

    val tasks = manager.tasks.value
    assertEquals(1, tasks.size)
    assertEquals("t2", tasks.first().id)
}

@Test
fun `remove on unknown id is a no-op`() = testScope.runTest {
    val engine = MockDownloaderEngine()
    val manager = DownloadQueueManager(engine, this)
    manager.enqueue(makeTask("t1"))

    manager.remove("nonexistent")

    assertEquals(1, manager.tasks.value.size)
}

@Test
fun `completed task stores outputPath from engine`() = testScope.runTest {
    val engine = MockDownloaderEngine(mockOutputPath = "downloads/video.mp4")
    val manager = DownloadQueueManager(engine, this)
    manager.enqueue(makeTask())

    manager.startNext()
    advanceUntilIdle()

    val task = manager.tasks.value.first()
    assertEquals(DownloadState.COMPLETED, task.state)
    assertEquals("downloads/video.mp4", task.outputPath)
}
```

- [ ] **Step 2: Run the new tests to confirm they fail**

```bash
./gradlew :shared:jvmTest --tests "com.homecinema.DownloadQueueManagerTest.remove*" \
  --tests "com.homecinema.DownloadQueueManagerTest.completed*"
```
Expected: FAIL — `remove` method not found, `outputPath` not set.

- [ ] **Step 3: Implement `remove()` and update `startNext()` + `markTaskCompleted`**

```kotlin
// DownloadQueueManager.kt — updated methods:

fun remove(taskId: String) {
    _tasks.value = _tasks.value.filter { it.id != taskId }
}

suspend fun startNext() {
    val nextTask = _tasks.value.firstOrNull { it.state == DownloadState.QUEUED } ?: return
    updateTaskState(nextTask.id, DownloadState.DOWNLOADING)
    scope.launch {
        try {
            val outputPath = engine.download(
                taskId = nextTask.id,
                url = nextTask.url,
                formatId = nextTask.format ?: "best",
                outputPath = platformDownloadDir(),
                onProgress = { progress -> updateTaskProgress(nextTask.id, progress) },
            )
            markTaskCompleted(nextTask.id, outputPath)
        } catch (e: Exception) {
            println(e)
            markTaskFailed(nextTask.id)
        }
    }
}

private fun markTaskCompleted(taskId: String, outputPath: String) {
    _tasks.value = _tasks.value.map { task ->
        if (task.id == taskId)
            task.copy(state = DownloadState.COMPLETED, outputPath = outputPath.ifBlank { null })
        else task
    }
}
```

- [ ] **Step 4: Run all queue manager tests**

```bash
./gradlew :shared:jvmTest --tests "com.homecinema.DownloadQueueManagerTest"
```
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add \
  shared/src/commonMain/kotlin/com/homecinema/core/usecase/DownloadQueueManager.kt \
  shared/src/commonTest/kotlin/com/homecinema/DownloadQueueManagerTest.kt
git commit -m "feat: add remove() to queue manager and store outputPath on completion"
```

---

### Task 4: Update `DownloadEventHandler` and `DownloadViewModel`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/ui/state/DownloadEventHandler.kt`
- Modify: `shared/src/commonMain/kotlin/com/homecinema/core/usecase/DownloadViewModel.kt`

- [ ] **Step 1: Add new methods to `DownloadEventHandler`**

```kotlin
interface DownloadEventHandler {
    fun onUrlChanged(url: String)
    fun onStartDownload(url: String)
    fun onCancelDownload(taskId: String)
    fun onSelectFormat(taskId: String, formatId: String)
    fun onRetryDownload(taskId: String)
    fun onRemoveDownload(taskId: String)
    fun onLocateDownload(taskId: String)
}
```

- [ ] **Step 2: Implement the new methods in `DownloadViewModel` and fix `onRetryDownload`**

The existing `onRetryDownload` has two bugs: it doesn't remove the failed task (leaving a duplicate) and it carries the stale `outputPath`. Fix both, and add the two new handlers.

```kotlin
override fun onRetryDownload(taskId: String) {
    scope.launch {
        val task = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return@launch
        val retryTask = task.copy(state = DownloadState.QUEUED, progress = 0f, outputPath = null)
        queueManager.remove(task.id)
        queueManager.enqueue(retryTask)
        queueManager.startNext()
    }
}

override fun onRemoveDownload(taskId: String) {
    queueManager.remove(taskId)
}

override fun onLocateDownload(taskId: String) {
    val path = _uiState.value.tasks.firstOrNull { it.id == taskId }?.outputPath
    if (!path.isNullOrBlank()) {
        revealInFileBrowser(path)
    }
}
```

Also add this import to `DownloadViewModel.kt` (it's in `com.homecinema.core.usecase`, so the top-level function from `com.homecinema.core` needs an explicit import):

```kotlin
import com.homecinema.core.revealInFileBrowser
```

- [ ] **Step 3: Verify the build compiles**

```bash
./gradlew :shared:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run all tests**

```bash
./gradlew :shared:jvmTest
```
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add \
  shared/src/commonMain/kotlin/com/homecinema/ui/state/DownloadEventHandler.kt \
  shared/src/commonMain/kotlin/com/homecinema/core/usecase/DownloadViewModel.kt
git commit -m "feat: add onRemoveDownload and onLocateDownload; fix onRetryDownload duplicate bug"
```

---

## Chunk 3: Platform `revealInFileBrowser`

### Task 5: Add `expect fun revealInFileBrowser` and desktop implementation

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/core/Platform.kt`
- Modify: `shared/src/desktopMain/kotlin/com/homecinema/core/Platform.desktop.kt`

- [ ] **Step 1: Add the expect declaration**

```kotlin
// Platform.kt
package com.homecinema.core

expect fun platformDownloadDir(): String
expect fun revealInFileBrowser(path: String)
```

- [ ] **Step 2: Add the desktop actual**

Detect OS and use the appropriate method. `Desktop.browseFileDirectory` is the cleanest option on macOS and Windows where supported; fall back to CLI on Linux.

```kotlin
// Platform.desktop.kt
package com.homecinema.core

import java.awt.Desktop
import java.io.File

actual fun platformDownloadDir(): String = "downloads"

actual fun revealInFileBrowser(path: String) {
    val file = File(path)
    val desktop = Desktop.getDesktop()
    if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
        desktop.browseFileDirectory(file)
        return
    }
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("mac") -> Runtime.getRuntime().exec(arrayOf("open", "-R", path))
        os.contains("win") -> Runtime.getRuntime().exec(arrayOf("explorer", "/select,$path"))
        else -> Runtime.getRuntime().exec(arrayOf("xdg-open", file.parent ?: path))
    }
}
```

- [ ] **Step 3: Verify desktop compiles (Android actual is added in the next task)**

Since `Platform.android.kt` also needs an actual, the build won't be fully clean until Task 6. Verify the desktop source set compiles in isolation:

```bash
./gradlew :shared:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL (JVM/desktop source set only)

---

### Task 6: Android context holder, `revealInFileBrowser` actual, and FileProvider setup

**Files:**
- Create: `shared/src/androidMain/kotlin/com/homecinema/core/AppContext.kt`
- Modify: `shared/src/androidMain/kotlin/com/homecinema/core/Platform.android.kt`
- Modify: `androidApp/src/main/kotlin/com/homecinema/android/HomeCinemaApp.kt`
- Modify: `androidApp/src/main/AndroidManifest.xml`
- Create: `androidApp/src/main/res/xml/file_paths.xml`

- [ ] **Step 1: Create the context holder**

```kotlin
// shared/src/androidMain/kotlin/com/homecinema/core/AppContext.kt
package com.homecinema.core

import android.content.Context

lateinit var appContext: Context
```

- [ ] **Step 2: Set the context in `HomeCinemaApp`**

```kotlin
// androidApp/.../HomeCinemaApp.kt
class HomeCinemaApp : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        appContext = this          // <-- add this line
        YoutubeDL.getInstance().init(this)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(this@HomeCinemaApp)
                Log.i("HomeCinemaApp", "yt-dlp updated successfully")
            } catch (e: Exception) {
                Log.w("HomeCinemaApp", "yt-dlp update failed: ${e.message}")
            }
        }
    }
}
```

Add the import at the top: `import com.homecinema.core.appContext`

- [ ] **Step 3: Implement Android `revealInFileBrowser`**

```kotlin
// shared/src/androidMain/kotlin/com/homecinema/core/Platform.android.kt
package com.homecinema.core

import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

actual fun platformDownloadDir(): String =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

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
```

- [ ] **Step 4: Create `file_paths.xml`**

```xml
<!-- androidApp/src/main/res/xml/file_paths.xml -->
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="external_files" path="." />
</paths>
```

Create the directory if it doesn't exist: `androidApp/src/main/res/xml/`

- [ ] **Step 5: Register the FileProvider in `AndroidManifest.xml`**

Inside the `<application>` block, after the `<service>` entry:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

- [ ] **Step 6: Verify the full build compiles**

```bash
./gradlew :shared:compileKotlinJvm :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Run all tests**

```bash
./gradlew :shared:jvmTest
```
Expected: All tests pass.

- [ ] **Step 8: Commit**

```bash
git add \
  shared/src/commonMain/kotlin/com/homecinema/core/Platform.kt \
  shared/src/desktopMain/kotlin/com/homecinema/core/Platform.desktop.kt \
  shared/src/androidMain/kotlin/com/homecinema/core/AppContext.kt \
  shared/src/androidMain/kotlin/com/homecinema/core/Platform.android.kt \
  androidApp/src/main/kotlin/com/homecinema/android/HomeCinemaApp.kt \
  androidApp/src/main/AndroidManifest.xml \
  androidApp/src/main/res/xml/file_paths.xml
git commit -m "feat: add revealInFileBrowser expect/actual for desktop and Android"
```

---

## Chunk 4: UI

### Task 7: Update `VideoItem` expect signature and both actuals

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/ui/components/VideoItem.kt`
- Modify: `shared/src/androidMain/kotlin/com/homecinema/ui/components/VideoItem.android.kt`
- Modify: `shared/src/desktopMain/kotlin/com/homecinema/ui/components/VideoItem.desktop.kt`

Button visibility rules:
- `QUEUED` → Remove
- `DOWNLOADING` → Cancel (existing — no change to cancel logic)
- `COMPLETED` → Locate, Remove
- `FAILED` → Retry, Remove

- [ ] **Step 1: Update the expect declaration**

```kotlin
// VideoItem.kt
package com.homecinema.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homecinema.domain.DownloadTask

@Composable
expect fun VideoItem(
    task: DownloadTask,
    onCancel: () -> Unit,
    onFormatSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier
)
```

- [ ] **Step 2: Update `VideoItem.android.kt`**

Replace the existing action button block. The existing Cancel condition (`DOWNLOADING || QUEUED`) must change to `DOWNLOADING` only. QUEUED now shows Remove.

```kotlin
@Composable
actual fun VideoItem(
    task: DownloadTask,
    onCancel: () -> Unit,
    onFormatSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.large)) {
            Text(
                text = task.videoInfo?.title ?: task.url,
                style = AppTypography.title,
                maxLines = 2,
            )
            Spacer(Modifier.height(AppSpacing.small))
            Text(
                text = "State: ${task.state.name}",
                style = AppTypography.body,
            )
            if (task.videoInfo != null) {
                Spacer(Modifier.height(AppSpacing.small))
                FormatSelector(
                    formats = task.videoInfo.formats,
                    selectedFormatId = task.format,
                    onFormatSelected = onFormatSelected,
                )
            }
            if (task.state == DownloadState.DOWNLOADING) {
                Spacer(Modifier.height(AppSpacing.small))
                AppProgressBar(progress = task.progress)
            }
            Spacer(Modifier.height(AppSpacing.medium))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (task.state) {
                    DownloadState.QUEUED -> {
                        AppButton(text = "Remove", onClick = onRemove)
                    }
                    DownloadState.DOWNLOADING -> {
                        AppButton(text = "Cancel", onClick = onCancel)
                    }
                    DownloadState.COMPLETED -> {
                        AppButton(text = "Locate", onClick = onLocate)
                        Spacer(Modifier.width(AppSpacing.medium))
                        AppButton(text = "Remove", onClick = onRemove)
                    }
                    DownloadState.FAILED -> {
                        AppButton(text = "Retry", onClick = onRetry)
                        Spacer(Modifier.width(AppSpacing.medium))
                        AppButton(text = "Remove", onClick = onRemove)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Update `VideoItem.desktop.kt`**

Same state-based logic, adapted for the desktop Row layout:

```kotlin
@Composable
actual fun VideoItem(
    task: DownloadTask,
    onCancel: () -> Unit,
    onFormatSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.large, vertical = AppSpacing.medium),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface)
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.videoInfo?.title ?: task.url,
                    style = AppTypography.title,
                    maxLines = 1
                )
                Text(
                    text = "State: ${task.state.name}",
                    style = AppTypography.body
                )
                if (task.videoInfo != null) {
                    FormatSelector(
                        formats = task.videoInfo.formats,
                        selectedFormatId = task.format,
                        onFormatSelected = onFormatSelected
                    )
                }
            }
            if (task.state == DownloadState.DOWNLOADING) {
                AppProgressBar(
                    progress = task.progress,
                    modifier = Modifier.width(AppSpacing.extraLarge * 5)
                )
            }
            Spacer(Modifier.width(AppSpacing.large))
            when (task.state) {
                DownloadState.QUEUED -> {
                    AppButton(text = "Remove", onClick = onRemove)
                }
                DownloadState.DOWNLOADING -> {
                    AppButton(text = "Cancel", onClick = onCancel)
                }
                DownloadState.COMPLETED -> {
                    AppButton(text = "Locate", onClick = onLocate)
                    Spacer(Modifier.width(AppSpacing.medium))
                    AppButton(text = "Remove", onClick = onRemove)
                }
                DownloadState.FAILED -> {
                    AppButton(text = "Retry", onClick = onRetry)
                    Spacer(Modifier.width(AppSpacing.medium))
                    AppButton(text = "Remove", onClick = onRemove)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Verify compile (both common and Android actuals)**

```bash
./gradlew :shared:compileKotlinJvm :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

---

### Task 8: Update both `DownloadScreen`s to pass new lambdas

**Files:**
- Modify: `shared/src/androidMain/kotlin/com/homecinema/ui/DownloadScreen.kt`
- Modify: `shared/src/desktopMain/kotlin/com/homecinema/ui/DownloadScreen.kt`

- [ ] **Step 1: Update both `VideoItem` call sites**

In **both** `DownloadScreen.kt` files, every `VideoItem(...)` call needs the three new lambdas. The pattern is identical in both:

```kotlin
VideoItem(
    task = task,
    onCancel = { viewModel.onCancelDownload(task.id) },
    onFormatSelected = { fmtId -> viewModel.onSelectFormat(task.id, fmtId) },
    onRetry = { viewModel.onRetryDownload(task.id) },
    onRemove = { viewModel.onRemoveDownload(task.id) },
    onLocate = { viewModel.onLocateDownload(task.id) }
)
```

The Android `DownloadScreen` has this call in two places (phone and tablet branch) — update both.

- [ ] **Step 2: Verify full build**

```bash
./gradlew :shared:compileKotlinJvm :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run all tests**

```bash
./gradlew :shared:jvmTest
```
Expected: All tests pass.

- [ ] **Step 4: Commit everything from Chunk 4**

```bash
git add \
  shared/src/commonMain/kotlin/com/homecinema/ui/components/VideoItem.kt \
  shared/src/androidMain/kotlin/com/homecinema/ui/components/VideoItem.android.kt \
  shared/src/desktopMain/kotlin/com/homecinema/ui/components/VideoItem.desktop.kt \
  shared/src/androidMain/kotlin/com/homecinema/ui/DownloadScreen.kt \
  shared/src/desktopMain/kotlin/com/homecinema/ui/DownloadScreen.kt
git commit -m "feat: add retry/remove/locate buttons to VideoItem with state-based visibility"
```
