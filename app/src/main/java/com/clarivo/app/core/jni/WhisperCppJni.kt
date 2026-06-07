package com.clarivo.app.core.jni

class WhisperCppJni {

    companion object {
        init {
            System.loadLibrary("clarivo-whisper")
        }
    }

    external fun init(modelPath: String): Long
    external fun free(ctx: Long)
    external fun transcribe(ctx: Long, samples: FloatArray, nSamples: Int, language: String): String
}
