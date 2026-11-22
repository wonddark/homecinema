# Spanish Localization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** All app-written text appears in Spanish when the device language is Spanish (any region), English otherwise, on Android and Desktop.

**Architecture:** Compose Multiplatform string resources (`composeResources/values` + `values-es`). Core code stops producing English: it returns enums/structured values (`FailureReason`, `HistoryGroup`, `SheetStamp`, counts), and composables in one file (`ui/text/Labels.kt`) turn them into text. Pure `String` formatters that tests cover take their words as parameters. The Android notification gets its text from suspend helpers in `shared`, because the generated `Res` class is internal to `shared`.

**Tech Stack:** Kotlin 2.0.21, Compose Multiplatform 1.7.3 (`compose.components.resources`, `Res` in package `homecinema.shared.generated.resources`), kotlinx-datetime, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-11-22-spanish-localization-design.md`

## Global Constraints

- Gradle needs `JAVA_HOME=/home/oz/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2`. Prefix every `./gradlew` command with it.
- Follow the device language only. No in-app picker, and nothing new in `AppSettings`.
- Do not translate: yt-dlp output, video titles, site/extractor names, the "GrabThatVideo" wordmark/app name, quality labels (`1080p`, `4K`, `128kbps`), and `desktopMain/ui/preview` data.
- Spanish register: neutral, "tú", no regional slang. `values-es` serves every `es-*` region.
- Number formatting is unchanged ("1.5 MB", "42%").
- Every key in `values/strings.xml` must exist in `values-es/strings.xml`. A test enforces this (Task 1).
- The Spanish copy in Task 1 is the approved text. Change it only to fix overflow (Task 6), and say so in the commit.

## Review Focus

1. **Device in `es-MX` / `es-419` / `es-ES`.** Expect Spanish, not English. Pinned by `SpanishStringsTest` (Task 1).
2. **Device in a language with no translation (e.g. `fr`).** Expect English, never a crash or missing text. Pinned by `SpanishStringsTest` (Task 1).
3. **`queue.json` written by an older build, with a FAILED task and `"errorReason":"Connection lost"`.** It must load. The task shows the generic failure label. Pinned in Task 2.
4. **Plural edge counts (0, 1, 2 files) in Spanish.** Expect "0 archivos", "1 archivo", "2 archivos". Pinned by `SpanishStringsTest` (Task 1).
5. **Longer Spanish strings overflowing** pills, chips, card buttons and menu rows at 360dp width. Checked with Spanish renders (Task 6).

---

### Task 1: String catalogs + catalog tests

**Files:**
- Create: `shared/src/commonMain/composeResources/values/strings.xml`
- Create: `shared/src/commonMain/composeResources/values-es/strings.xml`
- Test: `shared/src/desktopTest/kotlin/com/homecinema/ui/text/StringCatalogParityTest.kt`
- Test: `shared/src/desktopTest/kotlin/com/homecinema/ui/text/SpanishStringsTest.kt`

**Interfaces:**
- Produces: `Res.string.<key>`, `Res.plurals.files_count`, `Res.array.months_short` for every key below. Later tasks use these names exactly.

- [ ] **Step 1: Write `values/strings.xml` and `values-es/strings.xml`** with exactly these entries (key | English | Spanish):

```
history_title | History | Historial
action_back | Back | Atrás
action_more | More | Más
action_cancel | Cancel | Cancelar
action_paste | Paste | Pegar
action_go | Go | Ir
action_download | Download | Descargar
action_try_again | Try again | Reintentar
history_empty_title | No downloads yet | Aún no hay descargas
history_empty_body | Finished downloads will appear here. | Las descargas completadas aparecerán aquí.
history_search_hint | Search downloads | Buscar descargas
filter_all | All | Todo
filter_video | Video | Video
filter_audio | Audio | Audio
group_today | TODAY | HOY
group_yesterday | YESTERDAY | AYER
group_last_week | LAST WEEK | ÚLTIMA SEMANA
group_earlier | EARLIER | ANTERIORES
stamp_today | Today, %1$s | Hoy, %1$s
stamp_yesterday | Yesterday, %1$s | Ayer, %1$s
stamp_date | %1$d %2$s, %3$s | %1$d %2$s, %3$s
snackbar_path_copied | Path copied | Ruta copiada
snackbar_delete_failed | Couldn't delete file | No se pudo eliminar el archivo
file_play | Play | Reproducir
file_share | Share | Compartir
file_show_in_folder | Show in folder | Mostrar en carpeta
file_remove | Remove | Eliminar
file_options | File options | Opciones del archivo
file_not_found | File not found · moved or deleted | Archivo no encontrado · movido o eliminado
file_not_found_short | file not found | archivo no encontrado
menu_close | Close menu | Cerrar menú
menu_theme | Theme | Tema
menu_open_folder | Open downloads folder | Abrir carpeta de descargas
menu_clear_history | Clear history | Borrar historial
menu_selected | Selected | Seleccionado
theme_system | System | Sistema
theme_dark | Dark | Oscuro
theme_light | Light | Claro
theme_ocean | Ocean | Océano
theme_volt | Volt | Volt
confirm_delete_title | Delete this file? | ¿Eliminar este archivo?
confirm_delete_body | It will be removed from your device. | Se borrará de tu dispositivo.
confirm_delete_action | Delete | Eliminar
confirm_clear_title | Clear history? | ¿Borrar el historial?
confirm_clear_body | Your files stay on your device. | Tus archivos se quedan en tu dispositivo.
confirm_clear_action | Clear | Borrar
home_paste_link | Paste a link | Pega un enlace
home_works_with | Works with | Funciona con
fetch_loading | Fetching video info… | Obteniendo información del video…
fetch_blocked_title | This site blocks downloads from this app | Este sitio bloquea las descargas desde esta app
fetch_blocked_body | It refused the app's connection, even though the video may still play in a browser. | Rechazó la conexión de la app, aunque es posible que el video se reproduzca en un navegador.
fetch_failed_title | Couldn't fetch this video | No se pudo obtener este video
fetch_failed_body | The link may be private, removed, or from a site we don't support yet. | Puede que el enlace sea privado, se haya eliminado o sea de un sitio que aún no admitimos.
ffmpeg_hint | Install ffmpeg for higher qualities | Instala ffmpeg para obtener mayor calidad
downloads_section | Downloads | Descargas
downloads_in_progress | In progress | En curso
downloads_empty_title | Nothing downloading | No hay descargas en curso
downloads_empty_body | Paste a link above and your downloads will show up here. | Pega un enlace arriba y tus descargas aparecerán aquí.
url_hint | Paste a video link… | Pega el enlace de un video…
url_clear | Clear link | Borrar enlace
step_copy_link | Copy link | Copia el enlace
step_paste | Paste | Pega
step_download | Download | Descarga
format_video | MP4 · Video | MP4 · Video
quality_best | Best | Mejor
card_pause | Pause | Pausar
card_resume | Resume | Reanudar
card_retry | Retry | Reintentar
card_remove | Remove | Quitar
meta_queued | Queued | En cola
meta_paused | Paused | En pausa
meta_failed | Failed | Error
meta_starting | Starting… | Iniciando…
failure_blocked | Blocked by site | Bloqueado por el sitio
failure_connection | Connection lost | Se perdió la conexión
failure_unavailable | Unavailable | No disponible
failure_unsupported | Unsupported site | Sitio no compatible
failure_generic | Download failed | Falló la descarga
notif_channel_name | Downloads | Descargas
notif_downloading | Downloading %1$d | Descargando %1$d
notif_queued | %1$d queued | %1$d en cola
notif_paused | %1$d paused | %1$d en pausa
notif_idle | No active downloads | No hay descargas activas
```

Also add `<plurals name="files_count">` (EN one `%1$d file` / other `%1$d files`; ES one `%1$d archivo` / many `%1$d archivos` / other `%1$d archivos`) and `<string-array name="months_short">` (EN `Jan…Dec`; ES `ene feb mar abr may jun jul ago sep oct nov dic`). Use plain `'` in text, not `\'`. If a later task finds a literal not covered here (e.g. `HomeScreen.kt:100`, `FormatToggle`'s audio label), add a key to both files in that task.

