package com.homecinema.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.core.history.historyMeta
import com.homecinema.domain.HistoryEntry
import com.homecinema.domain.MediaKind
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.painterResource

private const val MISSING_THUMB_ALPHA = 0.35f

/**
 * One History list row: 72×48 thumbnail, title (2 lines max) with meta or the "file not found"
 * notice, and the ⋮ button. [showDivider] draws the `divider` rule under the row.
 */
@Composable
fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onMore: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistoryThumbnail(entry)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.title,
                    color = if (entry.fileMissing) colors.textSecondary else colors.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.fileMissing) MissingNotice() else HistoryMeta(historyMeta(entry))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(AppShapes.round)
                    .clickable(role = Role.Button, onClick = onMore),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(AppIcons.EllipsisVertical),
                    contentDescription = "File options",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(thickness = 1.dp, color = colors.divider)
        }
    }
}

/** The row/sheet thumbnail: the saved thumbnail with a duration badge for video, the audio tile otherwise. */
@Composable
fun HistoryThumbnail(entry: HistoryEntry, modifier: Modifier = Modifier) {
    val isVideo = entry.kind == MediaKind.VIDEO
    Thumbnail(
        model = entry.thumbnailPath.takeIf { isVideo },
        durationSeconds = entry.durationSeconds.takeIf { isVideo },
        kind = entry.kind,
        width = 72.dp,
        height = 48.dp,
        dimmed = if (entry.fileMissing) MISSING_THUMB_ALPHA else 1f,
        modifier = modifier,
    )
}

@Composable
private fun HistoryMeta(text: String) {
    Text(
        text = text,
        color = LocalAppColors.current.textSecondary,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MissingNotice() {
    val colors = LocalAppColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(AppIcons.Alert),
            contentDescription = null,
            tint = colors.warning,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "File not found · moved or deleted",
            color = colors.warning,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
