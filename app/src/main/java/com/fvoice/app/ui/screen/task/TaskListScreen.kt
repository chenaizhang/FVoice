package com.fvoice.app.ui.screen.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fvoice.app.R
import com.fvoice.app.core.model.ProcessTaskStatus
import com.fvoice.app.core.model.ProcessTaskType
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.component.FVoiceMaterialLogEntryItem
import com.fvoice.app.ui.component.FVoiceMaterialMessageCard
import com.fvoice.app.ui.component.FVoiceMaterialSegmentedColumn
import com.fvoice.app.ui.component.FVoiceMiuixLogEntryCard
import com.fvoice.app.ui.component.DeleteConfirmDialog
import com.fvoice.app.ui.component.FVoiceMiuixMessageCard
import com.fvoice.app.ui.component.LocalFVoiceMiuixBottomSpacing
import com.fvoice.app.ui.component.SearchStatus
import com.fvoice.app.ui.component.material.FVoiceMaterialSearchAppBar
import com.fvoice.app.ui.component.miuix.FVoiceSearchBarFake
import com.fvoice.app.ui.component.miuix.SearchBox
import com.fvoice.app.ui.component.miuix.SearchPager
import com.fvoice.app.ui.navigation3.LocalMainPagerState
import com.fvoice.app.ui.navigation3.LocalNavigator
import com.fvoice.app.ui.navigation3.Route
import com.fvoice.app.ui.theme.LocalEnableBlur
import com.fvoice.app.ui.theme.LocalUiMode
import com.fvoice.app.ui.util.BlurredBar
import com.fvoice.app.ui.util.rememberBlurBackdrop
import com.fvoice.app.viewmodel.TaskFilter
import com.fvoice.app.viewmodel.TaskItem
import com.fvoice.app.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onClearHistory: () -> Unit = {},
    onTaskClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh when Task page becomes visible in pager or when returning from detail
    val mainPagerState = LocalMainPagerState.current
    val navigator = LocalNavigator.current
    val isTopLevel by remember {
        derivedStateOf {
            navigator.backStack.lastOrNull() is Route.Main
        }
    }
    androidx.compose.runtime.LaunchedEffect(isTopLevel, mainPagerState.pagerState.settledPage) {
        if (mainPagerState.pagerState.settledPage == 1 && isTopLevel) {
            viewModel.refreshTasks()
        }
    }

    val denoiseLabel = stringResource(R.string.task_type_denoise)
    val transcribeLabel = stringResource(R.string.task_type_transcribe)
    val denoiseTranscribeLabel = stringResource(R.string.task_type_denoise_and_transcribe)
    val extractAudioLabel = stringResource(R.string.task_type_extract_audio)
    val realtimeRecordLabel = stringResource(R.string.task_type_realtime_record)
    val realtimeTranscribeLabel = stringResource(R.string.task_type_realtime_transcribe)
    val processModeLabels = mapOf(
        ProcessTaskType.DENOISE.labelKey to denoiseLabel,
        ProcessTaskType.TRANSCRIBE.labelKey to transcribeLabel,
        ProcessTaskType.DENOISE_AND_TRANSCRIBE.labelKey to denoiseTranscribeLabel,
        ProcessTaskType.EXTRACT_AUDIO.labelKey to extractAudioLabel,
        ProcessTaskType.REALTIME_RECORD.labelKey to realtimeRecordLabel,
        ProcessTaskType.REALTIME_TRANSCRIBE.labelKey to realtimeTranscribeLabel,
    )
    fun processModeLabelNonComposable(key: String): String = processModeLabels[key] ?: key

    val filteredTasks = uiState.tasks
        .sortedWith(
            compareBy<TaskItem> {
                if (it.status == ProcessTaskStatus.PROCESSING || it.status == ProcessTaskStatus.PENDING) 0 else 1
            }.thenBy {
                if (it.status == ProcessTaskStatus.PROCESSING || it.status == ProcessTaskStatus.PENDING) it.processingOrder else Int.MAX_VALUE
            }.thenByDescending {
                it.completedAtMillis
            },
        )
        .filter { task ->
            val matchesFilter = when (uiState.filter) {
                TaskFilter.ALL -> true
                TaskFilter.PROCESSING -> task.status == ProcessTaskStatus.PROCESSING || task.status == ProcessTaskStatus.PENDING
                TaskFilter.COMPLETED -> task.status == ProcessTaskStatus.COMPLETED
            }
            val matchesSearch = uiState.searchQuery.isBlank() ||
                    task.fileName.contains(uiState.searchQuery, ignoreCase = true) ||
                    processModeLabelNonComposable(task.processMode).contains(uiState.searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }

    val filterItems = listOf(
        stringResource(R.string.filter_all),
        stringResource(R.string.filter_processing),
        stringResource(R.string.filter_completed),
    )
    val selectedFilterIndex = when (uiState.filter) {
        TaskFilter.ALL -> 0
        TaskFilter.PROCESSING -> 1
        TaskFilter.COMPLETED -> 2
    }
    val onFilterSelected: (Int) -> Unit = { index ->
        viewModel.setFilter(
            when (index) {
                1 -> TaskFilter.PROCESSING
                2 -> TaskFilter.COMPLETED
                else -> TaskFilter.ALL
            },
        )
    }

    var searchStatus by remember { mutableStateOf(SearchStatus(label = "")) }

    LaunchedEffect(uiState.searchQuery, filteredTasks) {
        searchStatus = searchStatus.copy(
            searchText = uiState.searchQuery,
            resultStatus = if (uiState.searchQuery.isBlank()) SearchStatus.ResultStatus.DEFAULT else SearchStatus.ResultStatus.SHOW,
        )
    }

    fun onSearchStatusChange(nextStatus: SearchStatus) {
        searchStatus = nextStatus.copy(
            resultStatus = if (nextStatus.searchText.isBlank()) SearchStatus.ResultStatus.DEFAULT else SearchStatus.ResultStatus.SHOW,
        )
        viewModel.setSearchQuery(nextStatus.searchText)
    }

    var showClearConfirm by remember { mutableStateOf(false) }

    when (LocalUiMode.current) {
        UiMode.Miuix -> TaskListMiuix(
            tasks = filteredTasks,
            searchStatus = searchStatus,
            onSearchStatusChange = ::onSearchStatusChange,
            filterItems = filterItems,
            selectedFilterIndex = selectedFilterIndex,
            onFilterSelected = onFilterSelected,
            onClearHistory = { showClearConfirm = true },
            onTaskClick = onTaskClick,
        )

        UiMode.Material -> TaskListMaterial(
            tasks = filteredTasks,
            searchStatus = searchStatus,
            onSearchStatusChange = ::onSearchStatusChange,
            filterItems = filterItems,
            selectedFilterIndex = selectedFilterIndex,
            onFilterSelected = onFilterSelected,
            onClearHistory = { showClearConfirm = true },
            onTaskClick = onTaskClick,
        )
    }

    DeleteConfirmDialog(
        show = showClearConfirm,
        onConfirm = onClearHistory,
        onDismiss = { showClearConfirm = false }
    )
}