- [ ] **Step 2: Write `StringCatalogParityTest`.** Read both files from `src/commonMain/composeResources/...` (the test working directory is the `shared` module). Collect `name="…"` from `<string>`, `<plurals>` and `<string-array>` with a regex. Assert that the two key sets are equal, with a message listing the missing keys in each direction.

- [ ] **Step 3: Write `SpanishStringsTest`.** It saves and restores `Locale.getDefault()` around each test and uses `runTest`.

```kotlin
@Test fun `es-MX resolves Spanish`() = withLocale(Locale("es", "MX")) {
    assertEquals("Falló la descarga", getString(Res.string.failure_generic))
}
@Test fun `es-419 resolves Spanish`() = withLocale(Locale.forLanguageTag("es-419")) {
    assertEquals("Historial", getString(Res.string.history_title))
}
@Test fun `french falls back to English`() = withLocale(Locale.FRENCH) {
    assertEquals("Download failed", getString(Res.string.failure_generic))
}
@Test fun `spanish file counts`() = withLocale(Locale("es")) {
    assertEquals("0 archivos", getPluralString(Res.plurals.files_count, 0, 0))
    assertEquals("1 archivo", getPluralString(Res.plurals.files_count, 1, 1))
    assertEquals("2 archivos", getPluralString(Res.plurals.files_count, 2, 2))
}
@Test fun `spanish months`() = withLocale(Locale("es")) {
    assertEquals("nov", getStringArray(Res.array.months_short)[10])
}
```

