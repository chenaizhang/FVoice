package com.clarivo.app.core.model

data class AudioFormatInfo(
    val sampleRate: Int = 16000,
    val channels: Int = 1,
    val bitDepth: Int = 16,
    val codec: String = "pcm_s16le",
    val durationMs: Long = 0,
    val bitrate: Long = 0
)
