package com.homecinema.core.settings

import com.homecinema.core.storage.JsonFileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

enum class ThemePreference { SYSTEM, DARK, LIGHT, OCEAN, VOLT }

@Serializable
data class AppSettings(val theme: ThemePreference = ThemePreference.SYSTEM)

class SettingsRepository(private val store: JsonFileStore<AppSettings>) {
    private val _settings = MutableStateFlow(store.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setTheme(theme: ThemePreference) {
        val updated = _settings.value.copy(theme = theme)
        store.save(updated)
        _settings.value = updated
    }
}
