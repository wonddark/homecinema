I want you to implement the **DownloaderEngine abstraction** in the `shared/core/downloader` module for the KMP video downloader app.

Please do the following:

1. Create an **`expect` interface** called `DownloaderEngine` in `shared/core/downloader/DownloaderEngine.kt` with the following methods:

   - `suspend fun extractInfo(url: String): VideoInfo`  
     - Given a video URL, returns a `VideoInfo` object containing metadata.

   - `suspend fun download(url: String, formatId: String, outputPath: String, onProgress: (Float) -> Unit)`  
     - Downloads the video in the specified format to the `outputPath`.  
     - Calls `onProgress` with a float from 0.0 to 1.0 representing download progress.

   - `suspend fun cancel(taskId: String)`  
     - Cancels an ongoing download task by its unique `taskId`.

2. Create placeholder `actual` files for each platform (`androidApp` and `desktopApp`) with empty implementations, so that later we can implement platform-specific logic:
   - Android actual: will later use `youtubedl-android`
   - Desktop actual: will later use `yt-dlp` CLI

3. Include proper package declarations, imports, and suspend function usage.  

The goal is to **have a shared interface** for all platforms to use, so that the rest of the shared logic and UI can depend on this interface without platform-specific code.