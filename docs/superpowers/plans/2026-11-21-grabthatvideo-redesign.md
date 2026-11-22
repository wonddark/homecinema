# GrabThatVideo Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the per-platform HomeCinema UI with one shared, themed "GrabThatVideo" UI (Home + History) whose every designed action works, backed by a real format catalog, a persistent 4-slot download queue, and a persistent history.

**Architecture:** All UI moves to `shared/src/commonMain/kotlin/com/homecinema/ui/` and is rendered by one root `App()` composable from both entry points. Pure logic (format catalog, yt-dlp arg building, JSON parsing, failure mapping, history grouping) lives in plain `commonMain` functions so it is unit-tested in `commonTest`. Platform specifics (process control, file actions, storage dir, ffmpeg) stay behind `expect/actual` or the `DownloaderEngine` interface.

**Tech Stack:** Kotlin 2.0.21, Compose Multiplatform 1.7.3 (+ `compose.components.resources`), kotlinx-coroutines 1.9.0, kotlinx-serialization 1.7.3, youtubedl-android 0.18.1 (+ `ffmpeg` module), Coil 3.0.4 (+ `coil-network-ktor3`), Ktor client 3.0.1, Geist font, Lucide icons.

**Spec:** `docs/superpowers/specs/2026-11-21-grabthatvideo-redesign-design.md` (read it first — it holds the color table, copy, and per-state visuals). Visual reference: Pencil canvas `~/.pencil/documents/89ef9bde-8382-46be-bc30-6d07158b9dc5/untitled.pen` (read via Pencil MCP only; canvas wins for visuals).

## Global Constraints

- Package `com.homecinema`, module names and `applicationId = "com.homecinema.android"` unchanged. Only display name / wordmark / window title / notification title become `GrabThatVideo`.
- One shared UI in `commonMain`; delete `shared/src/{androidMain,desktopMain}/kotlin/com/homecinema/ui/DownloadScreen.kt` and `VideoItem.*.kt`.
- Max **4** concurrent downloads.
- Video format arg: `-f "<videoId>+bestaudio/<videoId>/best"` + `--merge-output-format mp4`. Audio: `-f <audioId>`, no conversion. Always `--continue --newline --no-mtime --no-playlist`.
- Output template: `%(title)s [%(id)s] <qualityLabel>.%(ext)s`.
- Quality labels: height 2160 → `4K`, 1440 → `2K`, else `<h>p`. Default chip: highest ≤ 1080, else lowest.
- Audio option only when an audio-only format exists; label `<EXT uppercase> · Audio`.
- Hidden (not built): See all, Details, Select, Sort by.
- Theme preference: System (default) / Dark / Light / Ocean / Volt, persisted in `settings.json`. System → Dark or Light.
- Storage dir: Android `context.filesDir`; Desktop `~/.grabthatvideo/`. Files: `history.json`, `queue.json`, `settings.json`, `thumbnails/<taskId>.jpg`.
- Desktop download dir: `~/Downloads/GrabThatVideo`. Android: public Downloads (unchanged).
- Restart: DOWNLOADING → PAUSED; QUEUED/PAUSED/FAILED keep state.
- History: no size cap; missing files are kept and flagged, never dropped.
- Remove (File menu) deletes file + thumbnail + entry after confirm; for missing files only entry + thumbnail. Clear history keeps files.
- Desktop window 420×860, min width 360; content max width 480, centered.
- New JSON fields must have defaults so existing serialized data still decodes.

## Review Focus

1. **App killed mid-download** → task restored as PAUSED, its `.part` file is *not* removed by the orphan sweep, Resume continues from the partial file. (Task 5 test `restore keeps part of paused task`.)
2. **Same video + same quality downloaded twice** → yt-dlp reports "already downloaded"; task completes with the existing path, and History holds one entry for that path (newest replaces older). (Task 4 test `add replaces entry with same filePath`.)
3. **Unknown file size** (`filesize` and `filesize_approx` null) → meta omits the size segment (`MP4 · 720p`), totals sum only known sizes. (Task 9 test `meta omits unknown size`.)
4. **Paste with empty or non-URL clipboard** → empty: nothing happens; non-URL text: placed in field, fetch runs and shows Fetch failed (not a crash). (Task 7 test `paste empty clipboard does nothing`.)
5. **Very long titles / CJK titles** → max 2 lines with ellipsis in every card and row; action buttons never pushed off-screen. (Task 10 manual check list item.)

---

## File Structure

