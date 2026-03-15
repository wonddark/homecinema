I want you to implement **unit tests** for the shared logic of the KMP video downloader app in the `shared/commonTest` module. The goal is to verify that the download queue, state updates, and progress handling work correctly across all platforms.

Please do the following:

1. **Test DownloadQueueManager**
   - Verify that tasks can be **enqueued** correctly.
   - Verify that `startNext()` updates task state from `QUEUED` → `DOWNLOADING` → `COMPLETED`.
   - Verify that a failed download is marked as `FAILED`.
   - Verify that `cancel(taskId)` updates the task state properly.

2. **Test DownloaderEngine Integration (mock)**
   - Create a mock `DownloaderEngine` implementation that simulates download progress.
   - Use it to test `DownloadQueueManager` without relying on actual platform download code.
   - Ensure progress updates are reflected correctly in `StateFlow`.

3. **Test State Observation**
   - Verify that changes in the queue manager (progress, completion, cancellation) are correctly emitted via `StateFlow` or observable state for the UI.

4. Use **Kotlin test framework** (`kotlin("test")`) in `commonTest`.
5. Include proper **package declarations, imports, and coroutine test setup** (`runTest` from kotlinx.coroutines.test).

The goal is to ensure that **all core shared logic is robust, predictable, and platform-independent**, before adding optional advanced features or integrating more complex platform APIs.
