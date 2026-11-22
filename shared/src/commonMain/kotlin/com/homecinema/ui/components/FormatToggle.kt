package com.homecinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import com.homecinema.domain.MediaKind
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Segmented `MP4 · Video` / `<EXT> · Audio` toggle. The video segment appears only when
 * [videoAvailable]; the audio segment only when [audioLabel] is non-null.
 */
@Composable
fun FormatToggle(
    selected: MediaKind,
    videoAvailable: Boolean,
    audioLabel: String?,
    onSelect: (MediaKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.inputBg, AppShapes.toggle)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (videoAvailable) {
            Segment(
                label = "MP4 · Video",
                icon = AppIcons.Video,
                selected = selected == MediaKind.VIDEO,
                onClick = { onSelect(MediaKind.VIDEO) },
            )
        }
        if (audioLabel != null) {
            Segment(
                label = audioLabel,
                icon = AppIcons.Music,
                selected = selected == MediaKind.AUDIO,
                onClick = { onSelect(MediaKind.AUDIO) },
            )
        }
    }
}

@Composable
private fun RowScope.Segment(
    label: String,
    icon: DrawableResource,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val background = if (selected) colors.control else Color.Transparent
    val content = if (selected) colors.textPrimary else colors.textSecondary
    Row(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(AppShapes.togglePill)
            .background(background)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