```
shared/src/commonMain/kotlin/com/homecinema/
  domain/            VideoInfo, VideoFormat, DownloadTask, DownloadState, MediaKind, HistoryEntry
  core/
    Platform.kt               expect: platformDownloadDir, appStorageDir, fileExists, revealInFileBrowser (existing+new)
    PlatformActions.kt        interface PlatformActions (openFile, shareFile, openFolder, deleteFile) + expect fun rememberPlatformActions()
    format/FormatCatalog.kt   buildFormatOptions(...)
    downloader/DownloaderEngine.kt, YtDlpArgs.kt, VideoInfoParser.kt, FailureReason.kt
    storage/JsonFileStore.kt  FileIO interface + JsonFileStore<T>
    history/HistoryRepository.kt, HistoryGrouping.kt
    settings/SettingsRepository.kt (ThemePreference)
    usecase/DownloadQueueManager.kt (rewritten), ThumbnailSaver.kt
  ui/
    App.kt
    theme/ AppColors.kt, Palettes.kt, AppTypography.kt, AppShapes.kt, AppTheme.kt
    components/ Wordmark, GlassSurface, RoundIconButton, PillButton, UrlInput, FormatToggle,
                QualityChips, DownloadProgressCard, HistoryRow, EmptyState, SectionLabel, Thumbnail, AppIcons
    home/ HomeViewModel.kt, HomeState.kt, HomeScreen.kt
    history/ HistoryViewModel.kt, HistoryState.kt, HistoryScreen.kt, FileSheet.kt, MoreMenu.kt, ConfirmDialog.kt
shared/src/commonMain/composeResources/font/ Geist-{Regular,Medium,SemiBold,Bold}.ttf
shared/src/commonMain/composeResources/drawable/ lucide_*.xml
```

Delete: `core/usecase/SmartFormatSelector.kt`, `core/usecase/DownloadHistory.kt`, `core/usecase/BatchDownloadHelper.kt`, `core/usecase/RetryPolicy.kt`, `core/usecase/DownloadViewModel.kt`, `ui/state/*`, all old `ui/components/*`, `ui/util/PlatformUtils*`, old drawables (`check_circle_24px`, `delete_24px`, `folder_open_24px`), and their tests. Each deletion happens in the task that replaces the code (noted per task).

---

### Task 1: Domain model + JSON parsing

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/domain/{VideoInfo,VideoFormat,DownloadTask,DownloadState}.kt`
- Create: `domain/MediaKind.kt`, `domain/HistoryEntry.kt`, `core/downloader/VideoInfoParser.kt`
- Modify: `core/downloader/DownloaderEngineAndroid.kt`, `DownloaderEngineDesktop.kt` (use the parser; delete their inline parsing)
- Test: `shared/src/commonTest/kotlin/com/homecinema/VideoInfoParserTest.kt`, `DomainSerializationTest.kt`

**Interfaces — Produces:**
```kotlin
enum class MediaKind { VIDEO, AUDIO }
enum class DownloadState { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }
data class VideoInfo(id: String, title: String, duration: Long, formats: List<VideoFormat>,
    thumbnailUrl: String? = null, extractor: String? = null, uploader: String? = null)
data class VideoFormat(formatId: String, resolution: String, size: Long? = null,
    acodec: String? = null, vcodec: String? = null, filesize_approx: Long? = null,
    height: Int? = null, ext: String? = null, abr: Double? = null) {
  val hasVideo: Boolean  // vcodec != null && vcodec != "none"
  val hasAudio: Boolean  // acodec != null && acodec != "none"
  val bestSize: Long?    // size ?: filesize_approx
}
data class DownloadTask(id, url, videoInfo: VideoInfo? = null, format: String? = null,
    state = QUEUED, progress = 0f, outputPath: String? = null,
    kind: MediaKind = VIDEO, qualityLabel: String = "", sizeBytes: Long? = null,
    addedAt: Long = 0L, errorReason: String? = null)
data class HistoryEntry(id: String, title: String, url: String, filePath: String,
    kind: MediaKind, ext: String, qualityLabel: String, sizeBytes: Long?, durationSeconds: Long,
    extractor: String?, thumbnailPath: String?, completedAt: Long, fileMissing: Boolean = false)
