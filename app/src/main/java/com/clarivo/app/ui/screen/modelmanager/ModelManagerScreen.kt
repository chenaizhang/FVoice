package com.clarivo.app.ui.screen.modelmanager

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.clarivo.app.ClarivoApplication
import com.clarivo.app.R
import com.clarivo.app.core.model.ModelInfo
import com.clarivo.app.core.model.ModelStatus
import com.clarivo.app.core.model.ModelType
import com.clarivo.app.data.model.UiMode as DataUiMode
import com.clarivo.app.ui.component.ClarivoMiuixCard
import androidx.compose.ui.unit.sp
import com.clarivo.app.ui.component.ClarivoMiuixInfoRow
import com.clarivo.app.ui.component.ClarivoMiuixPage
import com.clarivo.app.ui.navigation3.LocalNavigator
import com.clarivo.app.ui.theme.LocalUiMode
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    category: String = MODEL_CATEGORY_TRANSCRIBE
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    // Use ModelManager directly since it's accessible via application context
    val manager = remember { com.clarivo.app.core.modelmanager.ModelManager(context) }
    val models by manager.models.collectAsState()
    val currentAsr by manager.currentAsrModel.collectAsState()
    val showImportDialog = remember { mutableStateOf(false) }
    val importType = remember { mutableStateOf<ModelType?>(null) }
    val onBack = dropUnlessResumed { navigator.pop() }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val type = importType.value ?: return@rememberLauncherForActivityResult
        try {
            val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val success = manager.importModel(tempFile, type)
            tempFile.delete()
            Toast.makeText(
                context,
                if (success) context.getString(R.string.model_import_success) else context.getString(R.string.model_import_failed_simple),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.model_import_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    if (LocalUiMode.current == DataUiMode.Miuix) {
        ModelManagerMiuix(
            category = category,
            models = models.filterForCategory(category),
            currentAsr = currentAsr,
            onBack = onBack,
            onDelete = { id ->
                manager.deleteModel(id)
            },
            onSetCurrentAsr = { id ->
                manager.setCurrentAsrModel(id)
            },
            onImport = { type ->
                importType.value = type
                val mime = when (type) {
                    ModelType.ASR_WHISPER_TINY, ModelType.ASR_WHISPER_BASE,
                    ModelType.ASR_WHISPER_SMALL, ModelType.ASR_WHISPER_MEDIUM,
                    ModelType.ASR_WHISPER_LARGE -> "*/*"
                    ModelType.VAD_SILERO -> "*/*"
                    else -> "*/*"
                }
                importLauncher.launch(arrayOf(mime))
            }
        )
        return
    }
    val filteredModels = models.filterForCategory(category)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(modelManagerTitleRes(category))) },
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
            ModelDownloadGuideCard(category)

            Text(
                text = stringResource(R.string.model_installed, modelCategoryName(category)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (filteredModels.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.model_empty, modelCategoryName(category)),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            filteredModels.forEach { model ->
                ModelCardMaterial(
                    model = model,
                    isCurrentAsr = currentAsr?.id == model.id,
                    onDelete = { manager.deleteModel(model.id) },
                    onSetCurrentAsr = { manager.setCurrentAsrModel(model.id) }
                )
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))

            Text(
                text = stringResource(R.string.model_import_title, modelCategoryName(category)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImportButtonsMaterial(category, onImport = {
                        importType.value = it
                        importLauncher.launch(arrayOf("*/*"))
                    })
                }
            }
        }
    }
}

@Composable
private fun ModelManagerMiuix(
    category: String,
    models: List<ModelInfo>,
    currentAsr: ModelInfo?,
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    onSetCurrentAsr: (String) -> Unit,
    onImport: (ModelType) -> Unit
) {
    ClarivoMiuixPage(
        title = stringResource(modelManagerTitleRes(category)),
        navigationIcon = {
            MiuixIconButton(onClick = onBack) {
                MiuixIcon(MiuixIcons.Back, contentDescription = null)
            }
        }
    ) {
        item {
            ModelDownloadGuideCardMiuix(category)
        }

        item {
            MiuixText(
                text = stringResource(R.string.model_installed, modelCategoryName(category)),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        if (models.isEmpty()) {
            item {
                ClarivoMiuixCard {
                    MiuixText(stringResource(R.string.model_empty, modelCategoryName(category)))
                }
            }
        } else {
            items(models.size, key = { models[it].id }) { index ->
                val model = models[index]
                ModelCardMiuix(
                    model = model,
                    isCurrentAsr = currentAsr?.id == model.id,
                    onDelete = { onDelete(model.id) },
                    onSetCurrentAsr = { onSetCurrentAsr(model.id) }
                )
            }
        }

        item {
            MiuixText(
                text = stringResource(R.string.model_import_title, modelCategoryName(category)),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp)
            )
        }

        item {
            ClarivoMiuixCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImportButtonsMiuix(category, onImport)
                }
            }
        }
    }
}

