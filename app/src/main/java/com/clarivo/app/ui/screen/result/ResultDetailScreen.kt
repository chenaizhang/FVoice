package com.clarivo.app.ui.screen.result

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clarivo.app.R
import com.clarivo.app.core.model.OutputFileInfo
import com.clarivo.app.core.model.OutputFileType
import com.clarivo.app.core.model.ProcessTaskStatus
import com.clarivo.app.core.model.ProcessTaskType
import com.clarivo.app.core.model.TranscriptSegment
import com.clarivo.app.data.model.UiMode
import com.clarivo.app.ui.component.DeleteConfirmDialog
import com.clarivo.app.ui.component.ClarivoMiuixCard
import com.clarivo.app.ui.component.ClarivoMiuixInfoRow
import com.clarivo.app.ui.component.ClarivoMiuixPage
import com.clarivo.app.ui.component.ClarivoMiuixSegmentedControl
import com.clarivo.app.ui.component.ClarivoMiuixTitle
import com.clarivo.app.ui.navigation3.LocalNavigator
import com.clarivo.app.ui.screen.process.AudioPlayerPreview
import com.clarivo.app.ui.screen.process.AudioPlayerPreviewMiuix
import com.clarivo.app.ui.screen.process.VideoPlayerPreview
import com.clarivo.app.ui.theme.LocalUiMode
import com.clarivo.app.viewmodel.ResultDetailViewModel
import java.io.File
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDetailScreen(
    taskId: String
) {
    val navigator = LocalNavigator.current
    val viewModel: ResultDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var selectedTrack by remember { mutableIntStateOf(1) }
    var selectedExportFormat by remember { mutableStateOf("TXT") }
    val exportFormats = listOf("TXT", "SRT", "JSON")
    val onBack = dropUnlessResumed { navigator.pop() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val onDelete = {
        viewModel.deleteTask(taskId)
        onBack()
    }

    DisposableEffect(Unit) {
        val listener = {
            if (!showDeleteConfirm) {
                viewModel.loadTask(taskId)
            }
        }
        com.clarivo.app.ClarivoApplication.processTaskManager.addHistoryChangeListener(listener)
        onDispose {
            com.clarivo.app.ClarivoApplication.processTaskManager.removeHistoryChangeListener(listener)
        }
    }

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    val segments = uiState.transcriptResult?.segments.orEmpty()
    val showTranscript = uiState.taskType == ProcessTaskType.TRANSCRIBE ||
            uiState.taskType == ProcessTaskType.DENOISE_AND_TRANSCRIBE
    val hasDenoise = uiState.taskType == ProcessTaskType.DENOISE ||
            uiState.taskType == ProcessTaskType.DENOISE_AND_TRANSCRIBE
    val originalFile = uiState.audioFiles.firstOrNull { it.type == OutputFileType.AUDIO_EXTRACTED }
    val denoisedFile = uiState.audioFiles.firstOrNull { it.type == OutputFileType.AUDIO_DENOISED }
    val mergedVideoFile = uiState.videoFiles.firstOrNull { it.type == OutputFileType.VIDEO_DENOISED }
    val saveTargetFile = mergedVideoFile ?: denoisedFile ?: originalFile

    val sourceUri = uiState.sourceUri

    if (LocalUiMode.current == UiMode.Miuix) {
        ResultDetailMiuix(
            sourceFileName = uiState.sourceFileName,
            status = uiState.status,
            errorMessage = uiState.errorMessage,
            sourceUri = sourceUri,
            originalFile = originalFile,
            denoisedFile = denoisedFile,
            mergedVideoFile = mergedVideoFile,
            isVideoSource = uiState.isVideoSource,
            hasDenoise = hasDenoise,
            showTranscript = showTranscript,
            transcriptSegments = segments,
            transcriptFiles = uiState.transcriptFiles,
            selectedExportFormat = selectedExportFormat,
            exportFormats = exportFormats,
            saveTargetFile = saveTargetFile,
            selectedTrack = selectedTrack,
            onTrackSelected = { selectedTrack = it },
            onExportFormatSelected = { selectedExportFormat = it },
            onBack = onBack,
            onShowDeleteConfirm = { showDeleteConfirm = true }
        )
    } else {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.clear_history_title))
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResultSummaryMaterial(
                sourceFileName = uiState.sourceFileName,
                status = uiState.status,
                errorMessage = uiState.errorMessage
            )

            ResultPlayerMaterialCard(
                sourceUri = sourceUri,
                originalFile = originalFile,
                denoisedFile = denoisedFile,
                mergedVideoFile = mergedVideoFile,
                isVideoSource = uiState.isVideoSource,
                hasDenoise = hasDenoise,
                selectedTrack = selectedTrack,
                onTrackSelected = { selectedTrack = it }
            )

            SaveToLocalMaterialButton(
                targetFile = saveTargetFile,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (showTranscript) {
                ResultTranscriptMaterialCard(segments)

                ResultExportMaterialCard(
                    selectedExportFormat = selectedExportFormat,
                    exportFormats = exportFormats,
                    transcriptFiles = uiState.transcriptFiles,
                    onExportFormatSelected = { selectedExportFormat = it }
                )
            }
        }
    }
    }

    DeleteConfirmDialog(
        show = showDeleteConfirm,
        title = stringResource(R.string.delete_task_title),
        message = stringResource(R.string.delete_task_message),
        onConfirm = onDelete,
        onDismiss = { showDeleteConfirm = false }
    )
}

