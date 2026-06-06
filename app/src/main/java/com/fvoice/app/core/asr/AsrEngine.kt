package com.fvoice.app.core.asr

import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.model.TranscriptSegment
import com.fvoice.app.core.task.ProcessCancellationToken
import java.io.File

interface AsrEngine {

    val name: String

    fun isAvailable(): Boolean

    suspend fun initialize(model: ModelInfo): Boolean

    suspend fun transcribe(
        pcmFile: File,
        sampleRate: Int,
        language: String,
        cancellationToken: ProcessCancellationToken? = null
    ): List<TranscriptSegment>

    fun release()
}
