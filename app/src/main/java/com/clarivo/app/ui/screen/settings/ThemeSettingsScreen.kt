package com.clarivo.app.ui.screen.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.clarivo.app.R
import com.clarivo.app.data.model.UiMode
import com.clarivo.app.ui.component.LocalClarivoMiuixBottomSpacing
import com.clarivo.app.ui.navigation3.LocalNavigator
import com.clarivo.app.ui.theme.LocalEnableBlur
import com.clarivo.app.ui.theme.ColorMode
import com.clarivo.app.ui.theme.LocalUiMode
import com.clarivo.app.ui.theme.keyColorOptions
import com.clarivo.app.ui.util.BlurredBar
import com.clarivo.app.ui.util.rememberBlurBackdrop
import com.clarivo.app.viewmodel.SettingsUiState
import com.clarivo.app.viewmodel.SettingsViewModel
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ThemeSettingsScreen(
    viewModel: SettingsViewModel
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.uiState.collectAsState()
    val onBack = dropUnlessResumed { navigator.pop() }

    when (LocalUiMode.current) {
        UiMode.Miuix -> ThemeSettingsMiuix(
            uiState = uiState,
            onBack = onBack,
            onSetThemeMode = viewModel::setThemeMode,
            onSetMiuixMonet = viewModel::setMiuixMonet,
            onSetKeyColor = viewModel::setKeyColor,
            onSetPaletteStyle = viewModel::setPaletteStyle,
            onSetColorSpec = viewModel::setColorSpec,
            onSetEnableBlur = viewModel::setEnableBlur,
            onSetEnableFloatingBottomBar = viewModel::setEnableFloatingBottomBar,
            onSetEnableFloatingBottomBarBlur = viewModel::setEnableFloatingBottomBarBlur,
            onSetEnablePredictiveBack = viewModel::setEnablePredictiveBack,
            onSetPageScale = viewModel::setPageScale
        )

        UiMode.Material -> ThemeSettingsMaterial(
            uiState = uiState,
            onBack = onBack,
            onSetColorMode = viewModel::setColorMode,
            onSetKeyColor = viewModel::setKeyColor,
            onSetPaletteStyle = viewModel::setPaletteStyle,
            onSetColorSpec = viewModel::setColorSpec,
            onSetPageScale = viewModel::setPageScale
        )
    }
}

