package com.homecinema.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homecinema.core.settings.ThemePreference
import com.homecinema.ui.history.ConfirmCard
import com.homecinema.ui.history.ConfirmRequest
import com.homecinema.ui.history.HistoryScreen
import com.homecinema.ui.history.HistoryState
import com.homecinema.ui.history.MoreMenu
import com.homecinema.ui.theme.AppTheme
import com.homecinema.ui.theme.appBackground
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HistoryPreviewFrame(state: HistoryState, theme: ThemePreference) {
    AppTheme(theme) {
        Box(Modifier.size(390.dp, 824.dp).appBackground()) {
            HistoryScreen(state = state, actions = HistoryPreviewData.actions, onBack = {})
        }
    }
}

/** The More menu with the Theme list expanded, on the app background. */
@Composable
fun ThemeMenuPreviewFrame(theme: ThemePreference) {
    AppTheme(theme) {
        Box(Modifier.size(390.dp, 824.dp).appBackground(), contentAlignment = Alignment.Center) {
            MoreMenu(theme = theme, onTheme = {}, onOpenDownloadsFolder = {}, onClearHistory = {}, initiallyThemeExpanded = true)
        }
    }
}

/** Both confirm dialog cards, on the app background. */
@Composable
fun ConfirmPreviewFrame(theme: ThemePreference) {
    AppTheme(theme) {
        Box(Modifier.size(390.dp, 824.dp).appBackground(), contentAlignment = Alignment.Center) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.size(340.dp, 420.dp),
            ) {
                ConfirmCard(ConfirmRequest.ClearHistory, onConfirm = {}, onDismiss = {})
                ConfirmCard(
                    ConfirmRequest.DeleteFile(HistoryPreviewData.populated().groups.first().second.first()),
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
    }
}

@Preview @Composable fun HistoryPopulatedDark() = HistoryPreviewFrame(HistoryPreviewData.populated(), ThemePreference.DARK)
@Preview @Composable fun HistoryPopulatedVolt() = HistoryPreviewFrame(HistoryPreviewData.populated(), ThemePreference.VOLT)
@Preview @Composable fun HistoryEmptyDark() = HistoryPreviewFrame(HistoryPreviewData.empty, ThemePreference.DARK)
@Preview @Composable fun HistoryEmptyVolt() = HistoryPreviewFrame(HistoryPreviewData.empty, ThemePreference.VOLT)
@Preview @Composable fun HistorySheetDark() = HistoryPreviewFrame(HistoryPreviewData.sheetOpen(), ThemePreference.DARK)
@Preview @Composable fun HistorySheetVolt() = HistoryPreviewFrame(HistoryPreviewData.sheetOpen(), ThemePreference.VOLT)
@Preview @Composable fun HistoryMenuDark() = HistoryPreviewFrame(HistoryPreviewData.menuOpen(), ThemePreference.DARK)
@Preview @Composable fun HistoryMenuVolt() = HistoryPreviewFrame(HistoryPreviewData.menuOpen(), ThemePreference.VOLT)
@Preview @Composable fun HistoryMissingDark() =
    HistoryPreviewFrame(HistoryPreviewData.populated(missingStreetFood = true), ThemePreference.DARK)
@Preview @Composable fun HistoryMissingVolt() =
    HistoryPreviewFrame(HistoryPreviewData.populated(missingStreetFood = true), ThemePreference.VOLT)
@Preview @Composable fun HistoryLongTitlesDark() = HistoryPreviewFrame(HistoryPreviewData.longTitles(), ThemePreference.DARK)
@Preview @Composable fun HistoryLongTitlesVolt() = HistoryPreviewFrame(HistoryPreviewData.longTitles(), ThemePreference.VOLT)
@Preview @Composable fun HistoryThemeMenuDark() = ThemeMenuPreviewFrame(ThemePreference.DARK)
@Preview @Composable fun HistoryConfirmDark() = ConfirmPreviewFrame(ThemePreference.DARK)
