package com.fvoice.app.ui.screen.processing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fvoice.app.R
import com.fvoice.app.core.model.ProcessTaskStatus
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.service.ProcessForegroundService
import com.fvoice.app.ui.component.FVoiceMiuixCard
import com.fvoice.app.ui.component.FVoiceMiuixInfoRow
import com.fvoice.app.ui.component.LocalFVoiceMiuixBottomSpacing
import com.fvoice.app.ui.navigation3.LocalNavigator
import com.fvoice.app.ui.theme.LocalUiMode
import com.fvoice.app.viewmodel.ProcessingViewModel
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    taskId: String,
    onComplete: (String) -> Unit
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel: ProcessingViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var showLog by remember { mutableStateOf(false) }
    val cancelAndBack = dropUnlessResumed {
        viewModel.cancel()
        ProcessForegroundService.stop(context)
        navigator.pop()
    }

    val stages = listOf(
        stringResource(R.string.stage_read_file),
        stringResource(R.string.stage_extract_audio),
        stringResource(R.string.stage_denoise),
        stringResource(R.string.stage_merge_video),
        stringResource(R.string.stage_transcribe),
        stringResource(R.string.stage_export)
    )

    val currentStageIndex = remember(uiState.progress.currentStage) {
        stages.indexOfFirst { uiState.progress.currentStage.contains(it) }.coerceAtLeast(0)
    }

    val progress = uiState.progress.percent.coerceIn(0, 100)

    LaunchedEffect(taskId) {
        if (taskId.isNotBlank()) {
            com.fvoice.app.FVoiceApplication.processTaskManager.startTask(taskId) {
                ProcessForegroundService.start(context, taskId)
            }
        }
    }

    LaunchedEffect(uiState.task?.status) {
        if (uiState.task?.status == ProcessTaskStatus.COMPLETED) {
            onComplete(taskId)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        label = "progress"
    )
    val miuixBottomSpacing = LocalFVoiceMiuixBottomSpacing.current
    val materialBottomSpacing = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixScaffold(
            topBar = {
                MiuixTopAppBar(
                    title = stringResource(R.string.processing_title),
                    navigationIcon = {
                        MiuixIconButton(onClick = cancelAndBack) {
                            MiuixIcon(MiuixIcons.Back, contentDescription = null)
                        }
                    }
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MiuixText(
                    text = stringResource(R.string.processing_current_stage, uiState.progress.currentStage),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp)) {
                    MiuixCircularProgressIndicator(progress = animatedProgress, size = 168.dp, strokeWidth = 12.dp)
                    MiuixText(
                        text = "${progress}%",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.error != null) {
                    MiuixText(
                        text = uiState.error!!,
                        color = MiuixTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
                MiuixTextButton(
                    text = if (showLog) stringResource(R.string.hide_log) else stringResource(R.string.show_log),
                    onClick = { showLog = !showLog }
                )
                if (showLog) {
                    ProcessingLogMiuixCard(logs = uiState.logs)
                } else {
                    ProcessingStagesMiuixCard(
                        stages = stages,
                        currentStage = currentStageIndex,
                        progress = progress
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                MiuixButton(
                    onClick = cancelAndBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = miuixBottomSpacing + 12.dp)
                ) {
                    MiuixText(stringResource(R.string.cancel))
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.processing_title)) },
                navigationIcon = {
                    IconButton(onClick = cancelAndBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.processing_current_stage, uiState.progress.currentStage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp
                )
                Text(
                    text = "${progress}%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedButton(onClick = { showLog = !showLog }) {
                Text(if (showLog) stringResource(R.string.hide_log) else stringResource(R.string.show_log))
            }

            if (showLog) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (uiState.logs.isEmpty()) {
                            Text(stringResource(R.string.no_logs_yet), style = MaterialTheme.typography.bodySmall)
                        } else {
                            uiState.logs.forEach { logLine ->
                                Text(logLine, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        stages.forEachIndexed { index, stage ->
                            val stageColor = when {
                                index < currentStageIndex -> MaterialTheme.colorScheme.primary
                                index == currentStageIndex -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.outline
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (index < currentStageIndex) "✓" else if (index == currentStageIndex) "…" else "○",
                                        color = stageColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                                    Text(
                                        text = stage,
                                        color = stageColor,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(
                                    text = when {
                                        index < currentStageIndex -> stringResource(R.string.stage_completed)
                                        index == currentStageIndex -> stringResource(R.string.stage_in_progress)
                                        else -> stringResource(R.string.stage_waiting)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = stageColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = cancelAndBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = materialBottomSpacing + 12.dp)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun ProcessingStagesMiuixCard(
    stages: List<String>,
    currentStage: Int,
    progress: Int,
) {
    FVoiceMiuixCard {
        stages.forEachIndexed { index, stage ->
            val statusText = when {
                index < currentStage -> stringResource(R.string.stage_completed)
                index == currentStage -> stringResource(R.string.stage_in_progress)
                else -> stringResource(R.string.stage_waiting)
            }
            FVoiceMiuixInfoRow(
                title = stage,
                summary = statusText,
                end = {
                    MiuixText(
                        text = if (index < currentStage) stringResource(R.string.stage_ok) else if (index == currentStage) "${progress}%" else "--",
                        color = when {
                            index < currentStage -> MiuixTheme.colorScheme.primary
                            index == currentStage -> MiuixTheme.colorScheme.secondaryVariant
                            else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ProcessingLogMiuixCard(logs: List<String>) {
    FVoiceMiuixCard {
        if (logs.isEmpty()) {
            MiuixText(stringResource(R.string.no_logs_yet))
        } else {
            logs.forEach { logLine ->
                MiuixText(logLine, fontSize = 13.sp)
            }
        }
    }
}
