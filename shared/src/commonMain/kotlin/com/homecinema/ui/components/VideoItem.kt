package com.homecinema.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homecinema.domain.DownloadTask

@Composable
expect fun VideoItem(
    task: DownloadTask,
    onCancel: () -> Unit,
    onFormatSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier
)