@Composable
private fun ResultDetailMiuix(
    sourceFileName: String,
    status: ProcessTaskStatus,
    errorMessage: String,
    sourceUri: String,
    originalFile: OutputFileInfo?,
    denoisedFile: OutputFileInfo?,
    mergedVideoFile: OutputFileInfo?,
    isVideoSource: Boolean,
    hasDenoise: Boolean,
    showTranscript: Boolean,
    transcriptSegments: List<TranscriptSegment>,
    transcriptFiles: List<OutputFileInfo>,
    selectedExportFormat: String,
    exportFormats: List<String>,
    saveTargetFile: OutputFileInfo?,
    selectedTrack: Int,
    onTrackSelected: (Int) -> Unit,
    onExportFormatSelected: (String) -> Unit,
    onBack: () -> Unit,
    onShowDeleteConfirm: () -> Unit,
) {
    ClarivoMiuixPage(
        title = stringResource(R.string.result_detail_title),
        navigationIcon = {
            MiuixIconButton(onClick = onBack) {
                MiuixIcon(MiuixIcons.Back, contentDescription = null)
            }
        },
        actions = {
            MiuixIconButton(onClick = onShowDeleteConfirm) {
                MiuixIcon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.clear_history_title)
                )
            }
        }
    ) {
        item {
            ResultSummaryMiuixCard(sourceFileName, status, errorMessage)
        }

        item {
            ResultPlayerMiuixCard(
                sourceUri = sourceUri,
                originalFile = originalFile,
                denoisedFile = denoisedFile,
                mergedVideoFile = mergedVideoFile,
                isVideoSource = isVideoSource,
                hasDenoise = hasDenoise,
                selectedTrack = selectedTrack,
                onTrackSelected = onTrackSelected
            )
        }

        item {
            SaveToLocalMiuixButton(
                targetFile = saveTargetFile,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (showTranscript) {
            item {
                ClarivoMiuixTitle(
                    text = stringResource(R.string.transcript_text),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            item {
                ResultTranscriptMiuixCard(transcriptSegments)
            }

            item {
                ClarivoMiuixTitle(
                    text = stringResource(R.string.export_title),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            item {
                ResultExportMiuixCard(
                    selectedExportFormat = selectedExportFormat,
                    exportFormats = exportFormats,
                    transcriptFiles = transcriptFiles,
                    onExportFormatSelected = onExportFormatSelected
                )
            }
        }
    }
}

@Composable
private fun ResultSummaryMaterial(
    sourceFileName: String,
    status: ProcessTaskStatus,
    errorMessage: String,
) {
    Column {
        Text(
            text = sourceFileName.ifBlank { stringResource(R.string.result_unknown_file) },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.basicMarquee()
        )
        if (status == ProcessTaskStatus.FAILED && errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ResultPlayerMaterialCard(
    sourceUri: String,
    originalFile: OutputFileInfo?,
    denoisedFile: OutputFileInfo?,
    mergedVideoFile: OutputFileInfo?,
    isVideoSource: Boolean,
    hasDenoise: Boolean,
    selectedTrack: Int = 1,
    onTrackSelected: ((Int) -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                hasDenoise && isVideoSource -> {
                    if (selectedTrack == 1 && mergedVideoFile != null) {
                        VideoPlayerPreview(
                            sourceUri = mergedVideoFile.uri.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    } else if (selectedTrack == 1) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VideoPlayerPreview(
                                sourceUri = sourceUri,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                            AudioPlayerPreview(
                                sourceUri = denoisedFile?.uri?.toString() ?: "",
                                fileName = denoisedFile?.fileName ?: "",
                            )
                        }
                    } else {
                        VideoPlayerPreview(
                            sourceUri = sourceUri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    }
                }
                hasDenoise && !isVideoSource -> {
                    AudioPlayerPreview(
                        sourceUri = if (selectedTrack == 1) denoisedFile?.uri?.toString() ?: "" else originalFile?.uri?.toString() ?: "",
                        fileName = if (selectedTrack == 1) denoisedFile?.fileName ?: "" else originalFile?.fileName ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
                isVideoSource -> {
                    VideoPlayerPreview(
                        sourceUri = sourceUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                }
                else -> {
                    AudioPlayerPreview(
                        sourceUri = originalFile?.uri?.toString() ?: "",
                        fileName = originalFile?.fileName ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            }

            if (hasDenoise && onTrackSelected != null) {
                TrackToggleMaterial(
                    selectedTrack = selectedTrack,
                    onTrackSelected = onTrackSelected,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackToggleMaterial(
    selectedTrack: Int,
    onTrackSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedTrack == 0,
            onClick = { onTrackSelected(0) },
            label = { Text(stringResource(R.string.player_original)) }
        )
        FilterChip(
            selected = selectedTrack == 1,
            onClick = { onTrackSelected(1) },
            label = { Text(stringResource(R.string.player_denoised)) }
        )
    }
}

@Composable
private fun ResultTranscriptMaterialCard(segments: List<TranscriptSegment>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.transcript_text),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (segments.isEmpty()) {
                Text(
                    text = stringResource(R.string.result_no_transcript),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                segments.forEach { segment ->
                    TranscriptLineMaterial(formatTime(segment.startMs), segment.text)
                }
            }
        }
    }
}

@Composable
private fun TranscriptLineMaterial(time: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = time,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(text = text)
    }
}

@Composable
private fun ResultExportMaterialCard(
    selectedExportFormat: String,
    exportFormats: List<String>,
    transcriptFiles: List<OutputFileInfo>,
    onExportFormatSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val selectedFile = transcriptFiles.firstOrNull { it.matchesFormat(selectedExportFormat) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(selectedFile?.mimeType ?: "*/*")
    ) { uri ->
        if (uri != null && selectedFile != null) {
            copyFileToUri(context, selectedFile.uri, uri)
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.export_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                exportFormats.forEach { format ->
                    FilterChip(
                        selected = selectedExportFormat == format,
                        onClick = { onExportFormatSelected(format) },
                        label = { Text(format) }
                    )
                }
            }
            Button(
                onClick = { selectedFile?.let { saveLauncher.launch(it.fileName) } },
                enabled = selectedFile != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun ResultSummaryMiuixCard(
    sourceFileName: String,
    status: ProcessTaskStatus,
    errorMessage: String,
) {
    val title = sourceFileName.ifBlank { stringResource(R.string.result_unknown_file) }
    val summary = if (status == ProcessTaskStatus.FAILED && errorMessage.isNotBlank()) {
        errorMessage
    } else {
        ""
    }
    ClarivoMiuixCard {
        ClarivoMiuixInfoRow(
            title = title,
            summary = summary,
            end = {
                MiuixText(
                    text = statusLabel(status),
                    color = if (status == ProcessTaskStatus.COMPLETED) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        )
    }
}

@Composable
private fun ResultPlayerMiuixCard(
    sourceUri: String,
    originalFile: OutputFileInfo?,
    denoisedFile: OutputFileInfo?,
    mergedVideoFile: OutputFileInfo?,
    isVideoSource: Boolean,
    hasDenoise: Boolean,
    selectedTrack: Int = 1,
    onTrackSelected: ((Int) -> Unit)? = null,
) {
    ClarivoMiuixCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                hasDenoise && isVideoSource -> {
                    if (selectedTrack == 1 && mergedVideoFile != null) {
                        VideoPlayerPreview(
                            sourceUri = mergedVideoFile.uri.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            containerColor = MiuixTheme.colorScheme.surfaceContainer
                        )
                    } else if (selectedTrack == 1) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VideoPlayerPreview(
                                sourceUri = sourceUri,
                                containerColor = MiuixTheme.colorScheme.surfaceContainer
                            )
                            AudioPlayerPreviewMiuix(
                                sourceUri = denoisedFile?.uri?.toString() ?: "",
                                fileName = denoisedFile?.fileName ?: "",
                            )
                        }
                    } else {
                        VideoPlayerPreview(
                            sourceUri = sourceUri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            containerColor = MiuixTheme.colorScheme.surfaceContainer
                        )
                    }
                }
                hasDenoise && !isVideoSource -> {
                    AudioPlayerPreviewMiuix(
                        sourceUri = if (selectedTrack == 1) denoisedFile?.uri?.toString() ?: "" else originalFile?.uri?.toString() ?: "",
                        fileName = if (selectedTrack == 1) denoisedFile?.fileName ?: "" else originalFile?.fileName ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
                isVideoSource -> {
                    VideoPlayerPreview(
                        sourceUri = sourceUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        containerColor = MiuixTheme.colorScheme.surfaceContainer
                    )
                }
                else -> {
                    AudioPlayerPreviewMiuix(
                        sourceUri = originalFile?.uri?.toString() ?: "",
                        fileName = originalFile?.fileName ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            }

            if (hasDenoise && onTrackSelected != null) {
                TrackToggleMiuix(
                    selectedTrack = selectedTrack,
                    onTrackSelected = onTrackSelected,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackToggleMiuix(
    selectedTrack: Int,
    onTrackSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ClarivoMiuixSegmentedControl(
        options = listOf(
            stringResource(R.string.player_original),
            stringResource(R.string.player_denoised)
        ),
        selectedIndex = selectedTrack.coerceIn(0, 1),
        onSelected = onTrackSelected,
        modifier = modifier
    )
}

@Composable
private fun ResultTranscriptMiuixCard(segments: List<TranscriptSegment>) {
    ClarivoMiuixCard {
        if (segments.isEmpty()) {
            MiuixText(
                text = stringResource(R.string.result_no_transcript),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        } else {
            segments.forEach { segment ->
                TranscriptLineMiuix(formatTime(segment.startMs), segment.text)
            }
        }
    }
}

@Composable
private fun TranscriptLineMiuix(time: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MiuixText(
            text = time,
            color = MiuixTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        MiuixText(
            text = text,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ResultExportMiuixCard(
    selectedExportFormat: String,
    exportFormats: List<String>,
    transcriptFiles: List<OutputFileInfo>,
    onExportFormatSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val selectedFile = transcriptFiles.firstOrNull { it.matchesFormat(selectedExportFormat) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(selectedFile?.mimeType ?: "*/*")
    ) { uri ->
        if (uri != null && selectedFile != null) {
            copyFileToUri(context, selectedFile.uri, uri)
        }
    }
    MiuixCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = MiuixCardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            exportFormats.forEachIndexed { index, format ->
                ExportFormatMiuixRow(
                    format = format,
                    selected = selectedExportFormat == format,
                    onClick = { onExportFormatSelected(format) }
                )
                if (index != exportFormats.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    )
                }
            }
            MiuixButton(
                onClick = { selectedFile?.let { saveLauncher.launch(it.fileName) } },
                enabled = selectedFile != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                MiuixText(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun ExportFormatMiuixRow(
    format: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiuixText(
            text = format,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
        )
        if (selected) {
            MiuixText(
                text = stringResource(R.string.result_selected),
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun OutputFileInfo.matchesFormat(format: String): Boolean {
    return when (format.uppercase()) {
        "TXT" -> type == OutputFileType.TRANSCRIPT_TXT
        "SRT" -> type == OutputFileType.TRANSCRIPT_SRT
        "JSON" -> type == OutputFileType.TRANSCRIPT_JSON
        else -> fileName.endsWith(".${format.lowercase()}", ignoreCase = true)
    }
}

private fun List<OutputFileInfo>.selectAudioFile(selectedTrack: Int): OutputFileInfo? {
    val preferredType = if (selectedTrack == 0) {
        OutputFileType.AUDIO_EXTRACTED
    } else {
        OutputFileType.AUDIO_DENOISED
    }
    return firstOrNull { it.type == preferredType }
}

private fun openOutputFile(context: android.content.Context, fileInfo: OutputFileInfo) {
    try {
        val uri = fileInfo.uri.toShareableUri(context)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, fileInfo.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.error_cannot_open_file), Toast.LENGTH_SHORT).show()
    }
}

private fun Uri.toShareableUri(context: android.content.Context): Uri {
    if (scheme != "file") return this
    val file = File(path ?: return this)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun statusLabel(status: ProcessTaskStatus): String {
    return when (status) {
        ProcessTaskStatus.COMPLETED -> stringResource(R.string.status_completed)
        ProcessTaskStatus.FAILED -> stringResource(R.string.status_failed)
        ProcessTaskStatus.CANCELLED -> stringResource(R.string.status_cancelled)
        ProcessTaskStatus.PROCESSING -> stringResource(R.string.status_processing)
        ProcessTaskStatus.PENDING -> stringResource(R.string.status_pending)
    }
}

private fun formatTime(ms: Long): String {
    val sec = ms / 1000
    val min = sec / 60
    val hour = min / 60
    return String.format("%02d:%02d:%02d", hour, min % 60, sec % 60)
}

private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}


private fun copyFileToUri(context: android.content.Context, sourceUri: Uri, destUri: Uri) {
    try {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                input.copyTo(output)
            }
        }
        Toast.makeText(context, context.getString(R.string.save_success), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.save_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun SaveToLocalMaterialButton(
    targetFile: OutputFileInfo?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(targetFile?.mimeType ?: "*/*")
    ) { uri ->
        if (uri != null && targetFile != null) {
            copyFileToUri(context, targetFile.uri, uri)
        }
    }
    if (targetFile != null) {
        OutlinedButton(
            onClick = { saveLauncher.launch(targetFile.fileName) },
            modifier = modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save_to_local))
        }
    }
}

@Composable
private fun SaveToLocalMiuixButton(
    targetFile: OutputFileInfo?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(targetFile?.mimeType ?: "*/*")
    ) { uri ->
        if (uri != null && targetFile != null) {
            copyFileToUri(context, targetFile.uri, uri)
        }
    }
    if (targetFile != null) {
        MiuixButton(
            onClick = { saveLauncher.launch(targetFile.fileName) },
            modifier = modifier.fillMaxWidth()
        ) {
            MiuixText(stringResource(R.string.save_to_local))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MergedVideoPlayer(
    videoUri: String,
    audioUri: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black,
) {
    val context = LocalContext.current
    val player = remember(videoUri, audioUri) {
        ExoPlayer.Builder(context).build().apply {
            val videoMediaItem = MediaItem.fromUri(Uri.parse(videoUri))
            val audioMediaItem = MediaItem.fromUri(Uri.parse(audioUri))
            val factory = DefaultMediaSourceFactory(context)
            val videoSource = factory.createMediaSource(videoMediaItem)
            val audioSource = factory.createMediaSource(audioMediaItem)
            val mergingSource = MergingMediaSource(videoSource, audioSource)
            setMediaSource(mergingSource)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(modifier = modifier.background(containerColor)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(containerColor.toArgb())
                    this.player = player
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            onRelease = { view ->
                view.player = null
            }
        )
    }
}
