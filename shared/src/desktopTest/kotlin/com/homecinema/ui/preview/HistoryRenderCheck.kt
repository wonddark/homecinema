package com.homecinema.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.homecinema.core.settings.ThemePreference
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
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
            render(file, content)
            assertTrue(file.length() > 0, "empty render: $file")
        }
    }

    private fun render(file: File, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width = 780, height = 1648, density = Density(2f), content = content)
        try {
            // Let async work (resource fonts, Coil decoding, sheet animation) settle across a few frames.
            var time = 0L
            repeat(FRAMES) {
                scene.render(time)
                time += 100_000_000L
                Thread.sleep(60)
            }
            val bytes = scene.render(time).encodeToData(EncodedImageFormat.PNG)?.bytes ?: ByteArray(0)
            file.writeBytes(bytes)
        } finally {
            scene.close()
        }
    }

    /** A sunset-ish gradient PNG; null if Skia can't produce it (the thumbnail then shows its placeholder). */
    private fun writeFixtureThumbnail(): String? =
        runCatching {
            val surface = Surface.makeRasterN32Premul(320, 320)
            val paint = Paint().apply {
                shader = Shader.makeLinearGradient(
                    Point(0f, 0f),
                    Point(0f, 320f),
                    intArrayOf(Color.makeRGB(255, 170, 90), Color.makeRGB(200, 90, 80), Color.makeRGB(40, 50, 70)),
                )
            }
            surface.canvas.drawPaint(paint)
            val data = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG) ?: return null
            val file = File.createTempFile("history-thumb", ".png").apply { deleteOnExit() }
            file.writeBytes(data.bytes)
            file.absolutePath
        }.getOrNull()

    private companion object {
        const val FRAMES = 15
    }
}
