package com.homecinema.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.homecinema.core.settings.ThemePreference
import com.homecinema.ui.theme.AppTheme
import kotlin.system.exitProcess

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GrabThatVideo"
        ) {
            AppTheme(ThemePreference.SYSTEM) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("GrabThatVideo")
                }
            }
        }
    }
    exitProcess(0)
}
