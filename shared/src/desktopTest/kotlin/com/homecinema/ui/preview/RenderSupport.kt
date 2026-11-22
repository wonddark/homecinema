package com.homecinema.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import java.io.File
import java.util.Locale

private const val FRAMES = 15

/** Renders [content] at density 2 to a PNG, letting async work (resource fonts, Coil, animations) settle first. */
internal fun renderPng(file: File, width: Int, height: Int, content: @Composable () -> Unit) {
    // -PrenderLocale=es renders in that locale; set before the scene so resources resolve with it.
    val savedLocale = Locale.getDefault()
    System.getProperty("renderLocale")?.let { Locale.setDefault(Locale.forLanguageTag(it)) }
    val scene = ImageComposeScene(width = width, height = height, density = Density(2f), content = content)
    try {
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
        Locale.setDefault(savedLocale)
    }
}

/** A sunset-ish gradient PNG; null if Skia can't produce it (the thumbnail then shows its placeholder). */
internal fun writeFixtureThumbnail(): String? =
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
        val file = File.createTempFile("render-thumb", ".png").apply { deleteOnExit() }
        file.writeBytes(data.bytes)
        file.absolutePath
    }.getOrNull()
