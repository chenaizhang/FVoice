package com.clarivo.app.core.modelmanager

import android.content.Context
import com.clarivo.app.core.model.ModelInfo
import com.clarivo.app.core.model.ModelStatus
import com.clarivo.app.core.model.ModelType
import com.clarivo.app.util.ClarivoLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class ModelManager(private val context: Context) {

    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
    private val denoiseDir = File(modelsDir, "denoise").apply { mkdirs() }
    private val asrDir = File(modelsDir, "asr").apply { mkdirs() }
    private val vadDir = File(modelsDir, "vad").apply { mkdirs() }

    private val _models = MutableStateFlow(listOf<ModelInfo>())
    val models: StateFlow<List<ModelInfo>> = _models.asStateFlow()

    private val _currentAsrModel = MutableStateFlow<ModelInfo?>(null)
    val currentAsrModel: StateFlow<ModelInfo?> = _currentAsrModel.asStateFlow()

    init {
        installBundledModels(context)
        cleanupLegacyBundledModels(context)
        refreshModels()
    }

    fun refreshModels() {
        val list = buildList {
            addBundledModelInfos()
            val whisperFile = File(asrDir, "ggml-tiny.bin")
            val hasWhisperFile = whisperFile.isValidModelFile(ModelType.ASR_WHISPER_TINY)
            add(
                ModelInfo(
                    id = "whisper_tiny",
                    name = "Whisper Tiny",
                    type = ModelType.ASR_WHISPER_TINY,
                    version = "openai/whisper",
                    status = if (hasWhisperFile) ModelStatus.READY else ModelStatus.NOT_INSTALLED,
                    path = whisperFile.absolutePath,
                    isBundled = true,
                    sizeBytes = if (hasWhisperFile) whisperFile.length() else 0,
                    description = "Tiny ASR model for fast transcription"
                )
            )
            val vadFile = File(vadDir, "silero_vad.onnx")
            val hasVadFile = vadFile.isValidModelFile(ModelType.VAD_SILERO)
            add(
                ModelInfo(
                    id = "silero_vad",
                    name = "Silero VAD",
                    type = ModelType.VAD_SILERO,
                    version = "4.0",
                    status = if (hasVadFile) ModelStatus.READY else ModelStatus.NOT_INSTALLED,
                    path = vadFile.absolutePath,
                    isBundled = true,
                    sizeBytes = if (hasVadFile) vadFile.length() else 0,
                    description = "Voice activity detection model"
                )
            )
            // Scan user-imported models
            addAll(scanImportedModels())
        }
        _models.value = list
        if (_currentAsrModel.value == null) {
            _currentAsrModel.value = list.find { it.type == ModelType.ASR_WHISPER_TINY && it.status == ModelStatus.READY }
        }
    }

    private fun scanImportedModels(): List<ModelInfo> {
        val imported = mutableListOf<ModelInfo>()
        val bundledDestinations = bundledModelSpecs
            .map { File(modelsDir, it.destinationPath).absolutePath }
            .plus(legacyBundledDestinationPaths.map { File(modelsDir, it).absolutePath })
            .toSet()

        fun File.isUserImportedModel(): Boolean {
            return isFile && absolutePath !in bundledDestinations
        }

        // Scan ASR models
        asrDir.listFiles()?.filter { it.isUserImportedModel() && it.extension == "bin" }?.forEach { file ->
            if (file.name.startsWith("ggml-")) {
                val size = file.nameWithoutExtension.removePrefix("ggml-")
                val type = when (size) {
                    "tiny" -> ModelType.ASR_WHISPER_TINY
                    "base" -> ModelType.ASR_WHISPER_BASE
                    "small" -> ModelType.ASR_WHISPER_SMALL
                    "medium" -> ModelType.ASR_WHISPER_MEDIUM
                    "large" -> ModelType.ASR_WHISPER_LARGE
                    else -> ModelType.ASR_WHISPER_BASE
                }
                if (!file.isValidModelFile(type)) return@forEach
                imported.add(
                    ModelInfo(
                        id = "whisper_${size}_imported",
                        name = "Whisper ${size.replaceFirstChar { it.uppercase() }}",
                        type = type,
                        version = "openai/whisper",
                        status = ModelStatus.READY,
                        path = file.absolutePath,
                        isBundled = false,
                        sizeBytes = file.length(),
                        description = "User imported Whisper $size model"
                    )
                )
            }
        }

        // Scan denoise models
        denoiseDir.listFiles()?.filter {
            it.isUserImportedModel() && it.isValidModelFile(ModelType.DENOISE_DEEPFILTERNET)
        }?.forEach { file ->
            imported.add(
                ModelInfo(
                    id = "denoise_${file.name}",
                    name = file.nameWithoutExtension,
                    type = ModelType.DENOISE_DEEPFILTERNET,
                    version = "unknown",
                    status = ModelStatus.READY,
                    path = file.absolutePath,
                    isBundled = false,
                    sizeBytes = file.length(),
                    description = "User imported denoise model"
                )
            )
        }

        // Scan VAD models
        vadDir.listFiles()?.filter {
            it.isUserImportedModel() && it.extension == "onnx" && it.isValidModelFile(ModelType.VAD_SILERO)
        }?.forEach { file ->
            imported.add(
                ModelInfo(
                    id = "vad_${file.name}",
                    name = file.nameWithoutExtension,
                    type = ModelType.VAD_SILERO,
                    version = "unknown",
                    status = ModelStatus.READY,
                    path = file.absolutePath,
                    isBundled = false,
                    sizeBytes = file.length(),
                    description = "User imported VAD model"
                )
            )
        }

        return imported
    }

    private fun MutableList<ModelInfo>.addBundledModelInfos() {
        bundledModelSpecs
            .filter { it.type == ModelType.DENOISE_RNNOISE || it.type == ModelType.DENOISE_DEEPFILTERNET }
            .forEach { spec ->
                val file = File(modelsDir, spec.destinationPath)
                val hasFile = file.isValidModelFile(spec.type)
                add(
                    ModelInfo(
                        id = spec.id,
                        name = spec.name,
                        type = spec.type,
                        version = spec.version,
                        status = when {
                            !hasFile -> ModelStatus.NOT_INSTALLED
                            !spec.runtimeSupported -> ModelStatus.DISABLED
                            else -> ModelStatus.READY
                        },
                        path = file.absolutePath,
                        isBundled = true,
                        sizeBytes = if (hasFile) file.length() else 0,
                        description = spec.description
                    )
                )
            }
    }

    fun setCurrentAsrModel(modelId: String) {
        _currentAsrModel.value = _models.value.find { it.id == modelId }
    }

    fun deleteModel(modelId: String): Boolean {
        val model = _models.value.find { it.id == modelId } ?: return false
        if (model.isBundled) return false
        File(model.path).delete()
        refreshModels()
        return true
    }

    fun importModel(sourceFile: File, type: ModelType): Boolean {
        return try {
            val destDir = when (type) {
                ModelType.DENOISE_DEEPFILTERNET, ModelType.DENOISE_RNNOISE -> denoiseDir
                ModelType.ASR_WHISPER_TINY, ModelType.ASR_WHISPER_BASE,
                ModelType.ASR_WHISPER_SMALL, ModelType.ASR_WHISPER_MEDIUM,
                ModelType.ASR_WHISPER_LARGE -> asrDir
                ModelType.VAD_SILERO -> vadDir
            }
            val dest = File(destDir, sourceFile.name)
            sourceFile.copyTo(dest, overwrite = true)
            refreshModels()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getModelsDir(): File = modelsDir

    private fun File.isValidModelFile(type: ModelType): Boolean = isValidModelFile(this, type)

    companion object {
        private data class BundledModelSpec(
            val id: String,
            val name: String,
            val assetPath: String,
            val destinationPath: String,
            val type: ModelType,
            val version: String,
            val description: String,
            val runtimeSupported: Boolean = true
        )

        private val bundledModelSpecs = listOf(
            BundledModelSpec(
                id = "rnnoise_default",
                name = "RNNoise",
                assetPath = "models/denoise/rnnoise.bundled",
                destinationPath = "denoise/rnnoise.bundled",
                type = ModelType.DENOISE_RNNOISE,
                version = "default",
                description = "RNNoise built-in recurrent neural network denoise model"
            ),
            BundledModelSpec(
                id = "deepfilternet3_onnx",
                name = "DeepFilterNet3 ONNX",
                assetPath = "models/denoise/deepfilternet3_onnx.tgz",
                destinationPath = "denoise/deepfilternet3_onnx.tgz",
                type = ModelType.DENOISE_DEEPFILTERNET,
                version = "DeepFilterNet3 ONNX",
                description = "Official DeepFilterNet3 ONNX denoise model bundled with app"
            ),
            BundledModelSpec(
                id = "whisper_tiny",
                name = "Whisper Tiny",
                assetPath = "models/asr/ggml-tiny.bin",
                destinationPath = "asr/ggml-tiny.bin",
                type = ModelType.ASR_WHISPER_TINY,
                version = "openai/whisper",
                description = "Tiny ASR model for fast transcription"
            ),
            BundledModelSpec(
                id = "silero_vad",
                name = "Silero VAD",
                assetPath = "models/vad/silero_vad.onnx",
                destinationPath = "vad/silero_vad.onnx",
                type = ModelType.VAD_SILERO,
                version = "4.0",
                description = "Voice activity detection model"
            )
        )

        private val legacyBundledDestinationPaths = listOf(
            "denoise/deepfilternet_base.tgz",
            "denoise/deepfilternet_small.tgz",
            "denoise/deepfilternet_medium.tgz",
            "denoise/deepfilternet_lite.tgz"
        )

        fun installBundledModels(context: Context) {
            val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
            bundledModelSpecs.forEach { spec ->
                val destination = File(modelsDir, spec.destinationPath)
                val bundledSize = assetSize(context, spec.assetPath)
                if (isValidModelFile(destination, spec.type) &&
                    (bundledSize <= 0L || destination.length() == bundledSize)
                ) {
                    return@forEach
                }
                if (destination.exists()) {
                    ClarivoLogger.w("Replacing bundled model: ${spec.destinationPath}")
                }

                try {
                    destination.parentFile?.mkdirs()
                    context.assets.open(spec.assetPath).use { input ->
                        val tempFile = File(destination.parentFile, "${destination.name}.tmp")
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                        if (isValidModelFile(tempFile, spec.type)) {
                            if (destination.exists() && !destination.delete()) {
                                throw IllegalStateException("Cannot replace ${destination.absolutePath}")
                            }
                            if (!tempFile.renameTo(destination)) {
                                tempFile.copyTo(destination, overwrite = true)
                                tempFile.delete()
                            }
                            ClarivoLogger.i("Installed bundled model: ${spec.destinationPath}")
                        } else {
                            tempFile.delete()
                            ClarivoLogger.w("Bundled model asset is invalid: ${spec.assetPath}")
                        }
                    }
                } catch (e: java.io.FileNotFoundException) {
                    ClarivoLogger.w("Bundled model asset not found: ${spec.assetPath}")
                } catch (e: Exception) {
                    ClarivoLogger.e("Failed to install bundled model: ${spec.assetPath}", e)
                }
            }
        }

        fun cleanupLegacyBundledModels(context: Context) {
            val modelsDir = File(context.filesDir, "models")
            legacyBundledDestinationPaths.forEach { path ->
                val file = File(modelsDir, path)
                if (file.exists() && file.delete()) {
                    ClarivoLogger.i("Removed legacy fake bundled model: $path")
                }
            }
        }

        private fun isValidModelFile(file: File, type: ModelType): Boolean {
            if (!file.isFile || file.length() <= 0) return false
            return when (type) {
                ModelType.ASR_WHISPER_TINY,
                ModelType.ASR_WHISPER_BASE,
                ModelType.ASR_WHISPER_SMALL,
                ModelType.ASR_WHISPER_MEDIUM,
                ModelType.ASR_WHISPER_LARGE -> isValidWhisperModel(file)
                ModelType.VAD_SILERO -> isValidOnnxModel(file)
                ModelType.DENOISE_DEEPFILTERNET -> isValidDenoiseBundle(file)
                ModelType.DENOISE_RNNOISE -> file.length() > 0
            }
        }

        private fun assetSize(context: Context, assetPath: String): Long {
            return try {
                context.assets.open(assetPath).use { input ->
                    input.available().toLong()
                }
            } catch (e: Exception) {
                -1L
            }
        }

        private fun isValidWhisperModel(file: File): Boolean {
            if (file.length() < 1_000_000L) return false
            val header = file.readHeader(4) ?: return false
            return header.contentEquals(byteArrayOf('l'.code.toByte(), 'm'.code.toByte(), 'g'.code.toByte(), 'g'.code.toByte())) ||
                    header.contentEquals(byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte()))
        }

        private fun isValidOnnxModel(file: File): Boolean {
            return file.length() > 16 * 1024L
        }

        private fun isValidDenoiseBundle(file: File): Boolean {
            val header = file.readHeader(2) ?: return false
            val isGzip = header[0] == 0x1f.toByte() && header[1] == 0x8b.toByte()
            val isZip = header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
            return (isGzip || isZip) && file.length() > 1_000_000L
        }

        private fun File.readHeader(size: Int): ByteArray? {
            return try {
                inputStream().use { input ->
                    val header = ByteArray(size)
                    val read = input.read(header)
                    if (read == size) header else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