fun parseVideoInfo(json: String, fallbackId: String): VideoInfo
```
All `@Serializable`. `fileMissing` is `@Transient` (computed at load).

- [ ] **Step 1: Write failing tests**
  - `parseVideoInfo maps youtube dump` — fixture JSON with `id,title,duration,thumbnail,extractor_key:"Youtube",uploader`, formats `{format_id:"137",height:1080,ext:"mp4",vcodec:"avc1",acodec:"none",filesize:100}`, `{format_id:"140",ext:"m4a",vcodec:"none",acodec:"mp4a",abr:129.5}`. Assert `thumbnailUrl`, `extractor == "YouTube"` (map `Youtube`→`YouTube`, else `extractor_key` as-is), formats[0].hasVideo && !hasAudio, formats[1].abr == 129.5.
  - `parseVideoInfo tolerates missing fields` — `{"title":"x"}` → id == fallbackId, formats empty, duration 0.
  - `DownloadTask decodes legacy json` — `{"id":"a","url":"u","state":"QUEUED"}` decodes; `kind == VIDEO`, `addedAt == 0`.
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*VideoInfoParserTest*' --tests '*DomainSerializationTest*'` → FAIL (unresolved).
- [ ] **Step 3: Implement** the model changes and `parseVideoInfo` (Json `ignoreUnknownKeys = true`, `duration` may be a float → `toDouble().toLong()`). Point both engines' `extractInfo` at it. Update `MockDownloaderEngine` to compile.
- [ ] **Step 4: Run** same command → PASS; `./gradlew :shared:compileKotlinDesktop :shared:compileDebugKotlinAndroid` → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: extend domain model and share yt-dlp json parser`

---

### Task 2: Format catalog

**Files:**
- Create: `core/format/FormatCatalog.kt`
- Delete: `core/usecase/SmartFormatSelector.kt` (and its usages)
- Test: `commonTest/.../FormatCatalogTest.kt`

**Interfaces — Produces:**
```kotlin
data class QualityOption(val label: String, val height: Int, val formatId: String, val sizeBytes: Long?)
data class AudioOption(val formatId: String, val ext: String, val label: String /* "M4A · Audio" */, val abrKbps: Int?, val sizeBytes: Long?)
data class FormatOptions(val qualities: List<QualityOption>, val audio: AudioOption?, val defaultQuality: QualityOption?, val needsFfmpegHint: Boolean)
fun buildFormatOptions(formats: List<VideoFormat>, ffmpegAvailable: Boolean): FormatOptions
fun qualityLabel(height: Int): String
```
Rules: one `QualityOption` per distinct height, highest first; per height choose the format with largest `bestSize` (prefer `ext == "mp4"` on ties). With `ffmpegAvailable = false` consider only formats with `hasVideo && hasAudio`; if none, `qualities = [QualityOption("Best", 0, "best", null)]` and `needsFfmpegHint = true`. Audio = audio-only format with highest `abr`. Default = highest ≤ 1080 else lowest.

- [ ] **Step 1: Failing tests:** `labels 2160 as 4K and 1440 as 2K`; `dedupes heights highest first` (heights 1080,720,1080,480 → `["1080p","720p","480p"]`); `default is highest at or below 1080` (2160,1080,720 → 1080p) and `default falls back to lowest` (2160,1440 → 2K); `audio absent without audio-only format`; `audio picks highest abr` (abr 50/129 → 129, label `"M4A · Audio"`); `without ffmpeg only muxed formats` (video-only 1080 + muxed 360 → `["360p"]`); `without ffmpeg and no muxed gives Best` (needsFfmpegHint true, formatId "best").
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*FormatCatalogTest*'` → FAIL.
- [ ] **Step 3: Implement** `FormatCatalog.kt`.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: build quality and audio options from yt-dlp formats`

---

### Task 3: yt-dlp args, failure reasons, engine pause/cancel, ffmpeg

**Files:**
- Create: `core/downloader/YtDlpArgs.kt`, `core/downloader/FailureReason.kt`
- Modify: `core/downloader/DownloaderEngine.kt`, `DownloaderEngineAndroid.kt`, `DownloaderEngineDesktop.kt`, `androidApp/.../HomeCinemaApp.kt`, `gradle/libs.versions.toml`, `shared/build.gradle.kts`, `androidApp/build.gradle.kts`
- Test: `commonTest/.../YtDlpArgsTest.kt`, `FailureReasonTest.kt`

**Interfaces — Produces:**
```kotlin
interface DownloaderEngine {
  val ffmpegAvailable: Boolean
  suspend fun extractInfo(url: String): VideoInfo
  suspend fun download(task: DownloadTask, outputDir: String, onProgress: (Float) -> Unit): String // final path
  suspend fun pause(taskId: String)   // kill process, keep .part
  suspend fun cancel(taskId: String)  // kill process, delete .part files matching "[<videoId>] <qualityLabel>"
}
fun buildDownloadArgs(task: DownloadTask, outputDir: String, ffmpegAvailable: Boolean): List<String> // options only, URL excluded
fun outputTemplate(task: DownloadTask): String // "%(title)s [%(id)s] <qualityLabel>.%(ext)s"
fun parseFinalPath(outputLines: Sequence<String>): String  // Destination / already downloaded / [Merger] (existing logic, moved here)
fun mapFailureReason(message: String?): String
```
`mapFailureReason`: contains `Unable to download`/`timed out`/`Connection`/`Network` → `"Connection lost"`; `Private video`/`unavailable`/`removed` → `"Unavailable"`; `Unsupported URL` → `"Unsupported site"`; else `"Download failed"`.

- [ ] **Step 1: Failing tests:** `video args merge with bestaudio` (format "137", ffmpeg true → contains `-f`,`137+bestaudio/137/best`,`--merge-output-format`,`mp4`,`--continue`,`--no-playlist`); `video args without ffmpeg use format as is` (`-f 18`, no merge flag); `audio args have no merge` (kind AUDIO, `-f 140`); `output template includes id and quality` (`"/dl/%(title)s [%(id)s] 1080p.%(ext)s"`); `parseFinalPath prefers merger line`; `parseFinalPath handles already downloaded`; failure mapping cases above, one assertion each.
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*YtDlpArgsTest*' --tests '*FailureReasonTest*'` → FAIL.
- [ ] **Step 3: Implement** the pure functions, then the engines:
  - Android: `YoutubeDLRequest(url)` + each arg from `buildDownloadArgs`; `pause` = `destroyProcessById`; `ffmpegAvailable = true`. Add `youtubedl-android-ffmpeg = { module = "io.github.junkfood02.youtubedl-android:ffmpeg", version.ref = "youtubedl-android" }` to the catalog and both Android deps; call `FFmpeg.getInstance().init(this)` after `YoutubeDL.getInstance().init(this)`.
  - Desktop: `ffmpegAvailable` = `ProcessBuilder("ffmpeg","-version")` exits 0 (lazy, once); keep process map; `pause` = `destroy()`; track progress from `--newline` output as today.
  - Both: throw an exception carrying yt-dlp's last error line on non-zero exit.
