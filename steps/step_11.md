I want you to implement the **core features** of the KMP video downloader app, using the previously built shared modules, state, UI components, and platform-specific DownloaderEngine.

Please do the following:

1. **URL Input & Metadata Fetching**
   - Allow the user to input a video URL.
   - Use `DownloaderEngine.extractInfo(url)` to fetch `VideoInfo`.
   - Display video metadata (title, duration, formats) in the UI using shared `VideoItem` components.

2. **Format Selection**
   - Allow the user to select a video/audio format from the available formats of the fetched video.
   - Update the `DownloadTask.format` in shared state when the user selects a format.

3. **Enqueue Download**
   - Add the task to `DownloadQueueManager` using `enqueue()`.
   - Trigger `startNext()` automatically or via a “Download” button.

4. **Progress Display**
   - Show real-time download progress via `ProgressBar` in the UI.
   - Update `DownloadUiState` with progress using `StateFlow` from `DownloadQueueManager`.

5. **Cancel Download**
   - Allow canceling a download task using `DownloaderEngine.cancel(taskId)`.
   - Update task state in `DownloadQueueManager` and UI.

6. **Optional Features (if desired)**
   - Audio-only download mode.
   - Playlist or batch downloads.
   - Retry failed tasks automatically.
   - Save download history locally (using serialization).

7. Ensure all features are **platform-agnostic**, using shared components, state, and event handling.  
8. Include proper **error handling**, **coroutine usage**, and **UI updates** in real time.

The goal is to have a **fully functional video downloader** with all main interactions: paste URL, select format, download, track progress, and cancel, on both Android and Desktop platforms.
