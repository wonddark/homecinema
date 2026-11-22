package com.homecinema.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val background: Color,
    val glowTopRight: Color,
    val glowLeft: Color,
    val surface: Color,
    val surfaceSheet: Color,
    val surfaceMenu: Color,
    val inputBg: Color,
    val control: Color,
    val controlActive: Color,
    val border: Color,
    val borderMenu: Color,
    val divider: Color,
    val skeleton: Color,
    val skeletonAlt: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val textChip: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentBorder: Color,
    val audioThumbBg: Color,
    val error: Color,
    val errorSoft: Color,
    val errorBorder: Color,
    val warning: Color,
    val scrim: Color,
    val scrimMenu: Color,
    val durationBadge: Color,
    val isDark: Boolean,
)

val LocalAppColors = staticCompositionLocalOf { Palettes.Dark }