- [ ] **Step 4: Run** `./gradlew :shared:desktopTest --tests '*StringCatalogParityTest*' --tests '*SpanishStringsTest*'`. Expected: PASS. If `es-419` fails, Compose resources can't resolve that tag; replace the test with `Locale("es", "AR")` and note it in the commit.

- [ ] **Step 5: Commit** with `feat: add English and Spanish string catalogs`.

---

### Task 2: FailureReason enum replaces English error text

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/core/downloader/FailureReason.kt`
- Modify: `shared/src/commonMain/kotlin/com/homecinema/domain/DownloadTask.kt:18`
- Modify: `shared/src/commonMain/kotlin/com/homecinema/core/usecase/DownloadQueueManager.kt:114,148,153,167-175`
- Modify: `shared/src/desktopMain/kotlin/com/homecinema/ui/preview/HomePreviewData.kt:75`, `shared/src/desktopTest/kotlin/com/homecinema/e2e/DesktopE2ETest.kt:262`
- Test: `shared/src/commonTest/kotlin/com/homecinema/core/downloader/FailureReasonTest.kt`, `shared/src/commonTest/kotlin/com/homecinema/DownloadQueueManagerTest.kt`

**Interfaces:**
- Produces: `@Serializable enum class FailureReason { BLOCKED, CONNECTION_LOST, UNAVAILABLE, UNSUPPORTED, GENERIC }`; `fun mapFailureReason(message: String?): FailureReason`; `DownloadTask.failure: FailureReason? = null` (the `errorReason` field is removed).

- [ ] **Step 1: Update tests.** In `FailureReasonTest`, each expected string becomes its enum value (`"Connection lost"` → `FailureReason.CONNECTION_LOST`, `"Download failed"` → `GENERIC`, and so on). In `DownloadQueueManagerTest`, `errorReason` assertions become `failure` assertions. Add this test:

```kotlin
@Test fun `legacy queue with english errorReason loads as failed with unknown reason`() {
    // store pre-seeded with: [{"id":"t","url":"u","state":"FAILED","errorReason":"Connection lost", ...required fields}]
    // restore a manager from it
    val t = restored.tasks.value.single()
    assertEquals(DownloadState.FAILED, t.state)
    assertNull(t.failure)
}
```

Build the seed JSON with the same in-memory store/IO fake the existing restore test at `:261` uses.

- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*FailureReasonTest*' --tests '*DownloadQueueManagerTest*'`. Expected: compile FAIL.
- [ ] **Step 3: Implement.** Keep the current matching rules and their order. `fail(id, token, reason: FailureReason)`; retry clears `failure = null`. `JsonFileStore` already has `ignoreUnknownKeys = true`, so the legacy `errorReason` key is dropped. Update the preview data and the E2E message to use `failure`.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit** with `refactor: return FailureReason enum instead of English text`.

---

### Task 3: Labels file + download card

**Files:**
- Create: `shared/src/commonMain/kotlin/com/homecinema/ui/text/Labels.kt`
- Modify: `shared/src/commonMain/kotlin/com/homecinema/ui/components/DownloadProgressCard.kt` (buttons `:152-168`, `progressMeta` `:181-200`)
- Test: `shared/src/commonTest/kotlin/com/homecinema/ui/components/ProgressMetaTest.kt`

