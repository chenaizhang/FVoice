package com.fvoice.app.core.model

data class ProcessProgress(
    val percent: Int = 0,
    val currentStage: String = "",
    val currentStageResKey: String = "",
    val detailMessage: String = "",
    val bytesProcessed: Long = 0,
    val bytesTotal: Long = 0,
    val durationProcessedMs: Long = 0,
    val durationTotalMs: Long = 0,
    val isIndeterminate: Boolean = false
)
