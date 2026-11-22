package com.homecinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val EmptyShape = RoundedCornerShape(20.dp)

/** Outlined empty-state card: inbox badge, title, body and optional decorative step pills. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    showSteps: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, EmptyShape)
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(colors.skeleton, AppShapes.round),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(AppIcons.Inbox),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                modifier = Modifier.widthIn(max = 240.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (showSteps) {
            Steps()
        }
    }
}

@Composable
private fun Steps() {
    val colors = LocalAppColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepPill(AppIcons.Copy, "Copy link")
        StepChevron(colors.textTertiary)
        StepPill(AppIcons.Clipboard, "Paste")
        StepChevron(colors.textTertiary)
        StepPill(AppIcons.Download, "Download")
    }
}

@Composable
private fun StepChevron(tint: Color) {
    Icon(
        painter = painterResource(AppIcons.ChevronRight),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(12.dp),
    )
}

@Composable
private fun StepPill(icon: DrawableResource, label: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .background(colors.surface, AppShapes.pill)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = label,
            color = colors.textChip,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
