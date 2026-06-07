package com.clarivo.app.core.media

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.clarivo.app.core.exception.ClarivoException
import com.clarivo.app.core.task.ProcessCancellationToken
import com.clarivo.app.util.ClarivoLogger
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VideoAudioExtractor(private val context: Context) {

    fun extractToPcm(
        uri: Uri,
        outputFile: File,
        targetSampleRate: Int = 16000,
        cancellationToken: ProcessCancellationToken? = null
    ) {
        val extractor = MediaExtractor()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            extractor.setDataSource(pfd.fileDescriptor)
        } ?: throw ClarivoException.InvalidInputFile("Cannot open URI: $uri")

        var audioTrackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = f
                break
            }
        }

        if (audioTrackIndex == -1 || format == null) {
            extractor.release()
            throw ClarivoException.MediaProbeFailed("No audio track found")
        }

        extractor.selectTrack(audioTrackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false
        var outputSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, targetSampleRate)
        var outputChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
        var outputEncoding = AudioFormat.ENCODING_PCM_16BIT

        val decodedPcm = ByteArrayOutputStream()
        decodedPcm.use { pcmOut ->
            while (!sawOutputEOS) {
                cancellationToken?.throwIfCancelled()

                if (!sawInputEOS) {
                    val inputBufferId = codec.dequeueInputBuffer(10000)
                    if (inputBufferId >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferId)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            codec.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferId >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferId)!!
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)
                    outputBuffer.clear()

                    pcmOut.write(chunk)
                    codec.releaseOutputBuffer(outputBufferId, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outputFormat = codec.outputFormat
                    outputSampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE, outputSampleRate)
                    outputChannels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT, outputChannels)
                    outputEncoding = outputFormat.getInteger(
                        MediaFormat.KEY_PCM_ENCODING,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val pcm16 = when (outputEncoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> floatPcmToMono16k(decodedPcm.toByteArray(), outputSampleRate, outputChannels, targetSampleRate)
            else -> pcm16ToMono16k(decodedPcm.toByteArray(), outputSampleRate, outputChannels, targetSampleRate)
        }
        outputFile.writeBytes(pcm16)

        ClarivoLogger.i("Extracted audio to PCM: ${outputFile.absolutePath}, size=${outputFile.length()}")
    }

    fun extractToWav(
        uri: Uri,
        outputFile: File,
        targetSampleRate: Int = 16000,
        cancellationToken: ProcessCancellationToken? = null
    ) {
        val pcmFile = File(context.cacheDir, "extract_temp_${System.currentTimeMillis()}.pcm")
        try {
            extractToPcm(uri, pcmFile, targetSampleRate, cancellationToken)
            writePcmToWav(pcmFile, outputFile, targetSampleRate, 1, 16)
        } finally {
            pcmFile.delete()
        }
    }

    private fun writePcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ) {
        val pcmData = pcmFile.readBytes()
        val byteRate = sampleRate * channels * bitDepth / 8
        val totalDataLen = pcmData.size + 36
        val blockAlign = channels * bitDepth / 8

        FileOutputStream(wavFile).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1)
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(bitDepth.toShort())
            header.put("data".toByteArray())
            header.putInt(pcmData.size)
            fos.write(header.array())
            fos.write(pcmData)
        }
    }

    private fun pcm16ToMono16k(
        pcm: ByteArray,
        sourceSampleRate: Int,
        sourceChannels: Int,
        targetSampleRate: Int
    ): ByteArray {
        if (pcm.isEmpty()) return pcm
        val channels = sourceChannels.coerceAtLeast(1)
        val frameCount = pcm.size / 2 / channels
        val mono = FloatArray(frameCount)
        var byteIndex = 0
        for (frame in 0 until frameCount) {
            var sum = 0f
            repeat(channels) {
                val lo = pcm[byteIndex].toInt() and 0xFF
                val hi = pcm[byteIndex + 1].toInt()
                val sample = ((hi shl 8) or lo).toShort().toInt()
                sum += sample / 32768f
                byteIndex += 2
            }
            mono[frame] = sum / channels
        }
        return floatsToPcm16(resampleLinear(mono, sourceSampleRate, targetSampleRate))
    }

    private fun floatPcmToMono16k(
        pcm: ByteArray,
        sourceSampleRate: Int,
        sourceChannels: Int,
        targetSampleRate: Int
    ): ByteArray {
        if (pcm.isEmpty()) return pcm
        val channels = sourceChannels.coerceAtLeast(1)
        val frameCount = pcm.size / 4 / channels
        val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        val mono = FloatArray(frameCount)
        for (frame in 0 until frameCount) {
            var sum = 0f
            repeat(channels) {
                sum += buffer.float.coerceIn(-1f, 1f)
            }
            mono[frame] = sum / channels
        }
        return floatsToPcm16(resampleLinear(mono, sourceSampleRate, targetSampleRate))
    }

    private fun resampleLinear(samples: FloatArray, sourceRate: Int, targetRate: Int): FloatArray {
        if (samples.isEmpty() || sourceRate <= 0 || targetRate <= 0 || sourceRate == targetRate) {
            return samples
        }
        val outputSize = ((samples.size.toLong() * targetRate) / sourceRate).toInt().coerceAtLeast(1)
        val output = FloatArray(outputSize)
        val ratio = sourceRate.toDouble() / targetRate.toDouble()
        for (i in output.indices) {
            val sourcePosition = i * ratio
            val left = sourcePosition.toInt().coerceIn(0, samples.lastIndex)
            val right = (left + 1).coerceAtMost(samples.lastIndex)
            val fraction = (sourcePosition - left).toFloat()
            output[i] = samples[left] + (samples[right] - samples[left]) * fraction
        }
        return output
    }

    private fun floatsToPcm16(samples: FloatArray): ByteArray {
        val out = ByteArray(samples.size * 2)
        var index = 0
        samples.forEach { sample ->
            val intSample = (sample.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            out[index++] = (intSample.toInt() and 0xFF).toByte()
            out[index++] = ((intSample.toInt() shr 8) and 0xFF).toByte()
        }
        return out
    }
}
