package com.homecinema.ui.text

import androidx.compose.runtime.Composable
import com.homecinema.core.downloader.FailureReason
import com.homecinema.core.format.QualityOption
import com.homecinema.domain.DownloadTask
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.failure_blocked
import homecinema.shared.generated.resources.failure_connection
import homecinema.shared.generated.resources.failure_generic
import homecinema.shared.generated.resources.failure_unavailable
import homecinema.shared.generated.resources.failure_unsupported
import homecinema.shared.generated.resources.meta_failed
import homecinema.shared.generated.resources.meta_paused
import homecinema.shared.generated.resources.meta_queued
import homecinema.shared.generated.resources.meta_starting
import com.homecinema.core.history.HistoryGroup
import com.homecinema.core.history.SheetStamp
import com.homecinema.core.history.StampDay
import com.homecinema.core.settings.ThemePreference
import com.homecinema.ui.components.formatSize
import com.homecinema.ui.history.HistoryFilter
import com.homecinema.ui.history.HistorySnackbar
import homecinema.shared.generated.resources.group_today
import homecinema.shared.generated.resources.group_yesterday
import homecinema.shared.generated.resources.group_last_week
import homecinema.shared.generated.resources.group_earlier
import homecinema.shared.generated.resources.stamp_today
import homecinema.shared.generated.resources.stamp_yesterday
import homecinema.shared.generated.resources.stamp_date
import homecinema.shared.generated.resources.snackbar_path_copied
import homecinema.shared.generated.resources.snackbar_delete_failed
import homecinema.shared.generated.resources.theme_system
import homecinema.shared.generated.resources.theme_dark
import homecinema.shared.generated.resources.theme_light
import homecinema.shared.generated.resources.theme_ocean
import homecinema.shared.generated.resources.theme_volt
import homecinema.shared.generated.resources.filter_all
import homecinema.shared.generated.resources.filter_video
import homecinema.shared.generated.resources.filter_audio
import homecinema.shared.generated.resources.files_count
import homecinema.shared.generated.resources.months_short
import homecinema.shared.generated.resources.quality_best
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** Localized fragments of the download card meta line. */
data class ProgressLabels(
    val queued: String,
    val paused: String,
    val failed: String,
    val starting: String,
    val failure: String,
)

/** Human text for a failure; null (tasks saved before [FailureReason] existed) reads as generic. */
@Composable
fun failureLabel(reason: FailureReason?): String = stringResource(
    when (reason) {
        FailureReason.BLOCKED -> Res.string.failure_blocked
        FailureReason.CONNECTION_LOST -> Res.string.failure_connection
        FailureReason.UNAVAILABLE -> Res.string.failure_unavailable
        FailureReason.UNSUPPORTED -> Res.string.failure_unsupported
        FailureReason.GENERIC, null -> Res.string.failure_generic
    },
)

@Composable
fun progressLabels(task: DownloadTask): ProgressLabels = ProgressLabels(
    queued = stringResource(Res.string.meta_queued),
    paused = stringResource(Res.string.meta_paused),
    failed = stringResource(Res.string.meta_failed),
    starting = stringResource(Res.string.meta_starting),
    failure = failureLabel(task.failure),
)

@Composable
fun historyGroupLabel(group: HistoryGroup): String = stringResource(
    when (group) {
        HistoryGroup.TODAY -> Res.string.group_today
        HistoryGroup.YESTERDAY -> Res.string.group_yesterday
        HistoryGroup.LAST_WEEK -> Res.string.group_last_week
        HistoryGroup.EARLIER -> Res.string.group_earlier
    },
)

@Composable
fun sheetStampText(stamp: SheetStamp): String {
    val months = stringArrayResource(Res.array.months_short)
    return when (stamp.day) {
        StampDay.TODAY -> stringResource(Res.string.stamp_today, stamp.time)
        StampDay.YESTERDAY -> stringResource(Res.string.stamp_yesterday, stamp.time)
        StampDay.DATE -> stringResource(Res.string.stamp_date, stamp.dayOfMonth, months[stamp.monthNumber - 1], stamp.time)
    }
}

@Composable
fun historySummaryText(count: Int, bytes: Long?): String {
    val files = pluralStringResource(Res.plurals.files_count, count, count)
    if (bytes == null) return files
    return "$files · ${formatSize(bytes)}"
}

@Composable
fun snackbarText(snackbar: HistorySnackbar): String = stringResource(
    when (snackbar) {
        HistorySnackbar.PATH_COPIED -> Res.string.snackbar_path_copied
        HistorySnackbar.DELETE_FAILED -> Res.string.snackbar_delete_failed
    },
)

@Composable
fun themeLabel(theme: ThemePreference): String = stringResource(
    when (theme) {
        ThemePreference.SYSTEM -> Res.string.theme_system
        ThemePreference.DARK -> Res.string.theme_dark
        ThemePreference.LIGHT -> Res.string.theme_light
        ThemePreference.OCEAN -> Res.string.theme_ocean
        ThemePreference.VOLT -> Res.string.theme_volt
    },
)

@Composable
fun filterLabel(filter: HistoryFilter): String = stringResource(
    when (filter) {
        HistoryFilter.ALL -> Res.string.filter_all
        HistoryFilter.VIDEO -> Res.string.filter_video
        HistoryFilter.AUDIO -> Res.string.filter_audio
    },
)

/** "Best" is a localized placeholder; concrete qualities (1080p, 4K) are language-neutral data. */
@Composable
fun qualityOptionLabel(option: QualityOption): String =
    if (option.formatId == "best") stringResource(Res.string.quality_best) else option.label