- [ ] **Step 4: Run** tests → PASS; `./gradlew :androidApp:assembleDebug :desktopApp:compileKotlinDesktop` → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: format-aware yt-dlp args, pause support and ffmpeg merging`

---

### Task 4: Storage, history and settings repositories

**Files:**
- Create: `core/storage/JsonFileStore.kt`, `core/history/HistoryRepository.kt`, `core/settings/SettingsRepository.kt`
- Modify: `core/Platform.kt` (+ actuals): add `expect fun appStorageDir(): String`, `expect fun fileExists(path: String): Boolean`, `expect fun deleteLocalFile(path: String): Boolean`; change Desktop `platformDownloadDir()` to `~/Downloads/GrabThatVideo` (create if missing)
- Delete: `core/usecase/DownloadHistory.kt`
- Test: `commonTest/.../HistoryRepositoryTest.kt`, `SettingsRepositoryTest.kt`, `FakeFileIO.kt`

**Interfaces — Produces:**
```kotlin
interface FileIO { fun read(path: String): String?; fun write(path: String, text: String); fun exists(path: String): Boolean; fun delete(path: String): Boolean }
fun platformFileIO(): FileIO   // expect/actual, java.io.File on both
class JsonFileStore<T>(io: FileIO, path: String, serializer: KSerializer<T>, default: T) { fun load(): T; fun save(value: T) }
class HistoryRepository(store: JsonFileStore<List<HistoryEntry>>, io: FileIO) {
  val entries: StateFlow<List<HistoryEntry>>   // newest first, fileMissing computed via io.exists(filePath)
  fun add(entry: HistoryEntry)                 // replaces any entry with same filePath
  fun remove(id: String)                       // entry + thumbnail file
  fun clear()                                  // entries + thumbnails; media files kept
  fun refreshMissing()
}
enum class ThemePreference { SYSTEM, DARK, LIGHT, OCEAN, VOLT }
@Serializable data class AppSettings(val theme: ThemePreference = ThemePreference.SYSTEM)
class SettingsRepository(store: JsonFileStore<AppSettings>) { val settings: StateFlow<AppSettings>; fun setTheme(t: ThemePreference) }
```
Saves are synchronous on the caller (writes are tiny). Corrupt JSON → `default`.

- [ ] **Step 1: Failing tests:** `add persists newest first`; `add replaces entry with same filePath` (Review Focus 2); `missing file is flagged not dropped` (FakeFileIO lacks the path → `fileMissing == true`, size 1); `remove deletes thumbnail but not media`; `clear keeps media files`; `corrupt json loads empty`; `theme persists across instances`.
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*RepositoryTest*'` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: persistent history and settings repositories`

---

### Task 5: Download queue rewrite

**Files:**
- Modify (rewrite): `core/usecase/DownloadQueueManager.kt`
- Create: `core/usecase/ThumbnailSaver.kt`
- Modify: `gradle/libs.versions.toml`, `shared/build.gradle.kts` (Ktor client core 3.0.1; `ktor-client-okhttp` androidMain, `ktor-client-java` desktopMain)
- Delete: `core/usecase/{BatchDownloadHelper,RetryPolicy}.kt` and their tests
- Test: rewrite `commonTest/.../DownloadQueueManagerTest.kt`, update `MockDownloaderEngine.kt`

**Interfaces:**
- Consumes: `DownloaderEngine` (Task 3), `HistoryRepository`, `JsonFileStore`, `FileIO` (Task 4), `DownloadTask`/`HistoryEntry` (Task 1).
- Produces:
```kotlin
fun interface ThumbnailSaver { suspend fun save(url: String, taskId: String): String? }  // returns local path or null
class KtorThumbnailSaver(client: HttpClient, io: FileIO, dir: String) : ThumbnailSaver
class DownloadQueueManager(
  engine: DownloaderEngine, history: HistoryRepository, store: JsonFileStore<List<DownloadTask>>,
  thumbnails: ThumbnailSaver, io: FileIO, downloadDir: String,
  scope: CoroutineScope, clock: () -> Long, maxConcurrent: Int = 4) {
  val tasks: StateFlow<List<DownloadTask>>   // non-completed only, insertion order
  fun restore()                              // load queue.json, DOWNLOADING→PAUSED, sweep orphan .part files, then pump()
  fun enqueue(task: DownloadTask)            // sets addedAt = clock(), pump()
  fun pause(id: String); fun resume(id: String); fun retry(id: String)  // resume/retry → QUEUED, clear errorReason, pump()
  fun remove(id: String)                     // engine.cancel + drop
}
```
`pump()` starts QUEUED tasks while running < maxConcurrent. On success: build `HistoryEntry` (ext from path, `completedAt = clock()`, thumbnail via `thumbnails.save`), `history.add`, drop task. On failure: FAILED + `mapFailureReason`. A pause-triggered exception must not mark FAILED (track ids being paused). Persist the list on every state change except progress ticks (persist progress at most when state changes).

- [ ] **Step 1: Failing tests** (MockDownloaderEngine gains a `CompletableDeferred` gate per task so tests control completion):
  `runs at most 4 at once` (enqueue 6 → 4 DOWNLOADING, 2 QUEUED); `completion moves task to history and starts next`; `pause keeps task paused not failed`; `resume requeues and passes task again`; `retry clears error reason`; `failure maps reason` (engine throws "Unable to download webpage" → `errorReason == "Connection lost"`); `remove cancels engine`; `restore turns downloading into paused`; `restore keeps part of paused task` (Review Focus 1: FakeFileIO has `dl/T [abc] 1080p.mp4.part` referenced by a paused task and `dl/Other [zzz] 720p.mp4.part` unreferenced → only the second is deleted); `uses selected format id` (engine receives task.format "137").
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*DownloadQueueManagerTest*'` → FAIL.
- [ ] **Step 3: Implement.** Orphan sweep needs a directory listing: add `fun list(dir: String): List<String>` to `FileIO` (and the fake).
- [ ] **Step 4: Run** → PASS; full `./gradlew :shared:desktopTest` → PASS.
- [ ] **Step 5: Commit** `feat: persistent concurrent download queue with pause and resume`

