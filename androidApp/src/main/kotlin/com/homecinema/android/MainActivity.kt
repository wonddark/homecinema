package com.homecinema.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.homecinema.ui.App

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, DownloadService::class.java))
        val graph = DownloadService.graph
        setContent { App(graph) }
    }
}
