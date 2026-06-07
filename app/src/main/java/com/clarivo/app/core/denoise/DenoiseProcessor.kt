package com.clarivo.app.core.denoise

import com.clarivo.app.core.model.AudioFormatInfo
import com.clarivo.app.core.model.ModelInfo
import com.clarivo.app.core.task.ProcessCancellationToken
import com.clarivo.app.data.model.DenoiseStrength
import com.clarivo.app.util.ClarivoLogger
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
            throw com.clarivo.app.core.exception.ClarivoException.ModelNotAvailable("Denoise engine not available")
        }

        val selectedModel = model
            ?: throw com.clarivo.app.core.exception.ClarivoException.ModelNotAvailable("Denoise model not available")
        val ok = engine.initialize(selectedModel, strength)
        if (!ok) throw com.clarivo.app.core.exception.ClarivoException.DenoiseFailed("Engine init failed")

        try {
            engine.denoise(inputFile, outputFile, audioInfo, cancellationToken)
        } catch (e: com.clarivo.app.core.exception.ClarivoException.CancelledByUser) {
            throw e
        } catch (e: Exception) {
            ClarivoLogger.e("Denoise failed", e)
            throw com.clarivo.app.core.exception.ClarivoException.DenoiseFailed(e.message ?: "Unknown", e)
        } finally {
            engine.release()
        }
    }
}
