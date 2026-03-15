# Download Item Actions: Retry, Remove, Locate

**Date:** 2026-03-14
**Status:** Approved

## Overview

Add three contextual actions to each download item card: **retry** (re-queue a failed download), **remove** (delete the item from the list), and **locate** (reveal the downloaded file in the system file browser). Actions are shown based on the task's current state.

## Button Visibility Rules

| State        | Available Actions         |
|--------------|---------------------------|
| QUEUED       | Remove                    |
| DOWNLOADING  | Cancel (existing)         |
| COMPLETED    | Locate, Remove            |
| FAILED       | Retry, Remove             |

> **Note:** The existing code shows "Cancel" for both QUEUED and DOWNLOADING states. This spec intentionally removes Cancel from QUEUED items — a queued (not yet started) download is removed directly. The button rendering logic in both `VideoItem` actuals must be updated accordingly.

> **Note:** `DownloadQueueManager.remove()` is only called from the UI via the Remove button, which is hidden during DOWNLOADING. The implementation does not need to guard against cancelling an active download, but this is enforced solely by the visibility table. The function should be documented as "must only be called on non-active tasks."

## Section 1: Domain & Engine Changes

### `DownloadTask`
Add a nullable `outputPath: String?` field (default `null`). Populated when a download completes successfully. Used by `onLocateDownload` to know which file to reveal.

`DownloadTask` is `@Serializable`. Adding `outputPath: String? = null` is backwards-compatible with kotlinx.serialization — missing fields default to null when deserializing existing JSON. No `@SerialName` or `@Required` annotation is needed.

### `DownloaderEngine.download()`
Return type changes from `Unit` to `String` — the final absolute path of the downloaded file.

The desktop implementation (`DownloaderEngineDesktop`) captures the output path by parsing yt-dlp stdout for:
- `[download] Destination: <path>` — single-stream downloads
- `[Merger] Merging formats into "<path>"` — merged audio+video

Note: `redirectErrorStream(true)` is already set, so both stdout and stderr flow through the same stream — no separate stderr parsing is needed. Both yt-dlp line patterns appear in this merged stream.

The Android implementation (`DownloaderEngineAndroid`) returns the expected output path constructed from `outputPath` + `%(title)s` pattern — the exact filename is not available from `YoutubeDL.execute()`. Return `""` (empty string) if the path cannot be determined; `onLocateDownload` will no-op if `outputPath` is null or blank.

### `DownloadQueueManager`
- Add `remove(taskId: String)`: filters the task out of `_tasks` StateFlow. Must only be called on tasks that are not in DOWNLOADING state (enforced by UI visibility).
- Update `markTaskCompleted` to accept the `outputPath: String` returned by `download()` and store it on the task via `task.copy(outputPath = outputPath)`.

## Section 2: ViewModel & Event Handler

### `DownloadEventHandler` (interface)
Add:
```kotlin
fun onRemoveDownload(taskId: String)
fun onLocateDownload(taskId: String)
```
`onRetryDownload` already exists — its implementation needs a minor fix (see below).

### `DownloadViewModel`
- `onRemoveDownload(taskId)` → `queueManager.remove(taskId)`
- `onLocateDownload(taskId)` → looks up `task.outputPath`; calls `revealInFileBrowser(path)` only if non-null and non-blank
- `onRetryDownload(taskId)` — fix the existing implementation: the retry copy must set `outputPath = null` (to clear any stale path from the failed attempt) and the original FAILED task must be removed before enqueueing the retry copy to avoid duplicates:
  ```kotlin
  val retryTask = task.copy(state = DownloadState.QUEUED, progress = 0f, outputPath = null)
  queueManager.remove(task.id)
  queueManager.enqueue(retryTask)
  queueManager.startNext()
  ```

## Section 3: Platform `revealInFileBrowser`

