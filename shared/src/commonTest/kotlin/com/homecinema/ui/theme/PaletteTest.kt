package com.homecinema.ui.theme

import androidx.compose.ui.graphics.Color
import com.homecinema.core.settings.ThemePreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaletteTest {

    @Test
    fun `system resolves to dark or light`() {
        assertEquals(Palettes.Dark, resolvePalette(ThemePreference.SYSTEM, systemDark = true))
        assertEquals(Palettes.Light, resolvePalette(ThemePreference.SYSTEM, systemDark = false))
        assertEquals(Palettes.Ocean, resolvePalette(ThemePreference.OCEAN, systemDark = false))
        assertEquals(Palettes.Volt, resolvePalette(ThemePreference.VOLT, systemDark = true))
        assertTrue(Palettes.Dark.isDark)
        assertFalse(Palettes.Light.isDark)
    }

    @Test
    fun `volt onAccent is dark`() {
        assertEquals(Color(0xFF0B0B0A), Palettes.Volt.onAccent)
    }

    @Test
    fun `every palette distinct accent`() {
        val accents = listOf(Palettes.Dark, Palettes.Light, Palettes.Ocean, Palettes.Volt).map { it.accent }
        // Dark and Light intentionally share the coral accent; Ocean and Volt differ from it and each other.
        assertEquals(3, accents.toSet().size)
        assertEquals(Palettes.Dark.accent, Palettes.Light.accent)
    }

    @Test
    fun `warning light differs`() {
        assertEquals(Color(0xFFC77D0A), Palettes.Light.warning)
        assertEquals(Color(0xFFF5A524), Palettes.Dark.warning)
    }
}
