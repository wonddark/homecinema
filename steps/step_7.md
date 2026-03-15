I want you to implement the **platform-specific actual implementations** of the `DownloaderEngine` interface for the KMP video downloader app.

Please do the following:

1. **Android Implementation** (`androidApp/src/main/kotlin/.../DownloaderEngineAndroid.kt`):
   - Use the `io.github.junkfood02.youtubedl-android` library.
   - Implement all methods from the `DownloaderEngine` interface:
     - `extractInfo(url: String): VideoInfo`
     - `download(url: String, formatId: String, outputPath: String, onProgress: (Float) -> Unit)`
     - `cancel(taskId: String)`
   - Ensure proper coroutine usage for async operations.
   - Map library-specific results to shared `VideoInfo` and `DownloadTask`.

2. **Desktop Implementation** (`desktopApp/src/main/kotlin/.../DownloaderEngineDesktop.kt`):
   - Use the `yt-dlp` CLI via process execution.
   - Implement all methods from the `DownloaderEngine` interface:
     - `extractInfo(url: String): VideoInfo` → parse JSON output from yt-dlp.
     - `download(url: String, formatId: String, outputPath: String, onProgress: (Float) -> Unit)` → capture stdout progress and convert to 0.0–1.0.
     - `cancel(taskId: String)` → terminate process for the given task.
   - Ensure coroutine-friendly, non-blocking execution.

3. Both implementations must **adhere strictly to the shared interface** so that the rest of the shared module code remains platform-agnostic.

4. Include proper **package declarations, imports, and error handling**.

The goal is to have **fully working DownloaderEngine implementations** for both Android and Desktop platforms that the shared queue manager and UI can use without modification.