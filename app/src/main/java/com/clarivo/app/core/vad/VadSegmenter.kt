package com.clarivo.app.core.vad

import com.clarivo.app.core.model.ModelInfo
import com.clarivo.app.core.task.ProcessCancellationToken
import com.clarivo.app.util.ClarivoLogger
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
            ClarivoLogger.w("VAD engine not available; continuing without VAD segmentation")
            return emptyList()
        }

        val selectedModel = model
        if (selectedModel == null) {
            ClarivoLogger.w("VAD model not available; continuing without VAD segmentation")
            return emptyList()
        }
        val ok = engine.initialize(selectedModel)
        if (!ok) {
            ClarivoLogger.w("VAD engine init failed; continuing without VAD segmentation")
            return emptyList()
        }

        return try {
            engine.detectSegments(pcmFile, sampleRate, config, cancellationToken)
        } catch (e: com.clarivo.app.core.exception.ClarivoException.CancelledByUser) {
            throw e
        } catch (e: Exception) {
            ClarivoLogger.e("VAD failed", e)
            throw com.clarivo.app.core.exception.ClarivoException.AsrFailed(e.message ?: "Unknown", e)
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
