package com.homecinema.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homecinema.core.settings.ThemePreference
import com.homecinema.ui.home.HomeScreen
import com.homecinema.ui.home.HomeState
import com.homecinema.ui.theme.AppTheme
import com.homecinema.ui.theme.appBackground
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomePreviewFrame(state: HomeState, theme: ThemePreference) {
    AppTheme(theme) {
        Box(Modifier.width(390.dp).appBackground()) {
            HomeScreen(state = state, actions = HomePreviewData.actions, onOpenHistory = {})
        }
    }
}

@Preview @Composable fun HomeEmptyDark() = HomePreviewFrame(HomePreviewData.empty, ThemePreference.DARK)
@Preview @Composable fun HomeEmptyLight() = HomePreviewFrame(HomePreviewData.empty, ThemePreference.LIGHT)
@Preview @Composable fun HomeFetchingDark() = HomePreviewFrame(HomePreviewData.fetching, ThemePreference.DARK)
@Preview @Composable fun HomeFetchingLight() = HomePreviewFrame(HomePreviewData.fetching, ThemePreference.LIGHT)
@Preview @Composable fun HomeFailedDark() = HomePreviewFrame(HomePreviewData.failed, ThemePreference.DARK)
@Preview @Composable fun HomeFailedLight() = HomePreviewFrame(HomePreviewData.failed, ThemePreference.LIGHT)
@Preview @Composable fun HomeReadyDark() = HomePreviewFrame(HomePreviewData.ready(), ThemePreference.DARK)
@Preview @Composable fun HomeReadyLight() = HomePreviewFrame(HomePreviewData.ready(), ThemePreference.LIGHT)
@Preview @Composable fun HomeDownloadingDark() = HomePreviewFrame(HomePreviewData.downloading(), ThemePreference.DARK)
@Preview @Composable fun HomeDownloadingLight() = HomePreviewFrame(HomePreviewData.downloading(), ThemePreference.LIGHT)
