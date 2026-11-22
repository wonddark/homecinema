package com.homecinema.ui.preview

import androidx.compose.runtime.Composable
import com.homecinema.core.settings.ThemePreference
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Visual self-check for History: renders the designed states (and the Review Focus 5 long-title
 * case) at 390×824dp, density 2, to PNGs. Opt-in: runs only when the `historyRenderDir` system
 * property is set (Gradle: `-PhistoryRenderDir=<dir>`).
 */
class HistoryRenderCheck {

    @Test
    fun `renders history states`() {
        val dir = System.getProperty("historyRenderDir") ?: return
        val outDir = File(dir).apply { mkdirs() }
        val thumb = writeFixtureThumbnail()
        val frames: Map<String, @Composable () -> Unit> = mapOf(
            "history-populated-dark" to { HistoryPreviewFrame(HistoryPreviewData.populated(thumb), ThemePreference.DARK) },
            "history-missing-volt" to {
                HistoryPreviewFrame(HistoryPreviewData.populated(thumb, missingStreetFood = true), ThemePreference.VOLT)
            },
            "history-populated-light" to { HistoryPreviewFrame(HistoryPreviewData.populated(thumb), ThemePreference.LIGHT) },
            "history-empty-dark" to { HistoryPreviewFrame(HistoryPreviewData.empty, ThemePreference.DARK) },
            "history-sheet-dark" to { HistoryPreviewFrame(HistoryPreviewData.sheetOpen(thumb), ThemePreference.DARK) },
            "history-sheet-missing-volt" to {
                HistoryPreviewFrame(HistoryPreviewData.missingSheetOpen(thumb), ThemePreference.VOLT)
            },
            "history-menu-dark" to { HistoryPreviewFrame(HistoryPreviewData.menuOpen(thumb), ThemePreference.DARK) },
            "history-long-titles-dark" to { HistoryPreviewFrame(HistoryPreviewData.longTitles(thumb), ThemePreference.DARK) },
            "history-theme-menu-dark" to { ThemeMenuPreviewFrame(ThemePreference.DARK) },
            "history-confirm-dark" to { ConfirmPreviewFrame(ThemePreference.DARK) },
        )
        for ((name, content) in frames) {
            val file = File(outDir, "$name.png")
            renderPng(file, 780, 1648, content)
            assertTrue(file.length() > 0, "empty render: $file")
        }
    }
}
