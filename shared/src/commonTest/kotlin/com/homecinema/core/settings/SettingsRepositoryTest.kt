package com.homecinema.core.settings

import com.homecinema.core.storage.FakeFileIO
import com.homecinema.core.storage.JsonFileStore
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {
    private val io = FakeFileIO()

    private fun repo() = SettingsRepository(
        JsonFileStore(io, "/data/settings.json", AppSettings.serializer(), AppSettings())
    )

    @Test
    fun `theme persists across instances`() {
        assertEquals(ThemePreference.SYSTEM, repo().settings.value.theme)
        repo().setTheme(ThemePreference.OCEAN)
        assertEquals(ThemePreference.OCEAN, repo().settings.value.theme)
    }
}
