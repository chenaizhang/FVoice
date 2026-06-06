package com.fvoice.app.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.fvoice.app.data.model.UiMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    MONET_SYSTEM(3),
    MONET_LIGHT(4),
    MONET_DARK(5),
    DARK_AMOLED(6);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }

    val isSystem: Boolean get() = value == 0 || value == 3
    val isDark: Boolean get() = value == 2 || value == 5 || value == 6
    val isAmoled: Boolean get() = value == 6
    val isMonet: Boolean get() = value >= 3

    fun toNonMonetMode(): Int = when (this) {
        MONET_SYSTEM -> 0
        MONET_LIGHT -> 1
        MONET_DARK, DARK_AMOLED -> 2
        else -> value
    }

    fun toMonetMode(): Int = when (this) {
        SYSTEM -> 3
        LIGHT -> 4
        DARK -> 5
        else -> value
    }
}

data class AppSettings(
    val colorMode: ColorMode,
    val keyColor: Int,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
)

object ThemeController {
    fun getAppSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val uiMode = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        var colorModeValue = prefs.getInt("color_mode", ColorMode.SYSTEM.value)

        if (uiMode == UiMode.Miuix.value) {
            val miuixMonet = prefs.getBoolean("miuix_monet", false)
            val colorMode = ColorMode.fromValue(colorModeValue)
            colorModeValue = if (!miuixMonet && colorMode.isMonet) {
                colorMode.toNonMonetMode()
            } else if (miuixMonet && !colorMode.isMonet) {
                colorMode.toMonetMode()
            } else {
                colorModeValue
            }
        }

        val colorMode = ColorMode.fromValue(colorModeValue)
        val keyColor = prefs.getInt("key_color", 0)
        val paletteStyleStr = prefs.getString("color_style", PaletteStyle.TonalSpot.name)
        val paletteStyle = try {
            PaletteStyle.valueOf(paletteStyleStr!!)
        } catch (_: Exception) {
            PaletteStyle.TonalSpot
        }
        val colorSpecStr = prefs.getString("color_spec", ColorSpec.SpecVersion.Default.name)
        val colorSpec = try {
            ColorSpec.SpecVersion.valueOf(colorSpecStr!!)
        } catch (_: Exception) {
            ColorSpec.SpecVersion.Default
        }

        return AppSettings(colorMode, keyColor, paletteStyle, colorSpec)
    }
}

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

@Composable
fun FVoiceTheme(
    appSettings: AppSettings? = null,
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentAppSettings = appSettings ?: ThemeController.getAppSettings(context)

    when (uiMode) {
        UiMode.Miuix -> MiuixFVoiceTheme(
            appSettings = currentAppSettings,
            content = content
        )
        UiMode.Material -> MaterialFVoiceTheme(
            appSettings = currentAppSettings,
            content = content
        )
    }
}

@Composable
fun MaterialFVoiceTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appSettings.colorMode.value) {
        1, 4 -> false
        2, 5, 6 -> true
        else -> isSystemInDarkTheme()
    }
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && appSettings.keyColor == 0

    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        appSettings.keyColor != 0 -> rememberDynamicColorScheme(
            seedColor = androidx.compose.ui.graphics.Color(appSettings.keyColor),
            isDark = darkTheme,
            isAmoled = appSettings.colorMode.isAmoled,
            style = appSettings.paletteStyle
        )
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            setWindowBarColors(window, colorScheme.background.toArgb())
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Suppress("DEPRECATION")
private fun setWindowBarColors(window: Window, color: Int) {
    window.statusBarColor = color
    window.navigationBarColor = color
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    return when (LocalColorMode.current) {
        1, 4 -> false
        2, 5, 6 -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalColorMode = staticCompositionLocalOf { 0 }
val LocalEnableBlur = staticCompositionLocalOf { false }
val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }
val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { false }
val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }
