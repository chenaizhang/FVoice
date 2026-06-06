package com.fvoice.app.core.denoise

import com.fvoice.app.core.exception.FVoiceException
import com.fvoice.app.core.jni.RnNoiseJni
import com.fvoice.app.core.model.AudioFormatInfo
import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.task.ProcessCancellationToken
import com.fvoice.app.data.model.DenoiseStrength
import com.fvoice.app.util.FVoiceLogger
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
                FVoiceLogger.w("RNNoise init returned 0")
                return false
            }
            FVoiceLogger.i("RNNoise initialized: ctx=$ctx")
            true
        } catch (e: UnsatisfiedLinkError) {
            FVoiceLogger.e("RNNoise native library not loaded", e)
            false
        } catch (e: Exception) {
            FVoiceLogger.e("RNNoise init failed", e)
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
            throw FVoiceException.DenoiseFailed("RNNoise is not initialized")
        }

        try {
            val pcmIn = inputPcmFile.readBytes()
            cancellationToken?.throwIfCancelled()
            val pcmOut = jni!!.process(ctx, pcmIn, audioInfo.sampleRate)
            if (pcmOut.isEmpty()) {
                throw FVoiceException.DenoiseFailed("RNNoise returned empty audio")
            }
            outputPcmFile.writeBytes(pcmOut)
            FVoiceLogger.i("RNNoise done: ${outputPcmFile.name}")
        } catch (e: FVoiceException.CancelledByUser) {
            throw e
        } catch (e: Exception) {
            FVoiceLogger.e("RNNoise denoise failed", e)
            throw FVoiceException.DenoiseFailed(e.message ?: "Unknown", e)
        }
    }

    override fun release() {
        if (ctx != 0L && jni != null) {
            try {
                jni!!.free(ctx)
            } catch (e: Exception) {
                FVoiceLogger.e("RNNoise release failed", e)
            }
            ctx = 0
        }
    }
}
