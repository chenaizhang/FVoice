package com.fvoice.app.core.model

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessTask(
    val id: String,
    val type: ProcessTaskType = ProcessTaskType.DENOISE_AND_TRANSCRIBE,
    val status: ProcessTaskStatus = ProcessTaskStatus.PENDING,
    val sourceUri: String = "",
    val sourceFileName: String = "",
    val outputDirUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long = 0,
    val completedAt: Long = 0,
    val progress: Int = 0,
    val currentStage: String = "",
    val errorCode: String = "",
    val errorMessage: String = "",
    val isRealtime: Boolean = false,
    val settings: TaskSettings = TaskSettings(),
    // TODO: Implement fine-grained resume by tracking completed stages
    val completedStages: List<String> = emptyList()
) {
    fun toPendingCopy(): ProcessTask = copy(
        status = ProcessTaskStatus.PENDING,
        progress = 0,
        currentStage = "",
        errorCode = "",
        errorMessage = "",
        startedAt = 0,
        completedAt = 0
    )
}

@Serializable
data class TaskSettings(
    val denoiseStrength: String = "STANDARD",
    val outputFormats: List<String> = listOf("TXT", "SRT", "JSON"),
    val useVad: Boolean = true,
    val language: String = "auto",
    val modelId: String = "",
    val denoiseModelId: String = "",
    val asrModelId: String = ""
)
