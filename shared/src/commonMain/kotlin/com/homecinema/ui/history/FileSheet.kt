package com.homecinema.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.core.history.historyMeta
import com.homecinema.core.history.sheetTimestamp
import com.homecinema.domain.HistoryEntry
import com.homecinema.ui.components.AppIcons
import com.homecinema.ui.components.HistoryThumbnail
import com.homecinema.ui.text.sheetStampText
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.file_play
import homecinema.shared.generated.resources.file_remove
import homecinema.shared.generated.resources.file_share
import homecinema.shared.generated.resources.file_show_in_folder
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val DISABLED_ALPHA = 0.4f

/**
 * The File menu bottom sheet: grabber, file header and Play / Share / Show in folder / Remove.
 * Every action first slides the sheet away, then runs and dismisses it, so dialogs and the
 * snackbar never end up behind the sheet. For a missing file the first three are disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileSheet(entry: HistoryEntry, actions: HistoryActions) {
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val closeThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            action()
            actions.onDismissSheet()
        }
    }
    ModalBottomSheet(
        onDismissRequest = actions.onDismissSheet,
        sheetState = sheetState,
        shape = AppShapes.sheetTop,
        // No backdrop blur here, so composite the translucent token over the background to keep the sheet opaque.
        containerColor = colors.surfaceSheet.compositeOver(colors.background),
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = colors.scrim,
        dragHandle = { Grabber() },
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FileHeader(entry)
            Column {
                val enabled = !entry.fileMissing
                SheetAction(AppIcons.Play, stringResource(Res.string.file_play), enabled) { closeThen(actions.onPlay) }
                SheetAction(AppIcons.Share2, stringResource(Res.string.file_share), enabled) { closeThen(actions.onShare) }
                SheetAction(AppIcons.FolderOpen, stringResource(Res.string.file_show_in_folder), enabled) { closeThen(actions.onShowInFolder) }
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                SheetAction(AppIcons.Trash2, stringResource(Res.string.file_remove), enabled = true, destructive = true) {
                    closeThen(actions.onRemove)
                }
            }
        }
    }
}

/** Grabber plus the sheet's 1dp top edge in `border` (the sheet surface itself has no stroke). */
@Composable
private fun Grabber() {
    val edge = LocalAppColors.current.border
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawTopEdge(edge) }
            .padding(top = 10.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .background(LocalAppColors.current.controlActive, RoundedCornerShape(2.dp)),
        )
    }
}

/** Strokes the rounded top of the sheet shape, clipped to this box so only the top edge shows. */
private fun DrawScope.drawTopEdge(color: Color) {
    val outline = AppShapes.sheetTop.createOutline(Size(size.width, size.height * 2), layoutDirection, this)
    clipRect { drawOutline(outline, color, style = Stroke(width = 1.dp.toPx())) }
}

@Composable
private fun FileHeader(entry: HistoryEntry) {
    val colors = LocalAppColors.current
    val stamp = remember(entry) {
        sheetTimestamp(entry.completedAt, Clock.System.now().toEpochMilliseconds(), TimeZone.currentSystemDefault())
    }
    val meta = "${historyMeta(entry)} · ${sheetStampText(stamp)}"
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistoryThumbnail(entry)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)
    }
}

@Composable
private fun SheetAction(
    icon: DrawableResource,
    label: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val iconTint = if (destructive) colors.error else colors.textChip
    val labelColor = if (destructive) colors.error else colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
