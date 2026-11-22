package com.homecinema.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homecinema.core.rememberPlatformActions
import com.homecinema.ui.history.HistoryActions
import com.homecinema.ui.history.HistoryScreen
import com.homecinema.ui.history.HistoryState
import com.homecinema.ui.history.HistoryViewModel
import com.homecinema.ui.home.HomeActions
import com.homecinema.ui.home.HomeScreen
import com.homecinema.ui.theme.AppTheme
import com.homecinema.ui.theme.appBackground
import kotlinx.datetime.TimeZone

private enum class Screen { HOME, HISTORY }

/**
 * The shared app root: theme, background, insets, a centred 480dp column and Home/History switching.
 *
 * The [HistoryViewModel] lives in this composition (scope from [rememberCoroutineScope]) rather than in
 * [AppGraph]: it depends on the composition's [rememberPlatformActions] and its eager state collection
 * should stop when the UI goes away, while downloads (and Home) live on in the graph's scope.
 */
@Composable
fun App(graph: AppGraph) {
    val appSettings by graph.settings.settings.collectAsState()
    val platformActions = rememberPlatformActions()
    val uiScope = rememberCoroutineScope()
    val historyVm = remember(graph, platformActions, uiScope) {
        HistoryViewModel(
            history = graph.history,
            settings = graph.settings,
            actions = platformActions,
            downloadDir = graph.downloadDir,
            scope = uiScope,
            clock = graph.clock,
            zone = TimeZone.currentSystemDefault(),
        )
    }
    val homeActions = remember(graph) { HomeActions.from(graph.home) }
    val historyActions = remember(historyVm) { HistoryActions.from(historyVm) }
    var screen by remember { mutableStateOf(Screen.HOME) }

    AppTheme(appSettings.theme) {
        Box(Modifier.fillMaxSize().appBackground()) {
            Box(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(Modifier.fillMaxHeight().widthIn(max = 480.dp).fillMaxWidth()) {
                    when (screen) {
                        Screen.HOME -> {
                            val homeState by graph.home.state.collectAsState()
                            HomeScreen(homeState, homeActions, onOpenHistory = { screen = Screen.HISTORY })
                        }
                        Screen.HISTORY -> {
                            val historyState by historyVm.state.collectAsState()
                            LaunchedEffect(historyVm) { historyVm.onShown() }
                            val goHome = { screen = Screen.HOME }
                            PlatformBackHandler(enabled = true) { historyBack(historyState, historyActions, goHome) }
                            HistoryScreen(historyState, historyActions, onBack = goHome)
                        }
                    }
                }
            }
        }
    }
}

/** Back on History closes the innermost layer: confirm dialog, then file sheet, then More menu, then goes Home. */
private fun historyBack(state: HistoryState, actions: HistoryActions, goHome: () -> Unit) {
    when {
        state.confirm != null -> actions.onDismissConfirm()
        state.sheetFor != null -> actions.onDismissSheet()
        state.moreMenuOpen -> actions.onToggleMore()
        else -> goHome()
    }
}
