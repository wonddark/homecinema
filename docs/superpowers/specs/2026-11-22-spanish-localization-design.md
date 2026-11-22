# Spanish localization — design

Date: 2026-11-22 · Size: small change (short design)

## Intent

Make GrabThatVideo usable by Spanish-speaking people the app is shared with. They should
get Spanish automatically, with no setup, and it should read like natural Spanish.

## Goals / non-goals / anti-goals

- **Goal:** every piece of text the app itself writes appears in Spanish when the device
  language is Spanish (any region: `es`, `es-MX`, `es-ES`, …), and in English otherwise.
  Covers both Android and Desktop.
- **Goal:** adding another language later only takes one new `values-xx/strings.xml`.
- **Non-goal:** an in-app language picker (decided: follow the device language; it can
  be added later without redoing the translations).
- **Non-goal:** translating text that comes from outside the app: yt-dlp error output,
  video titles and site names. The app name "GrabThatVideo" also stays as it is.
- **Anti-goal:** leftover English mixed into Spanish screens (dates, counts, failure
  labels, the notification), or wording that reads like a literal translation.

## Approaches

- **A. Compose Multiplatform string resources, with core code returning labels instead
  of English (picked).** `composeResources/values/strings.xml` plus
  `values-es/strings.xml`. Core code returns enums or values, and the UI turns them into
  text. This is the standard approach, testable, and it follows device language changes.
- **B. Resources for screens only; core code looks up the language itself.** Fewer
  signature changes, but harder to test and text already on screen goes stale when the
  language changes.
- **C. A hand-written Kotlin map keyed by locale.** No resource tooling, but plurals are
  hand-rolled and it doesn't scale to more languages.

The user picked A.

## Design

1. **Resource files.** Every user-facing literal in `shared/src/commonMain` UI moves into
   `values/strings.xml` (English, the default). The Spanish translations go in
   `values-es/strings.xml`. Plurals ("1 file / 3 files", queued/downloading counts) use
   `<plurals>`. Screens use `stringResource` / `pluralStringResource`.
2. **Failure reasons.** `mapFailureReason` returns a `FailureReason` enum (BLOCKED,
   CONNECTION_LOST, UNAVAILABLE, UNSUPPORTED, GENERIC) instead of English text. The UI
   maps each value to a string resource. The persisted `queue.json` must keep loading:
   failures saved by older builds as English text should show the matching or generic
   label, never crash.
3. **History dates.** `HistoryGroup` drops its English `label`, and the UI maps each
   group to a resource. `sheetTimestamp` returns structured parts (today / yesterday /
   day + month, plus time) and the UI formats them. Short month names come from
   `strings.xml` (a string array), so both platforms agree. The current hard-coded
   `MONTHS` list is removed.
4. **Counts and meta lines.** `HistoryViewModel`'s "N files · size" and the Android
   notification summary ("Downloading 2 · 1 queued", "No active downloads") use plural
   resources. `DownloadService` reads them through the suspend `getString` /
   `getPluralString` from Compose resources. The notification channel name
   ("Downloads") is localized as well.
5. **Number format.** Sizes and progress stay as they are ("1.5 MB", "42%"). *(My call:
   decimal-comma localization is out of scope; flag it if wanted.)*
6. **Tests.** Tests that assert English text switch to asserting the enum or structured
   value. A test checks that `values-es/strings.xml` has every key in
   `values/strings.xml`, so a missing translation fails the build.
7. **Preview data** in `desktopMain/ui/preview` stays English (developer-only).

## Calls I made (check while reading)

- Spanish register: neutral Latin-American-friendly Spanish using "tú". Avoid regional
  slang.
- Any `es-*` region falls back to `values-es`. No regional variants.
- No decimal-comma formatting (item 5).
- Month names come from `strings.xml`, not platform date formatters, because
  kotlinx-datetime has no locale-aware formatting in common code.

## Left to the builder

- Resource key names, and the exact shape of the timestamp and legacy-failure mapping.
- How the Android service gets a coroutine for the suspend `getString` calls.
