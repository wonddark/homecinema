package com.homecinema.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.homecinema.core.usecase.DownloadViewModel
import com.homecinema.ui.components.AppButton
import com.homecinema.ui.components.AppTextField
import com.homecinema.ui.components.VideoItem
import com.homecinema.ui.theme.AppSpacing
import com.homecinema.ui.theme.AppTypography
import com.homecinema.ui.util.DeviceType
import com.homecinema.ui.util.rememberDeviceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(viewModel: DownloadViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val deviceType = rememberDeviceType()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HomeCinema", style = AppTypography.title) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(AppSpacing.large),
        ) {
            // URL input
            AppTextField(
                value = uiState.urlInput,
                onChange = viewModel::onUrlChanged,
                placeholder = "Video URL",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(AppSpacing.medium))

            AppButton(
                text = "Add",
                onClick = { viewModel.onStartDownload(uiState.urlInput) },
                enabled = uiState.urlInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                loading = uiState.isLoadingInfo,
            )

            Spacer(Modifier.height(AppSpacing.large))

            // Task list — adapt layout for device type
            if (deviceType == DeviceType.TABLET) {
                // Two-column grid for tablets
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.tasks) { task ->
                        VideoItem(
                            task = task,
                            onCancel = { viewModel.onCancelDownload(task.id) },
                            onFormatSelected = { fmtId ->
                                viewModel.onSelectFormat(
                                    task.id,
                                    fmtId,
                                )
                            },
                            onRetry = { viewModel.onRetryDownload(task.id) },
                            onRemove = { viewModel.onRemoveDownload(task.id) },
                            onLocate = { viewModel.onLocateDownload(task.id) }
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.tasks) { task ->
                        VideoItem(
                            task = task,
                            onCancel = { viewModel.onCancelDownload(task.id) },
                            onFormatSelected = { fmtId ->
                                viewModel.onSelectFormat(
                                    task.id,
                                    fmtId,
                                )
                            },
                            onRetry = { viewModel.onRetryDownload(task.id) },
                            onRemove = { viewModel.onRemoveDownload(task.id) },
                            onLocate = { viewModel.onLocateDownload(task.id) }
                        )
                    }
                }
            }
        }
    }
}