@Composable
private fun TaskListMiuix(
    tasks: List<TaskItem>,
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    filterItems: List<String>,
    selectedFilterIndex: Int,
    onFilterSelected: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onTaskClick: (String) -> Unit = {},
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    val layoutDirection = LocalLayoutDirection.current
    val dynamicTopPadding by remember { derivedStateOf { 12.dp * (1f - scrollBehavior.state.collapsedFraction) } }
    val density = LocalDensity.current
    val showFilterPopup = remember { mutableStateOf(false) }

    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                searchStatus.TopAppBarAnim(backgroundColor = barColor) {
                    MiuixTopAppBar(
                        color = barColor,
                        title = stringResource(R.string.nav_tasks),
                        scrollBehavior = scrollBehavior,
                        actions = {
                            Box {
                                OverlayListPopup(
                                    show = showFilterPopup.value,
                                    popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                                    alignment = PopupPositionProvider.Align.TopEnd,
                                    onDismissRequest = {
                                        showFilterPopup.value = false
                                    },
                                    content = {
                                        ListPopupColumn {
                                            filterItems.forEachIndexed { index, item ->
                                                val isSelected = index == selectedFilterIndex
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showFilterPopup.value = false
                                                            onFilterSelected(index)
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    MiuixText(
                                                        text = item,
                                                        color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                                                    )
                                                }
                                            }
                                        }
                                    },
                                )
                                MiuixIconButton(
                                    onClick = { showFilterPopup.value = true },
                                ) {
                                    MiuixIcon(
                                        imageVector = MiuixIcons.Filter,
                                        tint = MiuixTheme.colorScheme.onSurface,
                                        contentDescription = stringResource(R.string.task_filter_title),
                                    )
                                }
                            }
                        },
                        bottomContent = {
                            Box(
                                modifier = Modifier
                                    .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
                                    .onGloballyPositioned { coordinates ->
                                        with(density) {
                                            val newOffsetY = coordinates.positionInWindow().y.toDp()
                                            if (searchStatus.offsetY != newOffsetY) {
                                                onSearchStatusChange(searchStatus.copy(offsetY = newOffsetY))
                                            }
                                        }
                                    }
                                    .then(
                                        if (searchStatus.isCollapsed()) {
                                            Modifier.pointerInput(Unit) {
                                                detectTapGestures {
                                                    onSearchStatusChange(searchStatus.copy(current = SearchStatus.Status.EXPANDING))
                                                }
                                            }
                                        } else Modifier,
                                    ),
                            ) {
                                FVoiceSearchBarFake(
                                    label = searchStatus.label,
                                    searchBarTopPadding = dynamicTopPadding,
                                )
                            }
                        }
                    )
                }
            }
        },
        popupHost = {
            searchStatus.SearchPager(
                onSearchStatusChange = onSearchStatusChange,
                defaultResult = {},
                searchBarTopPadding = dynamicTopPadding,
                result = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical(),
                    ) {
                        if (tasks.isEmpty()) {
                            item {
                                FVoiceMiuixMessageCard(
                                    title = stringResource(R.string.nav_tasks),
                                    summary = stringResource(R.string.task_subtitle),
                                )
                            }
                        } else {
                            items(tasks.size, key = { index -> tasks[index].id }) { index ->
                                val task = tasks[index]
                                FVoiceMiuixLogEntryCard(
                                    title = task.fileName,
                                    description = processModeLabel(task.processMode),
                                    timestamp = taskCompletionText(task),
                                    tags = listOf(taskTypeTag(task.isRealtime)),
                                    status = taskStatusLabel(task.status),
                                    statusColor = taskStatusColorMiuix(task.status),
                                    onClick = { onTaskClick(task.id) },
                                )
                            }
                        }
                        item {
                            Spacer(Modifier.height(LocalFVoiceMiuixBottomSpacing.current + 12.dp))
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        searchStatus.SearchBox {
            Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 6.dp,
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                    ),
                    overscrollEffect = null,
                ) {
                    if (tasks.isEmpty()) {
                        item {
                            FVoiceMiuixMessageCard(
                                title = stringResource(R.string.nav_tasks),
                                summary = stringResource(R.string.task_subtitle),
                            )
                        }
                    } else {
                        items(tasks.size, key = { index -> tasks[index].id }) { index ->
                            val task = tasks[index]
                            FVoiceMiuixLogEntryCard(
                                title = task.fileName,
                                description = processModeLabel(task.processMode),
                                timestamp = taskCompletionText(task),
                                tags = listOf(taskTypeTag(task.isRealtime)),
                                status = taskStatusLabel(task.status),
                                statusColor = taskStatusColorMiuix(task.status),
                                onClick = { onTaskClick(task.id) },
                            )
                        }
                    }

                    if (tasks.isNotEmpty()) {
                        item {
                            MiuixButton(
                                onClick = onClearHistory,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                MiuixText(stringResource(R.string.clear_history_title))
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(LocalFVoiceMiuixBottomSpacing.current + 12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskListMaterial(
    tasks: List<TaskItem>,
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    filterItems: List<String>,
    selectedFilterIndex: Int,
    onFilterSelected: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onTaskClick: (String) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FVoiceMaterialSearchAppBar(
                title = { Text(stringResource(R.string.nav_tasks)) },
                searchText = searchStatus.searchText,
                onSearchTextChange = {
                    onSearchStatusChange(searchStatus.copy(searchText = it))
                },
                onClearClick = {
                    onSearchStatusChange(searchStatus.copy(searchText = ""))
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = stringResource(R.string.task_filter_title),
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                    ) {
                        filterItems.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                trailingIcon = {
                                    if (index == selectedFilterIndex) {
                                        Icon(
                                            imageVector = Icons.Filled.FilterList,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                onClick = {
                                    showFilterMenu = false
                                    onFilterSelected(index)
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                searchContent = { bottomPadding, _ ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp + bottomPadding,
                        ),
                    ) {
                        if (tasks.isEmpty()) {
                            item {
                                FVoiceMaterialMessageCard(
                                    title = stringResource(R.string.nav_tasks),
                                    summary = stringResource(R.string.task_subtitle),
                                )
                            }
                        } else {
                            item {
                                FVoiceMaterialSegmentedColumn(itemCount = tasks.size) { index ->
                                    val task = tasks[index]
                                    FVoiceMaterialLogEntryItem(
                                        title = task.fileName,
                                        description = processModeLabel(task.processMode),
                                        timestamp = taskCompletionText(task),
                                        tags = listOf(taskTypeTag(task.isRealtime)),
                                        status = taskStatusLabel(task.status),
                                        statusColor = taskStatusColorMaterial(task.status),
                                        onClick = { onTaskClick(task.id) },
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (tasks.isEmpty()) {
                    item {
                        FVoiceMaterialMessageCard(
                            title = stringResource(R.string.nav_tasks),
                            summary = stringResource(R.string.task_subtitle),
                        )
                    }
                } else {
                    item {
                        FVoiceMaterialSegmentedColumn(itemCount = tasks.size) { index ->
                            val task = tasks[index]
                            FVoiceMaterialLogEntryItem(
                                title = task.fileName,
                                description = processModeLabel(task.processMode),
                                timestamp = taskCompletionText(task),
                                tags = listOf(taskTypeTag(task.isRealtime)),
                                status = taskStatusLabel(task.status),
                                statusColor = taskStatusColorMaterial(task.status),
                                onClick = { onTaskClick(task.id) },
                            )
                        }
                    }
                }

                if (tasks.isNotEmpty()) {
                    item {
                        TextButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.clear_history_title))
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
                }
            }
        }
    }
}

@Composable
private fun taskCompletionText(task: TaskItem): String {
    return if (task.status == ProcessTaskStatus.PROCESSING || task.status == ProcessTaskStatus.PENDING) {
        ""
    } else {
        formatCompletedAt(task.completedAtMillis)
    }
}

@Composable
private fun taskStatusLabel(status: ProcessTaskStatus): String {
    return when (status) {
        ProcessTaskStatus.COMPLETED -> stringResource(R.string.status_completed)
        ProcessTaskStatus.PROCESSING -> stringResource(R.string.status_processing)
        ProcessTaskStatus.FAILED -> stringResource(R.string.status_failed)
        ProcessTaskStatus.PENDING -> stringResource(R.string.status_pending)
        ProcessTaskStatus.CANCELLED -> stringResource(R.string.status_cancelled)
    }
}

@Composable
private fun taskStatusColorMiuix(status: ProcessTaskStatus): Color {
    return when (status) {
        ProcessTaskStatus.COMPLETED -> MiuixTheme.colorScheme.primary
        ProcessTaskStatus.PROCESSING -> MiuixTheme.colorScheme.secondaryVariant
        ProcessTaskStatus.FAILED -> MiuixTheme.colorScheme.error
        ProcessTaskStatus.PENDING -> MiuixTheme.colorScheme.onSurfaceVariantActions
        ProcessTaskStatus.CANCELLED -> MiuixTheme.colorScheme.onSurfaceVariantActions
    }
}

@Composable
private fun taskStatusColorMaterial(status: ProcessTaskStatus): Color {
    return when (status) {
        ProcessTaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        ProcessTaskStatus.PROCESSING -> MaterialTheme.colorScheme.tertiary
        ProcessTaskStatus.FAILED -> MaterialTheme.colorScheme.error
        ProcessTaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        ProcessTaskStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatCompletedAt(millis: Long): String {
    if (millis <= 0) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
}

private val taskTypeLabelKeys = mapOf(
    ProcessTaskType.DENOISE.labelKey to R.string.task_type_denoise,
    ProcessTaskType.TRANSCRIBE.labelKey to R.string.task_type_transcribe,
    ProcessTaskType.DENOISE_AND_TRANSCRIBE.labelKey to R.string.task_type_denoise_and_transcribe,
    ProcessTaskType.EXTRACT_AUDIO.labelKey to R.string.task_type_extract_audio,
    ProcessTaskType.REALTIME_RECORD.labelKey to R.string.task_type_realtime_record,
    ProcessTaskType.REALTIME_TRANSCRIBE.labelKey to R.string.task_type_realtime_transcribe,
)

@Composable
private fun processModeLabel(key: String): String {
    val resId = taskTypeLabelKeys[key] ?: return key
    return stringResource(resId)
}

@Composable
private fun taskTypeTag(isRealtime: Boolean): String {
    return if (isRealtime) stringResource(R.string.task_type_realtime) else stringResource(R.string.task_type_file)
}
