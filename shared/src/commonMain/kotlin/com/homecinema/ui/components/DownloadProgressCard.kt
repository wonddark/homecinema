package com.homecinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.ui.text.ProgressLabels
import com.homecinema.ui.text.progressLabels
import com.homecinema.ui.theme.AppColors
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.card_pause
import homecinema.shared.generated.resources.card_remove
import homecinema.shared.generated.resources.card_resume
import homecinema.shared.generated.resources.card_retry
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private val TrackShape = RoundedCornerShape(2.dp)

/**
 * One in-progress download (DOWNLOADING, QUEUED, PAUSED or FAILED).
 * [highlighted] draws the `accentBorder`; the caller passes it for the first downloading card only.
 */
@Composable
fun DownloadProgressCard(
    task: DownloadTask,
    highlighted: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val failed = task.state == DownloadState.FAILED
    val stateChangedAt = remember(task.state) { TimeSource.Monotonic.markNow() }
    val guardedRemove = { if (removeAllowed(task.state, stateChangedAt.elapsedNow())) onRemove() }
    val border = when {
        failed -> colors.errorBorder
        highlighted -> colors.accentBorder
        else -> Color.Transparent
    }
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = AppShapes.progressItem, borderColor = border) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(
                model = task.videoInfo?.thumbnailUrl,
                durationSeconds = null,
                kind = task.kind,
                width = 64.dp,
                height = 64.dp,
                dimmed = if (failed) 0.55f else 1f,
                cornerRadius = 12.dp,
            )
            Info(task, colors, Modifier.weight(1f))
            Actions(task, colors, onPause, onResume, onRetry, guardedRemove)
        }
    }
}

@Composable
private fun Info(task: DownloadTask, colors: AppColors, modifier: Modifier) {
    val failed = task.state == DownloadState.FAILED
    val accentColor = when (task.state) {
        DownloadState.FAILED -> colors.error
        DownloadState.PAUSED -> colors.textSecondary
        else -> colors.accent
    }
    val percentColor = when (task.state) {
        DownloadState.FAILED -> colors.error
        DownloadState.PAUSED -> colors.textSecondary
        else -> colors.textPrimary
    }
    val progress = task.progress.coerceIn(0f, 1f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = task.videoInfo?.title ?: task.url,
            color = if (failed) colors.textSecondary else colors.textPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = progressMeta(task, progressLabels(task)),
            color = if (failed) colors.error else colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(colors.control, TrackShape),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(accentColor, TrackShape),
                )
            }
            Text(
                text = "${(progress * 100).roundToInt()}%",
                color = percentColor,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun Actions(
    task: DownloadTask,
    colors: AppColors,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        when (task.state) {
            DownloadState.DOWNLOADING, DownloadState.COMPLETED ->
                RoundIconButton(AppIcons.Pause, colors.textPrimary, onPause, stringResource(Res.string.card_pause))
            DownloadState.QUEUED -> RemoveButton(colors, onRemove)
            DownloadState.PAUSED -> {
                RoundIconButton(AppIcons.Play, colors.accent, onResume, stringResource(Res.string.card_resume))
                RemoveButton(colors, onRemove)
            }
            DownloadState.FAILED -> {
                RoundIconButton(AppIcons.RotateCcw, colors.accent, onRetry, stringResource(Res.string.card_retry))
                RemoveButton(colors, onRemove)
            }
        }
    }
}

@Composable
private fun RemoveButton(colors: AppColors, onRemove: () -> Unit) {
    RoundIconButton(AppIcons.X, colors.textPrimary, onRemove, stringResource(Res.string.card_remove))
}

/** How long Remove ignores taps after Pause/failure moved it under the finger (Pause sat where Remove now is). */
private val RemoveGuard = 600.milliseconds

/** False while a PAUSED/FAILED card is younger than [RemoveGuard], so a double-tap on Pause cannot remove it. */
internal fun removeAllowed(state: DownloadState, sinceStateChange: Duration): Boolean {
    val guarded = state == DownloadState.PAUSED || state == DownloadState.FAILED
    return !guarded || sinceStateChange >= RemoveGuard
}

/**
 * Meta line under the title. Segments whose value is null or blank are dropped:
 * Downloading `"<extractor> · <quality> · <size>"` (`Starting…` at 0%), Queued `"… · Queued"`,
 * Paused `"Paused · <quality> · <size>"`, Failed `"Failed · <reason>"`.
 */
fun progressMeta(task: DownloadTask, labels: ProgressLabels): String {
    val extractor = task.videoInfo?.extractor
    val quality = task.qualityLabel
    val size = task.sizeBytes?.let(::formatSize)
    val segments = when (task.state) {
        DownloadState.QUEUED -> listOf(extractor, quality, labels.queued)
        DownloadState.PAUSED -> listOf(labels.paused, quality, size)
        DownloadState.FAILED -> listOf(labels.failed, labels.failure)
        DownloadState.DOWNLOADING, DownloadState.COMPLETED -> {
            val tail = if (task.progress == 0f) labels.starting else size
            listOf(extractor, quality, tail)
        }
    }
    return segments.filterNot { it.isNullOrBlank() }.joinToString(" · ")
}
