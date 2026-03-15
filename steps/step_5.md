I want you to implement **shared UI components** for the KMP video downloader app in the `shared/ui/components` module. These components will be **atomic, stateless, and reusable** across all platforms, with **expect/actual** for platform-specific layouts where needed.

Please do the following:

1. Create the following components:

   - `VideoItem`  
     - **expect function**: `@Composable fun VideoItem(task: DownloadTask, onCancel: () -> Unit)`  
     - Displays video title, selected format, and download progress.
     - Platform-specific actual implementations will handle layout differences (Column for Android, Row for Desktop).

   - `ProgressBar`  
     - Simple composable showing progress from 0.0 to 1.0.

   - `Button`  
     - Reusable composable button with text and click handler.

   - `FormatSelector`  
     - Dropdown or selector component for choosing video/audio format.

2. Create a `theme` package for shared styling tokens:
   - `Colors.kt` → primary, secondary, background colors
   - `Typography.kt` → title and body text styles
   - `Spacing.kt` → small, medium, large spacing values

3. Ensure **all components use shared theme tokens** for colors, fonts, and spacing.
4. Components must be **stateless**:
   - Receive data and callbacks via parameters.
   - Do not hold their own state; state comes from shared `DownloadUiState`.

5. Include proper **package declarations, imports, and Compose annotations** (`@Composable`).

The goal is to have **shared building blocks** that can be reused in platform-specific screens and respond automatically to theme updates.