package com.fvoice.app.core.model

import android.net.Uri

sealed class ProcessResult {
    abstract val taskId: String

    data class Success(
        override val taskId: String,
        val outputFiles: List<OutputFileInfo>,
        val transcriptResult: TranscriptResult? = null,
        val processedDurationMs: Long = 0
    ) : ProcessResult()

    data class Failure(
        override val taskId: String,
        val errorCode: String,
        val errorMessage: String,
        val recoverable: Boolean = false
    ) : ProcessResult()

    data class Cancelled(
        override val taskId: String,
        val reason: String = "user_cancelled"
    ) : ProcessResult()
}

data class OutputFileInfo(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long = 0,
    val type: OutputFileType
)

enum class OutputFileType {
    AUDIO_DENOISED,
    AUDIO_EXTRACTED,
    VIDEO_DENOISED,
    TRANSCRIPT_TXT,
    TRANSCRIPT_SRT,
    TRANSCRIPT_VTT,
    TRANSCRIPT_JSON,
    LOG
}
