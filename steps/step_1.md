I want you to create a Kotlin Multiplatform (KMP) project for a video downloader app with the following structure:

1. Modules:
   - `shared` → contains all shared business logic, domain models, and shared UI components.
   - `androidApp` → Android platform module, will implement Android-specific UI and actual DownloaderEngine.
   - `desktopApp` → Desktop JVM platform module, will implement Desktop UI and actual DownloaderEngine.

2. KMP configuration:
   - Targets: Android and JVM (Desktop)
   - Dependencies:
     - kotlinx.coroutines (for shared asynchronous logic)
     - kotlinx.serialization (for JSON parsing)
     - Compose Multiplatform runtime in shared module
     - Android module: Compose UI
     - Desktop module: Compose Multiplatform Desktop
   - Use Kotlin 1.9+.

3. Gradle setup:
   - `settings.gradle.kts` includes all three modules.
   - `shared/build.gradle.kts` configured with multiplatform targets, dependencies, and source sets (`commonMain`, `commonTest`, `androidMain`, `desktopMain`).
   - Platform modules have basic Gradle setup linking to `shared`.

4. Folder skeleton:
   - `shared/core/`, `shared/domain/`, `shared/ui/`, `shared/util/`
   - `androidApp/src/main/kotlin/...` and `desktopApp/src/main/kotlin/...` for platform-specific code.

5. No actual feature implementation yet, just project scaffolding, Gradle files, and folders in place.

Please generate a complete implementation plan for Claude to create this KMP project with all modules, Gradle configs, and folder skeleton ready for development.