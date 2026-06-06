package com.fvoice.app.core.denoise

import com.fvoice.app.core.model.AudioFormatInfo
import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.task.ProcessCancellationToken
import com.fvoice.app.data.model.DenoiseStrength
import com.fvoice.app.util.FVoiceLogger
import java.io.File

class DenoiseProcessor(private val engine: DenoiseEngine) {

    suspend fun process(
        inputFile: File,
        outputFile: File,
        audioInfo: AudioFormatInfo,
        model: ModelInfo? = null,
        strength: DenoiseStrength = DenoiseStrength.STANDARD,
        cancellationToken: ProcessCancellationToken? = null
    ) {
        if (!engine.isAvailable()) {
            throw com.fvoice.app.core.exception.FVoiceException.ModelNotAvailable("Denoise engine not available")
        }

        val selectedModel = model
            ?: throw com.fvoice.app.core.exception.FVoiceException.ModelNotAvailable("Denoise model not available")
        val ok = engine.initialize(selectedModel, strength)
        if (!ok) throw com.fvoice.app.core.exception.FVoiceException.DenoiseFailed("Engine init failed")

        try {
            engine.denoise(inputFile, outputFile, audioInfo, cancellationToken)
        } catch (e: com.fvoice.app.core.exception.FVoiceException.CancelledByUser) {
            throw e
        } catch (e: Exception) {
            FVoiceLogger.e("Denoise failed", e)
            throw com.fvoice.app.core.exception.FVoiceException.DenoiseFailed(e.message ?: "Unknown", e)
        } finally {
            engine.release()
        }
    }
}
