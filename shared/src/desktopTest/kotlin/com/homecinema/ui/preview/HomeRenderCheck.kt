package com.homecinema.ui.preview

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.homecinema.core.settings.ThemePreference
import com.homecinema.ui.home.HomeState
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
 * Smoke test + visual self-check: renders every Home state in Dark and Light at 390dp, density 2,
 * to PNGs. Thumbnails use a locally generated image (exercises the local-path branch, no network).
 */
class HomeRenderCheck {

    private val outDir: File = run {
        val preferred = File(System.getProperty("homeRenderDir") ?: DEFAULT_DIR)
        if (preferred.mkdirs() || preferred.isDirectory) preferred else File(System.getProperty("java.io.tmpdir"), "home-render").apply { mkdirs() }
    }

    @Test
    fun `renders every home state in dark and light`() {
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
                render(state, theme, file)
                assertTrue(file.length() > 0, "empty render: $file")
            }
        }
    }

    private fun render(state: HomeState, theme: ThemePreference, file: File) {
        val scene = ImageComposeScene(width = 780, height = 1800, density = Density(2f)) {
            HomePreviewFrame(state, theme)
        }
        try {
            // Let async work (resource fonts, Coil decoding) settle across a few frames.
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
            val file = File.createTempFile("home-thumb", ".png").apply { deleteOnExit() }
            file.writeBytes(data.bytes)
            file.absolutePath
        }.getOrNull()

    private companion object {
        const val FRAMES = 15
        const val DEFAULT_DIR =
            "/tmp/claude-1000/-home-oz-Projects-Personal-homecinema/abc4f7ab-0f48-43fe-b403-964668d658ec/scratchpad/render"
    }
}
