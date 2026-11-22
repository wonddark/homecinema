package com.homecinema.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.core.settings.ThemePreference
import com.homecinema.domain.HistoryEntry
import com.homecinema.ui.components.AppIcons
import com.homecinema.ui.components.EmptyState
import com.homecinema.ui.components.GlassSurface
import com.homecinema.ui.components.HistoryRow
import com.homecinema.ui.components.SectionLabel
import com.homecinema.ui.components.Snackbar
import com.homecinema.ui.components.Wordmark
import com.homecinema.ui.text.filterLabel
import com.homecinema.ui.text.historyGroupLabel
import com.homecinema.ui.text.historySummaryText
import com.homecinema.ui.text.snackbarText
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.action_back
import homecinema.shared.generated.resources.action_more
import homecinema.shared.generated.resources.history_empty_body
import homecinema.shared.generated.resources.history_empty_title
import homecinema.shared.generated.resources.history_search_hint
import homecinema.shared.generated.resources.history_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The [HistoryViewModel] actions the History screen can trigger. */
@Immutable
data class HistoryActions(
    val onQuery: (String) -> Unit,
    val onFilter: (HistoryFilter) -> Unit,
    val onRowClick: (HistoryEntry) -> Unit,
    val onOpenSheet: (HistoryEntry) -> Unit,
    val onDismissSheet: () -> Unit,
    val onPlay: () -> Unit,
    val onShare: () -> Unit,
    val onShowInFolder: () -> Unit,
    val onRemove: () -> Unit,
    val onConfirm: () -> Unit,
    val onDismissConfirm: () -> Unit,
    val onToggleMore: () -> Unit,
    val onOpenDownloadsFolder: () -> Unit,
    val onClearHistory: () -> Unit,
    val onTheme: (ThemePreference) -> Unit,
    val onSnackbarShown: () -> Unit,
) {
    companion object {
        fun from(vm: HistoryViewModel) = HistoryActions(
            onQuery = vm::onQuery,
            onFilter = vm::onFilter,
            onRowClick = vm::onRowClick,
            onOpenSheet = vm::onOpenSheet,
            onDismissSheet = vm::onDismissSheet,
            onPlay = vm::onPlay,
            onShare = vm::onShare,
            onShowInFolder = vm::onShowInFolder,
            onRemove = vm::onRemove,
            onConfirm = vm::onConfirm,
            onDismissConfirm = vm::onDismissConfirm,
            onToggleMore = vm::onToggleMore,
            onOpenDownloadsFolder = vm::onOpenDownloadsFolder,
            onClearHistory = vm::onClearHistory,
            onTheme = vm::onTheme,
            onSnackbarShown = vm::onSnackbarShown,
        )
    }
}

/**
 * History: header, search, filter chips and the day-grouped list, plus the More menu, the file
 * sheet, confirm dialogs and the snackbar. Stateless; the app background and system insets are
 * applied by the root.
 */
@Composable
fun HistoryScreen(
    state: HistoryState,
    actions: HistoryActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                onBack = onBack,
                onMore = actions.onToggleMore,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            )
            HistoryList(state, actions)
        }
        if (state.moreMenuOpen) {
            MoreMenuOverlay(
                theme = state.theme,
                onDismiss = actions.onToggleMore,
                onTheme = actions.onTheme,
                onOpenDownloadsFolder = actions.onOpenDownloadsFolder,
                onClearHistory = actions.onClearHistory,
            )
        }
        state.sheetFor?.let { entry -> FileSheet(entry, actions) }
        state.confirm?.let { request ->
            ConfirmDialog(request = request, onConfirm = actions.onConfirm, onDismiss = actions.onDismissConfirm)
        }
        state.snackbar?.let { snackbar ->
            Snackbar(
                message = snackbarText(snackbar),
                onShown = actions.onSnackbarShown,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            )
        }
    }
}

/** Back + wordmark/"History" titles + More. Fixed at the top so the More menu can overlay its button. */
@Composable
internal fun Header(onBack: () -> Unit, onMore: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(HEADER_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderButton(AppIcons.ChevronLeft, stringResource(Res.string.action_back), onBack)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Wordmark(showMark = false)
            Text(
                text = stringResource(Res.string.history_title),
                color = LocalAppColors.current.textSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        HeaderButton(AppIcons.Ellipsis, stringResource(Res.string.action_more), onMore)
    }
}

internal val HEADER_HEIGHT = 44.dp

/** 44dp round glass button; [active] = the More button while its menu is open. */
@Composable
internal fun HeaderButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val colors = LocalAppColors.current
    val base = Modifier.size(HEADER_HEIGHT).clip(AppShapes.round)
    val styled = if (active) {
        base.background(colors.control).border(1.dp, colors.controlActive, AppShapes.round)
    } else {
        base.background(colors.surface)
    }
    Box(
        modifier = styled.clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HistoryList(state: HistoryState, actions: HistoryActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
    ) {
        item(key = "search") { SearchField(state.query, actions.onQuery) }
        item(key = "filters") {
            FilterBar(state, actions.onFilter, Modifier.padding(top = 20.dp))
        }
        if (state.groups.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(Res.string.history_empty_title),
                    body = stringResource(Res.string.history_empty_body),
                    showSteps = false,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
        state.groups.forEach { (group, entries) ->
            item(key = "label-${group.name}") {
                SectionLabel(historyGroupLabel(group), Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
            itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                HistoryRow(
                    entry = entry,
                    onClick = { actions.onRowClick(entry) },
                    onMore = { actions.onOpenSheet(entry) },
                    showDivider = index < entries.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val colors = LocalAppColors.current
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary, fontSize = 15.sp)
    GlassSurface(modifier = Modifier.fillMaxWidth().height(48.dp), shape = AppShapes.toggle) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(AppIcons.Search),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(stringResource(Res.string.history_search_hint), style = textStyle, color = colors.textTertiary)
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun FilterBar(state: HistoryState, onFilter: (HistoryFilter) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryFilter.entries.forEach { filter ->
                FilterChip(
                    label = filterLabel(filter),
                    count = state.counts[filter] ?: 0,
                    selected = filter == state.filter,
                    onClick = { onFilter(filter) },
                )
            }
        }
        Text(
            text = historySummaryText(state.summaryCount, state.summaryBytes),
            color = LocalAppColors.current.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun FilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val labelColor = if (selected) colors.onAccent else colors.textChip
    val countColor = if (selected) colors.onAccent.copy(alpha = 0.7f) else colors.textTertiary
    val background = if (selected) colors.accent else colors.surface
    val border = if (selected) Color.Transparent else colors.border
    Row(
        modifier = Modifier
            .clip(AppShapes.pill)
            .background(background)
            .border(1.dp, border, AppShapes.pill)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = count.toString(),
            color = countColor,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}
