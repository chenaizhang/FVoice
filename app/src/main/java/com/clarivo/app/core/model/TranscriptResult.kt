package com.clarivo.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranscriptResult(
    val sourceFileName: String = "",
    val sourceUri: String = "",
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val language: String = "auto",
    @SerialName("duration_ms")
    val durationMs: Long = 0,
    val modelName: String = "",
    val modelType: String = "",
    val segments: List<TranscriptSegment> = emptyList()
)
