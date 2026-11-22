package com.homecinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Fully round `control` button holding a single icon; [active] uses `controlActive`. */
@Composable
fun RoundIconButton(
    icon: DrawableResource,
    tint: Color,
    onClick: () -> Unit,
    contentDescription: String?,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 16.dp,
) {
    val colors = LocalAppColors.current
    val background = if (active) colors.controlActive else colors.control
    Box(
        modifier = modifier
            .size(size)
            .clip(AppShapes.round)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
