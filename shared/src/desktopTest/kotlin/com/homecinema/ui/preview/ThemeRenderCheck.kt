package com.homecinema.ui.preview

import androidx.compose.runtime.Composable
import com.homecinema.core.settings.ThemePreference
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Theme check: renders every Home state and the main History states in each of the four palettes, for
 * side-by-side comparison with the design frames. Opt-in: runs only when the `themeRenderDir` system
 * property is set (Gradle: `-PthemeRenderDir=<dir>`).
 */
class ThemeRenderCheck {

    @Test
    fun `renders home and history in every palette`() {
        val dir = System.getProperty("themeRenderDir") ?: return
        val outDir = File(dir).apply { mkdirs() }
        val thumb = writeFixtureThumbnail()
        val home = mapOf(
            "home-1-empty" to HomePreviewData.empty,
            "home-2-fetching" to HomePreviewData.fetching,
            "home-2b-failed" to HomePreviewData.failed,
            "home-3-ready" to HomePreviewData.ready(thumb),
            "home-4-downloading" to HomePreviewData.downloading(thumb),
        )
        val history = mapOf(
            "history-populated" to HistoryPreviewData.populated(thumb),
            "history-missing" to HistoryPreviewData.populated(thumb, missingStreetFood = true),
            "history-sheet" to HistoryPreviewData.sheetOpen(thumb),
            "history-menu" to HistoryPreviewData.menuOpen(thumb),
            "history-empty" to HistoryPreviewData.empty,
        )
        for (theme in PALETTES) {
            val suffix = theme.name.lowercase()
            for ((name, state) in home) {
                render(File(outDir, "$name-$suffix.png")) { HomePreviewFrame(state, theme) }
            }
            for ((name, state) in history) {
                render(File(outDir, "$name-$suffix.png")) { HistoryPreviewFrame(state, theme) }
            }
        }
    }

    private fun render(file: File, content: @Composable () -> Unit) {
        renderPng(file, 780, 1688, content)
        assertTrue(file.length() > 0, "empty render: $file")
    }

    private companion object {
        val PALETTES = listOf(ThemePreference.DARK, ThemePreference.LIGHT, ThemePreference.OCEAN, ThemePreference.VOLT)
    }
}
