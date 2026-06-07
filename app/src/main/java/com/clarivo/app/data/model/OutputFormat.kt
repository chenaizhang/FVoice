package com.clarivo.app.data.model

enum class OutputFormat(val extension: String, val label: String) {
    TXT("txt", "TXT"),
    MARKDOWN("md", "Markdown"),
    SRT("srt", "SRT"),
    VTT("vtt", "VTT")
}