**Interfaces:**
- Consumes: `FailureReason` (Task 2), Task 1 keys.
- Produces:
  - `data class ProgressLabels(val queued: String, val paused: String, val failed: String, val starting: String, val failure: String)`
  - `fun progressMeta(task: DownloadTask, labels: ProgressLabels): String`
  - In `Labels.kt`: `@Composable fun failureLabel(reason: FailureReason?): String` (null → `failure_generic`) and `@Composable fun progressLabels(task: DownloadTask): ProgressLabels`

- [ ] **Step 1: Update `ProgressMetaTest`.** Use a fixed `val en = ProgressLabels("Queued", "Paused", "Failed", "Starting…", "Connection lost")`, pass it in every call, and keep the existing expected strings. Add this test:

```kotlin
@Test fun `failed uses provided failure text`() =
    assertEquals("Error · Se perdió la conexión",
        progressMeta(task(DownloadState.FAILED), en.copy(failed = "Error", failure = "Se perdió la conexión")))
```

- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*ProgressMetaTest*'`. Expected: compile FAIL.
- [ ] **Step 3: Implement** `progressMeta` with labels, plus `Labels.kt`. Wire the card's Pause/Resume/Retry/Remove buttons to `card_*` and its meta line to `progressMeta(task, progressLabels(task))`.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit** with `feat: localize download card`.

---

### Task 4: History: structured dates, summary, snackbar, screens

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/core/history/HistoryGrouping.kt`
- Modify: `shared/src/commonMain/kotlin/com/homecinema/ui/history/HistoryState.kt`, `HistoryViewModel.kt`, `HistoryScreen.kt`, `FileSheet.kt`, `MoreMenu.kt`, `ConfirmDialog.kt`
- Modify: `shared/src/commonMain/kotlin/com/homecinema/ui/components/HistoryRow.kt`, `Snackbar.kt`
- Modify: `shared/src/commonMain/kotlin/com/homecinema/ui/text/Labels.kt`
- Test: `shared/src/commonTest/kotlin/com/homecinema/core/history/HistoryGroupingTest.kt`, `shared/src/commonTest/kotlin/com/homecinema/ui/history/HistoryViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 keys; `Labels.kt` (Task 3).
- Produces:
  - `enum class HistoryGroup { TODAY, YESTERDAY, LAST_WEEK, EARLIER }` (the `label` property is removed)
  - `enum class StampDay { TODAY, YESTERDAY, DATE }`
  - `data class SheetStamp(val day: StampDay, val dayOfMonth: Int, val monthNumber: Int, val time: String)`
  - `fun sheetTimestamp(completedAt: Long, now: Long, zone: TimeZone): SheetStamp`
  - `HistoryState`: `summaryCount: Int = 0`, `summaryBytes: Long? = null` (replacing `summary`), and `snackbar: HistorySnackbar? = null`, where `enum class HistorySnackbar { PATH_COPIED, DELETE_FAILED }`
  - In `Labels.kt`: `@Composable` `historyGroupLabel(HistoryGroup)`, `sheetStampText(SheetStamp)`, `historySummaryText(count: Int, bytes: Long?)` (`files_count` + `" · " + formatSize` when bytes != null), `snackbarText(HistorySnackbar)`, `themeLabel(ThemePreference)`, `filterLabel(HistoryFilter)`. Move the last two from `MoreMenu.kt`/`HistoryScreen.kt` and make them `@Composable`.

- [ ] **Step 1: Update tests.** In `sheet timestamp variants`, assert `SheetStamp(StampDay.TODAY, 21, 11, "18:42")`, `SheetStamp(StampDay.YESTERDAY, 20, 11, "09:05")` and `SheetStamp(StampDay.DATE, 12, 11, "18:42")`. In `HistoryViewModelTest`, assertions on `summary` become `summaryCount`/`summaryBytes`, and the snackbar strings become `HistorySnackbar.PATH_COPIED` / `DELETE_FAILED`. Grouping tests stay as they are.
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*HistoryGroupingTest*' --tests '*HistoryViewModelTest*'`. Expected: compile FAIL.
- [ ] **Step 3: Implement.** Delete `MONTHS`. `sheetStampText` picks `stamp_today` / `stamp_yesterday` / `stamp_date`, and for `stamp_date` the month comes from `stringArrayResource(Res.array.months_short)[monthNumber - 1]`. Replace every literal in the listed screens with its Task 1 key (`stringResource`). The `Snackbar` component takes resolved text.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit** with `feat: localize history screens`.

