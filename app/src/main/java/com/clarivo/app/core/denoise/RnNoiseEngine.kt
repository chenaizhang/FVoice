package com.clarivo.app.core.denoise

import com.clarivo.app.core.exception.ClarivoException
import com.clarivo.app.core.jni.RnNoiseJni
import com.clarivo.app.core.model.AudioFormatInfo
import com.clarivo.app.core.model.ModelInfo
import com.clarivo.app.core.task.ProcessCancellationToken
import com.clarivo.app.data.model.DenoiseStrength
import com.clarivo.app.util.ClarivoLogger
import java.io.File

class RnNoiseEngine : DenoiseEngine {

    override val name: String = "RNNoise"

    private var jni: RnNoiseJni? = null
    private var ctx: Long = 0

    override fun isAvailable(): Boolean = true

    override suspend fun initialize(model: ModelInfo, strength: DenoiseStrength): Boolean {
        return try {
            jni = RnNoiseJni()
            ctx = jni!!.init()
            if (ctx == 0L) {
                ClarivoLogger.w("RNNoise init returned 0")
                return false
            }
            ClarivoLogger.i("RNNoise initialized: ctx=$ctx")
            true
        } catch (e: UnsatisfiedLinkError) {
            ClarivoLogger.e("RNNoise native library not loaded", e)
            false
        } catch (e: Exception) {
            ClarivoLogger.e("RNNoise init failed", e)
            false
        }
    }

    override suspend fun denoise(
        inputPcmFile: File,
        outputPcmFile: File,
        audioInfo: AudioFormatInfo,
        cancellationToken: ProcessCancellationToken?
    ) {
        if (ctx == 0L || jni == null) {
            throw ClarivoException.DenoiseFailed("RNNoise is not initialized")
        }

        try {
            val pcmIn = inputPcmFile.readBytes()
            cancellationToken?.throwIfCancelled()
            val pcmOut = jni!!.process(ctx, pcmIn, audioInfo.sampleRate)
            if (pcmOut.isEmpty()) {
                throw ClarivoException.DenoiseFailed("RNNoise returned empty audio")
            }
            outputPcmFile.writeBytes(pcmOut)
            ClarivoLogger.i("RNNoise done: ${outputPcmFile.name}")
        } catch (e: ClarivoException.CancelledByUser) {
            throw e
        } catch (e: Exception) {
            ClarivoLogger.e("RNNoise denoise failed", e)
            throw ClarivoException.DenoiseFailed(e.message ?: "Unknown", e)
        }
    }

    override fun release() {
        if (ctx != 0L && jni != null) {
            try {
                jni!!.free(ctx)
            } catch (e: Exception) {
                ClarivoLogger.e("RNNoise release failed", e)
            }
            ctx = 0
        }
    }
}
