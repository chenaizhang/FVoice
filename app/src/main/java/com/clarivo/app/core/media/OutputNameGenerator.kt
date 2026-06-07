package com.clarivo.app.core.media

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OutputNameGenerator {

    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generate(
        sourceFileName: String,
        processType: ProcessType,
        extension: String,
        conflictIndex: Int = 0
    ): String {
        val baseName = sourceFileName.substringBeforeLast(".", sourceFileName)
        val timestamp = timestampFormat.format(Date())
        val typeLabel = processType.suffix
        val indexSuffix = if (conflictIndex > 0) "_${String.format("%03d", conflictIndex)}" else ""
        return "${baseName}_${timestamp}_${typeLabel}${indexSuffix}.${extension}"
    }

    enum class ProcessType(val suffix: String) {
        DENOISED("denoised"),
        DENOISED_VIDEO("denoised_video"),
        EXTRACTED_AUDIO("extracted_audio"),
        TRANSCRIPT("transcript"),
        SUBTITLE("subtitle"),
        SEGMENTS("segments"),
        LOG("log")
    }
}
