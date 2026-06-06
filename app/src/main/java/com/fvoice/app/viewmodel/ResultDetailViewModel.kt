package com.fvoice.app.viewmodel

import androidx.lifecycle.ViewModel
import com.fvoice.app.core.model.OutputFileInfo
import com.fvoice.app.core.model.ProcessResult
import com.fvoice.app.core.model.ProcessTaskStatus
import com.fvoice.app.core.model.ProcessTaskType
import com.fvoice.app.core.model.TranscriptResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.fvoice.app.FVoiceApplication

data class ResultDetailUiState(
    val taskId: String = "",
    val sourceFileName: String = "",
    val sourceUri: String = "",
    val status: ProcessTaskStatus = ProcessTaskStatus.COMPLETED,
    val taskType: ProcessTaskType = ProcessTaskType.DENOISE_AND_TRANSCRIBE,
    val isVideoSource: Boolean = false,
    val transcriptResult: TranscriptResult? = null,
    val outputFiles: List<OutputFileInfo> = emptyList(),
    val audioFiles: List<OutputFileInfo> = emptyList(),
    val videoFiles: List<OutputFileInfo> = emptyList(),
    val transcriptFiles: List<OutputFileInfo> = emptyList(),
    val errorMessage: String = ""
)

class ResultDetailViewModel : ViewModel() {

    private val taskManager = FVoiceApplication.processTaskManager

    private val _uiState = MutableStateFlow(ResultDetailUiState())
    val uiState: StateFlow<ResultDetailUiState> = _uiState.asStateFlow()

    fun deleteTask(taskId: String) {
        taskManager.deleteTask(taskId)
    }

    fun loadTask(taskId: String) {
        val history = taskManager.loadHistory()
        val task = history.find { it.id == taskId }

        // Try to get result from lastResult if taskId matches
        val lastResult = taskManager.lastResult
        if (lastResult != null && lastResult.taskId == taskId) {
            loadResult(lastResult)
            task?.let {
                _uiState.value = _uiState.value.copy(
                    sourceFileName = it.sourceFileName,
                    sourceUri = it.sourceUri,
                    status = it.status,
                    taskType = it.type,
                    isVideoSource = isVideoFileName(it.sourceFileName),
                    errorMessage = it.errorMessage
                )
            }
            return
        }

        val storedResult = taskManager.loadStoredResult(taskId)
        if (storedResult != null) {
            loadResult(storedResult)
            task?.let {
                _uiState.value = _uiState.value.copy(
                    sourceFileName = it.sourceFileName,
                    sourceUri = it.sourceUri,
                    status = it.status,
                    taskType = it.type,
                    isVideoSource = isVideoFileName(it.sourceFileName),
                    errorMessage = it.errorMessage
                )
            }
            return
        }

        if (task != null) {
            _uiState.value = ResultDetailUiState(
                taskId = task.id,
                sourceFileName = task.sourceFileName,
                sourceUri = task.sourceUri,
                status = task.status,
                taskType = task.type,
                isVideoSource = isVideoFileName(task.sourceFileName)
            )
        }
    }

    private fun isVideoFileName(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v")
    }

    fun loadResult(result: ProcessResult) {
        when (result) {
            is ProcessResult.Success -> {
                val audio = result.outputFiles.filter {
                    it.type == com.fvoice.app.core.model.OutputFileType.AUDIO_DENOISED
                            || it.type == com.fvoice.app.core.model.OutputFileType.AUDIO_EXTRACTED
                }
                val video = result.outputFiles.filter {
                    it.type == com.fvoice.app.core.model.OutputFileType.VIDEO_DENOISED
                }
                val transcripts = result.outputFiles.filter {
                    it.type == com.fvoice.app.core.model.OutputFileType.TRANSCRIPT_TXT
                            || it.type == com.fvoice.app.core.model.OutputFileType.TRANSCRIPT_SRT
                            || it.type == com.fvoice.app.core.model.OutputFileType.TRANSCRIPT_VTT
                            || it.type == com.fvoice.app.core.model.OutputFileType.TRANSCRIPT_JSON
                }
                _uiState.value = ResultDetailUiState(
                    taskId = result.taskId,
                    status = ProcessTaskStatus.COMPLETED,
                    transcriptResult = result.transcriptResult,
                    outputFiles = result.outputFiles,
                    audioFiles = audio,
                    videoFiles = video,
                    transcriptFiles = transcripts
                )
            }
            is ProcessResult.Failure -> {
                _uiState.value = _uiState.value.copy(
                    taskId = result.taskId,
                    status = ProcessTaskStatus.FAILED,
                    errorMessage = result.errorMessage
                )
            }
            is ProcessResult.Cancelled -> {
                _uiState.value = _uiState.value.copy(
                    taskId = result.taskId,
                    status = ProcessTaskStatus.CANCELLED
                )
            }
        }
    }
}