---

### Task 6: Theme, fonts and icons

**Files:**
- Create: `ui/theme/{AppColors,Palettes,AppTypography,AppShapes}.kt`; rewrite `ui/theme/AppTheme.kt`; delete `ui/theme/{Colors,Typography,Spacing}.kt`
- Create: `composeResources/font/Geist-{Regular,Medium,SemiBold,Bold}.ttf`, `composeResources/drawable/lucide_*.xml`, `ui/components/AppIcons.kt`
- Delete: old drawables `check_circle_24px.xml`, `delete_24px.xml`, `folder_open_24px.xml`
- Test: `commonTest/.../PaletteTest.kt`

**Interfaces — Produces:**
```kotlin
@Immutable data class AppColors(/* every token in spec "Color tokens" table, camelCase, as Color */ val isDark: Boolean)
object Palettes { val Dark: AppColors; val Light: AppColors; val Ocean: AppColors; val Volt: AppColors }
fun resolvePalette(pref: ThemePreference, systemDark: Boolean): AppColors
val LocalAppColors: ProvidableCompositionLocal<AppColors>
@Composable fun AppTheme(preference: ThemePreference, content: @Composable () -> Unit) // provides LocalAppColors, Geist typography, maps to MaterialTheme colorScheme
@Composable fun Modifier.appBackground(): Modifier   // background + 2 radial glows from spec
object AppIcons { val Hook: DrawableResource; ... }  // one entry per Lucide icon listed in the spec + `alert`
```
Values: copy hex values from the spec table exactly. Derived tokens: `accentSoft = accent.copy(alpha=0.12f)`, `accentBorder = accent.copy(alpha=0.40f)`, `errorSoft = 0.14f`, `errorBorder = 0.30f`.

