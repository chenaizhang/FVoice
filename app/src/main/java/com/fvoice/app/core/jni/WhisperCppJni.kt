package com.fvoice.app.core.jni

class WhisperCppJni {

    companion object {
        init {
            System.loadLibrary("fvoice-whisper")
        }
    }

    external fun init(modelPath: String): Long
    external fun free(ctx: Long)
    external fun transcribe(ctx: Long, samples: FloatArray, nSamples: Int, language: String): String
}
