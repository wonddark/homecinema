# GrabThatVideo Redesign — Design Spec

**Date:** 2026-11-21
**Status:** Approved
**Design source:** Pencil canvas `untitled.pen` (32 frames: 8 screens × 4 color schemes)

## Intent

Rebuild the HomeCinema app UI to match the new "GrabThatVideo" design, with every action in the design actually working. The design replaces both the current Android and Desktop UIs with one shared, mobile-first UI.

## Goals

- Pixel-faithful implementation of the 8 designed screens/states in all 4 color schemes.
- One shared UI in `commonMain`, used by both Android and Desktop.
- Real behaviour behind every designed control (except the ones explicitly skipped below).
- Download formats driven by what yt-dlp reports for the link (`-F` / `--dump-json` formats), never hardcoded.
- History that survives restarts, with thumbnails.

## Non-goals

- Screens that are not designed: **"See all"**, **"Details"**, **"Select"**, **"Sort by"**. Their entry points are **hidden** until designs exist.
- A desktop-specific (wide) layout. Desktop shows the mobile layout in a centered column.
- Renaming packages, modules or the `applicationId`.
- Audio conversion (no MP3 transcoding).

## Anti-goals

- Showing a quality chip that can't actually be downloaded.
- Video downloads that end up without sound.
- Losing a paused download's progress.

## Decisions (made by the user)

| Topic | Decision |
|---|---|
| Platforms | Shared UI in `commonMain` for Android + Desktop. Per-platform `DownloadScreen`/`VideoItem` actuals are deleted. |
| Themes | Follow system (Dark ↔ Light) by default; user can pick Dark / Light / Ocean / Volt / System from the History "…" menu; choice persisted. |
| Branding | Display name + in-app wordmark become **GrabThatVideo**. Package `com.homecinema`, modules, `applicationId` unchanged. |
| Pause | Pause = stop the yt-dlp process, keep the `.part` file. Resume = re-run with `--continue`. |
| Audio | The audio option appears **only if the link has an audio-only format**. Download that stream as-is (e.g. m4a/webm/opus); no conversion. Label uses the real extension, e.g. `M4A · Audio`. |
| Quality chips | Built from the link's available video heights as listed by yt-dlp. Only real, downloadable qualities are shown. |
| Merging | Add ffmpeg so video-only streams are merged with best audio: `-f <videoFormatId>+bestaudio`. Android: youtubedl-android `ffmpeg` module. Desktop: `ffmpeg` on PATH. |
| Undesigned screens | Skipped; entry points hidden. |
| Persistence | History stored as a JSON file (kotlinx-serialization). Thumbnails via **Coil 3**. |
| Queue persistence | Queue saved to `queue.json`. On restart, tasks that were DOWNLOADING come back as PAUSED; QUEUED/PAUSED/FAILED keep their state. |
| Thumbnails | On completion, the thumbnail is downloaded to app storage (`thumbnails/<taskId>.jpg`) and History uses that local copy. Deleted with Remove / Clear history. In-progress cards use the remote URL. |
| Remove (File menu) | Removes the History entry **and deletes the file**, after a confirm dialog. On Android 11+ when the app no longer owns the file, use a MediaStore delete request (system prompt). |

## Approaches considered

- **A — Rewrite UI in `commonMain`, extend existing core (chosen).** Two screens (Home, History) switched by a simple state holder; no navigation library. Theme tokens in a `CompositionLocal`. Keeps the existing plain-class `DownloadViewModel`, `DownloadQueueManager`, `DownloaderEngine`. Least new infrastructure.
- **B — A + Navigation Compose, lifecycle `ViewModel`, Koin.** More conventional, scales to many screens, but adds three dependencies and rewires both entry points for a two-screen app.
- **C — Reskin existing components.** Rejected: the new layouts share almost nothing with the current ones, and the per-platform screens contradict the shared-UI decision.

## Visual system

### Typography
- Font: **Geist** (bundled as compose resource, weights 400/500/600/700).
- Sizes used: 9, 10, 11, 12, 13, 14, 15, 16, 18, 20. Section labels (`PASTE A LINK`, `TODAY`) are 11/600 uppercase with letter spacing.

### Icons
- **Lucide** icons, bundled as vector drawables in `composeResources/drawable` (only the ones used): `hook` (logo), `arrow-right`, `link-2`, `x`, `youtube`, `instagram`, `twitter`, `facebook`, `globe`, `video`, `music`, `arrow-down-to-line`, `inbox`, `copy`, `clipboard`, `download`, `chevron-right`, `chevron-left`, `loader-circle`, `circle-alert`, `rotate-ccw`, `pause`, `play`, `ellipsis`, `ellipsis-vertical`, `search`, `audio-lines`, `share-2`, `folder-open`, `trash-2`, `palette` (theme entry, see below), `alert`. Unused existing drawables are removed.

### Color tokens

Each scheme is one `AppColors` instance. Token names are mine; values are taken from the canvas.

| Token | Dark | Light | Ocean | Volt |
|---|---|---|---|---|
| `background` | #0E0F13 | #F6F5F2 | #0A0F1C | #0B0B0A |
| `glowTopRight` (radial, center 0.95/0.02) | #FF5A36 @22% | #FFB08F @55% | #3B9EFF @22% | #C6F432 @15% |
| `glowLeft` (radial, center 0/0.55) | #7A4BFF @18% | #E7C8FF @55% | #8A5CFF @18% | #22D3A6 @14% |
| `surface` (cards, glass, 70% alpha) | #181A20B3 | #FFFFFFB8 | #121A2BB3 | #161614B3 |
| `surfaceSheet` | #181A20F2 | #FFFFFFF2 | #121A2BF2 | #161614F2 |
| `surfaceMenu` | #1D1F26 | #FFFFFF | #16203A | #1B1B18 |
| `inputBg` | #0E0F13 | #F6F5F2 | #0A0F1C | #0B0B0A |
| `control` (chips, round buttons, track) | #262932 | #ECEAE5 | #1F2A40 | #262622 |
| `controlActive` / grabber | #3A3E49 | #D4D1CA | #33405A | #3A3A34 |
| `border` | #262932 | #ECEAE5 | #1F2A40 | #262622 |
| `borderMenu` | #2C2F38 | #E4E2DC | #26324C | #2C2C27 |
| `divider` (list rows) | #1C1E25 | #EEECE7 | #172034 | #1C1C19 |
| `skeleton` / badge | #22252D | #E8E6E1 | #1C2638 | #22221E |
| `skeletonAlt` | #1F2128 | #EFEDE8 | #18213A | #1C1C19 |
| `textPrimary` | #F4F5F7 | #16171B | #EEF3FB | #F5F5EF |
| `textSecondary` | #8A8F9C | #6E7280 | #7D8AA3 | #8E8E84 |
| `textTertiary` (placeholder) | #5C6170 | #A3A7B0 | #55627A | #5E5E56 |
| `textMuted` ("Works with") | #6B7080 | #8A8E99 | #66738C | #6E6E65 |
| `textChip` | #C2C6D0 | #3C3F47 | #B8C4D9 | #C8C8BE |
| `accent` | #FF5A36 | #FF5A36 | #3B9EFF | #C6F432 |
| `onAccent` | #FFFFFF | #FFFFFF | #FFFFFF | #0B0B0A |
| `accentSoft` (selected chip bg, 12%) | accent @12% | accent @12% | accent @12% | accent @12% |
| `accentBorder` (active item, 40%) | accent @40% | … | … | … |
| `audioThumbBg` | #1F1512 | #FFEAE3 | #0E2440 | #1E2410 |
| `error` | #FF4D4F | #E5383B | #FF5C6C | #FF5B4A |
| `warning` (missing file) | #F5A524 | #C77D0A | #F5A524 | #F5A524 |
| `errorSoft` (14%) / `errorBorder` (30%) | derived | derived | derived | derived |
| `scrim` (sheet) | #000000A6 | #00000059 | #000000A6 | #000000A6 |
| `scrimMenu` | #00000080 | #00000040 | #00000080 | #00000080 |
| `durationBadge` | #000000B3 | all | all | all |

