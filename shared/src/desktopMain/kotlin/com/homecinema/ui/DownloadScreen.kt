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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.homecinema.core.usecase.DownloadViewModel
import com.homecinema.ui.components.AppButton
import com.homecinema.ui.components.VideoItem
import com.homecinema.ui.theme.AppSpacing
import com.homecinema.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(viewModel: DownloadViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("HomeCinema", style = AppTypography.title) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sidebar: input panel
            Column(
                modifier = Modifier
                    .width(AppSpacing.extraLarge * 15)
                    .padding(AppSpacing.large)
            ) {
                Text("Add Download", style = AppTypography.title)
                Spacer(Modifier.height(AppSpacing.medium))
                OutlinedTextField(
                    value = uiState.urlInput,
                    onValueChange = viewModel::onUrlChanged,
                    label = { Text("Video URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(AppSpacing.medium))
                if (uiState.isLoadingInfo) {
                    CircularProgressIndicator()
                } else {
                    AppButton(
                        text = "Download",
                        onClick = { viewModel.onStartDownload(uiState.urlInput) },
                        enabled = uiState.urlInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.width(AppSpacing.medium))

            // Main content: download list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(AppSpacing.large)
            ) {
                items(uiState.tasks) { task ->
                    VideoItem(
                        task = task,
                        onCancel = { viewModel.onCancelDownload(task.id) },
                        onFormatSelected = { fmtId -> viewModel.onSelectFormat(task.id, fmtId) },
                        onRetry = { viewModel.onRetryDownload(task.id) },
                        onRemove = { viewModel.onRemoveDownload(task.id) },
                        onLocate = { viewModel.onLocateDownload(task.id) }
                    )
                }
            }
        }
    }
}
