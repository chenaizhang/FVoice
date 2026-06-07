package com.clarivo.app.core.recording

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.clarivo.app.core.task.ProcessCancellationToken
import com.clarivo.app.util.ClarivoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        val BUFFER_SIZE: Int
            get() = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT).coerceAtLeast(4096)
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile
    private var isRecording = false

    fun prepare(): Boolean {
        if (audioRecord != null) return true
        return try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )
            audioRecord?.state == AudioRecord.STATE_INITIALIZED
        } catch (e: Exception) {
            ClarivoLogger.e("AudioRecord prepare failed", e)
            false
        }
    }

    suspend fun startRecording(
        outputFile: File,
        cancellationToken: ProcessCancellationToken? = null,
        onAmplitude: (Float) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        if (isRecording) {
            ClarivoLogger.w("Already recording")
            return@withContext
        }
        if (audioRecord == null && !prepare()) {
            throw com.clarivo.app.core.exception.ClarivoException.ServiceBindFailed("AudioRecord not initialized")
        }

        val record = audioRecord ?: return@withContext
        isRecording = true
        record.startRecording()
        ClarivoLogger.i("Recording started: ${outputFile.absolutePath}")

        FileOutputStream(outputFile).use { fos ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (isActive && isRecording) {
                cancellationToken?.throwIfCancelled()
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    fos.write(buffer, 0, read)
                    val amplitude = calculateAmplitude(buffer, read)
                    onAmplitude(amplitude)
                }
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            ClarivoLogger.i("Recording stopped")
        } catch (e: Exception) {
            ClarivoLogger.e("Error stopping recording", e)
        }
    }

    fun release() {
        isRecording = false
        audioRecord?.release()
        audioRecord = null
        ClarivoLogger.i("AudioRecorder released")
    }

    fun isRecording(): Boolean = isRecording

    private fun calculateAmplitude(buffer: ByteArray, readSize: Int): Float {
        var sum = 0.0
        var i = 0
        while (i < readSize - 1) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            sum += kotlin.math.abs(sample)
            i += 2
        }
        val avg = if (readSize > 0) sum / (readSize / 2) else 0.0
        return (avg / 32768.0).toFloat().coerceIn(0f, 1f)
    }

    fun writeWavHeader(pcmFile: File, wavFile: File) {
        val pcmData = pcmFile.readBytes()
        val byteRate = SAMPLE_RATE * 1 * 16 / 8
        val totalDataLen = pcmData.size + 36
        val blockAlign = 1 * 16 / 8

        FileOutputStream(wavFile).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1)
            header.putShort(1)
            header.putInt(SAMPLE_RATE)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(16)
            header.put("data".toByteArray())
            header.putInt(pcmData.size)
            fos.write(header.array())
            fos.write(pcmData)
        }
    }
}
