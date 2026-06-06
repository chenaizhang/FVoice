package com.fvoice.app.core.task

import android.content.Context
import android.net.Uri
import com.fvoice.app.R
import com.fvoice.app.core.asr.TranscriptExporter
import com.fvoice.app.core.asr.TranscriptionProcessor
import com.fvoice.app.core.asr.WhisperCppEngine
import com.fvoice.app.core.denoise.AndroidDeepFilterNetEngine
import com.fvoice.app.core.denoise.DenoiseProcessor
import com.fvoice.app.core.denoise.RnNoiseEngine
import com.fvoice.app.core.model.ModelType
import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.media.MediaExportManager
import com.fvoice.app.core.media.MediaProbe
import com.fvoice.app.core.media.OutputNameGenerator
import com.fvoice.app.core.media.VideoAudioExtractor
import com.fvoice.app.core.model.OutputFileInfo
import com.fvoice.app.core.model.OutputFileType
import com.fvoice.app.core.model.ProcessProgress
import com.fvoice.app.core.model.ProcessResult
import com.fvoice.app.core.model.ProcessTask
import com.fvoice.app.core.model.ProcessTaskStatus
import com.fvoice.app.core.model.ProcessTaskType
import com.fvoice.app.core.model.TaskSettings
import com.fvoice.app.core.model.TranscriptResult
import com.fvoice.app.core.modelmanager.ModelManager
import com.fvoice.app.core.recording.RecordingSession
import com.fvoice.app.core.vad.SileroVadEngine
import com.fvoice.app.core.vad.VadSegmenter
import com.fvoice.app.data.model.DenoiseStrength
import com.fvoice.app.util.FVoiceLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProcessTaskManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val stateStore = ProcessStateStore(appContext)
    private val queue = ProcessQueue()
    private val mediaProbe = MediaProbe(appContext)
    private val videoExtractor = VideoAudioExtractor(appContext)
    private val exportManager = MediaExportManager(appContext)
    private val videoAudioMuxer = com.fvoice.app.core.media.VideoAudioMuxer(appContext)
    private val transcriptExporter = TranscriptExporter()
    private val modelManager = ModelManager(appContext)

    private val _progress = MutableStateFlow(ProcessProgress())
    val progress: StateFlow<ProcessProgress> = _progress.asStateFlow()

    private val _resultEvent = MutableSharedFlow<ProcessResult>()
    val resultEvent: SharedFlow<ProcessResult> = _resultEvent.asSharedFlow()

    private val _historyChanged = MutableStateFlow(0L)
    val historyChanged: StateFlow<Long> = _historyChanged.asStateFlow()

    private val historyChangeListeners = mutableListOf<() -> Unit>()

    fun addHistoryChangeListener(listener: () -> Unit) {
        historyChangeListeners.add(listener)
    }

    fun removeHistoryChangeListener(listener: () -> Unit) {
        historyChangeListeners.remove(listener)
    }

    private fun notifyHistoryChanged() {
        _historyChanged.value = System.currentTimeMillis()
        historyChangeListeners.forEach { it() }
    }

    @Volatile
    private var _lastResult: ProcessResult? = null
    val lastResult: ProcessResult? get() = _lastResult

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentTask: StateFlow<ProcessTask?> = queue.currentTask
    val isRunning: StateFlow<Boolean> = queue.isRunning

    init {
        stateStore.cleanupOrphanedTempFiles(appContext)
        val orphaned = stateStore.loadCurrentTask()
        if (orphaned != null && (orphaned.status == ProcessTaskStatus.PROCESSING || orphaned.status == ProcessTaskStatus.PENDING)) {
            stateStore.markInterruptedAsFailed()
        }
    }

    fun enqueue(
        type: ProcessTaskType,
        sourceUri: Uri,
        sourceFileName: String,
        settings: TaskSettings = TaskSettings()
    ): ProcessTask {
        if (queue.isRunning.value) {
            throw com.fvoice.app.core.exception.FVoiceException.TaskAlreadyRunning()
        }
        val task = queue.enqueue(type, sourceUri.toString(), sourceFileName, settings)
        stateStore.saveCurrentTask(task)
        return task
    }

    fun startTask(taskId: String, onStartService: () -> Unit) {
        if (queue.isRunning.value) {
            throw com.fvoice.app.core.exception.FVoiceException.TaskAlreadyRunning()
        }
        val token = queue.startTask(taskId)
        val task = queue.currentTask.value ?: return
        stateStore.saveCurrentTask(task)
        onStartService()

        scope.launch {
            try {
                processTask(task, token)
            } catch (e: com.fvoice.app.core.exception.FVoiceException.CancelledByUser) {
                FVoiceLogger.i("Task cancelled: $taskId")
                queue.cancelCurrent()
                val t = queue.currentTask.value ?: task.copy(status = ProcessTaskStatus.CANCELLED)
                stateStore.archiveTask(t)
                stateStore.clearCurrentTask()
                val result = ProcessResult.Cancelled(taskId)
                _lastResult = result
                _resultEvent.emit(result)
            } catch (e: Exception) {
                FVoiceLogger.e("Task failed: $taskId", e)
                val (code, message) = ProcessErrorMapper.map(appContext, e)
                queue.failTask(taskId, code, message)
                val t = queue.currentTask.value ?: task
                stateStore.archiveTask(t)
                stateStore.clearCurrentTask()
                val result = ProcessResult.Failure(taskId, code, message)
                _lastResult = result
                _resultEvent.emit(result)
            }
        }
    }

    fun cancelCurrent() {
        queue.cancelCurrent()
    }

    fun loadHistory(): List<ProcessTask> = stateStore.loadAllTasks()

    fun loadStoredResult(taskId: String): ProcessResult.Success? {
        val stored = stateStore.loadResult(taskId) ?: return null
        return ProcessResult.Success(
            taskId = stored.taskId,
            outputFiles = stored.toOutputFiles(),
            transcriptResult = stored.transcriptResult
        )
    }

    fun clearHistory() {
        stateStore.clearHistory()
        notifyHistoryChanged()
    }

    fun deleteTask(taskId: String) {
        stateStore.deleteTask(taskId)
        queue.removeCurrentTask(taskId)
        notifyHistoryChanged()
    }

    /**
     * TODO: Add large file warning (4GB / 2 hours) before processing.
     * TODO: Implement internal chunking for long audio files instead of processing the whole file at once.
     */
    private suspend fun processTask(task: ProcessTask, token: ProcessCancellationToken) {
        val sourceUri = Uri.parse(task.sourceUri)
        val tempDir = File(appContext.cacheDir, "process_temp/${task.id}").apply { mkdirs() }
        val outputFiles = mutableListOf<OutputFileInfo>()
        var transcriptResult: TranscriptResult? = null
        val taskLogFile = File(appContext.filesDir, "logs/tasks/${task.sourceFileName}_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}_log.txt").apply { parentFile?.mkdirs() }
        val initialLogLines = com.fvoice.app.util.FVoiceLogger.getRecentLogs(1000).size
        fun appendTaskLog(msg: String) {
            try {
                taskLogFile.appendText("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $msg\n")
            } catch (_: Exception) {}
        }
        appendTaskLog("Task started: ${task.type} | ${task.sourceFileName}")

        try {
            // Stage 1: Probe
            emitProgress(5, appContext.getString(R.string.stage_read_file))
            val mediaInfo = withContext(Dispatchers.IO) {
                mediaProbe.probe(sourceUri, task.sourceFileName)
            } ?: throw com.fvoice.app.core.exception.FVoiceException.MediaProbeFailed("Cannot probe file")
            token.throwIfCancelled()

            val selectedDenoiseModel = if (task.type == ProcessTaskType.DENOISE || task.type == ProcessTaskType.DENOISE_AND_TRANSCRIBE) {
                resolveDenoiseModel(task)
            } else {
                null
            }
            val targetSampleRate = if (selectedDenoiseModel?.type == ModelType.DENOISE_DEEPFILTERNET) {
                48000
            } else {
                16000
            }

            // Stage 2: Decode/extract audio to the sample rate required by the selected engine.
            emitProgress(15, appContext.getString(R.string.stage_extract_audio))
            val audioFile = File(tempDir, OutputNameGenerator.generate(
                task.sourceFileName,
                OutputNameGenerator.ProcessType.EXTRACTED_AUDIO,
                "wav"
            ))
            withContext(Dispatchers.IO) {
                videoExtractor.extractToWav(sourceUri, audioFile, targetSampleRate = targetSampleRate, cancellationToken = token)
            }
            token.throwIfCancelled()

            exportManager.exportToDownloads(
                audioFile,
                audioFile.name,
                "audio/wav",
                OutputFileType.AUDIO_EXTRACTED
            )?.let { outputFiles.add(it) }

            var workingFile = audioFile

            // Stage 3: Denoise
            if (task.type == ProcessTaskType.DENOISE || task.type == ProcessTaskType.DENOISE_AND_TRANSCRIBE) {
                emitProgress(40, appContext.getString(R.string.stage_denoise))
                val denoisedWav = File(tempDir, OutputNameGenerator.generate(
                    task.sourceFileName,
                    OutputNameGenerator.ProcessType.DENOISED,
                    "wav"
                ))
                val denoiseModel = selectedDenoiseModel
                val denoiseEngine = when (denoiseModel?.type) {
                    ModelType.DENOISE_RNNOISE -> RnNoiseEngine()
                    else -> AndroidDeepFilterNetEngine(appContext)
                }
                val denoiser = DenoiseProcessor(denoiseEngine)
                val audioInfo = com.fvoice.app.core.model.AudioFormatInfo(
                    sampleRate = targetSampleRate,
                    channels = 1,
                    bitDepth = 16
                )
                val denoiseStrength = runCatching {
                    DenoiseStrength.valueOf(task.settings.denoiseStrength)
                }.getOrDefault(DenoiseStrength.STANDARD)
                withContext(Dispatchers.IO) {
                    denoiser.process(
                        inputFile = workingFile,
                        outputFile = denoisedWav,
                        audioInfo = audioInfo,
                        model = denoiseModel,
                        strength = denoiseStrength,
                        cancellationToken = token
                    )
                }
                workingFile = denoisedWav
                token.throwIfCancelled()

                // Export denoised audio
                val exported = exportManager.exportToDownloads(
                    denoisedWav,
                    denoisedWav.name,
                    "audio/wav",
                    OutputFileType.AUDIO_DENOISED
                )
                exported?.let { outputFiles.add(it) }

                // If source is video, mux denoised audio back into video
                if (mediaInfo.isVideo) {
                    emitProgress(55, appContext.getString(R.string.stage_merge_video))
                    val mergedVideo = File(tempDir, OutputNameGenerator.generate(
                        task.sourceFileName,
                        OutputNameGenerator.ProcessType.DENOISED_VIDEO,
                        "mp4"
                    ))
                    val muxSuccess = withContext(Dispatchers.IO) {
                        videoAudioMuxer.muxVideoWithPcmAudio(
                            sourceVideoUri = sourceUri,
                            audioWavFile = denoisedWav,
                            outputFile = mergedVideo,
                            cancellationToken = token
                        )
                    }
                    if (muxSuccess) {
                        exportManager.exportToDownloads(
                            mergedVideo,
                            mergedVideo.name,
                            "video/mp4",
                            OutputFileType.VIDEO_DENOISED
                        )?.let { outputFiles.add(it) }
                    } else {
                        appendTaskLog("Video merge failed, falling back to audio-only output")
                    }
                    token.throwIfCancelled()
                }
            }

            // Stage 4: Transcribe
            if (task.type == ProcessTaskType.TRANSCRIBE || task.type == ProcessTaskType.DENOISE_AND_TRANSCRIBE) {
                emitProgress(70, appContext.getString(R.string.stage_transcribe))

                val vadSegments = if (task.settings.useVad && targetSampleRate == 16000) {
                    val vadModel = modelManager.models.value.find { it.type == com.fvoice.app.core.model.ModelType.VAD_SILERO }
                    val vad = VadSegmenter(SileroVadEngine())
                    withContext(Dispatchers.IO) {
                        vad.segment(workingFile, 16000, model = vadModel, cancellationToken = token)
                    }
                } else null
                token.throwIfCancelled()

                val asrModel = task.settings.asrModelId
                    .ifBlank { task.settings.modelId }
                    .takeIf { it.isNotBlank() }
                    ?.let { modelId ->
                        modelManager.models.value.find {
                            it.id == modelId && it.status == com.fvoice.app.core.model.ModelStatus.READY
                        }
                    }
                    ?: modelManager.currentAsrModel.value
                        ?.takeIf { it.status == com.fvoice.app.core.model.ModelStatus.READY }
                val asr = TranscriptionProcessor(WhisperCppEngine())
                transcriptResult = withContext(Dispatchers.IO) {
                    asr.process(
                        pcmFile = workingFile,
                        sampleRate = targetSampleRate,
                        language = task.settings.language,
                        sourceFileName = task.sourceFileName,
                        sourceUri = task.sourceUri,
                        model = asrModel,
                        segments = vadSegments,
                        cancellationToken = token
                    )
                }
                token.throwIfCancelled()

                // Export transcripts
                emitProgress(90, appContext.getString(R.string.stage_export))
                task.settings.outputFormats.forEach { format ->
                    val ext = format.lowercase()
                    val outFile = File(tempDir, OutputNameGenerator.generate(
                        task.sourceFileName,
                        if (ext == "srt" || ext == "vtt") OutputNameGenerator.ProcessType.SUBTITLE else OutputNameGenerator.ProcessType.TRANSCRIPT,
                        ext
                    ))
                    when (format.uppercase()) {
                        "TXT" -> transcriptExporter.exportTxt(transcriptResult, outFile)
                        "SRT" -> transcriptExporter.exportSrt(transcriptResult, outFile)
                        "VTT" -> transcriptExporter.exportVtt(transcriptResult, outFile)
                        "JSON" -> transcriptExporter.exportJson(transcriptResult, outFile)
                    }
                    val type = when (format.uppercase()) {
                        "TXT" -> OutputFileType.TRANSCRIPT_TXT
                        "SRT" -> OutputFileType.TRANSCRIPT_SRT
                        "VTT" -> OutputFileType.TRANSCRIPT_VTT
                        "JSON" -> OutputFileType.TRANSCRIPT_JSON
                        else -> OutputFileType.TRANSCRIPT_TXT
                    }
                    val exported = exportManager.exportToDownloads(outFile, outFile.name, "text/plain", type)
                        ?: exportManager.exportToDownloads(outFile, outFile.name, "application/json", type)
                    exported?.let { outputFiles.add(it) }
                }
            }

            // Stage 5: Complete
            emitProgress(100, appContext.getString(R.string.stage_completed))
            queue.completeTask(task.id)
            val completedTask = queue.currentTask.value ?: task
            stateStore.archiveTask(completedTask)
            stateStore.clearCurrentTask()
            val result = ProcessResult.Success(task.id, outputFiles, transcriptResult)
            stateStore.saveResult(task.id, outputFiles, transcriptResult)
            _lastResult = result
            _resultEvent.emit(result)

            appendTaskLog("Task completed successfully")

        } catch (e: Exception) {
            appendTaskLog("Task failed: ${e.message}")
            throw e
        } finally {
            // Always cleanup temp files after task completes/fails/cancels
            tempDir.deleteRecursively()
            // Collect recent logs and append to task log file
            try {
                val recentLogs = com.fvoice.app.util.FVoiceLogger.getRecentLogs(500)
                if (recentLogs.isNotEmpty()) {
                    taskLogFile.appendText("\n--- Processing Logs ---\n")
                    recentLogs.forEach { taskLogFile.appendText("$it\n") }
                }
                // Export task log to Downloads if file has content
                if (taskLogFile.length() > 0) {
                    exportManager.exportToDownloads(
                        taskLogFile,
                        taskLogFile.name,
                        "text/plain",
                        OutputFileType.LOG
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun copyUriToTemp(uri: Uri, tempDir: File, fileName: String): File {
        return withContext(Dispatchers.IO) {
            val ext = fileName.substringAfterLast(".", "bin")
            val tempFile = File(tempDir, "input_copy.$ext")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        }
    }

    private fun resolveDenoiseModel(task: ProcessTask): ModelInfo? {
        val readyModels = modelManager.models.value.filter {
            it.status == com.fvoice.app.core.model.ModelStatus.READY
        }
        val requestedModelId = task.settings.denoiseModelId.takeIf { it.isNotBlank() }
        if (requestedModelId != null) {
            return readyModels.find { it.id == requestedModelId }
                ?: throw com.fvoice.app.core.exception.FVoiceException.DenoiseFailed(
                    "Selected denoise model is not available: $requestedModelId"
                )
        }

        val denoiseStrength = runCatching {
            DenoiseStrength.valueOf(task.settings.denoiseStrength)
        }.getOrDefault(DenoiseStrength.STANDARD)
        val preferredId = when (denoiseStrength) {
            DenoiseStrength.STANDARD -> "rnnoise_default"
            DenoiseStrength.STRONG -> "deepfilternet3_onnx"
            DenoiseStrength.CUSTOM -> "deepfilternet3_onnx"
        }

        return readyModels.find { it.id == preferredId }
            ?: readyModels.find { it.id == "deepfilternet3_onnx" }
            ?: readyModels.find { it.id == "rnnoise_default" }
            ?: readyModels.find {
                it.type == ModelType.DENOISE_DEEPFILTERNET || it.type == ModelType.DENOISE_RNNOISE
            }
    }

    private fun emitProgress(percent: Int, stage: String) {
        _progress.value = ProcessProgress(percent = percent, currentStage = stage)
        val task = queue.currentTask.value ?: return
        queue.updateProgress(task.id, percent, stage)
        stateStore.saveCurrentTask(queue.currentTask.value ?: return)
    }

    private fun guessAudioMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            else -> "audio/*"
        }
    }

    fun shutdown() {
        scope.cancel()
    }

    companion object {
        @Volatile
        private var instance: ProcessTaskManager? = null

        fun getInstance(context: Context): ProcessTaskManager {
            return instance ?: synchronized(this) {
                instance ?: ProcessTaskManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