@Composable
private fun ThemeSettingsMiuix(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSetThemeMode: (Int) -> Unit,
    onSetMiuixMonet: (Boolean) -> Unit,
    onSetKeyColor: (Int) -> Unit,
    onSetPaletteStyle: (PaletteStyle) -> Unit,
    onSetColorSpec: (ColorSpec.SpecVersion) -> Unit,
    onSetEnableBlur: (Boolean) -> Unit,
    onSetEnableFloatingBottomBar: (Boolean) -> Unit,
    onSetEnableFloatingBottomBarBlur: (Boolean) -> Unit,
    onSetEnablePredictiveBack: (Boolean) -> Unit,
    onSetPageScale: (Float) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val isDark = uiState.colorMode.isDark || (uiState.colorMode.isSystem && isSystemInDarkTheme())
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixTopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings_theme),
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            MiuixIcon(MiuixIcons.Back, contentDescription = null)
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    ThemePreviewCardMiuix(
                        keyColor = uiState.keyColor,
                        isDark = isDark,
                        miuixMonet = uiState.miuixMonet,
                        enableFloatingBottomBar = uiState.enableFloatingBottomBar,
                        enableFloatingBottomBarBlur = uiState.enableFloatingBottomBarBlur,
                        paletteStyle = uiState.paletteStyle,
                        colorSpec = uiState.colorSpec,
                    )
                    Spacer(modifier = Modifier.height(72.dp))

                    val themeItems = listOf(
                        stringResource(R.string.settings_theme_mode_system),
                        stringResource(R.string.settings_theme_mode_light),
                        stringResource(R.string.settings_theme_mode_dark),
                    )
                    TabRow(
                        tabs = themeItems,
                        selectedTabIndex = (if (uiState.colorMode.value >= 3) uiState.colorMode.value - 3 else uiState.colorMode.value).coerceIn(0, 2),
                        onTabSelected = onSetThemeMode,
                        height = 48.dp,
                    )

                MiuixCard(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.settings_monet),
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.Wallpaper,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_monet),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        checked = uiState.miuixMonet,
                        onCheckedChange = onSetMiuixMonet
                    )

                    AnimatedVisibility(visible = uiState.miuixMonet) {
                        Column {
                            val colorItems = listOf(
                                stringResource(R.string.settings_key_color_default),
                                stringResource(R.string.color_red),
                                stringResource(R.string.color_pink),
                                stringResource(R.string.color_purple),
                                stringResource(R.string.color_deep_purple),
                                stringResource(R.string.color_indigo),
                                stringResource(R.string.color_blue),
                                stringResource(R.string.color_cyan),
                                stringResource(R.string.color_teal),
                                stringResource(R.string.color_green),
                                stringResource(R.string.color_yellow),
                                stringResource(R.string.color_amber),
                                stringResource(R.string.color_orange),
                                stringResource(R.string.color_brown),
                                stringResource(R.string.color_blue_grey),
                                stringResource(R.string.color_sakura),
                            )
                            val colorValues = listOf(0) + keyColorOptions.map { it.first }
                            OverlayDropdownPreference(
                                title = stringResource(R.string.key_color),
                                items = colorItems,
                                startAction = {
                                    MiuixIcon(
                                        Icons.Rounded.Colorize,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.key_color),
                                        tint = MiuixTheme.colorScheme.onBackground
                                    )
                                },
                                selectedIndex = colorValues.indexOf(uiState.keyColor).takeIf { it >= 0 } ?: 0,
                                onSelectedIndexChange = { index ->
                                    onSetKeyColor(colorValues[index])
                                }
                            )

                            AnimatedVisibility(visible = uiState.keyColor != 0) {
                                Column {
                                    val styles = PaletteStyle.entries
                                    OverlayDropdownPreference(
                                        title = stringResource(R.string.settings_color_style),
                                        startAction = {
                                            MiuixIcon(
                                                Icons.Rounded.Style,
                                                modifier = Modifier.padding(end = 6.dp),
                                                contentDescription = stringResource(R.string.settings_color_style),
                                                tint = MiuixTheme.colorScheme.onBackground
                                            )
                                        },
                                        items = styles.map { it.name },
                                        selectedIndex = styles.indexOfFirst { it.name == uiState.paletteStyle.name }.coerceAtLeast(0),
                                        onSelectedIndexChange = { index ->
                                            onSetPaletteStyle(styles[index])
                                        }
                                    )

                                    val specs = ColorSpec.SpecVersion.entries
                                    OverlayDropdownPreference(
                                        title = stringResource(R.string.settings_color_spec),
                                        startAction = {
                                            MiuixIcon(
                                                Icons.Rounded.DesignServices,
                                                modifier = Modifier.padding(end = 6.dp),
                                                contentDescription = stringResource(R.string.settings_color_spec),
                                                tint = MiuixTheme.colorScheme.onBackground
                                            )
                                        },
                                        items = specs.map { it.name },
                                        selectedIndex = specs.indexOfFirst { it.name == uiState.colorSpec.name }.coerceAtLeast(0),
                                        onSelectedIndexChange = { index ->
                                            onSetColorSpec(specs[index])
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                MiuixCard(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_enable_blur),
                            summary = stringResource(R.string.settings_enable_blur_summary),
                            startAction = {
                                MiuixIcon(
                                    Icons.Rounded.BlurOn,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_enable_blur),
                                    tint = MiuixTheme.colorScheme.onBackground
                                )
                            },
                            checked = uiState.enableBlur,
                            onCheckedChange = onSetEnableBlur
                        )
                    }
                    SwitchPreference(
                        title = stringResource(R.string.settings_floating_bottom_bar),
                        summary = stringResource(R.string.settings_floating_bottom_bar_summary),
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.CallToAction,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_floating_bottom_bar),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        checked = uiState.enableFloatingBottomBar,
                        onCheckedChange = onSetEnableFloatingBottomBar
                    )
                    AnimatedVisibility(
                        visible = uiState.enableFloatingBottomBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_enable_glass),
                            summary = stringResource(R.string.settings_enable_glass_summary),
                            startAction = {
                                MiuixIcon(
                                    Icons.Rounded.WaterDrop,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_enable_glass),
                                    tint = MiuixTheme.colorScheme.onBackground
                                )
                            },
                            checked = uiState.enableFloatingBottomBarBlur,
                            onCheckedChange = onSetEnableFloatingBottomBarBlur
                        )
                    }
                }

                MiuixCard(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_enable_predictive_back),
                            summary = stringResource(R.string.settings_enable_predictive_back_summary),
                            startAction = {
                                MiuixIcon(
                                    Icons.AutoMirrored.Rounded.MenuOpen,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_enable_predictive_back),
                                    tint = MiuixTheme.colorScheme.onBackground
                                )
                            },
                            checked = uiState.enablePredictiveBack,
                            onCheckedChange = onSetEnablePredictiveBack
                        )
                    }

                    var sliderValue by remember(uiState.pageScale) { mutableFloatStateOf(uiState.pageScale) }
                    ArrowPreference(
                        title = stringResource(R.string.settings_page_scale),
                        summary = stringResource(R.string.settings_page_scale_summary),
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.AspectRatio,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_page_scale),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        endActions = {
                            MiuixText(
                                text = "${(sliderValue * 100).toInt()}%",
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = { },
                        bottomAction = {
                            MiuixSlider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = { onSetPageScale(sliderValue) },
                                valueRange = 0.8f..1.1f,
                                showKeyPoints = true,
                                keyPoints = listOf(0.8f, 0.9f, 1.0f, 1.1f),
                            )
                        }
                    )
                }
            }
                item {
                    Spacer(
                        Modifier.height(
                            LocalClarivoMiuixBottomSpacing.current + 12.dp
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSettingsMaterial(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSetColorMode: (ColorMode) -> Unit,
    onSetKeyColor: (Int) -> Unit,
    onSetPaletteStyle: (PaletteStyle) -> Unit,
    onSetColorSpec: (ColorSpec.SpecVersion) -> Unit,
    onSetPageScale: (Float) -> Unit,
) {
    val isDark = uiState.colorMode.isDark || (uiState.colorMode.isSystem && isSystemInDarkTheme())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_theme)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemePreviewCardMaterial(
                keyColor = uiState.keyColor,
                isDark = isDark,
                paletteStyle = uiState.paletteStyle,
                colorSpec = uiState.colorSpec,
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(keyColorOptions) { (color, _) ->
                    ColorButtonMaterial(
                        color = if (color == 0) Color.Unspecified else Color(color),
                        selected = uiState.keyColor == color,
                        isDark = isDark,
                        paletteStyle = uiState.paletteStyle,
                        colorSpec = uiState.colorSpec,
                        onClick = { onSetKeyColor(color) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ColorMode.SYSTEM to Icons.Filled.Brightness4,
                    ColorMode.LIGHT to Icons.Filled.Brightness7,
                    ColorMode.DARK to Icons.Filled.Brightness3,
                    ColorMode.DARK_AMOLED to Icons.Filled.Brightness1
                ).forEach { (mode, icon) ->
                    FilterChip(
                        selected = uiState.colorMode == mode,
                        onClick = { onSetColorMode(mode) },
                        label = {},
                        leadingIcon = { Icon(icon, contentDescription = colorModeLabel(mode)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    MaterialDropdownPreference(
                        title = stringResource(R.string.palette_style),
                        selected = uiState.paletteStyle,
                        options = PaletteStyle.entries.map { SettingOption(it, it.name) },
                        onSelected = onSetPaletteStyle
                    )
                    HorizontalDivider()
                    MaterialDropdownPreference(
                        title = stringResource(R.string.color_spec),
                        selected = uiState.colorSpec,
                        options = ColorSpec.SpecVersion.entries.map { SettingOption(it, it.name) },
                        onSelected = onSetColorSpec
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                var sliderValue by remember(uiState.pageScale) { mutableFloatStateOf(uiState.pageScale) }
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, contentDescription = null)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(text = stringResource(R.string.page_scale), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${(sliderValue * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onSetPageScale(sliderValue) },
                        valueRange = 0.8f..1.1f
                    )
                }
            }

            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
        }
    }
}

@Composable
private fun ThemePreviewCardMaterial(
    keyColor: Int,
    isDark: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenRatio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    val colorScheme = if (keyColor == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = if (keyColor == 0) MaterialTheme.colorScheme.primary else Color(keyColor),
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
        )
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .aspectRatio(screenRatio),
            color = colorScheme.background,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.app_name), color = colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorScheme.secondaryContainer)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(colorScheme.surfaceContainerHighest)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(colorScheme.surfaceContainerHighest)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorScheme.surfaceContainerHigh)
                    )
                }
                Surface(color = colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorButtonMaterial(
    color: Color,
    selected: Boolean,
    isDark: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (color == Color.Unspecified && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
        )
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(colorScheme.primaryContainer, 180f, 180f, true)
                drawArc(colorScheme.tertiaryContainer, 0f, 180f, true)
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(2.dp, colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, null, tint = colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewCardMiuix(
    keyColor: Int,
    isDark: Boolean,
    miuixMonet: Boolean,
    enableFloatingBottomBar: Boolean,
    enableFloatingBottomBarBlur: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
) {
    val configuration = LocalConfiguration.current
    val screenRatio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    val seedColor = if (keyColor == 0) MiuixTheme.colorScheme.primary else Color(keyColor)
    val effectiveStyle = if (keyColor == 0) PaletteStyle.TonalSpot else paletteStyle
    val effectiveSpec = if (keyColor == 0) ColorSpec.SpecVersion.Default else colorSpec
    val dynamic = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        style = effectiveStyle,
        specVersion = effectiveSpec,
    )
    val bgColor = if (miuixMonet) dynamic.background else MiuixTheme.colorScheme.surface
    val textColor = if (miuixMonet) dynamic.onSurface else MiuixTheme.colorScheme.onBackground
    val accentCardColor = when {
        miuixMonet -> dynamic.secondaryContainer
        isDark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val cardColor = if (miuixMonet) dynamic.surfaceContainerHighest else MiuixTheme.colorScheme.surfaceVariant
    val navBarColor = if (miuixMonet) dynamic.surfaceContainer else MiuixTheme.colorScheme.surface
    val iconColor = if (miuixMonet) dynamic.primary else MiuixTheme.colorScheme.primary
    val navSelectedColor = MiuixTheme.colorScheme.onSurfaceContainer
    val navUnselectedColor = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.5f)
    val permissionCardColor = when {
        miuixMonet -> dynamic.secondaryContainer
        isDark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val permissionIconColor = Color(0xFF36D167)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .aspectRatio(screenRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiuixText(text = stringResource(R.string.app_name), fontSize = 12.sp, color = textColor)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(62.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(permissionCardColor)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                    )
                }
            }

            if (enableFloatingBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .height(28.dp)
                        .fillMaxWidth(0.58f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (enableFloatingBottomBarBlur) navBarColor.copy(alpha = 0.5f) else navBarColor)
                        .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(textColor.copy(alpha = 0.1f))
                    )
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth()
                            .background(navBarColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun colorModeLabel(mode: ColorMode): String {
    return when (mode) {
        ColorMode.SYSTEM -> stringResource(R.string.mode_system)
        ColorMode.LIGHT -> stringResource(R.string.mode_light)
        ColorMode.DARK -> stringResource(R.string.mode_dark)
        ColorMode.MONET_SYSTEM -> stringResource(R.string.mode_monet_system)
        ColorMode.MONET_LIGHT -> stringResource(R.string.mode_monet_light)
        ColorMode.MONET_DARK -> stringResource(R.string.mode_monet_dark)
        ColorMode.DARK_AMOLED -> stringResource(R.string.mode_dark_amoled)
    }
}