Background = `background` fill + the two radial glows layered on top (sizes 1.5×0.75 and 1.3×0.7 of the screen).

### Shape & spacing
- Screen padding 20 horizontal; vertical gap between sections 28.
- Radii: card 24, input 16, toggle 14 (inner pill 10), quality chip 18, progress item 18, list thumb 10, round icon buttons fully round, filter chips/step pills 999, sheet top 24, menu 16.
- Glass elements (`surface`) use a background blur where the platform supports it; fall back to the plain translucent fill.

## Screens & behaviour

### Home (states 1 · Empty, 2 · Fetching, 2b · Fetch failed, 3 · Ready, 4 · Downloading)

**Header:** Logo Mark (`hook` in `accent`) + wordmark "GrabThat" (`textPrimary`) "Video" (`accent`), 20/700. Right: pill button **History →** opens History.

**Download card** ("PASTE A LINK"):
- **Idle input:** link icon + placeholder "Paste a video link…" + accent **Paste** button that reads the clipboard into the field and starts fetching. Typing/pasting a URL and pressing IME "Go" also fetches.
- **Works with** row: YouTube, Instagram, X, Facebook, Other icons (decorative, shown in idle and failed states).
- **Fetching:** input border = `accent`, link icon = accent, clear (×) button replaces Paste. Skeleton preview (thumb + two lines) and "⟳ Fetching video info…" status. Clear cancels the fetch and returns to idle.
- **Fetch failed:** input border and icon = `error`. Error block: alert badge, "Couldn't fetch this video", "The link may be private, removed, or from a site we don't support yet." Full-width **Try again** button re-runs the fetch for the same URL. Clear returns to idle.
- **Ready:** preview row (thumbnail with duration badge, title, `"<Extractor> · <Uploader>"`), then:
  - **Format toggle** `MP4 · Video` / `<EXT> · Audio`. The audio segment exists only if the link has an audio-only format; with no audio-only format, the toggle is hidden and Video is implied. If the link has no video formats at all (audio-only source), only Audio is offered.
  - **Quality chips** (Video only): one chip per distinct available video height, highest first, labelled `2160p`→`4K`, `1440p`→`2K`, else `<h>p`. Default selection = highest ≤ 1080p, else the lowest available. Selected chip uses `accentSoft` fill + `accent` border/text.
  - **Download** button (accent, full width): enqueues the task, clears the input back to idle, and starts it.

**Downloads section** (below the card):
- No active tasks → section title "Downloads" + empty state card ("Nothing downloading", body text, and three decorative step pills "Copy link › Paste › Download").
- Active tasks → title "In progress". ("See all" is hidden — non-goal.) One card per non-completed task:
  - **Downloading:** accent-bordered card when it is the first active item, thumbnail, title, meta `"<Extractor> · <quality> · <size>"` (or `Starting…` before size is known), progress bar + `%`, round **Pause** button.
  - **Queued:** same layout, meta ends with `Queued`, round **Remove (×)** button.
  - **Paused:** meta starts with `Paused`, bar and % in `textSecondary`, buttons **Resume (play, accent)** and **Remove (×)**.
  - **Failed:** card border `errorBorder`, thumb at 55% opacity, title in `textSecondary`, meta `Failed · <reason>` in `error`, bar and % in `error`, buttons **Retry (rotate-ccw, accent)** and **Remove (×)**.
- When a task completes it leaves this list and is added to History.

### History (5 · History, 6 · File menu, 7 · More menu)

**Header:** round **Back** button (chevron-left) → Home; centered wordmark with "History" subtitle; round **More (…)** button opens the More menu (button shows active state while open).

**Search field:** "Search downloads"; filters the list by title (case-insensitive, live).

**Filter bar:** chips **All n / Video n / Audio n** (counts reflect the current search); selected chip = accent fill. Right side: `"<n> files · <total size>"` for the current filter.

