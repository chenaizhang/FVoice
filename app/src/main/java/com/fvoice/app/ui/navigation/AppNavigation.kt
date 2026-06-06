package com.fvoice.app.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import com.fvoice.app.ui.component.BottomNavBar
import com.fvoice.app.ui.component.LocalFVoiceMiuixBottomSpacing
import com.fvoice.app.ui.navigation3.LocalMainPagerState
import com.fvoice.app.ui.navigation3.LocalNavigator
import com.fvoice.app.ui.navigation3.MainPagerState
import com.fvoice.app.ui.navigation3.Navigator
import com.fvoice.app.ui.navigation3.Route
import com.fvoice.app.ui.navigation3.rememberMainPagerState
import com.fvoice.app.ui.screen.about.AboutScreen
import com.fvoice.app.ui.screen.home.HomeScreen
import com.fvoice.app.ui.screen.home.PermissionSettingsScreen
import com.fvoice.app.ui.screen.process.ProcessSettingsScreen
import com.fvoice.app.ui.screen.processing.ProcessingScreen
import com.fvoice.app.ui.screen.result.ResultDetailScreen
import com.fvoice.app.ui.screen.settings.SettingsScreen
import com.fvoice.app.ui.screen.settings.ThemeSettingsScreen
import com.fvoice.app.ui.screen.task.TaskListScreen
import com.fvoice.app.R
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.theme.LocalEnableBlur
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBar
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBarBlur
import com.fvoice.app.ui.theme.LocalUiMode
import com.fvoice.app.ui.animation.predictiveback.MiuixPredictiveBackAnimation
import com.fvoice.app.ui.animation.predictiveback.NoPredictiveBackAnimation
import com.fvoice.app.ui.util.rememberBlurBackdrop
import com.fvoice.app.viewmodel.HomeViewModel
import com.fvoice.app.viewmodel.SettingsViewModel
import com.fvoice.app.viewmodel.TaskViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

@Composable
fun AppNavigation(
    navigator: Navigator,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
    enablePredictiveBack: Boolean = true,
) {
    val mainScreenEntry: @Composable () -> Unit = {
        MainPager(
            initialPage = initialPage,
            onPageChanged = onPageChanged,
        )
    }

    val navDisplay: @Composable () -> Unit = {
        val predictiveBackAnimationHandler = remember(enablePredictiveBack) {
            if (enablePredictiveBack) MiuixPredictiveBackAnimation() else NoPredictiveBackAnimation()
        }

        val navigationScope = rememberCoroutineScope()
        var gestureState: NavigationEventState<SceneInfo<NavKey>>? = null
        val onBack: () -> Unit = {
            navigationScope.launch {
                predictiveBackAnimationHandler.onBackPressed(
                    gestureState?.transitionState,
                    navigator.current()
                )
                navigator.pop()
            }
        }

        val entries = rememberDecoratedNavEntries(
            backStack = navigator.backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
                NavEntryDecorator(
                    onPop = { key ->
                        predictiveBackAnimationHandler.onPagePop(
                            contentPageKey = key,
                            animationScope = navigationScope
                        )
                    }
                ) { content ->
                    with(predictiveBackAnimationHandler) {
                        Box(
                            modifier = Modifier.predictiveBackAnimationDecorator(
                                gestureState?.transitionState,
                                content.contentKey,
                                navigator.current()
                            )
                        ) {
                            content.Content()
                        }
                    }
                }
            ),
            entryProvider = entryProvider {
                entry<Route.Main> { mainScreenEntry() }
                entry<Route.Home> { mainScreenEntry() }
                entry<Route.Task> { mainScreenEntry() }
                entry<Route.Settings> { mainScreenEntry() }

                entry<Route.ProcessSettings> { route: Route.ProcessSettings ->
                    ProcessSettingsScreen(
                        sourceUri = route.sourceUri,
                        fileName = route.fileName,
                        onStartProcess = { taskId ->
                            navigator.push(Route.Processing(taskId))
                        }
                    )
                }

                entry<Route.Processing> { route: Route.Processing ->
                    ProcessingScreen(
                        taskId = route.taskId,
                        onComplete = { completedTaskId ->
                            navigator.replace(Route.ResultDetail(completedTaskId))
                        }
                    )
                }

                entry<Route.ResultDetail> { route: Route.ResultDetail ->
                    ResultDetailScreen(
                        taskId = route.taskId
                    )
                }

                entry<Route.ThemeSettings> {
                    val settingsViewModel: SettingsViewModel = viewModel()
                    ThemeSettingsScreen(
                        viewModel = settingsViewModel
                    )
                }

                entry<Route.PermissionSettings> {
                    PermissionSettingsScreen()
                }

                entry<Route.About> {
                    AboutScreen()
                }

                entry<Route.ModelManager> { route: Route.ModelManager ->
                    com.fvoice.app.ui.screen.modelmanager.ModelManagerScreen(
                        category = route.category
                    )
                }
            }
        )

        val sceneState = rememberSceneState(
            entries = entries,
            sceneStrategies = listOf(SinglePaneSceneStrategy()),
            sceneDecoratorStrategies = emptyList(),
            sharedTransitionScope = null,
            onBack = onBack
        )
        val scene = sceneState.currentScene

        val currentInfo = SceneInfo(scene)
        val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
        gestureState = rememberNavigationEventState(
            currentInfo = currentInfo,
            backInfo = previousSceneInfos
        )

        NavigationBackHandler(
            state = gestureState,
            isBackEnabled = scene.previousEntries.isNotEmpty(),
            onBackCompleted = onBack,
            onBackCancelled = {}
        )

        NavDisplay(
            sceneState = sceneState,
            navigationEventState = gestureState,
            contentAlignment = Alignment.TopStart,
            sizeTransform = null,
            transitionEffects = NavDisplayTransitionEffects(
                blockInputDuringTransition = true
            ),
            predictivePopTransitionSpec = { swipeEdge ->
                with(predictiveBackAnimationHandler) {
                    onPredictivePopTransitionSpec(swipeEdge = swipeEdge)
                }
            },
            popTransitionSpec = {
                with(predictiveBackAnimationHandler) {
                    onPopTransitionSpec()
                }
            },
            transitionSpec = {
                with(predictiveBackAnimationHandler) {
                    onTransitionSpec()
                }
            }
        )
    }

    when (LocalUiMode.current) {
        UiMode.Material -> Scaffold { navDisplay() }
        UiMode.Miuix -> MiuixScaffold { navDisplay() }
    }
}

