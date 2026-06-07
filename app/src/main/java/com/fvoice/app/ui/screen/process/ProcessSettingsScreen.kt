package com.fvoice.app.ui.screen.process

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.DialogProperties
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fvoice.app.R
import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.model.ModelStatus
import com.fvoice.app.core.model.ModelType
import com.fvoice.app.core.model.ProcessTaskType
import com.fvoice.app.core.model.TaskSettings
import com.fvoice.app.core.modelmanager.ModelManager
import com.fvoice.app.data.model.DenoiseStrength
import com.fvoice.app.data.model.ProcessMode
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.component.FVoiceMiuixCard
import com.fvoice.app.ui.component.FVoiceMiuixPage
import com.fvoice.app.ui.component.FVoiceMiuixSegmentedControl
import com.fvoice.app.ui.component.FVoiceMiuixTitle
import com.fvoice.app.ui.navigation3.LocalNavigator
import com.fvoice.app.ui.screen.settings.MaterialDropdownPreference
import com.fvoice.app.ui.screen.settings.MiuixDropdownPreference
import com.fvoice.app.ui.screen.settings.SettingOption
import com.fvoice.app.ui.theme.LocalUiMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class ModelChoice {
    DEFAULT, CUSTOM
}

private enum class TranscribeLanguage(val value: String) {
    CHINESE("zh"),
    ENGLISH("en"),
    MIXED("auto")
}

@Composable
private fun ProcessMode.label(): String = when (this) {
    ProcessMode.DENOISE_AND_TRANSCRIBE -> stringResource(R.string.process_mode_denoise_and_transcribe)
    ProcessMode.DENOISE_ONLY -> stringResource(R.string.process_mode_denoise_only)
    ProcessMode.TRANSCRIBE_ONLY -> stringResource(R.string.process_mode_transcribe_only)
}

@Composable
private fun DenoiseStrength.label(): String = when (this) {
    DenoiseStrength.STANDARD -> stringResource(R.string.denoise_strength_standard)
    DenoiseStrength.STRONG -> stringResource(R.string.denoise_strength_strong)
    DenoiseStrength.CUSTOM -> stringResource(R.string.denoise_strength_custom)
}

@Composable
private fun ModelChoice.label(): String = when (this) {
    ModelChoice.DEFAULT -> stringResource(R.string.model_choice_default)
    ModelChoice.CUSTOM -> stringResource(R.string.model_choice_custom)
}

