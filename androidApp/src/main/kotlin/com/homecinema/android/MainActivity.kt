package com.homecinema.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.homecinema.core.usecase.DownloadViewModel
import com.homecinema.ui.DownloadScreen
import com.homecinema.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    private val viewModel by lazy {
        DownloadViewModel(
            engine = DownloadService.engine!!,
            queueManager = DownloadService.queueManager!!
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, DownloadService::class.java))
        setContent {
            AppTheme {
                DownloadScreen(viewModel = viewModel)
            }
        }
    }
}