- [ ] **Step 1: Failing tests:** `system resolves to dark or light`; `volt onAccent is dark` (`Palettes.Volt.onAccent == Color(0xFF0B0B0A)`); `every palette distinct accent`; `warning light differs` (`Palettes.Light.warning == Color(0xFFC77D0A)`).
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*PaletteTest*'` → FAIL.
- [ ] **Step 3: Implement** tokens and theme. Fonts: download Geist v1.4.01 TTFs from the `vercel/geist-font` GitHub release into `composeResources/font/`. Icons: take SVGs from npm `lucide-static@0.460.0` (`icons/<name>.svg`) and convert to Android vector XML (24×24 viewport, `strokeColor="#FF000000"`, `strokeWidth="2"`, round caps/joins, `fillColor` transparent); a small conversion script in the scratchpad turning `circle/rect/line/polyline/polygon` into path data is acceptable. Tint at use site.
- [ ] **Step 4: Run** tests → PASS; `./gradlew :shared:generateComposeResClass :shared:compileKotlinDesktop` → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: GrabThatVideo theme tokens, Geist font and Lucide icons`

---

### Task 7: Home view-model

**Files:**
- Create: `ui/home/HomeState.kt`, `ui/home/HomeViewModel.kt`
- Delete: `core/usecase/DownloadViewModel.kt`, `ui/state/*`
- Test: `commonTest/.../HomeViewModelTest.kt`

**Interfaces — Produces:**
```kotlin
sealed interface FetchState {
  data object Idle : FetchState
  data class Fetching(val url: String) : FetchState
  data class Failed(val url: String) : FetchState
  data class Ready(val url: String, val info: VideoInfo, val options: FormatOptions,
                   val kind: MediaKind, val selected: QualityOption?) : FetchState
}
data class HomeState(val input: String = "", val fetch: FetchState = FetchState.Idle, val tasks: List<DownloadTask> = emptyList())
class HomeViewModel(engine: DownloaderEngine, queue: DownloadQueueManager, scope: CoroutineScope, idGen: () -> String) {
  val state: StateFlow<HomeState>
  fun onInputChange(text: String); fun onSubmit(); fun onPaste(clipboard: String?); fun onClear()
  fun onRetryFetch(); fun onSelectKind(kind: MediaKind); fun onSelectQuality(q: QualityOption); fun onDownload()
  fun onPause(id: String); fun onResume(id: String); fun onRetry(id: String); fun onRemove(id: String)
}
```
`onClear` cancels the in-flight fetch job. `onDownload` builds the task (`format`, `kind`, `qualityLabel` = chip label or `"<abr>kbps"`, `sizeBytes`), enqueues, resets to Idle with empty input. Kind defaults to VIDEO when qualities exist, else AUDIO.

- [ ] **Step 1: Failing tests:** `submit goes fetching then ready`; `extract failure goes failed`; `retry fetch reuses url`; `clear during fetch returns idle and ignores late result`; `paste empty clipboard does nothing` (Review Focus 4, null and `"  "`); `paste text fills input and fetches`; `download enqueues selected format and resets` (queue receives task with `format == "137"`, `qualityLabel == "1080p"`; state Idle, input ""); `audio only source defaults to audio kind`.
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*HomeViewModelTest*'` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: home view-model with fetch state machine`

---

### Task 8: Shared components + Home screen

**Files:**
- Create: `ui/components/{Wordmark,GlassSurface,RoundIconButton,PillButton,UrlInput,FormatToggle,QualityChips,DownloadProgressCard,EmptyState,SectionLabel,Thumbnail}.kt`, `ui/home/HomeScreen.kt`
- Delete: old `ui/components/{AppButton,AppProgressBar,AppTextField,FormatSelector,VideoItem}.kt`
- Modify: `gradle/libs.versions.toml`, `shared/build.gradle.kts` (Coil 3.0.4: `io.coil-kt.coil3:coil-compose`, `coil-network-ktor3`)

**Interfaces:**
- Consumes: `HomeState`, `HomeViewModel` (Task 7), `LocalAppColors`, `AppIcons`, `qualityLabel` (Tasks 2, 6).
- Produces: `@Composable fun HomeScreen(state: HomeState, actions: HomeActions, onOpenHistory: () -> Unit)` where `HomeActions` is a data class of the view-model's lambdas; `@Composable fun Thumbnail(model: Any?, durationSeconds: Long?, kind: MediaKind, width: Dp, height: Dp, dimmed: Float = 1f)` (reused by Task 10); `EmptyState(title, body, showSteps: Boolean)`; `GlassSurface`, `RoundIconButton(icon, tint, onClick, contentDescription, active = false)`.

Visual source per state (screenshot each frame from Pencil while building; values in the spec): 1·Empty `FAKZX`, 2·Fetching `XzV7d`, 2b·Fetch failed `X8ycb`, 3·Ready `pyyQD`, 4·Downloading `AiOXm`. Paste button reads `LocalClipboardManager.current.getText()?.text`. Duration badge format `m:ss` / `h:mm:ss`. Desktop hint row "Install ffmpeg for higher qualities" when `options.needsFfmpegHint`. `DownloadProgressCard` covers DOWNLOADING (meta `"<extractor> · <quality> · <size>"`, or `"<extractor> · <quality> · Starting…"` while progress == 0; accent border only on the first active card), QUEUED (meta ends `Queued`, Remove button), PAUSED, FAILED (per spec). In-progress thumbnails load the remote `thumbnailUrl`. Hidden: "See all".

- [ ] **Step 1: Add** Compose `@Preview` functions (desktop `androidx.compose.desktop.ui.tooling.preview.Preview`) for HomeScreen in each FetchState × Dark and Light, under `desktopMain/.../ui/preview/HomePreviews.kt`.
- [ ] **Step 2: Build** `./gradlew :shared:compileKotlinDesktop :shared:compileDebugKotlinAndroid` → BUILD SUCCESSFUL.
- [ ] **Step 3: Visual check** — render previews (IDE) or run desktop after Task 11; compare against Pencil screenshots of the five frames; card radii, colors and copy match the spec.
- [ ] **Step 4: Commit** `feat: shared components and home screen`

---

### Task 9: History view-model

**Files:**
- Create: `core/history/HistoryGrouping.kt`, `ui/history/HistoryState.kt`, `ui/history/HistoryViewModel.kt`
- Test: `commonTest/.../HistoryGroupingTest.kt`, `HistoryViewModelTest.kt`

**Interfaces — Produces:**
```kotlin
enum class HistoryGroup(val label: String) { TODAY("TODAY"), YESTERDAY("YESTERDAY"), LAST_WEEK("LAST WEEK"), EARLIER("EARLIER") }
fun groupOf(completedAt: Long, now: Long, zone: TimeZone): HistoryGroup   // calendar days in zone; 2–7 days → LAST_WEEK
fun formatSize(bytes: Long): String          // "248 MB", "1.1 GB", "9.4 MB", "860 KB"
fun historyMeta(e: HistoryEntry): String     // "MP4 · 1080p · 248 MB" / "M4A · 128kbps · 9.4 MB"; size omitted if null
fun sheetTimestamp(completedAt: Long, now: Long, zone: TimeZone): String // "Today, 18:42" / "Yesterday, 09:05" / "12 Nov, 18:42"
enum class HistoryFilter { ALL, VIDEO, AUDIO }
data class HistoryState(query: String, filter: HistoryFilter, counts: Map<HistoryFilter, Int>,
    summary: String /* "14 files · 2.3 GB" */, groups: List<Pair<HistoryGroup, List<HistoryEntry>>>,
    sheetFor: HistoryEntry?, moreMenuOpen: Boolean, confirm: ConfirmRequest?)
