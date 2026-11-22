package com.homecinema.ui

import androidx.compose.runtime.Composable

/** Calls [onBack] on the platform's back gesture (Android) or Escape key (desktop) while [enabled]. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
