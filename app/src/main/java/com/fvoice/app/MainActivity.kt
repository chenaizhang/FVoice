package com.fvoice.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.navigation.AppNavigation
import com.fvoice.app.ui.navigation3.LocalNavigator
import com.fvoice.app.ui.navigation3.Route
import com.fvoice.app.ui.navigation3.rememberNavigator
import com.fvoice.app.ui.theme.FVoiceTheme
import com.fvoice.app.ui.theme.LocalColorMode
import com.fvoice.app.ui.theme.LocalEnableBlur
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBar
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBarBlur
import com.fvoice.app.ui.theme.LocalUiMode
import com.fvoice.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val settingsRepository = com.fvoice.app.data.preferences.SettingsRepositoryImpl()
        val language = settingsRepository.language
        val locale = if (language == "system") {
            newBase.resources.configuration.locales.get(0)
        } else {
            java.util.Locale.Builder().setLanguage(language).build()
        }
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val mainViewModel: MainViewModel = viewModel { MainViewModel(application) }
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val darkMode = uiState.appSettings.colorMode.isDark ||
                    (uiState.appSettings.colorMode.isSystem && isSystemInDarkTheme())

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale) {
                Density(
                    density = systemDensity.density * uiState.pageScale,
                    fontScale = systemDensity.fontScale
                )
            }
            val initialRoute = remember { Route.Main }
            val navigator = rememberNavigator(initialRoute)
            val selectedMainPage = uiState.selectedMainPage

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalDensity provides density,
                LocalUiMode provides uiState.uiMode,
                LocalColorMode provides uiState.appSettings.colorMode.value,
                LocalEnableBlur provides uiState.enableBlur,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur,
            ) {
                FVoiceTheme(
                    appSettings = uiState.appSettings,
                    uiMode = uiState.uiMode
                ) {
                    AppNavigation(
                        navigator = navigator,
                        initialPage = selectedMainPage,
                        onPageChanged = mainViewModel::setSelectedMainPage,
                            enablePredictiveBack = uiState.enablePredictiveBack
                    )
                }
            }
        }
    }
}
