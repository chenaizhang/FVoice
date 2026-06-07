package com.fvoice.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvoice.app.FVoiceApplication
import com.fvoice.app.R
import com.fvoice.app.ui.theme.ColorMode
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.data.preferences.SettingsRepositoryImpl
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

data class SettingsUiState(
    val uiMode: UiMode = UiMode.Miuix,
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = true,
    val enableFloatingBottomBarBlur: Boolean = true,
    val pageScale: Float = 1.0f,
    val checkUpdate: Boolean = true,
    val language: String = "system",
    val enablePredictiveBack: Boolean = true,
    val backupMessage: String? = null
)

class SettingsViewModel : ViewModel() {

    private val settingsRepository = SettingsRepositoryImpl()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                uiMode = UiMode.fromValue(settingsRepository.uiMode),
                colorMode = ColorMode.fromValue(settingsRepository.colorMode),
                miuixMonet = settingsRepository.miuixMonet,
                keyColor = settingsRepository.keyColor,
                paletteStyle = try {
                    PaletteStyle.valueOf(settingsRepository.colorStyle)
                } catch (_: Exception) {
                    PaletteStyle.TonalSpot
                },
                colorSpec = try {
                    ColorSpec.SpecVersion.valueOf(settingsRepository.colorSpec)
                } catch (_: Exception) {
                    ColorSpec.SpecVersion.Default
                },
                enableBlur = settingsRepository.enableBlur,
                enableFloatingBottomBar = settingsRepository.enableFloatingBottomBar,
                enableFloatingBottomBarBlur = settingsRepository.enableFloatingBottomBarBlur,
                pageScale = settingsRepository.pageScale,
                checkUpdate = settingsRepository.checkUpdate,
                language = settingsRepository.language,
                enablePredictiveBack = settingsRepository.enablePredictiveBack,
            )
        }
    }

    fun setUiMode(mode: UiMode) {
        val oldMode = UiMode.fromValue(settingsRepository.uiMode)
        val currentColorMode = ColorMode.fromValue(settingsRepository.colorMode)

        val newColorMode = when {
            oldMode == UiMode.Material && mode == UiMode.Miuix -> {
                val baseMode = if (currentColorMode == ColorMode.DARK_AMOLED) {
                    ColorMode.DARK
                } else {
                    currentColorMode
                }
                if (settingsRepository.miuixMonet && !baseMode.isMonet) {
                    ColorMode.fromValue(baseMode.toMonetMode())
                } else if (!settingsRepository.miuixMonet && baseMode.isMonet) {
                    ColorMode.fromValue(baseMode.toNonMonetMode())
                } else {
                    baseMode
                }
            }
            oldMode == UiMode.Miuix && mode == UiMode.Material && currentColorMode.isMonet -> {
                ColorMode.fromValue(currentColorMode.toNonMonetMode())
            }
            else -> currentColorMode
        }
        settingsRepository.uiMode = mode.value
        settingsRepository.colorMode = newColorMode.value
        loadSettings()
    }

    fun setThemeMode(mode: Int) {
        val effectiveMode = if (settingsRepository.uiMode == UiMode.Miuix.value && _uiState.value.miuixMonet) {
            mode + 3
        } else {
            mode
        }
        settingsRepository.colorMode = effectiveMode
        loadSettings()
    }

    fun setColorMode(mode: ColorMode) {
        settingsRepository.colorMode = mode.value
        loadSettings()
    }

    fun setMiuixMonet(enabled: Boolean) {
        val currentMode = ColorMode.fromValue(settingsRepository.colorMode)
        val newMode = if (enabled) {
            if (!currentMode.isMonet) ColorMode.fromValue(currentMode.toMonetMode()) else currentMode
        } else {
            if (currentMode.isMonet) ColorMode.fromValue(currentMode.toNonMonetMode()) else currentMode
        }
        settingsRepository.miuixMonet = enabled
        settingsRepository.colorMode = newMode.value
        loadSettings()
    }

    fun setKeyColor(color: Int) {
        settingsRepository.keyColor = color
        loadSettings()
    }

    fun setPaletteStyle(style: PaletteStyle) {
        settingsRepository.colorStyle = style.name
        loadSettings()
    }

    fun setColorSpec(spec: ColorSpec.SpecVersion) {
        settingsRepository.colorSpec = spec.name
        loadSettings()
    }

    fun setEnableBlur(enabled: Boolean) {
        settingsRepository.enableBlur = enabled
        loadSettings()
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        settingsRepository.enableFloatingBottomBar = enabled
        loadSettings()
    }

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) {
        settingsRepository.enableFloatingBottomBarBlur = enabled
        loadSettings()
    }

    fun setCheckUpdate(enabled: Boolean) {
        settingsRepository.checkUpdate = enabled
        loadSettings()
    }

    fun checkForUpdate(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val url = java.net.URL("https://api.github.com/repos/chenaizhang/FVoice/releases/latest")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                val latestTag = json.optString("tag_name", "").removePrefix("v")
                val currentVersion = com.fvoice.app.BuildConfig.VERSION_NAME
                val ctx = FVoiceApplication.instance
                val hasUpdate = latestTag.isNotBlank() && latestTag != currentVersion
                onResult(hasUpdate, if (hasUpdate) ctx.getString(R.string.update_latest_version, latestTag) else ctx.getString(R.string.update_already_latest))
            } catch (e: Exception) {
                onResult(false, FVoiceApplication.instance.getString(R.string.update_check_failed, e.message ?: ""))
            }
        }
    }

    fun setLanguage(language: String) {
        settingsRepository.language = language
        loadSettings()
    }

    fun setEnablePredictiveBack(enabled: Boolean) {
        settingsRepository.enablePredictiveBack = enabled
        loadSettings()
    }

    fun setPageScale(scale: Float) {
        settingsRepository.pageScale = scale
        loadSettings()
    }

    fun exportConfig(outputStream: OutputStream) {
        runCatching {
            val json = JSONObject()
                .put("ui_mode", settingsRepository.uiMode)
                .put("color_mode", settingsRepository.colorMode)
                .put("miuix_monet", settingsRepository.miuixMonet)
                .put("key_color", settingsRepository.keyColor)
                .put("color_style", settingsRepository.colorStyle)
                .put("color_spec", settingsRepository.colorSpec)
                .put("enable_blur", settingsRepository.enableBlur)
                .put("enable_floating_bottom_bar", settingsRepository.enableFloatingBottomBar)
                .put("enable_floating_bottom_bar_blur", settingsRepository.enableFloatingBottomBarBlur)
                .put("page_scale", settingsRepository.pageScale)
                .put("check_update", settingsRepository.checkUpdate)
                .put("language", settingsRepository.language)
                .put("enable_predictive_back", settingsRepository.enablePredictiveBack)
            outputStream.bufferedWriter().use { it.write(json.toString(2)) }
            setBackupMessage(FVoiceApplication.instance.getString(R.string.config_exported))
        }.onFailure {
            setBackupMessage(FVoiceApplication.instance.getString(R.string.config_export_failed, it.message ?: FVoiceApplication.instance.getString(R.string.error_unknown)))
        }
    }

    fun importConfig(inputStream: InputStream) {
        runCatching {
            val json = JSONObject(inputStream.bufferedReader().use { it.readText() })
            settingsRepository.uiMode = json.optString("ui_mode", settingsRepository.uiMode)
            settingsRepository.colorMode = json.optInt("color_mode", settingsRepository.colorMode)
            settingsRepository.miuixMonet = json.optBoolean("miuix_monet", settingsRepository.miuixMonet)
            settingsRepository.keyColor = json.optInt("key_color", settingsRepository.keyColor)
            settingsRepository.colorStyle = json.optString("color_style", settingsRepository.colorStyle)
            settingsRepository.colorSpec = json.optString("color_spec", settingsRepository.colorSpec)
            settingsRepository.enableBlur = json.optBoolean("enable_blur", settingsRepository.enableBlur)
            settingsRepository.enableFloatingBottomBar =
                json.optBoolean("enable_floating_bottom_bar", settingsRepository.enableFloatingBottomBar)
            settingsRepository.enableFloatingBottomBarBlur =
                json.optBoolean("enable_floating_bottom_bar_blur", settingsRepository.enableFloatingBottomBarBlur)
            settingsRepository.pageScale =
                json.optDouble("page_scale", settingsRepository.pageScale.toDouble()).toFloat().coerceIn(0.8f, 1.2f)
            settingsRepository.checkUpdate = json.optBoolean("check_update", settingsRepository.checkUpdate)
            settingsRepository.language = json.optString("language", settingsRepository.language)
            settingsRepository.enablePredictiveBack =
                json.optBoolean("enable_predictive_back", settingsRepository.enablePredictiveBack)
            loadSettings()
            setBackupMessage(FVoiceApplication.instance.getString(R.string.config_imported))
        }.onFailure {
            setBackupMessage(FVoiceApplication.instance.getString(R.string.config_import_failed, it.message ?: FVoiceApplication.instance.getString(R.string.error_unknown)))
        }
    }

    fun clearBackupMessage() {
        _uiState.update { it.copy(backupMessage = null) }
    }

    private fun setBackupMessage(message: String) {
        _uiState.update { it.copy(backupMessage = message) }
    }
}
