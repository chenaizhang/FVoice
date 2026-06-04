package com.fvoice.app.ui.navigation

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.fvoice.app.ui.component.BottomNavBar
import com.fvoice.app.ui.component.LocalFVoiceMiuixBottomSpacing
import com.fvoice.app.ui.navigation3.LocalMainPagerState
import com.fvoice.app.ui.navigation3.LocalNavigator
import com.fvoice.app.ui.navigation3.MainPagerState
import com.fvoice.app.ui.navigation3.Route
import com.fvoice.app.ui.navigation3.rememberMainPagerState
import com.fvoice.app.ui.navigation3.rememberNavigator
import com.fvoice.app.ui.screen.about.AboutScreen
import com.fvoice.app.ui.screen.home.HomeScreen
import com.fvoice.app.ui.screen.home.PermissionSettingsScreen
import com.fvoice.app.ui.screen.process.ProcessSettingsScreen
import com.fvoice.app.ui.screen.processing.ProcessingScreen
import com.fvoice.app.ui.screen.result.ResultDetailScreen
import com.fvoice.app.ui.screen.settings.SettingsScreen
import com.fvoice.app.ui.screen.settings.ThemeSettingsScreen
import com.fvoice.app.ui.screen.task.TaskListScreen
import com.fvoice.app.ui.screen.transcript.TranscriptScreen
import com.fvoice.app.ui.screen.welcome.WelcomeScreen
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.theme.LocalEnableBlur
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBar
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBarBlur
import com.fvoice.app.ui.theme.LocalUiMode
import com.fvoice.app.ui.util.rememberBlurBackdrop
import com.fvoice.app.viewmodel.HomeViewModel
import com.fvoice.app.viewmodel.SettingsViewModel
import com.fvoice.app.viewmodel.TaskViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

@Composable
fun AppNavigation(
    isFirstLaunch: Boolean,
    onFirstLaunchComplete: () -> Unit,
) {
    val startRoute = if (isFirstLaunch) Route.Welcome else Route.Main
    val navigator = rememberNavigator(startRoute)
    val context = LocalContext.current
    val activity = context as? Activity

    CompositionLocalProvider(LocalNavigator provides navigator) {
        val navDisplay: @Composable () -> Unit = {
            NavDisplay(
                backStack = navigator.backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                onBack = {
                    if (navigator.backStackSize() == 1) {
                        activity?.finish()
                    } else {
                        navigator.pop()
                    }
                },
                entryProvider = entryProvider {
                    entry<Route.Welcome> {
                        WelcomeScreen(
                            onGetStarted = {
                                onFirstLaunchComplete()
                                navigator.replaceAll(listOf(Route.Main))
                            }
                        )
                    }

                    entry<Route.Main> {
                        MainPager()
                    }

                    entry<Route.ProcessSettings> {
                        ProcessSettingsScreen(
                            onStartProcess = { navigator.push(Route.Processing) },
                            onBack = { navigator.pop() }
                        )
                    }

                    entry<Route.Processing> {
                        ProcessingScreen(
                            onCancel = { navigator.pop() },
                            onComplete = { navigator.replace(Route.ResultDetail) }
                        )
                    }

                    entry<Route.ResultDetail> {
                        ResultDetailScreen(onBack = { navigator.pop() })
                    }

                    entry<Route.ThemeSettings> {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        ThemeSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navigator.pop() }
                        )
                    }

                    entry<Route.PermissionSettings> {
                        PermissionSettingsScreen(onBack = { navigator.pop() })
                    }

                    entry<Route.About> {
                        AboutScreen(onBack = { navigator.pop() })
                    }
                }
            )
        }

        when (LocalUiMode.current) {
            UiMode.Material -> Scaffold { navDisplay() }
            UiMode.Miuix -> MiuixScaffold { navDisplay() }
        }
    }
}

@Composable
private fun MainPager() {
    val navigator = LocalNavigator.current
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { MainPagerTab.entries.size }
    )
    val mainPagerState = rememberMainPagerState(pagerState)

    LaunchedEffect(pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    MainScreenBackHandler(
        enabled = mainPagerState.selectedPage != 0,
        onBack = { mainPagerState.animateToPage(0) }
    )

    CompositionLocalProvider(
        com.fvoice.app.ui.navigation3.LocalMainPagerState provides mainPagerState
    ) {
        MainScreenWithNavBar {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = MainPagerTab.entries.size - 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (MainPagerTab.entries[page]) {
                    MainPagerTab.Home -> {
                        val homeViewModel: HomeViewModel = viewModel()
                        HomeScreen(
                            viewModel = homeViewModel,
                            onImportAudio = { navigator.push(Route.ProcessSettings) },
                            onImportVideo = { navigator.push(Route.ProcessSettings) },
                            onTaskClick = { navigator.push(Route.ResultDetail) },
                            onNavigateToPermissions = { navigator.push(Route.PermissionSettings) }
                        )
                    }

                    MainPagerTab.Task -> {
                        val taskViewModel: TaskViewModel = viewModel()
                        TaskListScreen(viewModel = taskViewModel)
                    }

                    MainPagerTab.Transcript -> TranscriptScreen()

                    MainPagerTab.Settings -> {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateToThemeSettings = { navigator.push(Route.ThemeSettings) },
                            onNavigateToAbout = { navigator.push(Route.About) }
                        )
                    }
                }
            }
        }
    }
}

private enum class MainPagerTab {
    Home, Task, Transcript, Settings
}

@Composable
private fun MainScreenBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enabled,
        onBackCompleted = onBack
    )
}

@Composable
private fun MainScreenWithNavBar(
    content: @Composable () -> Unit
) {
    val mainPagerState = com.fvoice.app.ui.navigation3.LocalMainPagerState.current
    val blurBackdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val surfaceColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixScaffold(
            bottomBar = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    BottomNavBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                    )
                }
            },
            contentWindowInsets = WindowInsets.systemBars
        ) { innerPadding ->
            val bottomSpacing = innerPadding.calculateBottomPadding()
            CompositionLocalProvider(
                LocalFVoiceMiuixBottomSpacing provides bottomSpacing
            ) {
                Box(
                    modifier = Modifier
                        .then(if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier)
                        .then(
                            if (enableFloatingBottomBar && enableFloatingBottomBarBlur) {
                                Modifier.layerBackdrop(backdrop)
                            } else {
                                Modifier
                            }
                        )
                        .fillMaxSize()
                ) {
                    content()
                }
            }
        }

        UiMode.Material -> Scaffold(
            bottomBar = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    BottomNavBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                    )
                }
            },
            contentWindowInsets = WindowInsets.systemBars
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                content()
            }
        }
    }
}
