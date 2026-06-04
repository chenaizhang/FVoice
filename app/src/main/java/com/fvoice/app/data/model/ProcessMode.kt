package com.fvoice.app.data.model

enum class ProcessMode(val label: String) {
    DENOISE_AND_TRANSCRIBE("降噪并转写"),
    DENOISE_ONLY("仅降噪"),
    TRANSCRIBE_ONLY("仅转写")
}
