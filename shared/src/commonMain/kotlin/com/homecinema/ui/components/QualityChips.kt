package com.homecinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.core.format.QualityOption
import com.homecinema.ui.text.qualityOptionLabel
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors

private const val MAX_FILLED_CHIPS = 4
private val ScrollingChipWidth = 72.dp

/**
 * One chip per quality. Up to four chips share the row equally (as designed); more chips keep a
 * fixed width and the row scrolls horizontally.
 */
@Composable
fun QualityChips(
    options: List<QualityOption>,
    selected: QualityOption?,
    onSelect: (QualityOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = options.size <= MAX_FILLED_CHIPS
    val rowModifier = if (fill) modifier.fillMaxWidth() else modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val widthModifier = if (fill) Modifier.weight(1f) else Modifier.width(ScrollingChipWidth)
            QualityChip(
                label = qualityOptionLabel(option),
                selected = option == selected,
                onClick = { onSelect(option) },
                modifier = widthModifier,
            )
        }
    }
}

@Composable
private fun QualityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalAppColors.current
    val background = if (selected) colors.accentSoft else Color.Transparent
    val border = if (selected) colors.accent else colors.border
    val text = if (selected) colors.accent else colors.textSecondary
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(AppShapes.qualityChip)
            .background(background)
            .border(1.dp, border, AppShapes.qualityChip)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = text,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
