package com.homecinema.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/** Routes a window-level back key (Escape) to the most recently registered enabled [PlatformBackHandler]. */
class DesktopBackDispatcher {
    private val handlers = ArrayDeque<() -> Unit>()

    /** Runs the innermost handler; returns false when none is registered so the key keeps propagating. */
    fun onBack(): Boolean {
        val handler = handlers.lastOrNull() ?: return false
        handler()
        return true
    }

    internal fun register(handler: () -> Unit) = handlers.addLast(handler)

    internal fun unregister(handler: () -> Unit) {
        handlers.remove(handler)
    }
}

/** Provided by the desktop window; without it [PlatformBackHandler] is a no-op. */
val LocalDesktopBackDispatcher = staticCompositionLocalOf<DesktopBackDispatcher?> { null }

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val dispatcher = LocalDesktopBackDispatcher.current ?: return
    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(dispatcher, enabled) {
        val handler: () -> Unit = { currentOnBack() }
        if (enabled) dispatcher.register(handler)
        onDispose { dispatcher.unregister(handler) }
    }
}
