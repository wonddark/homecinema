package com.homecinema.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.ui.theme.AppColors
import com.homecinema.ui.theme.AppSpacing
import com.homecinema.ui.theme.AppTypography

@Composable
actual fun VideoItem(
    task: DownloadTask,
    onCancel: () -> Unit,
    onFormatSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.large, vertical = AppSpacing.medium),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface)
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.videoInfo?.title ?: task.url,
                    style = AppTypography.title,
                    maxLines = 1
                )
                Text(
                    text = "State: ${task.state.name}",
                    style = AppTypography.body
                )
                if (task.videoInfo != null) {
                    FormatSelector(
                        formats = task.videoInfo.formats,
                        selectedFormatId = task.format,
                        onFormatSelected = onFormatSelected
                    )
                }
            }
            if (task.state == DownloadState.DOWNLOADING) {
                AppProgressBar(
                    progress = task.progress,
                    modifier = Modifier.width(AppSpacing.extraLarge * 5)
                )
            }
            Spacer(Modifier.width(AppSpacing.large))
            when (task.state) {
                DownloadState.QUEUED -> AppButton(text = "Remove", onClick = onRemove)
                DownloadState.DOWNLOADING -> AppButton(text = "Cancel", onClick = onCancel)
                DownloadState.COMPLETED -> {
                    AppButton(text = "Locate", onClick = onLocate)
                    Spacer(Modifier.width(AppSpacing.medium))
                    AppButton(text = "Remove", onClick = onRemove)
                }
                DownloadState.FAILED -> {
                    AppButton(text = "Retry", onClick = onRetry)
                    Spacer(Modifier.width(AppSpacing.medium))
                    AppButton(text = "Remove", onClick = onRemove)
                }
            }
        }
    }
}
