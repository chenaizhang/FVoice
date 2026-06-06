package com.fvoice.app.core.vad

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.task.ProcessCancellationToken
import com.fvoice.app.util.FVoiceLogger
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SileroVadEngine : VadEngine {

    override val name: String = "SileroVAD"

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var modelPath: String = ""

    override fun isAvailable(): Boolean {
        return true
    }

    override suspend fun initialize(model: ModelInfo): Boolean {
        modelPath = model.path
        if (modelPath.isBlank() || !File(modelPath).exists()) {
            FVoiceLogger.w("SileroVAD model not found at $modelPath")
            return false
        }
        return try {
            env = OrtEnvironment.getEnvironment()
            session = env?.createSession(modelPath, OrtSession.SessionOptions())
            FVoiceLogger.i("SileroVAD ONNX session created: $modelPath")
            true
        } catch (e: Exception) {
            FVoiceLogger.e("SileroVAD init failed", e)
            false
        }
    }

    override suspend fun detectSegments(
        pcmFile: File,
        sampleRate: Int,
        config: VadEngine.VadConfig,
        cancellationToken: ProcessCancellationToken?
    ): List<VadSegmenter.VadSegment> {
        if (session == null || env == null) {
            FVoiceLogger.w("SileroVAD session is unavailable")
            return emptyList()
        }

        FVoiceLogger.i("SileroVAD detecting: ${pcmFile.name}")
        return try {
            detectWithOnnx(pcmFile, sampleRate, config, cancellationToken)
        } catch (e: com.fvoice.app.core.exception.FVoiceException.CancelledByUser) {
            throw e
        } catch (e: Exception) {
            FVoiceLogger.e("SileroVAD ONNX inference failed", e)
            emptyList()
        }
    }

    override fun release() {
        runCatching { session?.close() }
        runCatching { env?.close() }
        session = null
        env = null
    }

    private fun detectWithOnnx(
        pcmFile: File,
        sampleRate: Int,
        config: VadEngine.VadConfig,
        cancellationToken: ProcessCancellationToken?
    ): List<VadSegmenter.VadSegment> {
        val pcmBytes = pcmFile.readBytes()
        val samples = pcmBytesToFloats(pcmBytes)

        // Silero VAD expects 512 samples per frame at 16kHz (32ms)
        val frameSize = when (sampleRate) {
            8000 -> 256
            16000 -> 512
            else -> 512
        }
        val threshold = when (config.sensitivity) {
            VadEngine.Sensitivity.LOW -> 0.7f
            VadEngine.Sensitivity.MEDIUM -> 0.5f
            VadEngine.Sensitivity.HIGH -> 0.3f
        }

        val speechFrames = mutableListOf<Pair<Int, Float>>()
        var frameIndex = 0

        // ONNX state inputs (h, c) for LSTM - shape [2, 1, 64]
        var h = Array(2) { Array(1) { FloatArray(64) { 0f } } }
        var c = Array(2) { Array(1) { FloatArray(64) { 0f } } }

        while (frameIndex * frameSize + frameSize <= samples.size) {
            cancellationToken?.throwIfCancelled()

            val start = frameIndex * frameSize
            val frame = samples.copyOfRange(start, start + frameSize)

            val inputTensor = OnnxTensor.createTensor(env, arrayOf(frame))
            val hTensor = OnnxTensor.createTensor(env, h)
            val cTensor = OnnxTensor.createTensor(env, c)
            val srTensor = OnnxTensor.createTensor(env, longArrayOf(sampleRate.toLong()))

            val outputs = session?.run(
                mapOf("input" to inputTensor, "h" to hTensor, "c" to cTensor, "sr" to srTensor)
            )

            val prob = (outputs?.get(0)?.value as? Array<*>)?.let {
                @Suppress("UNCHECKED_CAST")
                (it[0] as? Array<FloatArray>)?.get(0)?.get(0) ?: 0f
            } ?: 0f

            // Update states
            @Suppress("UNCHECKED_CAST")
            h = outputs?.get(1)?.value as? Array<Array<FloatArray>> ?: h
            @Suppress("UNCHECKED_CAST")
            c = outputs?.get(2)?.value as? Array<Array<FloatArray>> ?: c

            if (prob >= threshold) {
                speechFrames.add(frameIndex to prob)
            }

            inputTensor.close()
            hTensor.close()
            cTensor.close()
            srTensor.close()
            outputs?.close()

            frameIndex++
        }

        return mergeFramesToSegments(
            speechFrames,
            frameSize,
            sampleRate,
            config,
            samples.size
        )
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

    private fun mergeFramesToSegments(
        speechFrames: List<Pair<Int, Float>>,
        frameSize: Int,
        sampleRate: Int,
        config: VadEngine.VadConfig,
        totalSamples: Int
    ): List<VadSegmenter.VadSegment> {
        if (speechFrames.isEmpty()) return emptyList()

        val segments = mutableListOf<VadSegmenter.VadSegment>()
        var segStart = speechFrames.first().first
        var prevFrame = speechFrames.first().first

        fun addSegment(start: Int, end: Int) {
            val startMs = (start * frameSize * 1000L / sampleRate) - config.prePaddingMs
            val endMs = (end * frameSize * 1000L / sampleRate) + config.postPaddingMs
            val clampedStart = startMs.coerceAtLeast(0)
            val clampedEnd = endMs.coerceAtMost(totalSamples * 1000L / sampleRate)
            if (clampedEnd - clampedStart >= config.minSpeechMs) {
                segments.add(VadSegmenter.VadSegment(clampedStart, clampedEnd))
            }
        }

        for (i in 1 until speechFrames.size) {
            val currentFrame = speechFrames[i].first
            val gapMs = (currentFrame - prevFrame) * frameSize * 1000L / sampleRate
            if (gapMs > config.minSilenceMs) {
                addSegment(segStart, prevFrame)
                segStart = currentFrame
            }
            prevFrame = currentFrame
        }
        addSegment(segStart, prevFrame)

        // Split long segments
        val maxSegmentMs = config.maxSegmentMs
        if (maxSegmentMs <= 0) return segments

        val result = mutableListOf<VadSegmenter.VadSegment>()
        segments.forEach { seg ->
            var start = seg.startMs
            while (start < seg.endMs) {
                val end = (start + maxSegmentMs).coerceAtMost(seg.endMs)
                result.add(VadSegmenter.VadSegment(start, end))
                start = end
            }
        }
        return result
    }
}
