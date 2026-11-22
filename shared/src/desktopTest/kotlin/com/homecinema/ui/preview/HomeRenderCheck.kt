package com.homecinema.ui.preview

import com.homecinema.core.settings.ThemePreference
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Visual self-check: renders every Home state in Dark and Light at 390dp, density 2, to PNGs.
 * Thumbnails use a locally generated image (local-path branch, no network). Opt-in: runs only when
 * the `homeRenderDir` system property is set (Gradle: `-PhomeRenderDir=<dir>`).
 */
class HomeRenderCheck {

    @Test
    fun `renders every home state in dark and light`() {
        val dir = System.getProperty("homeRenderDir") ?: return
        val outDir = File(dir).apply { mkdirs() }
        val thumb = writeFixtureThumbnail()
        val states = mapOf(
            "1-empty" to HomePreviewData.empty,
            "2-fetching" to HomePreviewData.fetching,
            "2b-failed" to HomePreviewData.failed,
            "3-ready" to HomePreviewData.ready(thumb),
            "4-downloading" to HomePreviewData.downloading(thumb),
        )
        for ((name, state) in states) {
            for (theme in listOf(ThemePreference.DARK, ThemePreference.LIGHT)) {
                val file = File(outDir, "home-$name-${theme.name.lowercase()}.png")
                renderPng(file, 780, 1800) { HomePreviewFrame(state, theme) }
                assertTrue(file.length() > 0, "empty render: $file")
            }
        }
    }
}
