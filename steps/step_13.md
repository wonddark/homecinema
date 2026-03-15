I want you to implement **optional advanced features** for the KMP video downloader app to enhance usability and functionality. These features are not mandatory but will improve the user experience.

Please do the following:

1. **Batch or Playlist Downloads**
   - Allow the user to input multiple URLs or a playlist link.
   - Enqueue multiple `DownloadTask` objects in the `DownloadQueueManager`.
   - Handle progress and completion for each task individually.

2. **Smart Format Selection**
   - Implement a default “best quality” option.
   - Allow the user to prioritize video quality, file size, or audio-only downloads.

3. **Retry Failed Downloads**
   - Automatically retry failed tasks a configurable number of times.
   - Update `DownloadTask` state accordingly.

4. **Cookies / Proxy Support**
   - Enable optional authentication via cookies for restricted content.
   - Allow optional HTTP proxy configuration.

5. **Local History / Favorites**
   - Store completed downloads locally using serialization.
   - Display a list of previously downloaded videos in a separate UI screen.

6. Ensure that **all features integrate cleanly with shared state and components**.
7. Maintain **platform-agnostic logic** in the shared module wherever possible, delegating platform-specific behavior to the actual DownloaderEngine implementations.

The goal is to provide **enhanced usability and flexibility**, while keeping the core app architecture clean and shared across platforms.