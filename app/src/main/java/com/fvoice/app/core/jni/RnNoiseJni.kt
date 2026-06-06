package com.fvoice.app.core.jni

class RnNoiseJni {

    companion object {
        init {
            System.loadLibrary("fvoice-rnnoise")
        }
    }

    external fun init(): Long
    external fun free(ctx: Long)
    external fun process(ctx: Long, pcmIn: ByteArray, sampleRate: Int): ByteArray
}