**Groups:** `TODAY`, `YESTERDAY`, `LAST WEEK` (2–7 days ago), `EARLIER` (older). Newest first. Each row: thumbnail 72×48 with duration badge (video) or `audioThumbBg` tile with `audio-lines` icon (audio); title (max 2 lines); meta `"<EXT> · <quality or bitrate> · <size>"` (video `MP4 · 1080p · 248 MB`, audio `M4A · 128 kbps · 9.4 MB`); **⋮** button opens the File menu. Tapping a row = **Play**.

**Missing file row** (designed in the Volt History frame, "Street food tour in Tokyo"): thumbnail at 35% opacity, title in `textSecondary`, meta replaced by `alert` icon (13) + "File not found · moved or deleted" in `warning` (12/400). In its File menu, Play / Share / Show in folder are disabled (my call); Remove only removes the History entry and thumbnail (nothing to delete). Tapping the row opens the File menu instead of playing.

**History empty state:** (my call — not designed) reuse the Home empty-state card with title "No downloads yet" and body "Finished downloads will appear here."

**File menu (bottom sheet):** grabber, file header (thumb, title, `meta · <Today, 18:42>`), actions:
- **Play** — open the file with the system default app.
- **Share** — Android: share sheet (`ACTION_SEND` via FileProvider). Desktop: copy the file path to the clipboard and show a short "Path copied" snackbar (my call; Desktop has no share sheet).
- **Show in folder** — existing `revealInFileBrowser`.
- **Details** — hidden (non-goal).
- **Remove** (error color) — confirm dialog ("Delete this file? It will be removed from your device."), then removes the entry from History, deletes the file and its local thumbnail.
Scrim tap or swipe down closes.

**More menu (dropdown anchored to … button):**
- **Select** — hidden (non-goal).
- **Sort by** — hidden (non-goal).
- **Theme** — *added by me* to satisfy the theme picker decision: row with `palette` icon, "Theme", current value on the right, chevron; opens a submenu/list: System, Dark, Light, Ocean, Volt.
- **Open downloads folder** — opens the download directory in the system file browser.
- **Clear history** (error color) — confirm dialog, then clears History entries (files are kept; my call).

## Core changes

### Domain
- `VideoInfo`: add `thumbnailUrl: String?`, `extractor: String?` (e.g. "YouTube"), `uploader: String?`.
- `VideoFormat`: add `height: Int?`, `ext: String?`, `abr: Double?` (audio bitrate), `hasVideo`, `hasAudio` derived from `vcodec`/`acodec` (`"none"` = absent). Keep `filesize`/`filesize_approx` for sizes.
- `DownloadState`: add `PAUSED`.
- `DownloadTask`: add `kind: MediaKind` (VIDEO/AUDIO), `qualityLabel: String`, `sizeBytes: Long?`, `addedAt: Long`, `completedAt: Long?`, `errorReason: String?`, `durationSeconds: Long`.

### Engine (`DownloaderEngine`)
- `extractInfo` parses the extra fields from `--dump-json`.
- `download(task, outputDir, onProgress)`:
  - Video: `-f "<videoId>+bestaudio/<videoId>/best"` with `--merge-output-format mp4`.
  - Audio: `-f <audioId>`.
  - Always `--continue`, `--newline`, `--no-mtime`, `--no-playlist`.
  - Returns the final file path (merger output when present).
- `pause(taskId)`: destroy the process; the `.part` file stays.
- `cancel(taskId)`: destroy the process and delete any `.part` files for that task.
- Android: add `io.github.junkfood02.youtubedl-android:ffmpeg` (same version 0.18.1) and call `FFmpeg.getInstance().init(context)` in `HomeCinemaApp`.
- Desktop: detect `ffmpeg` on PATH at startup. If missing, video downloads fall back to formats that already contain audio; show a one-line hint in the Ready state: "Install ffmpeg for higher qualities".

