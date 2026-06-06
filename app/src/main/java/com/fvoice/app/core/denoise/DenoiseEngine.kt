package com.fvoice.app.core.denoise

import com.fvoice.app.core.model.AudioFormatInfo
import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.task.ProcessCancellationToken
import com.fvoice.app.data.model.DenoiseStrength
import java.io.File

interface DenoiseEngine {

    val name: String

    fun isAvailable(): Boolean

    suspend fun initialize(model: ModelInfo, strength: DenoiseStrength = DenoiseStrength.STANDARD): Boolean

    suspend fun denoise(
        inputPcmFile: File,
        outputPcmFile: File,
        audioInfo: AudioFormatInfo,
        cancellationToken: ProcessCancellationToken? = null
    )

    fun release()
}
