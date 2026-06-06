package com.fvoice.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranscriptSegment(
    val index: Int,
    @SerialName("start_ms")
    val startMs: Long,
    @SerialName("end_ms")
    val endMs: Long,
    val text: String,
    val confidence: Float? = null,
    val speaker: String? = null
)
