package com.homecinema.ui.text

import androidx.compose.runtime.Composable
import com.homecinema.core.downloader.FailureReason
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