### Queue (`DownloadQueueManager`)
- Use the task's selected format (remove the hardcoded `"best"`).
- Up to **4 concurrent** downloads; others stay `QUEUED`.
- Actions: `pause`, `resume` (→ QUEUED → runs with `--continue`), `retry` (same as resume, clears `errorReason`), `remove` (cancel + delete partial file).
- On completion: hand the task to `DownloadHistory` and remove it from the queue.
- Failure reason: a short human-readable string mapped from the yt-dlp error ("Connection lost", "Unavailable", "Unsupported site", else "Download failed").

### History & settings persistence
- `DownloadHistory` becomes a `StateFlow`-backed repository that loads/saves `history.json` on every change (debounced). Cap removed (my call; the history is the user's library).
- `ThemePreference` (System/Dark/Light/Ocean/Volt) saved in `settings.json`.
- Storage location (expect/actual): Android `context.filesDir`, Desktop `~/.grabthatvideo/`.
- Entries whose file no longer exists on disk are **kept** and flagged `fileMissing` (checked at load and when History opens).

### Download directory
- Android: unchanged (public Downloads).
- Desktop: change from relative `downloads` to `~/Downloads/GrabThatVideo` (my call — the relative path depends on the working directory).

## UI architecture

- `ui/theme/`: `AppColors` (tokens above) ×4, `AppTypography` (Geist), `AppShapes`, `LocalAppColors`, `AppTheme(themePreference)`. Material3 `MaterialTheme` gets a color scheme mapped from the tokens so stock components fit in.
- `ui/home/HomeScreen.kt`, `ui/history/HistoryScreen.kt`, `ui/history/FileSheet.kt`, `ui/history/MoreMenu.kt`.
- `ui/components/`: Wordmark, GlassSurface, PillButton, RoundIconButton, UrlInput, FormatToggle, QualityChips, DownloadProgressCard, HistoryRow, EmptyState, SectionLabel, ThumbnailImage (Coil 3 + duration badge + audio fallback).
- `App.kt` (commonMain): root composable holding `currentScreen` (Home/History) and the theme; both entry points call it. Android back button on History → Home.
- State: `HomeViewModel` (fetch state machine: Idle/Fetching/Failed/Ready + queue tasks) and `HistoryViewModel` (search, filter, groups, menus). Plain classes with a `CoroutineScope`, like today.
- Platform services (expect/actual): `clipboardText()`, `openFile(path)`, `shareFile(path)`, `openDownloadsFolder()`, `revealInFileBrowser(path)` (exists).
- Desktop window: 420×860 default, min width 360; content column max width 480, centered.
- Composables receive immutable state (data classes / `@Immutable`) and lambdas for actions; no logic in the composable body beyond layout.

## Testing

- Unit tests (commonTest, with `MockDownloaderEngine`): format → chip mapping (heights, 4K/2K labels, default selection, audio availability), queue concurrency limit, pause/resume/retry/remove transitions, history grouping by day, search/filter counts, failure-reason mapping, JSON round-trip with old data (new fields default).
- Manual check on Android device and Desktop for each of the 8 states in each theme.

## Left to the builder

- Exact animation/transition choices (keep subtle: fade between fetch states, sheet slide).
- Snackbar styling for transient messages.
- Notification text in `DownloadService` (rename "HomeCinema" → "GrabThatVideo").

## Design coverage

The Pencil canvas is the visual reference; where it and this spec disagree, the canvas wins for visuals, the spec wins for behaviour. Designed card states: Downloading, Paused, Failed. **Not designed, styled from tokens (my call):** Queued card, theme submenu, History empty state, confirm dialogs, snackbar.

## Builder-check calls (mine)

- Desktop without ffmpeg: only formats that already contain audio are offered as chips; if none exist, a single "Best" chip maps to `-f best`.
- Output template includes id + quality so files are stable per task and qualities don't collide: `%(title)s [%(id)s] <quality>.%(ext)s` (audio: `<ext>` quality label, e.g. `128kbps`).
- Startup sweep: `.part` files in the download dir not referenced by any queue task are deleted.
