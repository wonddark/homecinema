package com.homecinema.ui.text

import com.homecinema.core.notification.NotificationCounts
import com.homecinema.core.notification.notificationText
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.failure_generic
import homecinema.shared.generated.resources.files_count
import homecinema.shared.generated.resources.history_title
import homecinema.shared.generated.resources.months_short
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class SpanishStringsTest {
    private fun withLocale(locale: Locale, block: suspend () -> Unit) {
        val saved = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            runTest { block() }
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun `es-MX resolves Spanish`() = withLocale(Locale.forLanguageTag("es-MX")) {
        assertEquals("Falló la descarga", getString(Res.string.failure_generic))
    }

    @Test
    fun `es-419 resolves Spanish`() = withLocale(Locale.forLanguageTag("es-419")) {
        assertEquals("Historial", getString(Res.string.history_title))
    }

    @Test
    fun `french falls back to English`() = withLocale(Locale.FRENCH) {
        assertEquals("Download failed", getString(Res.string.failure_generic))
    }

    @Test
    fun `spanish file counts`() = withLocale(Locale.forLanguageTag("es")) {
        assertEquals("0 archivos", getPluralString(Res.plurals.files_count, 0, 0))
        assertEquals("1 archivo", getPluralString(Res.plurals.files_count, 1, 1))
        assertEquals("2 archivos", getPluralString(Res.plurals.files_count, 2, 2))
    }

    @Test
    fun `spanish months`() = withLocale(Locale.forLanguageTag("es")) {
        assertEquals("nov", getStringArray(Res.array.months_short)[10])
    }

    @Test
    fun `spanish notification text`() = withLocale(Locale.forLanguageTag("es")) {
        assertEquals("Descargando 2 · 1 en cola", notificationText(NotificationCounts(2, 1, 0)))
        assertEquals("No hay descargas activas", notificationText(NotificationCounts(0, 0, 0)))
    }
}