---

### Task 5: Home screen, inputs, format picker

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/homecinema/ui/home/HomeScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/homecinema/ui/components/UrlInput.kt`, `EmptyState.kt`, `FormatToggle.kt`, `PillButton.kt` (KDoc example only)
- Modify: wherever `QualityOption.label` is displayed (find it with `grep -rn '\.label' ui/home ui/components`)
- Modify: `shared/src/commonMain/kotlin/com/homecinema/ui/text/Labels.kt`

**Interfaces:**
- Consumes: Task 1 keys.
- Produces: `@Composable fun qualityOptionLabel(option: QualityOption): String` in `Labels.kt`. It returns `stringResource(Res.string.quality_best)` when `option.formatId == "best"`, and `option.label` otherwise. `FormatCatalog`'s `"Best"` stays as data.

- [ ] **Step 1: Replace literals.** Swap each literal in the listed files for its key. Content descriptions count as text. Compose animation `label =` strings and state keys (`"fetch-state"`, `"spinner"`) are debug-only and stay as they are.
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest`. Expected: PASS (no behavior tests change).
- [ ] **Step 3: Check for leftovers.** Run `grep -rnE '"[A-Z][a-z]+( [a-z]+)*[.…?]?"' shared/src/commonMain/kotlin/com/homecinema/ui`. Expected: only the wordmark, theme/debug labels, and the exclusions in Global Constraints.
- [ ] **Step 4: Commit** with `feat: localize home screen`.

---

### Task 6: Android notification + Spanish renders

**Files:**
- Create: `shared/src/commonMain/kotlin/com/homecinema/core/notification/DownloadNotificationText.kt`
- Modify: `androidApp/src/main/kotlin/com/homecinema/android/DownloadService.kt`
- Modify: `shared/src/desktopTest/kotlin/com/homecinema/ui/preview/RenderSupport.kt`, `shared/build.gradle.kts:76` (forward the `renderLocale` property)
- Test: `shared/src/commonTest/kotlin/com/homecinema/core/notification/NotificationCountsTest.kt`, plus one case added to `SpanishStringsTest`

**Interfaces:**
- Produces:
  - `data class NotificationCounts(val downloading: Int, val queued: Int, val paused: Int)`
  - `fun notificationCounts(tasks: List<DownloadTask>): NotificationCounts`
  - `suspend fun notificationText(counts: NotificationCounts): String` (non-zero parts joined by `" · "`, `notif_idle` when none)
  - `suspend fun downloadsChannelName(): String`

- [ ] **Step 1: Write the tests.**
  - `NotificationCountsTest`: three tasks in DOWNLOADING/QUEUED/PAUSED plus one COMPLETED give `NotificationCounts(1, 1, 1)`.
  - `SpanishStringsTest`: `notificationText(NotificationCounts(2, 1, 0))` gives `"Descargando 2 · 1 en cola"`, and `NotificationCounts(0, 0, 0)` gives `"No hay descargas activas"`.
- [ ] **Step 2: Run** `./gradlew :shared:desktopTest --tests '*NotificationCountsTest*' --tests '*SpanishStringsTest*'`. Expected: compile FAIL.
- [ ] **Step 3: Implement.** Write the helpers. In `DownloadService`, delete the private `notificationText`. In `onCreate`, get the channel name and first text with `runBlocking { … }` (a small asset read, needed because `startForeground` must run synchronously). The flow becomes `.map { notificationText(notificationCounts(it)) }`. Recreating the channel with the same ID on each `onCreate` updates its name after a language change.
- [ ] **Step 4: Run** the same command plus `./gradlew :androidApp:assembleDebug`. Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Add Spanish renders.** `RenderSupport` calls `Locale.setDefault(Locale.forLanguageTag(System.getProperty("renderLocale")))` when the property is set. Then render: `./gradlew :shared:desktopTest -PrenderLocale=es -PhomeRenderDir=<scratch>/es-home -PhistoryRenderDir=<scratch>/es-history`. Open the PNGs and check for no English (apart from the exclusions) and no clipped or overflowing text. If something overflows, shorten the Spanish string in `values-es` and record it in the commit body.
- [ ] **Step 6: Run** the full suite `./gradlew :shared:desktopTest :androidApp:assembleDebug`. Expected: PASS.
- [ ] **Step 7: Commit** with `feat: localize Android notification; Spanish render check`.