@Composable
private fun TranscribeLanguage.label(): String = when (this) {
    TranscribeLanguage.CHINESE -> stringResource(R.string.language_chinese)
    TranscribeLanguage.ENGLISH -> stringResource(R.string.language_english)
    TranscribeLanguage.MIXED -> stringResource(R.string.language_chinese_english)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessSettingsScreen(
    sourceUri: String,
    fileName: String,
    onStartProcess: (String) -> Unit
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val taskManager = com.fvoice.app.FVoiceApplication.processTaskManager
    val modelManager = remember { ModelManager(context.applicationContext) }
    val models by modelManager.models.collectAsState()
    val currentAsr by modelManager.currentAsrModel.collectAsState()
    val editedLabel = remember(sourceUri) { queryFileEditedLabel(context, Uri.parse(sourceUri)) }
    val isVideoSource = remember(sourceUri, fileName) { isVideoSource(context, Uri.parse(sourceUri), fileName) }

    var selectedMode by remember { mutableStateOf(ProcessMode.DENOISE_AND_TRANSCRIBE) }
    var selectedStrength by remember { mutableStateOf(DenoiseStrength.STANDARD) }
    var asrChoice by remember { mutableStateOf(ModelChoice.DEFAULT) }
    var selectedLanguage by remember { mutableStateOf(TranscribeLanguage.MIXED) }
    var selectedCustomDenoiseModelId by remember { mutableStateOf("") }
    var selectedCustomAsrModelId by remember { mutableStateOf("") }
    val onBack = dropUnlessResumed { navigator.pop() }

    val denoiseModels = remember(models) {
        models.filter { it.type.isDenoiseModel() && it.status == ModelStatus.READY }
    }
    val customDenoiseModels = denoiseModels
    val selectedCustomDenoiseModel = denoiseModels.firstOrNull { it.id == selectedCustomDenoiseModelId }
        ?: denoiseModels.firstOrNull()
    val activeDenoiseModel = if (selectedStrength == DenoiseStrength.CUSTOM) {
        selectedCustomDenoiseModel ?: denoiseModels.defaultDenoiseModel()
    } else {
        denoiseModels.modelForStrength(selectedStrength)
    }

    val asrModels = remember(models) {
        models.filter { it.type.isAsrModel() && it.status == ModelStatus.READY }
    }
    val customAsrModels = remember(asrModels) { asrModels.filterNot { it.isBundled } }
    val selectedCustomAsrModel = customAsrModels.firstOrNull { it.id == selectedCustomAsrModelId }
        ?: customAsrModels.firstOrNull()
    val activeAsrModel = if (asrChoice == ModelChoice.CUSTOM) {
        selectedCustomAsrModel ?: currentAsr ?: asrModels.firstOrNull()
    } else {
        currentAsr ?: asrModels.firstOrNull()
    }
    val canStart = (!selectedMode.needsDenoise() || activeDenoiseModel != null) &&
            (!selectedMode.needsTranscribe() || activeAsrModel != null)

    val startTask = {
        val type = when (selectedMode) {
            ProcessMode.DENOISE_ONLY -> ProcessTaskType.DENOISE
            ProcessMode.TRANSCRIBE_ONLY -> ProcessTaskType.TRANSCRIBE
            ProcessMode.DENOISE_AND_TRANSCRIBE -> ProcessTaskType.DENOISE_AND_TRANSCRIBE
        }
        val settings = TaskSettings(
            denoiseStrength = selectedStrength.name,
            outputFormats = listOf("TXT", "SRT", "JSON"),
            denoiseModelId = activeDenoiseModel?.id.orEmpty(),
            asrModelId = activeAsrModel?.id.orEmpty(),
            language = selectedLanguage.value
        )
        val task = taskManager.enqueue(
            type = type,
            sourceUri = Uri.parse(sourceUri),
            sourceFileName = fileName,
            settings = settings
        )
        onStartProcess(task.id)
    }

    if (LocalUiMode.current == UiMode.Miuix) {
        FVoiceMiuixPage(
            title = stringResource(R.string.process_settings_title),
            navigationIcon = {
                MiuixIconButton(onClick = onBack) {
                    MiuixIcon(MiuixIcons.Back, contentDescription = null)
                }
            }
        ) {
            item { SourcePlayerMiuix(sourceUri, fileName, isVideoSource) }
            item { FileInfoMiuix(fileName, editedLabel) }
            item { FVoiceMiuixTitle(stringResource(R.string.process_mode), Modifier.padding(top = 12.dp)) }
            item {
                FVoiceMiuixCard {
                    ProcessMode.entries.forEach { mode ->
                        ProcessModeMiuixRow(
                            title = mode.label(),
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode }
                        )
                    }
                }
            }
            if (selectedMode.needsDenoise()) {
                item { FVoiceMiuixTitle(stringResource(R.string.denoise_strength), Modifier.padding(top = 12.dp)) }
                item {
                    FVoiceMiuixSegmentedControl(
                        options = DenoiseStrength.entries.map { it.label() },
                        selectedIndex = DenoiseStrength.entries.indexOf(selectedStrength).coerceAtLeast(0),
                        onSelected = { selectedStrength = DenoiseStrength.entries[it] },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (selectedStrength == DenoiseStrength.CUSTOM && customDenoiseModels.isNotEmpty()) {
                    item {
                        MiuixDropdownPreference(
                            title = stringResource(R.string.denoise_model_label),
                            selected = selectedCustomDenoiseModel?.id.orEmpty(),
                            options = customDenoiseModels.map { SettingOption(it.id, it.name) },
                            onSelected = { selectedCustomDenoiseModelId = it }
                        )
                    }
                }
                item {
                    SettingExplanationMiuix(
                        denoiseDescription(selectedStrength, activeDenoiseModel, selectedStrength == DenoiseStrength.CUSTOM && customDenoiseModels.isEmpty())
                    )
                }
            }
            if (selectedMode.needsTranscribe()) {
                item { FVoiceMiuixTitle(stringResource(R.string.transcribe_model_label), Modifier.padding(top = 12.dp)) }
                item {
                    FVoiceMiuixSegmentedControl(
                        options = ModelChoice.entries.map { it.label() },
                        selectedIndex = ModelChoice.entries.indexOf(asrChoice),
                        onSelected = { asrChoice = ModelChoice.entries[it] },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (asrChoice == ModelChoice.CUSTOM && customAsrModels.isNotEmpty()) {
                    item {
                        ModelChipRowMiuix(
                            models = customAsrModels,
                            selectedModelId = selectedCustomAsrModel?.id.orEmpty(),
                            onSelected = { selectedCustomAsrModelId = it }
                        )
                    }
                }
                item { FVoiceMiuixTitle(stringResource(R.string.transcribe_language_label), Modifier.padding(top = 12.dp)) }
                item {
                    FVoiceMiuixSegmentedControl(
                        options = TranscribeLanguage.entries.map { it.label() },
                        selectedIndex = TranscribeLanguage.entries.indexOf(selectedLanguage),
                        onSelected = { selectedLanguage = TranscribeLanguage.entries[it] },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    SettingExplanationMiuix(
                        asrDescription(asrChoice, activeAsrModel, asrChoice == ModelChoice.CUSTOM && customAsrModels.isEmpty())
                    )
                }
            }
            item {
                MiuixButton(
                    onClick = startTask,
                    enabled = canStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    MiuixText(stringResource(R.string.start_process))
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.process_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SourcePlayerMaterial(sourceUri, fileName, isVideoSource)
            FileInfoMaterial(fileName, editedLabel)

            Text(
                text = stringResource(R.string.process_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ProcessMode.entries.forEach { mode ->
                        ProcessModeMaterialRow(
                            title = mode.label(),
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode }
                        )
                    }
                }
            }

            if (selectedMode.needsDenoise()) {
                Text(
                    text = stringResource(R.string.denoise_strength),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FilterChipRow(
                    labels = DenoiseStrength.entries.map { it.label() },
                    selectedIndex = DenoiseStrength.entries.indexOf(selectedStrength),
                    onSelected = { selectedStrength = DenoiseStrength.entries[it] }
                )
                if (selectedStrength == DenoiseStrength.CUSTOM && customDenoiseModels.isNotEmpty()) {
                    MaterialDropdownPreference(
                        title = stringResource(R.string.denoise_model_label),
                        selected = selectedCustomDenoiseModel?.id.orEmpty(),
                        options = customDenoiseModels.map { SettingOption(it.id, it.name) },
                        onSelected = { selectedCustomDenoiseModelId = it }
                    )
                }
                SettingExplanationMaterial(
                    denoiseDescription(selectedStrength, activeDenoiseModel, selectedStrength == DenoiseStrength.CUSTOM && customDenoiseModels.isEmpty())
                )
            }

            if (selectedMode.needsTranscribe()) {
                Text(
                    text = stringResource(R.string.transcribe_model_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FilterChipRow(
                    labels = ModelChoice.entries.map { it.label() },
                    selectedIndex = ModelChoice.entries.indexOf(asrChoice),
                    onSelected = { asrChoice = ModelChoice.entries[it] }
                )
                if (asrChoice == ModelChoice.CUSTOM && customAsrModels.isNotEmpty()) {
                    ModelChipRowMaterial(
                        models = customAsrModels,
                        selectedModelId = selectedCustomAsrModel?.id.orEmpty(),
                        onSelected = { selectedCustomAsrModelId = it }
                    )
                }
                Text(
                    text = stringResource(R.string.transcribe_language_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FilterChipRow(
                    labels = TranscribeLanguage.entries.map { it.label() },
                    selectedIndex = TranscribeLanguage.entries.indexOf(selectedLanguage),
                    onSelected = { selectedLanguage = TranscribeLanguage.entries[it] }
                )
                SettingExplanationMaterial(
                    asrDescription(asrChoice, activeAsrModel, asrChoice == ModelChoice.CUSTOM && customAsrModels.isEmpty())
                )
            }

            Button(
                onClick = startTask,
                enabled = canStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.start_process))
            }
        }
    }
}

@Composable
private fun SourcePlayerMaterial(
    sourceUri: String,
    fileName: String,
    isVideo: Boolean
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        if (isVideo) {
            VideoPlayerPreview(
                sourceUri = sourceUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        } else {
            AudioPlayerPreview(
                sourceUri = sourceUri,
                fileName = fileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            )
        }
    }
}

@Composable
private fun SourcePlayerMiuix(
    sourceUri: String,
    fileName: String,
    isVideo: Boolean
) {
    FVoiceMiuixCard {
        if (isVideo) {
            VideoPlayerPreview(
                sourceUri = sourceUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                containerColor = MiuixTheme.colorScheme.surfaceContainer
            )
        } else {
            AudioPlayerPreviewMiuix(
                sourceUri = sourceUri,
                fileName = fileName,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun VideoPlayerPreview(
    sourceUri: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black,
) {
    val context = LocalContext.current
    val uri = remember(sourceUri) { Uri.parse(sourceUri) }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }
    var fullscreen by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(
        modifier = modifier
            .background(containerColor)
            .aspectRatio(16f / 9f)
    ) {
        Media3PlayerView(
            player = if (fullscreen) null else player,
            modifier = Modifier.fillMaxSize(),
            shutterColor = containerColor
        )
        PlayerControlsOverlay(
            player = player,
            fullscreen = false,
            onFullscreenChange = { fullscreen = true },
            modifier = Modifier.fillMaxSize()
        )
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(containerColor)
            ) {
                HideSystemBarsEffect()
                Media3PlayerView(
                    player = player,
                    modifier = Modifier.fillMaxSize()
                )
                PlayerControlsOverlay(
                    player = player,
                    fullscreen = true,
                    onFullscreenChange = { fullscreen = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
internal fun AudioPlayerPreview(
    sourceUri: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uri = remember(sourceUri) { Uri.parse(sourceUri) }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.audio_preview_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = fileName.ifBlank { stringResource(R.string.result_unknown_file) },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        AudioControls(
            player = player,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun AudioPlayerPreviewMiuix(
    sourceUri: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uri = remember(sourceUri) { Uri.parse(sourceUri) }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.audio_preview_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                    text = fileName.ifBlank { stringResource(R.string.result_unknown_file) },
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        AudioControls(
            player = player,
            modifier = Modifier.fillMaxWidth(),
            iconTint = MiuixTheme.colorScheme.primary,
            timeTextColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            sliderColor = MiuixTheme.colorScheme.primary
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun Media3PlayerView(
    player: ExoPlayer?,
    modifier: Modifier = Modifier,
    shutterColor: androidx.compose.ui.graphics.Color = Color.Black
) {
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(shutterColor.toArgb())
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

@Composable
internal fun PlayerControlsOverlay(
    player: ExoPlayer,
    fullscreen: Boolean,
    onFullscreenChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var currentMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            durationMs = player.duration.coerceAtLeast(0L)
            currentMs = player.currentPosition.coerceAtLeast(0L)
            if (!isSeeking) {
                sliderValue = if (durationMs > 0L) {
                    currentMs.toFloat() / durationMs.toFloat()
                } else {
                    0f
                }.coerceIn(0f, 1f)
            }
            delay(250)
        }
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.68f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.playbackState == Player.STATE_ENDED) {
                            player.seekTo(0)
                        }
                        player.play()
                    }
                    isPlaying = player.isPlaying
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                    tint = Color.White
                )
            }
            Text(
                text = formatPlaybackTime(currentMs),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    isSeeking = true
                    sliderValue = value
                    currentMs = (durationMs * value).toLong()
                },
                onValueChangeFinished = {
                    player.seekTo((durationMs * sliderValue).toLong())
                    isSeeking = false
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(
                text = formatPlaybackTime(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            IconButton(onClick = onFullscreenChange) {
                Icon(
                    imageVector = if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (fullscreen) stringResource(R.string.player_exit_fullscreen) else stringResource(R.string.player_fullscreen),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
internal fun AudioControls(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    timeTextColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    sliderColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var currentMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            durationMs = player.duration.coerceAtLeast(0L)
            currentMs = player.currentPosition.coerceAtLeast(0L)
            if (!isSeeking) {
                sliderValue = if (durationMs > 0L) {
                    currentMs.toFloat() / durationMs.toFloat()
                } else {
                    0f
                }.coerceIn(0f, 1f)
            }
            delay(250)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.playbackState == Player.STATE_ENDED) {
                            player.seekTo(0)
                        }
                        player.play()
                    }
                    isPlaying = player.isPlaying
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                    tint = iconTint
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    isSeeking = true
                    sliderValue = value
                    currentMs = (durationMs * value).toLong()
                },
                onValueChangeFinished = {
                    player.seekTo((durationMs * sliderValue).toLong())
                    isSeeking = false
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = sliderColor,
                    activeTrackColor = sliderColor,
                    inactiveTrackColor = sliderColor.copy(alpha = 0.3f)
                )
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlaybackTime(currentMs),
                color = timeTextColor,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatPlaybackTime(durationMs),
                color = timeTextColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun HideSystemBarsEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        val activityWindow = view.context.findActivity()?.window
        val windows = listOfNotNull(dialogWindow, activityWindow).distinct()

        windows.forEach { window ->
            hideSystemBars(window)
        }
        onDispose {
            windows.forEach { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@Suppress("DEPRECATION")
internal fun hideSystemBars(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
}

internal tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

internal fun formatPlaybackTime(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(Locale.getDefault(), hours, minutes, seconds)
    } else {
        "%d:%02d".format(Locale.getDefault(), minutes, seconds)
    }
}

@Composable
private fun FileInfoMaterial(
    fileName: String,
    createdLabel: String
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        FileInfoContentMaterial(fileName)
    }
}

@Composable
private fun FileInfoMiuix(
    fileName: String,
    createdLabel: String
) {
    FVoiceMiuixCard {
        FileNameMiuix(fileName)
    }
}

@Composable
private fun FileInfoContentMaterial(
    fileName: String
) {
    Column(modifier = Modifier.padding(14.dp)) {
        Text(
            text = fileName.ifBlank { stringResource(R.string.result_unknown_file) },
            modifier = Modifier.basicMarquee(),
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FileNameMiuix(fileName: String) {
    MiuixText(
        text = fileName.ifBlank { stringResource(R.string.result_unknown_file) },
        modifier = Modifier.basicMarquee(),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
}

@Composable
private fun ProcessModeMaterialRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProcessModeMiuixRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiuixRadioButton(selected = selected, onClick = onClick)
        MiuixText(
            text = title,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FilterChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun ModelChipRowMaterial(
    models: List<ModelInfo>,
    selectedModelId: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        models.forEach { model ->
            FilterChip(
                selected = selectedModelId == model.id,
                onClick = { onSelected(model.id) },
                label = { Text(model.name, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun ModelChipRowMiuix(
    models: List<ModelInfo>,
    selectedModelId: String,
    onSelected: (String) -> Unit
) {
    FVoiceMiuixSegmentedControl(
        options = models.map { it.name },
        selectedIndex = models.indexOfFirst { it.id == selectedModelId }.coerceAtLeast(0),
        onSelected = { onSelected(models[it].id) },
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SettingExplanationMaterial(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SettingExplanationMiuix(text: String) {
    FVoiceMiuixCard {
        MiuixText(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun ProcessMode.needsDenoise(): Boolean {
    return this == ProcessMode.DENOISE_ONLY || this == ProcessMode.DENOISE_AND_TRANSCRIBE
}

private fun ProcessMode.needsTranscribe(): Boolean {
    return this == ProcessMode.TRANSCRIBE_ONLY || this == ProcessMode.DENOISE_AND_TRANSCRIBE
}

private fun ModelType.isAsrModel(): Boolean {
    return this == ModelType.ASR_WHISPER_TINY ||
            this == ModelType.ASR_WHISPER_BASE ||
            this == ModelType.ASR_WHISPER_SMALL ||
            this == ModelType.ASR_WHISPER_MEDIUM ||
            this == ModelType.ASR_WHISPER_LARGE
}

private fun ModelType.isDenoiseModel(): Boolean {
    return this == ModelType.DENOISE_RNNOISE || this == ModelType.DENOISE_DEEPFILTERNET
}

private fun List<ModelInfo>.defaultDenoiseModel(): ModelInfo? {
    return firstOrNull { it.id == "deepfilternet3_onnx" }
        ?: firstOrNull { it.type == ModelType.DENOISE_DEEPFILTERNET }
        ?: firstOrNull { it.id == "rnnoise_default" }
}

private fun List<ModelInfo>.modelForStrength(strength: DenoiseStrength): ModelInfo? {
    return when (strength) {
        DenoiseStrength.STANDARD -> firstOrNull { it.id == "rnnoise_default" }
            ?: firstOrNull { it.type == ModelType.DENOISE_RNNOISE }
        DenoiseStrength.STRONG -> firstOrNull { it.id == "deepfilternet3_onnx" }
        DenoiseStrength.CUSTOM -> defaultDenoiseModel()
    }
}

@Composable
private fun denoiseDescription(
    strength: DenoiseStrength,
    model: ModelInfo?,
    customMissing: Boolean
): String {
    val modelName = model?.name ?: stringResource(R.string.denoise_no_model)
    return stringResource(R.string.denoise_description_format, modelName)
}

@Composable
private fun asrDescription(
    choice: ModelChoice,
    model: ModelInfo?,
    customMissing: Boolean
): String {
    val modelName = model?.name ?: stringResource(R.string.asr_no_model)
    return when (choice) {
        ModelChoice.DEFAULT -> stringResource(R.string.asr_default_description, modelName)
        ModelChoice.CUSTOM -> if (customMissing) {
            stringResource(R.string.asr_custom_missing_description, modelName)
        } else {
            stringResource(R.string.asr_custom_description, modelName)
        }
    }
}

private fun queryFileEditedLabel(context: Context, uri: Uri): String {
    val millis = queryMediaTime(context, uri)
    return if (millis > 0L) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
        context.getString(R.string.file_edit_time, timeStr)
    } else {
        context.getString(R.string.file_edit_time_unknown)
    }
}

private fun queryMediaTime(context: Context, uri: Uri): Long {
    if (uri.scheme == "file") {
        val fileTime = uri.path?.let { java.io.File(it).lastModified() } ?: 0L
        if (fileTime > 0L) return fileTime
    }

    val projections = listOf(
        arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
        arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED, MediaStore.MediaColumns.DATE_MODIFIED),
        arrayOf(MediaStore.MediaColumns.DATE_MODIFIED, MediaStore.MediaColumns.DATE_ADDED),
        arrayOf(MediaStore.MediaColumns.DATE_MODIFIED)
    )
    projections.forEach { projection ->
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use
                val documentModifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val documentModified = if (documentModifiedIndex >= 0) cursor.getLong(documentModifiedIndex) else 0L
                if (documentModified > 0L) return normalizeTimestamp(documentModified)
                val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val modified = if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) else 0L
                if (modified > 0L) return normalizeTimestamp(modified)
                val addedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val added = if (addedIndex >= 0) cursor.getLong(addedIndex) else 0L
                if (added > 0L) return normalizeTimestamp(added)
            }
        } catch (_: Exception) {
        }
    }
    return 0L
}

private fun normalizeTimestamp(value: Long): Long {
    return if (value > 10_000_000_000L) value else value * 1000L
}

private fun isVideoSource(context: Context, uri: Uri, fileName: String): Boolean {
    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    if (mime?.startsWith("video/") == true) return true
    if (mime?.startsWith("audio/") == true) return false

    val lower = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
    return lower in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v")
}
