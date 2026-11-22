package com.homecinema.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

private const val SNACKBAR_MILLIS = 2_000L

/** A transient glass pill ("Path copied"); calls [onShown] after ~2s so the caller can clear it. */
@Composable
fun Snackbar(message: String, onShown: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnShown = rememberUpdatedState(onShown)
    LaunchedEffect(message) {
        delay(SNACKBAR_MILLIS)
        currentOnShown.value()
    }
    GlassSurface(modifier = modifier, shape = AppShapes.pill) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            color = LocalAppColors.current.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
