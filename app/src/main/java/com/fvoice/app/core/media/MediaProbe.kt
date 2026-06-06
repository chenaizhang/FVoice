package com.fvoice.app.core.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.fvoice.app.core.model.AudioFormatInfo
import com.fvoice.app.core.model.MediaFileInfo
import com.fvoice.app.util.FVoiceLogger
import java.io.FileDescriptor

class MediaProbe(private val context: Context) {

    fun probe(uri: Uri, fileName: String): MediaFileInfo? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                probeFd(pfd.fileDescriptor, uri, fileName)
            }
        } catch (e: Exception) {
            FVoiceLogger.e("MediaProbe failed for $fileName", e)
            null
        }
    }

    private fun probeFd(fd: FileDescriptor, uri: Uri, fileName: String): MediaFileInfo {
        val extractor = MediaExtractor()
        extractor.setDataSource(fd)

        var hasAudio = false
        var durationMs = 0L
        var sampleRate = 16000
        var channels = 1
        var mime = "audio/mp4"

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val trackMime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (trackMime.startsWith("audio/")) {
                hasAudio = true
                durationMs = format.getLong(MediaFormat.KEY_DURATION, 0L) / 1000
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 16000)
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
                mime = trackMime
                break
            }
        }

        extractor.release()

        val isVideo = mime.startsWith("video/") || isVideoFile(fileName)

        return MediaFileInfo(
            uri = uri,
            fileName = fileName,
            mimeType = mime,
            durationMs = durationMs,
            isVideo = isVideo,
            hasAudioTrack = hasAudio,
            audioFormatInfo = AudioFormatInfo(
                sampleRate = sampleRate,
                channels = channels,
                durationMs = durationMs
            )
        )
    }

    private fun isVideoFile(fileName: String): Boolean {
        val videoExts = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv")
        return videoExts.contains(fileName.substringAfterLast(".", "").lowercase())
    }
}