@Composable
private fun MainPager(
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { MainPagerTab.entries.size }
    )
    val mainPagerState = rememberMainPagerState(pagerState)

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        onPageChanged(settledPage)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    CompositionLocalProvider(
        com.fvoice.app.ui.navigation3.LocalMainPagerState provides mainPagerState
    ) {
        MainScreenBackHandler(
            mainState = mainPagerState,
            navController = navigator
        )

        MainScreenWithNavBar { pagerBackdropModifier ->
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = MainPagerTab.entries.size - 1,
                modifier = Modifier
                    .fillMaxSize()
                    .then(pagerBackdropModifier)
            ) { page ->
                when (MainPagerTab.entries[page]) {
                    MainPagerTab.Home -> {
                        val homeViewModel: HomeViewModel = viewModel()
                        val audioLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            if (uri != null) {
                                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "audio_file"
                                navigator.push(Route.ProcessSettings(sourceUri = uri.toString(), fileName = name))
                            }
                        }
                        val videoLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            if (uri != null) {
                                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "video_file"
                                navigator.push(Route.ProcessSettings(sourceUri = uri.toString(), fileName = name))
                            }
                        }
                        HomeScreen(
                            viewModel = homeViewModel,
                            onImportAudio = { audioLauncher.launch(arrayOf("audio/*")) },
                            onImportVideo = { videoLauncher.launch(arrayOf("video/*")) },
                            onNavigateToPermissions = { navigator.push(Route.PermissionSettings) }
                        )
                    }

                    MainPagerTab.Task -> {
                        val taskViewModel: TaskViewModel = viewModel()
                        TaskListScreen(
                            viewModel = taskViewModel,
                            onClearHistory = { taskViewModel.clearHistory() },
                            onTaskClick = { taskId -> navigator.push(Route.ResultDetail(taskId)) }
                        )
                    }

                    MainPagerTab.Settings -> {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateToThemeSettings = { navigator.push(Route.ThemeSettings) },
                            onNavigateToAbout = { navigator.push(Route.About) },
                            onNavigateToModelManager = { category -> navigator.push(Route.ModelManager(category)) }
                        )
                    }
                }
            }
        }
    }
}

private enum class MainPagerTab {
    Home, Task, Settings
}

@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main && navController.backStackSize() == 1 && mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        }
    )
}

@Composable
private fun MainScreenWithNavBar(
    content: @Composable (Modifier) -> Unit
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
            val pagerBackdropModifier =
                if (enableFloatingBottomBar && enableFloatingBottomBarBlur) {
                    Modifier.layerBackdrop(backdrop)
                } else {
                    Modifier
                }
            CompositionLocalProvider(
                LocalFVoiceMiuixBottomSpacing provides bottomSpacing
            ) {
                Box(
                    modifier = Modifier
                        .then(if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier)
                        .fillMaxSize()
                ) {
                    content(pagerBackdropModifier)
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
                content(Modifier)
            }
        }
    }
}
