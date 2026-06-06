package com.fvoice.app.core.jni

class DeepFilterNetJni {

    companion object {
        init {
            System.loadLibrary("fvoice-df")
        }
    }

    external fun isNativeEngineAvailable(): Boolean
    external fun init(modelPath: String): Long
    external fun free(ctx: Long)
    external fun process(ctx: Long, pcmIn: ByteArray, sampleRate: Int): ByteArray
}
