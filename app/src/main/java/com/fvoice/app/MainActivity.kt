package com.fvoice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.navigation.AppNavigation
import com.fvoice.app.ui.theme.FVoiceTheme
import com.fvoice.app.ui.theme.LocalColorMode
import com.fvoice.app.ui.theme.LocalEnableBlur
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBar
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBarBlur
import com.fvoice.app.ui.theme.LocalUiMode
import com.fvoice.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel { MainViewModel(application) }
            val uiState by mainViewModel.uiState.collectAsState()
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale) {
                Density(
                    density = systemDensity.density * uiState.pageScale,
                    fontScale = systemDensity.fontScale
                )
            }

            CompositionLocalProvider(
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
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            isFirstLaunch = uiState.isFirstLaunch,
                            onFirstLaunchComplete = { mainViewModel.markFirstLaunchComplete() }
                        )
                    }
                }
            }
        }
    }
}
