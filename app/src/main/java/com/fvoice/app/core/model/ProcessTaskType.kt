package com.fvoice.app.core.model

enum class ProcessTaskType(val labelKey: String) {
    DENOISE("task_type_denoise"),
    TRANSCRIBE("task_type_transcribe"),
    DENOISE_AND_TRANSCRIBE("task_type_denoise_and_transcribe"),
    EXTRACT_AUDIO("task_type_extract_audio"),
    REALTIME_RECORD("task_type_realtime_record"),
    REALTIME_TRANSCRIBE("task_type_realtime_transcribe")
}
