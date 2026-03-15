I want you to implement **platform-specific screens** for the KMP video downloader app using Compose UI, in the following way:

1. **Android Screen** (`androidApp/src/main/kotlin/.../DownloadScreen.kt`):
   - Use Jetpack Compose for Android.
   - Observe the shared `DownloadUiState` from the `DownloadQueueManager`.
   - Display a list of download tasks using the shared `VideoItem` component.
   - Include actions for:
     - Starting a new download (text input + button)
     - Canceling a download
     - Selecting a format via `FormatSelector`
   - Layout should use **Column or LazyColumn**, with proper spacing from `AppSpacing`.

2. **Desktop Screen** (`desktopApp/src/main/kotlin/.../DownloadScreen.kt`):
   - Use Compose Multiplatform for Desktop.
   - Observe the shared `DownloadUiState`.
   - Display download tasks using the shared `VideoItem` component.
   - Include actions for starting downloads, canceling, and selecting format.
   - Layout should be **Row or LazyVerticalGrid** to take advantage of larger screens.

3. **Shared Components Usage**:
   - Use atomic shared components (`VideoItem`, `ProgressBar`, `Button`, `FormatSelector`) in both screens.
   - Do **not** implement layout logic in shared components; only use them as building blocks.

4. Include proper **package declarations, imports, and Compose annotations** (`@Composable`).

The goal is to have **fully functional platform screens** that render the shared UI state, react to events via `DownloadEventHandler`, and use shared components for consistent look and feel.
