package com.homecinema.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import com.homecinema.core.settings.ThemePreference

@Composable
fun AppTheme(preference: ThemePreference, content: @Composable () -> Unit) {
    val colors = resolvePalette(preference, isSystemInDarkTheme())
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = appTypography(),
            content = content,
        )
    }
}

private fun AppColors.toMaterialColorScheme() =
    if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = background,
            onBackground = textPrimary,
            surface = surfaceMenu,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            surfaceVariant = control,
            outline = border,
            error = error,
            scrim = scrim,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = background,
            onBackground = textPrimary,
            surface = surfaceMenu,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            surfaceVariant = control,
            outline = border,
            error = error,
            scrim = scrim,
        )
    }

/** Background fill plus the two elliptical radial glows from the spec. */
@Composable
fun Modifier.appBackground(): Modifier {
    val colors = LocalAppColors.current
    return drawBehind {
        drawRect(colors.background)
        drawGlow(colors.glowTopRight, centerX = 0.95f, centerY = 0.02f, widthFactor = 1.5f, heightFactor = 0.75f)
        drawGlow(colors.glowLeft, centerX = 0f, centerY = 0.55f, widthFactor = 1.3f, heightFactor = 0.7f)
    }
}

private fun DrawScope.drawGlow(
    color: Color,
    centerX: Float,
    centerY: Float,
    widthFactor: Float,
    heightFactor: Float,
) {
    val center = Offset(size.width * centerX, size.height * centerY)
    val radiusX = size.width * widthFactor / 2f
    val radiusY = size.height * heightFactor / 2f
    // Draw a circular gradient of radius radiusX, squashed vertically into an ellipse.
    scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = center,
                radius = radiusX,
            ),
            radius = radiusX,
            center = center,
        )
    }
}
