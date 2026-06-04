package com.fvoice.app.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.data.preferences.SettingsRepositoryImpl
import com.fvoice.app.ui.theme.AppSettings
import com.fvoice.app.ui.theme.ColorMode
import com.fvoice.app.ui.theme.ThemeController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val appSettings: AppSettings = AppSettings(
        colorMode = ColorMode.SYSTEM,
        keyColor = 0,
        paletteStyle = com.materialkolor.PaletteStyle.TonalSpot,
        colorSpec = com.materialkolor.dynamiccolor.ColorSpec.SpecVersion.Default
    ),
    val uiMode: UiMode = UiMode.Miuix,
    val enableBlur: Boolean = false,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val pageScale: Float = 1.0f,
    val isFirstLaunch: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepositoryImpl()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in setOf(
                "color_mode",
                "miuix_monet",
                "key_color",
                "color_style",
                "color_spec",
                "enable_blur",
                "enable_floating_bottom_bar",
                "enable_floating_bottom_bar_blur",
                "ui_mode",
                "page_scale"
            )
        ) {
            refreshSettings()
        }
    }

    init {
        settingsRepository.registerListener(prefsListener)
        refreshSettings()
        checkFirstLaunch()
    }

    private fun refreshSettings() {
        val context = getApplication<Application>().applicationContext
        val appSettings = ThemeController.getAppSettings(context)
        val uiMode = UiMode.fromValue(settingsRepository.uiMode)
        val enableBlur = settingsRepository.enableBlur
        val enableFloatingBottomBar = settingsRepository.enableFloatingBottomBar
        val enableFloatingBottomBarBlur = settingsRepository.enableFloatingBottomBarBlur
        val pageScale = settingsRepository.pageScale
        _uiState.update {
            it.copy(
                appSettings = appSettings,
                uiMode = uiMode,
                enableBlur = enableBlur,
                enableFloatingBottomBar = enableFloatingBottomBar,
                enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
                pageScale = pageScale
            )
        }
    }

    private fun checkFirstLaunch() {
        val firstLaunch = settingsRepository.prefs.getBoolean("first_launch", true)
        _uiState.update { it.copy(isFirstLaunch = firstLaunch) }
    }

    fun markFirstLaunchComplete() {
        settingsRepository.prefs.edit().putBoolean("first_launch", false).apply()
        _uiState.update { it.copy(isFirstLaunch = false) }
    }

    override fun onCleared() {
        settingsRepository.unregisterListener(prefsListener)
        super.onCleared()
    }
}
