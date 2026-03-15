I want you to implement **shared UI state and event interfaces** for the KMP video downloader app in the `shared/ui/state` module. These will allow the platform screens to observe state changes and trigger actions in a platform-agnostic way.

Please do the following:

1. Create a data class `DownloadUiState` with:
   - `tasks: List<DownloadTask>` → current list of download tasks
   - `isDownloading: Boolean` → whether any task is currently downloading
   - `selectedFormat: String?` → currently selected format (optional)

2. Create an interface `DownloadEventHandler` with methods:
   - `fun onStartDownload(url: String)` → triggered when the user starts a new download
   - `fun onCancelDownload(taskId: String)` → triggered when the user cancels a download
   - `fun onSelectFormat(taskId: String, formatId: String)` → triggered when a user selects a different format

3. Ensure the **state is platform-agnostic** and can be observed in Compose screens via `StateFlow` or other reactive patterns.
4. Include proper **package declarations and imports**.

The goal is to have a **shared state representation and unified event handling** for all UI screens, so that components remain stateless and all business logic interacts via the shared event handler.