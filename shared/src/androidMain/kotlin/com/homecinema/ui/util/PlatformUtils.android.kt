package com.homecinema.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

actual fun getDeviceType(): DeviceType {
    // Will be called from a Composable context via rememberDeviceType()
    return DeviceType.MOBILE
}

@Composable
fun rememberDeviceType(): DeviceType {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp >= 840 -> DeviceType.DESKTOP
        configuration.screenWidthDp >= 600 -> DeviceType.TABLET
        else -> DeviceType.MOBILE
    }
}
