I want you to implement a **Download Queue Manager** in the `shared/core/usecase` module for the KMP video downloader app. This manager will handle all download tasks and provide a shared state observable by the UI.

Please do the following:

1. Create a Kotlin class `DownloadQueueManager` with the following responsibilities:
   - Maintain a list of `DownloadTask` objects representing queued and ongoing downloads.
   - Allow tasks to be **enqueued**, **started**, and **canceled**.
   - Update each task’s state (`QUEUED`, `DOWNLOADING`, `COMPLETED`, `FAILED`) as appropriate.
   - Provide a **shared observable state** (e.g., `StateFlow<List<DownloadTask>>`) so that UI components can react to changes automatically.
   - Notify progress for each task via a callback, connected to the `DownloaderEngine`’s `onProgress`.

2. Implement the following methods:
   - `fun enqueue(task: DownloadTask)` → add a new task to the queue.
   - `suspend fun startNext()` → start downloading the next queued task.
   - `fun cancel(taskId: String)` → cancel a task using `DownloaderEngine.cancel`.
   - `private fun updateTaskProgress(taskId: String, progress: Float)` → update the progress of a task internally.
   - `private fun markTaskCompleted(taskId: String)` → set task state to `COMPLETED`.
   - `private fun markTaskFailed(taskId: String)` → set task state to `FAILED`.

3. Use **Kotlin Coroutines** to handle asynchronous downloads.
4. Ensure the class **depends only on the shared DownloaderEngine interface** (not platform-specific implementations).
5. Include package declarations, imports, and proper suspend function usage.

The goal is to have a **centralized, platform-agnostic download manager** that handles the queue, progress, and state updates for the UI.