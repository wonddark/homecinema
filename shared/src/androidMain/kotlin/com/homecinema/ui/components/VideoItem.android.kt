package com.homecinema.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.ui.theme.AppColors
import com.homecinema.ui.theme.AppSpacing
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.delete_24px
import homecinema.shared.generated.resources.folder_open_24px
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun VideoItem(
    task: DownloadTask,
    onCancel: () -> Unit,
    onFormatSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.large)) {
            Text(
                text = task.videoInfo?.title ?: task.url,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )

            Spacer(Modifier.height(AppSpacing.small))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = task.state.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (task.videoInfo != null) {
                    FormatSelector(
                        formats = task.videoInfo.formats,
                        selectedFormatId = task.format,
                        onFormatSelected = onFormatSelected,
                    )
                }
            }

            if (task.state == DownloadState.DOWNLOADING) {
                Spacer(Modifier.height(AppSpacing.small))
                AppProgressBar(progress = task.progress)
            }

            Spacer(Modifier.height(AppSpacing.small))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (task.state) {
                    DownloadState.QUEUED -> {
                        AppButton(text = "Remove", onClick = onRemove)
                    }

                    DownloadState.DOWNLOADING -> {
                        AppButton(text = "Cancel", onClick = onCancel)
                    }

                    DownloadState.COMPLETED -> {
                        AppButton(icon = {
                            Icon(
                                painter = painterResource(Res.drawable.folder_open_24px),
                                contentDescription = "Remove from list",
                                modifier = Modifier.size(18.dp),
                            )
                        }, onClick = onLocate)

                        AppButton(icon = {
                            Icon(
                                painter = painterResource(Res.drawable.delete_24px),
                                contentDescription = "Remove from list",
                                modifier = Modifier.size(18.dp),
                            )
                        }, onClick = onRemove)
                    }

                    DownloadState.FAILED -> {
                        AppButton(text = "Retry", onClick = onRetry)

                        AppButton(icon = {
                            Icon(
                                painter = painterResource(Res.drawable.delete_24px),
                                contentDescription = "Remove from list",
                                modifier = Modifier.size(18.dp),
                            )
                        }, onClick = onRemove)
                    }
                }
            }
        }
    }
}
