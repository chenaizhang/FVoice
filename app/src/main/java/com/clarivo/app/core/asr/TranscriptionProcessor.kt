package com.clarivo.app.core.asr

import com.clarivo.app.core.model.ModelInfo
import com.clarivo.app.core.model.TranscriptResult
import com.clarivo.app.core.model.TranscriptSegment
import com.clarivo.app.core.task.ProcessCancellationToken
import com.clarivo.app.util.ClarivoLogger
import java.io.File

class TranscriptionProcessor(private val engine: AsrEngine) {

    suspend fun process(
        pcmFile: File,
        sampleRate: Int,
        language: String = "auto",
        sourceFileName: String = "",
        sourceUri: String = "",
        model: ModelInfo? = null,
        segments: List<com.clarivo.app.core.vad.VadSegmenter.VadSegment>? = null,
        cancellationToken: ProcessCancellationToken? = null
    ): TranscriptResult {
        if (!engine.isAvailable()) {
            throw com.clarivo.app.core.exception.ClarivoException.ModelNotAvailable("ASR engine not available")
        }

        val selectedModel = model
            ?: throw com.clarivo.app.core.exception.ClarivoException.ModelNotAvailable("ASR model not available")
        val ok = engine.initialize(selectedModel)
        if (!ok) throw com.clarivo.app.core.exception.ClarivoException.AsrFailed("Engine init failed")

        return try {
            val transcriptSegments = if (segments.isNullOrEmpty()) {
                engine.transcribe(pcmFile, sampleRate, language, cancellationToken)
            } else {
                val all = mutableListOf<TranscriptSegment>()
                segments.forEachIndexed { index, segment ->
                    cancellationToken?.throwIfCancelled()
                    // TODO: Slice PCM by VAD segment before passing audio to the ASR engine.
                    val segs = engine.transcribe(pcmFile, sampleRate, language, cancellationToken)
                    segs.forEach { s ->
                        all.add(
                            s.copy(
                                index = all.size,
                                startMs = s.startMs + segment.startMs,
                                endMs = (s.endMs + segment.startMs).coerceAtMost(segment.endMs)
                            )
                        )
                    }
                }
                all
            }

            if (transcriptSegments.isEmpty()) {
                throw com.clarivo.app.core.exception.ClarivoException.AsrFailed("No transcription was produced")
            }

            val durationMs = transcriptSegments.lastOrNull()?.endMs ?: 0L

            TranscriptResult(
                sourceFileName = sourceFileName,
                sourceUri = sourceUri,
                language = language,
                durationMs = durationMs,
                modelName = selectedModel.name,
                modelType = selectedModel.type.name,
                segments = transcriptSegments
            )
        } catch (e: com.clarivo.app.core.exception.ClarivoException.CancelledByUser) {
            throw e
        } catch (e: Exception) {
            ClarivoLogger.e("Transcription failed", e)
            throw com.clarivo.app.core.exception.ClarivoException.AsrFailed(e.message ?: "Unknown", e)
        } finally {
            engine.release()
        }
    }
}
