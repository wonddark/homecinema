package com.homecinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors

/**
 * Translucent `surface` fill with a 1dp stroke. Real background blur is skipped; the fill alone
 * reads as glass over the app background. Pass [borderColor] = [Color.Transparent] for no stroke.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.card,
    borderColor: Color = LocalAppColors.current.border,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surface, shape)
            .border(1.dp, borderColor, shape),
        content = content,
    )
}
