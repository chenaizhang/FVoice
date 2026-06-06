package com.fvoice.app.core.vad

import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.task.ProcessCancellationToken
import com.fvoice.app.util.FVoiceLogger
import java.io.File

class VadSegmenter(private val engine: VadEngine) {

    suspend fun segment(
        pcmFile: File,
        sampleRate: Int,
        config: VadEngine.VadConfig = VadEngine.VadConfig(),
        model: ModelInfo? = null,
        cancellationToken: ProcessCancellationToken? = null
    ): List<VadSegment> {
        if (!engine.isAvailable()) {
            FVoiceLogger.w("VAD engine not available; continuing without VAD segmentation")
            return emptyList()
        }

        val selectedModel = model
        if (selectedModel == null) {
            FVoiceLogger.w("VAD model not available; continuing without VAD segmentation")
            return emptyList()
        }
        val ok = engine.initialize(selectedModel)
        if (!ok) {
            FVoiceLogger.w("VAD engine init failed; continuing without VAD segmentation")
            return emptyList()
        }

        return try {
            engine.detectSegments(pcmFile, sampleRate, config, cancellationToken)
        } catch (e: com.fvoice.app.core.exception.FVoiceException.CancelledByUser) {
            throw e
        } catch (e: Exception) {
            FVoiceLogger.e("VAD failed", e)
            throw com.fvoice.app.core.exception.FVoiceException.AsrFailed(e.message ?: "Unknown", e)
        } finally {
            engine.release()
        }
    }

    data class VadSegment(
        val startMs: Long,
        val endMs: Long
    ) {
        val durationMs: Long get() = endMs - startMs
    }
}