Add `expect fun revealInFileBrowser(path: String)` to `Platform.kt` (in `core/`). This belongs alongside `platformDownloadDir()` in `Platform.kt` rather than `PlatformUtils.kt` (which lives in `ui/util/` and is UI-focused — device type detection). `revealInFileBrowser` is a system I/O capability, not a UI utility.

### Desktop (`Platform.desktop.kt`)
Uses `java.awt.Desktop.getDesktop().browseFileDirectory(File(path))` where supported. Falls back to platform CLI:
- macOS: `open -R <path>`
- Windows: `explorer /select,<path>`
- Linux: `xdg-open <parent directory>` (file highlighting not universally supported on Linux)

Detect OS via `System.getProperty("os.name")`.

### Android (`Platform.android.kt`)
`Intent` dispatch requires a `Context`, which is Android-specific and cannot appear in the common `expect` signature. The Android actual will read from a module-level `lateinit var appContext: android.content.Context` defined in `shared/androidMain`. This reference is set once from `HomeCinemaApp.onCreate()` (in `androidApp`) before any downloads are started.

The actual fires `Intent(Intent.ACTION_VIEW)` with the file URI via `FileProvider`, opening the file in the default handler (e.g. video player). Full file-manager reveal is not reliably possible on Android without manager-specific intents.

`FileProvider` requires a `<provider>` entry in `AndroidManifest.xml` and a `res/xml/file_paths.xml` resource in `androidApp`. These must be added as part of this feature.

**New file required:** `shared/src/androidMain/kotlin/com/homecinema/core/AppContext.kt` — holds the `lateinit var appContext`.

**Change required in `androidApp`:** `HomeCinemaApp.onCreate()` must set `appContext = this` after `super.onCreate()`.

## Section 4: UI Changes

### `VideoItem` (common expect)
Add 3 new lambda parameters:
```kotlin
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

### `VideoItem.android.kt` and `VideoItem.desktop.kt`
Render action buttons based on task state (see visibility table above). The existing cancel-for-QUEUED branch must be updated — QUEUED now shows only "Remove".

### `DownloadScreen` (Android and Desktop)
Pass the new lambdas to `VideoItem`:
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

## Files to Change

| File | Change |
|------|--------|
| `domain/DownloadTask.kt` | Add `outputPath: String?` |
| `core/downloader/DownloaderEngine.kt` | Change `download()` return type to `String` |
| `core/downloader/DownloaderEngineDesktop.kt` | Parse output path from yt-dlp stdout, return it |
| `androidMain/core/downloader/DownloaderEngineAndroid.kt` | Return constructed path or empty string |
| `core/usecase/DownloadQueueManager.kt` | Add `remove()`, update `markTaskCompleted` with outputPath |
| `core/usecase/DownloadViewModel.kt` | Add `onRemoveDownload`, `onLocateDownload`; fix `onRetryDownload` |
| `ui/state/DownloadEventHandler.kt` | Add `onRemoveDownload`, `onLocateDownload` |
| `core/Platform.kt` | Add `expect fun revealInFileBrowser(path: String)` |
| `desktopMain/core/Platform.desktop.kt` | Implement `revealInFileBrowser` |
| `androidMain/core/Platform.android.kt` | Implement `revealInFileBrowser` using `appContext` |
| `androidMain/core/AppContext.kt` | **New file** — `lateinit var appContext: android.content.Context` |
| `androidApp/.../HomeCinemaApp.kt` | Set `appContext = this` in `onCreate()` |
| `ui/components/VideoItem.kt` | Add 3 lambda params to expect |
| `ui/components/VideoItem.android.kt` | Implement new buttons; fix QUEUED to show Remove not Cancel |
| `ui/components/VideoItem.desktop.kt` | Implement new buttons; fix QUEUED to show Remove not Cancel |
| `androidMain/ui/DownloadScreen.kt` | Pass new lambdas |
| `desktopMain/ui/DownloadScreen.kt` | Pass new lambdas |
| `commonTest/MockDownloaderEngine.kt` | Update `download()` return type to `String`, return `""` |