@Composable
private fun ModelCardMiuix(
    model: ModelInfo,
    isCurrentAsr: Boolean,
    onDelete: () -> Unit,
    onSetCurrentAsr: () -> Unit
) {
    ClarivoMiuixCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MiuixText(
                        text = model.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    MiuixText(
                        text = modelMetadataText(model),
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    if (model.isBundled) {
                        MiuixText(
                            text = stringResource(R.string.model_bundled),
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }
                if (isCurrentAsr) {
                    MiuixText(
                        text = stringResource(R.string.model_current),
                        color = MiuixTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (!model.isBundled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (model.type.name.startsWith("ASR")) {
                        MiuixButton(
                            onClick = onSetCurrentAsr,
                            modifier = Modifier.weight(1f)
                        ) {
                            MiuixText(stringResource(R.string.model_set_as_current))
                        }
                    }
                    MiuixButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        MiuixText(stringResource(R.string.model_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCardMaterial(
    model: ModelInfo,
    isCurrentAsr: Boolean,
    onDelete: () -> Unit,
    onSetCurrentAsr: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = modelMetadataText(model),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (model.isBundled) {
                        Text(
                            text = stringResource(R.string.model_bundled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (isCurrentAsr) {
                    Text(
                        text = stringResource(R.string.model_current),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (!model.isBundled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (model.type.name.startsWith("ASR")) {
                        OutlinedButton(
                            onClick = onSetCurrentAsr,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.model_set_as_current))
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.model_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelDownloadGuideCard(category: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.model_download_guide_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            modelGuideLines(category).forEach { lineRes ->
                Text(
                    text = stringResource(lineRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ModelDownloadGuideCardMiuix(category: String) {
    ClarivoMiuixCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MiuixText(
                text = stringResource(R.string.model_download_guide_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.primary
            )
            modelGuideLines(category).forEach { lineRes ->
                MiuixText(
                    text = stringResource(lineRes),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

@Composable
private fun ImportButtonsMaterial(category: String, onImport: (ModelType) -> Unit) {
    importOptionsForCategory(category).forEach { option ->
        OutlinedButton(
            onClick = { onImport(option.type) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(option.labelRes))
        }
    }
}

@Composable
private fun ImportButtonsMiuix(category: String, onImport: (ModelType) -> Unit) {
    importOptionsForCategory(category).forEach { option ->
        MiuixButton(
            onClick = { onImport(option.type) },
            modifier = Modifier.fillMaxWidth()
        ) {
            MiuixText(stringResource(option.labelRes))
        }
    }
}

private const val MODEL_CATEGORY_DENOISE = "denoise"
private const val MODEL_CATEGORY_TRANSCRIBE = "transcribe"

private data class ImportOption(
    val type: ModelType,
    val labelRes: Int
)

private fun List<ModelInfo>.filterForCategory(category: String): List<ModelInfo> {
    return when (category) {
        MODEL_CATEGORY_DENOISE -> filter {
            it.type == ModelType.DENOISE_DEEPFILTERNET || it.type == ModelType.DENOISE_RNNOISE
        }
        else -> filter {
            it.type == ModelType.ASR_WHISPER_TINY ||
                    it.type == ModelType.ASR_WHISPER_BASE ||
                    it.type == ModelType.ASR_WHISPER_SMALL ||
                    it.type == ModelType.ASR_WHISPER_MEDIUM ||
                    it.type == ModelType.ASR_WHISPER_LARGE ||
                    it.type == ModelType.VAD_SILERO
        }
    }
}

private fun modelManagerTitleRes(category: String): Int = when (category) {
    MODEL_CATEGORY_DENOISE -> R.string.model_denoise_category
    else -> R.string.model_transcribe_category
}

@Composable
private fun modelCategoryName(category: String): String = when (category) {
    MODEL_CATEGORY_DENOISE -> stringResource(R.string.model_denoise_category)
    else -> stringResource(R.string.model_transcribe_category)
}

private fun modelGuideLines(category: String): List<Int> {
    return when (category) {
        MODEL_CATEGORY_DENOISE -> listOf(R.string.model_download_guide_denoise)
        else -> listOf(
            R.string.model_download_guide_whisper,
            R.string.model_download_guide_vad
        )
    }
}

@Composable
private fun modelMetadataText(model: ModelInfo): String {
    val sizeText = if (model.type == ModelType.DENOISE_RNNOISE && model.isBundled) {
        stringResource(R.string.model_builtin_native)
    } else {
        formatSize(model.sizeBytes, stringResource(R.string.model_not_installed))
    }
    val versionText = model.version.takeIf { it.isNotBlank() } ?: "-"
    return "$versionText | ${model.type.name} | ${model.status.name} | $sizeText"
}

private fun importOptionsForCategory(category: String): List<ImportOption> {
    return when (category) {
        MODEL_CATEGORY_DENOISE -> listOf(
            ImportOption(ModelType.DENOISE_DEEPFILTERNET, R.string.model_import_denoise)
        )
        else -> listOf(
            ImportOption(ModelType.ASR_WHISPER_BASE, R.string.model_import_whisper),
            ImportOption(ModelType.VAD_SILERO, R.string.model_import_vad)
        )
    }
}

private fun formatSize(bytes: Long, notInstalled: String): String {
    if (bytes <= 0) return notInstalled
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
