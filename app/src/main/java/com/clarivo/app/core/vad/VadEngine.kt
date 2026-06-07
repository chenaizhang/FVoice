package com.clarivo.app.core.vad

import com.clarivo.app.core.model.ModelInfo
import com.clarivo.app.core.task.ProcessCancellationToken
import java.io.File

interface VadEngine {

    val name: String

    fun isAvailable(): Boolean

    suspend fun initialize(model: ModelInfo): Boolean

    suspend fun detectSegments(
        pcmFile: File,
        sampleRate: Int,
        config: VadConfig = VadConfig(),
        cancellationToken: ProcessCancellationToken? = null
    ): List<VadSegmenter.VadSegment>

    fun release()

    data class VadConfig(
        val sensitivity: Sensitivity = Sensitivity.MEDIUM,
        val prePaddingMs: Long = 200,
        val postPaddingMs: Long = 400,
        val minSpeechMs: Long = 300,
        val minSilenceMs: Long = 500,
        val maxSegmentMs: Long = 30000
    )

    enum class Sensitivity { LOW, MEDIUM, HIGH }
}
