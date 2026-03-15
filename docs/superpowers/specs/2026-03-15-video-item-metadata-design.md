# VideoItem Metadata Strip — Design Spec

**Date:** 2026-03-15
**Status:** Approved

## Overview

Update the `VideoItem` component to display a compact metadata strip below the video title, showing file size, source domain, added date, and resolution (or audio sample rate). Replace the raw `"State: ENUM_NAME"` text with a human-readable status string.

## Domain Changes

### `DownloadTask`
Add `addedAt: Long = 0L` (epoch milliseconds). Default of `0L` ensures backwards compatibility with previously serialized data.

```kotlin
@Serializable
data class DownloadTask(
    val id: String,
    val url: String,
    val videoInfo: VideoInfo? = null,
    val format: String? = null,
    val state: DownloadState = DownloadState.QUEUED,
    val progress: Float = 0f,
    val outputPath: String? = null,
    val addedAt: Long = 0L
)
```

### `VideoFormat`
Add `audioSampleRate: Int? = null` for audio-only formats.

```kotlin
@Serializable
data class VideoFormat(
    val formatId: String,      // unchanged — field 1
    val resolution: String,    // unchanged — field 2
    val size: Long? = null,    // unchanged — field 3
    val acodec: String? = null,         // unchanged — field 4
    val vcodec: String? = null,         // unchanged — field 5
    val filesize_approx: Long? = null,  // unchanged — field 6
    val audioSampleRate: Int? = null    // NEW — field 7 (appended last)
)
```
Field order is preserved exactly. `kotlinx-serialization` is name-based so order does not affect deserialization, but the field is appended last to minimise diff noise.

## Shared Helper Logic (commonMain)

A new file `shared/src/commonMain/kotlin/com/homecinema/ui/components/VideoItemHelpers.kt` with pure top-level functions:

### `formatFileSize(bytes: Long?): String?`
- Returns `null` if input is null or 0
- Formats as: `< 1 MB → "X KB"`, `< 1 GB → "X.X MB"`, else `"X.X GB"`

### `extractSource(url: String): String?`
- Parses the hostname from a URL using simple string operations (no URL library)
- Strips `www.` prefix
- Returns `null` if parsing fails
- Examples: `"https://www.youtube.com/watch?v=abc"` → `"youtube.com"`

### `formatAddedDate(epochMillis: Long): String?`
- Returns `null` if `epochMillis == 0L`
- Formats using `kotlinx-datetime` as `"MMM DD YYYY"` (e.g. `"Mar 15 2026"`)

### `formatMediaSpec(format: VideoFormat?): String?`
- Returns `null` if format is null
- If `vcodec` is non-null and not `"none"` → returns `format.resolution` (e.g. `"1080p"`)
- Else if `audioSampleRate` is non-null → returns `"${audioSampleRate} Hz"` (e.g. `"44100 Hz"`)
- Else returns `null`

### `buildMetadataStrip(task: DownloadTask): String`
- Finds the selected format: `task.videoInfo?.formats?.find { it.formatId == task.format }`
- Collects non-null parts: `[formatMediaSpec(...), formatFileSize(...), extractSource(...), formatAddedDate(...)]`
- Joins with `" · "`
- Returns empty string if all parts are null

### `formatState(state: DownloadState): String`
Maps enum to human-readable: `QUEUED → "Queued"`, `DOWNLOADING → "Downloading"`, `COMPLETED → "Completed"`, `FAILED → "Failed"`

## VideoItem UI Changes

Both Android and Desktop implementations are updated identically for the metadata strip content; only layout differs (existing vertical vs horizontal structure is preserved).

**Replace:**
```kotlin
Text(text = "State: ${task.state.name}", style = AppTypography.body)
```

**With two Texts:**
```kotlin
Text(text = formatState(task.state), style = AppTypography.body)
val metadata = buildMetadataStrip(task)
if (metadata.isNotEmpty()) {
    Text(text = metadata, style = AppTypography.caption)
}
```

The metadata strip sits below the state text. If all metadata fields are null/empty (e.g. `videoInfo` not yet fetched, `addedAt` is 0), the strip is omitted entirely.

## Prerequisites

### Add `kotlinx-datetime` dependency

`kotlinx-datetime` is **not** currently in the project. Before implementing `formatAddedDate`, add it:

1. In `gradle/libs.versions.toml`:
   ```toml
   [versions]
   kotlinx-datetime = "0.6.1"

   [libraries]
   kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
   ```

2. In `shared/build.gradle.kts`, inside `commonMain.dependencies`:
   ```kotlin
   implementation(libs.kotlinx.datetime)
   ```

## Assumptions & Constraints

- No new UI components are needed
- `AppTypography.caption` exists in the theme; use `AppColors.textSecondary` for the metadata strip text color
- The `addedAt` field must be set at task creation time in the ViewModel/repository layer (outside scope of this spec, but noted)
- Audio-only detection: `vcodec == null || vcodec == "none"` signals audio-only format

## Out of Scope

- Setting `addedAt` at task creation (repository layer change, separate concern)
- Thumbnail images
- Tooltip/expand interactions on the metadata strip