sealed interface ConfirmRequest { data class DeleteFile(val entry: HistoryEntry) : ConfirmRequest; data object ClearHistory : ConfirmRequest }
class HistoryViewModel(history: HistoryRepository, settings: SettingsRepository, actions: PlatformActions, downloadDir: String, scope: CoroutineScope, clock: () -> Long, zone: TimeZone) {
  val state: StateFlow<HistoryState>
  fun onQuery(q: String); fun onFilter(f: HistoryFilter); fun onRowClick(e: HistoryEntry)  // missing → open sheet, else play
  fun onOpenSheet(e: HistoryEntry); fun onDismissSheet(); fun onPlay(); fun onShare(); fun onShowInFolder()
  fun onRemove()     // missing → history.remove directly; else confirm DeleteFile
  fun onConfirm(); fun onDismissConfirm()
  fun onToggleMore(); fun onOpenDownloadsFolder(); fun onClearHistory(); fun onTheme(t: ThemePreference)
}
```
Uses `kotlinx-datetime` 0.6.1 (add to catalog, commonMain). `PlatformActions` is defined in Task 10; define the interface here and the actuals there.

- [ ] **Step 1: Failing tests:** grouping across midnight (`23:59 yesterday` → YESTERDAY, `00:01 today` → TODAY in zone `Europe/Madrid`); `8 days ago is EARLIER`; `formatSize` four cases above; `meta omits unknown size` (Review Focus 3); `counts follow search` (query "pasta" → ALL 1, VIDEO 1, AUDIO 0); `summary sums known sizes only`; `missing row click opens sheet`; `remove missing skips confirm` (no file delete called); `confirm delete removes file and entry` (fake actions records `deleteFile(path)`).
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*History*Test*'` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: history view-model with grouping, search and filters`

---

### Task 10: Platform actions + History screen, sheet, menu, dialogs

**Files:**
- Create: `core/PlatformActions.kt` + `androidMain/.../core/PlatformActions.android.kt`, `desktopMain/.../core/PlatformActions.desktop.kt`
- Create: `ui/components/HistoryRow.kt`, `ui/history/{HistoryScreen,FileSheet,MoreMenu,ConfirmDialog}.kt`
- Modify: `androidApp/src/main/res/xml/file_paths.xml` (ensure Downloads path is shared)

**Interfaces:**
- Consumes: `HistoryState`, `HistoryViewModel` (Task 9), components from Task 8.
- Produces:
```kotlin
interface PlatformActions {
  fun openFile(path: String); fun shareFile(path: String): ShareResult   // ShareResult { SHARED, PATH_COPIED }
  fun openFolder(path: String); fun revealInFolder(path: String)
  suspend fun deleteFile(path: String): Boolean
}
@Composable expect fun rememberPlatformActions(): PlatformActions
@Composable fun HistoryScreen(state: HistoryState, actions: HistoryActions, onBack: () -> Unit)
```
- Android: `openFile` = `ACTION_VIEW` with FileProvider URI + MIME from extension; `shareFile` = `ACTION_SEND` chooser; `deleteFile` = `File.delete()`, and if that fails on API 30+, find the MediaStore `_ID` by path and launch `MediaStore.createDeleteRequest` through an `ActivityResultLauncher` held by `rememberPlatformActions` (suspend until the result).
- Desktop: `openFile` = `Desktop.open`; `shareFile` copies the path to the system clipboard → `PATH_COPIED` (UI shows snackbar "Path copied"); `openFolder` = `Desktop.open(dir)`; `deleteFile` = `File.delete()`.

Visual source: 5·History `lJ8vd`, 6·File menu `G9NvYQ`, 7·More menu `LUvRg`, missing row in Volt History `sA8H6` ("Street food tour in Tokyo"). More menu items: Theme (palette icon, current value, chevron → inline list System/Dark/Light/Ocean/Volt), Open downloads folder, divider, Clear history. File sheet: Play, Share, Show in folder, divider, Remove (Details hidden); for `fileMissing` the first three are disabled at 40% opacity. Confirm dialog copy: DeleteFile → "Delete this file?" / "It will be removed from your device." / "Delete"; ClearHistory → "Clear history?" / "Your files stay on your device." / "Clear". History empty state: "No downloads yet" / "Finished downloads will appear here." (no step pills).

- [ ] **Step 1: Add** desktop previews for HistoryScreen (populated, empty, sheet open, menu open, missing row) × Dark and Volt in `desktopMain/.../ui/preview/HistoryPreviews.kt`.
- [ ] **Step 2: Build** `./gradlew :shared:compileKotlinDesktop :shared:compileDebugKotlinAndroid` → BUILD SUCCESSFUL.
- [ ] **Step 3: Visual check** against the four Pencil frames; include Review Focus 5: a 120-char title and a CJK title in previews wrap to 2 lines with ellipsis and the ⋮ button stays visible.
- [ ] **Step 4: Commit** `feat: history screen with file sheet, more menu and platform actions`

---

### Task 11: App root, entry points, branding

**Files:**
- Create: `ui/App.kt`
- Modify: `androidApp/.../MainActivity.kt`, `DownloadService.kt`, `HomeCinemaApp.kt`, `AndroidManifest.xml` (`android:label="GrabThatVideo"`), `desktopApp/.../Main.kt`, `desktopApp/build.gradle.kts` (`description`, keep `packageName`)
- Delete: `shared/src/{androidMain,desktopMain}/kotlin/com/homecinema/ui/DownloadScreen.kt`, `ui/components/VideoItem.*.kt`, `commonMain/.../ui/components/VideoItem.kt`, `ui/util/PlatformUtils*`

**Interfaces:**
- Consumes: everything above.
- Produces: `class AppGraph(engine, storageDir, downloadDir, scope)` (commonMain, builds FileIO, stores, repositories, queue, HttpClient, view-models; calls `queue.restore()` once) and `@Composable fun App(graph: AppGraph)` holding `var screen by remember { mutableStateOf(Screen.HOME) }`, `AppTheme(settings.theme)`, `Modifier.appBackground()`, `widthIn(max = 480.dp)` centered column, status-bar insets (`WindowInsets.safeDrawing`). Android `BackHandler` on History → Home (androidMain `expect/actual` `PlatformBackHandler(enabled, onBack)`; desktop no-op).

`DownloadService` owns the single `AppGraph` (companion, created in `onCreate`) — replaces today's `engine`/`queueManager` statics; notification title "GrabThatVideo", text counts include PAUSED. Desktop `Main.kt`: window title "GrabThatVideo", `rememberWindowState(size = DpSize(420.dp, 860.dp))`, `window.minimumSize = Dimension(360, 600)`.

- [ ] **Step 1: Build** `./gradlew :androidApp:assembleDebug :desktopApp:compileKotlinDesktop` → BUILD SUCCESSFUL; `./gradlew :shared:desktopTest` → all PASS.
- [ ] **Step 2: Grep** `grep -rn "HomeCinema\"" androidApp desktopApp shared/src` → only class names/packages, no user-visible strings.
- [ ] **Step 3: Commit** `feat: wire shared GrabThatVideo app into Android and Desktop`

---

### Task 12: End-to-end verification

**Files:** none (fixes go back to the owning task's files).

- [ ] **Step 1: Desktop run** `./gradlew :desktopApp:run`; with a public YouTube link walk states 1 → 2 → 3 → 4, pick 720p, download; pause at ~20%, quit app, relaunch → task shows Paused, resume completes; file in `~/Downloads/GrabThatVideo` named `Title [id] 720p.mp4` with audio.
- [ ] **Step 2: Invalid link** `https://vimeo.com/private/88213` → Fetch failed; Try again re-fetches.
- [ ] **Step 3: History** — row appears under TODAY with local thumbnail; Play opens file; Share → "Path copied"; Show in folder; rename the file on disk, reopen History → missing row; Remove on missing row removes without dialog; Remove on a normal row asks then deletes file; Clear history keeps files.
- [ ] **Step 4: Themes** — switch System/Dark/Light/Ocean/Volt from the More menu; restart keeps choice; compare each Home/History state with the matching Pencil frame row (y=0 Dark, 1000 Light, 2000 Ocean, 3000 Volt).
- [ ] **Step 5: Android** `./gradlew :androidApp:installDebug`; repeat Steps 1–3 on a device/emulator (API 30+), including 4 simultaneous downloads (5th stays Queued), audio-only download (M4A), back button from History, and deleting a file downloaded before a reinstall (system prompt appears).
- [ ] **Step 6: Commit** any fixes with `fix:` messages; final `./gradlew :shared:desktopTest :androidApp:assembleDebug` → green.
