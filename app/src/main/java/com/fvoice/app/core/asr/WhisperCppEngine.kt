package com.fvoice.app.core.asr

import com.fvoice.app.core.jni.WhisperCppJni
import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.model.TranscriptSegment
import com.fvoice.app.core.task.ProcessCancellationToken
import com.fvoice.app.util.FVoiceLogger
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WhisperCppEngine : AsrEngine {

    override val name: String = "WhisperCpp"

    private var jni: WhisperCppJni? = null
    private var ctx: Long = 0
    private var modelPath: String = ""

    override fun isAvailable(): Boolean {
        return true
    }

    override suspend fun initialize(model: ModelInfo): Boolean {
        return try {
            modelPath = model.path
            if (modelPath.isBlank()) {
                FVoiceLogger.w("WhisperCpp model path is blank; transcription will return no segments")
                return false
            }
            jni = WhisperCppJni()
            ctx = jni!!.init(modelPath)
            if (ctx == 0L) {
                FVoiceLogger.w("WhisperCpp init returned 0; transcription will return no segments")
                return false
            }
            FVoiceLogger.i("WhisperCpp initialized: ctx=$ctx")
            true
        } catch (e: UnsatisfiedLinkError) {
            FVoiceLogger.e("Native library not loaded; transcription will return no segments", e)
            false
        } catch (e: Exception) {
            FVoiceLogger.e("WhisperCpp init failed; transcription will return no segments", e)
            false
        }
    }

    override suspend fun transcribe(
        pcmFile: File,
        sampleRate: Int,
        language: String,
        cancellationToken: ProcessCancellationToken?
    ): List<TranscriptSegment> {
        if (ctx == 0L || jni == null) {
            throw com.fvoice.app.core.exception.FVoiceException.AsrFailed("WhisperCpp is not initialized")
        }

        FVoiceLogger.i("WhisperCpp transcribing: ${pcmFile.name}")
        return try {
            val samples = audioFileToFloats(pcmFile)
            if (samples.isEmpty()) {
                throw com.fvoice.app.core.exception.FVoiceException.AsrFailed("Input audio has no readable PCM samples")
            }

            cancellationToken?.throwIfCancelled()

            val json = jni!!.transcribe(ctx, samples, samples.size, normalizeLanguage(language))
            FVoiceLogger.i("WhisperCpp raw result: $json")
            if (json.contains("\"error\"")) {
                throw com.fvoice.app.core.exception.FVoiceException.AsrFailed(json)
            }

            parseResult(json, pcmFile)
        } catch (e: com.fvoice.app.core.exception.FVoiceException.CancelledByUser) {
            throw e
        } catch (e: com.fvoice.app.core.exception.FVoiceException.AsrFailed) {
            FVoiceLogger.e("WhisperCpp transcribe failed", e)
            throw e
        } catch (e: Exception) {
            FVoiceLogger.e("WhisperCpp transcribe failed", e)
            throw com.fvoice.app.core.exception.FVoiceException.AsrFailed(e.message ?: "Unknown", e)
        }
    }

    override fun release() {
        if (ctx != 0L && jni != null) {
            try {
                jni!!.free(ctx)
            } catch (e: Exception) {
                FVoiceLogger.e("WhisperCpp release failed", e)
            }
            ctx = 0
        }
    }

    private fun audioFileToFloats(file: File): FloatArray {
        val bytes = file.readBytes()
        return if (bytes.size > 44
            && bytes[0] == 'R'.code.toByte()
            && bytes[1] == 'I'.code.toByte()
            && bytes[2] == 'F'.code.toByte()
            && bytes[3] == 'F'.code.toByte()
        ) {
            wavBytesToFloats(bytes)
        } else {
            pcmBytesToFloats(bytes)
        }
    }

    private fun wavBytesToFloats(wav: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 12
        var channels = 1
        var sampleRate = 16000
        var bitsPerSample = 16
        var dataOffset = -1
        var dataSize = 0

        while (offset + 8 <= wav.size) {
            val id = String(wav, offset, 4)
            val size = buffer.getInt(offset + 4)
            val payloadOffset = offset + 8
            if (payloadOffset + size > wav.size) break
            when (id) {
                "fmt " -> {
                    channels = buffer.getShort(payloadOffset + 2).toInt().coerceAtLeast(1)
                    sampleRate = buffer.getInt(payloadOffset + 4).coerceAtLeast(1)
                    bitsPerSample = buffer.getShort(payloadOffset + 14).toInt()
                }
                "data" -> {
                    dataOffset = payloadOffset
                    dataSize = size
                    break
                }
            }
            offset = payloadOffset + size + (size % 2)
        }

        if (dataOffset < 0 || dataSize <= 0 || bitsPerSample != 16) {
            return FloatArray(0)
        }

        val frameCount = dataSize / 2 / channels
        val mono = FloatArray(frameCount)
        var index = dataOffset
        for (frame in 0 until frameCount) {
            var sum = 0f
            repeat(channels) {
                val lo = wav[index].toInt() and 0xFF
                val hi = wav[index + 1].toInt()
                val sample = ((hi shl 8) or lo).toShort().toInt()
                sum += sample / 32768.0f
                index += 2
            }
            mono[frame] = sum / channels
        }
        return resampleLinear(mono, sampleRate, 16000)
    }

    private fun pcmBytesToFloats(pcm: ByteArray): FloatArray {
        val samples = FloatArray(pcm.size / 2)
        for (i in samples.indices) {
            val lo = pcm[i * 2].toInt() and 0xFF
            val hi = pcm[i * 2 + 1].toInt()
            val sample = (hi shl 8) or lo
            samples[i] = sample / 32768.0f
        }
        return samples
    }

    private fun resampleLinear(samples: FloatArray, sourceRate: Int, targetRate: Int): FloatArray {
        if (samples.isEmpty() || sourceRate <= 0 || targetRate <= 0 || sourceRate == targetRate) {
            return samples
        }
        val outputSize = ((samples.size.toLong() * targetRate) / sourceRate).toInt().coerceAtLeast(1)
        val output = FloatArray(outputSize)
        val ratio = sourceRate.toDouble() / targetRate.toDouble()
        for (i in output.indices) {
            val sourcePosition = i * ratio
            val left = sourcePosition.toInt().coerceIn(0, samples.lastIndex)
            val right = (left + 1).coerceAtMost(samples.lastIndex)
            val fraction = (sourcePosition - left).toFloat()
            output[i] = samples[left] + (samples[right] - samples[left]) * fraction
        }
        return output
    }

    private fun normalizeLanguage(language: String): String {
        val normalized = language.trim().lowercase()
        return when {
            normalized.isBlank() || normalized == "system" || normalized == "auto" -> "auto"
            normalized == "zh-cn" || normalized == "zh_hans" || normalized == "chinese" -> "zh"
            normalized == "en-us" || normalized == "english" -> "en"
            else -> normalized.substringBefore('-').substringBefore('_')
        }
    }

    @Serializable
    private data class WhisperSegment(
        val start: Float = 0f,
        val end: Float = 0f,
        val text: String = ""
    )

    @Serializable
    private data class WhisperResult(
        val segments: List<WhisperSegment> = emptyList()
    )

    private val whisperJson = Json { ignoreUnknownKeys = true }

    private suspend fun parseResult(json: String, pcmFile: File): List<TranscriptSegment> {
        return try {
            val result = whisperJson.decodeFromString(WhisperResult.serializer(), json)
            if (result.segments.isEmpty()) {
                throw com.fvoice.app.core.exception.FVoiceException.AsrFailed("Whisper returned empty segments")
            }
            result.segments.mapIndexed { index, seg ->
                TranscriptSegment(
                    index = index,
                    startMs = (seg.start * 1000).toLong(),
                    endMs = (seg.end * 1000).toLong(),
                    text = com.github.houbb.opencc4j.util.ZhConverterUtil.toSimple(seg.text.trim())
                )
            }
        } catch (e: Exception) {
            FVoiceLogger.e("Failed to parse whisper result; returning empty transcript", e)
            emptyList()
        }
    }
}
