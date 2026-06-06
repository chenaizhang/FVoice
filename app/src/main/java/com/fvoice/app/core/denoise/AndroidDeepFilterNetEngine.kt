package com.fvoice.app.core.denoise

import android.content.Context
import com.fvoice.app.core.exception.FVoiceException
import com.fvoice.app.core.jni.DeepFilterNetJni
import com.fvoice.app.core.model.AudioFormatInfo
import com.fvoice.app.core.model.ModelInfo
import com.fvoice.app.core.task.ProcessCancellationToken
import com.fvoice.app.data.model.DenoiseStrength
import com.fvoice.app.util.FVoiceLogger
import java.io.File

class AndroidDeepFilterNetEngine(
    @Suppress("unused") private val context: Context
) : DenoiseEngine {

    override val name: String = "DeepFilterNet"

    private var jni: DeepFilterNetJni? = null
    private var ctx: Long = 0L

    override fun isAvailable(): Boolean {
        return try {
            DeepFilterNetJni().isNativeEngineAvailable()
        } catch (e: UnsatisfiedLinkError) {
            FVoiceLogger.e("DeepFilterNet native library not loaded", e)
            false
        } catch (e: Exception) {
            FVoiceLogger.e("DeepFilterNet availability check failed", e)
            false
        }
    }

    override suspend fun initialize(model: ModelInfo, strength: DenoiseStrength): Boolean {
        val file = File(model.path)
        if (!file.isFile || file.length() <= 0L) {
            FVoiceLogger.w("DeepFilterNet model file not found: ${model.path}")
            return false
        }

        return try {
            val instance = DeepFilterNetJni()
            val nativeCtx = instance.init(file.absolutePath)
            if (nativeCtx == 0L) {
                FVoiceLogger.w("DeepFilterNet init returned 0: ${model.name}")
                return false
            }
            jni = instance
            ctx = nativeCtx
            FVoiceLogger.i("DeepFilterNet initialized with official libDF: ${model.name}, ctx=$ctx")
            true
        } catch (e: UnsatisfiedLinkError) {
            FVoiceLogger.e("DeepFilterNet native library not loaded", e)
            false
        } catch (e: Exception) {
            FVoiceLogger.e("DeepFilterNet initialize failed: ${model.name}", e)
            false
        }
    }

    override suspend fun denoise(
        inputPcmFile: File,
        outputPcmFile: File,
        audioInfo: AudioFormatInfo,
        cancellationToken: ProcessCancellationToken?
    ) {
        val instance = jni ?: throw FVoiceException.DenoiseFailed("DeepFilterNet is not initialized")
        if (ctx == 0L) {
            throw FVoiceException.DenoiseFailed("DeepFilterNet is not initialized")
        }
        if (audioInfo.sampleRate != REQUIRED_SAMPLE_RATE || audioInfo.channels != 1 || audioInfo.bitDepth != 16) {
            throw FVoiceException.DenoiseFailed(
                "DeepFilterNet requires 48 kHz mono 16-bit WAV, got ${audioInfo.sampleRate} Hz, ${audioInfo.channels} ch, ${audioInfo.bitDepth}-bit"
            )
        }

        try {
            val pcmIn = inputPcmFile.readBytes()
            cancellationToken?.throwIfCancelled()
            val pcmOut = instance.process(ctx, pcmIn, audioInfo.sampleRate)
            if (pcmOut.isEmpty()) {
                throw FVoiceException.DenoiseFailed("DeepFilterNet returned empty audio")
            }
            outputPcmFile.writeBytes(pcmOut)
            FVoiceLogger.i("DeepFilterNet done with official libDF: ${outputPcmFile.name}")
        } catch (e: FVoiceException.CancelledByUser) {
            throw e
        } catch (e: Exception) {
            FVoiceLogger.e("DeepFilterNet denoise failed", e)
            throw FVoiceException.DenoiseFailed(e.message ?: "Unknown", e)
        }
    }

    override fun release() {
        if (ctx != 0L && jni != null) {
            try {
                jni!!.free(ctx)
            } catch (e: Exception) {
                FVoiceLogger.e("DeepFilterNet release failed", e)
            }
            ctx = 0L
        }
        jni = null
    }

    private companion object {
        const val REQUIRED_SAMPLE_RATE = 48_000
    }
}
