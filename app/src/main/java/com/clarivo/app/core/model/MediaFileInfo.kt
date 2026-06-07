package com.clarivo.app.core.model

import android.net.Uri

data class MediaFileInfo(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long = 0,
    val durationMs: Long = 0,
    val isVideo: Boolean = false,
    val hasAudioTrack: Boolean = false,
    val audioFormatInfo: AudioFormatInfo? = null
)
