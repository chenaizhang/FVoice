package com.clarivo.app.core.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.clarivo.app.core.task.ProcessCancellationToken
import com.clarivo.app.util.ClarivoLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Muxes original video track with processed PCM audio into an MP4 file.
 * The PCM audio is encoded to AAC via MediaCodec, then combined with the
 * copied video track using MediaMuxer.
 *
 * Implementation details:
 * - Video frames are first copied to a temporary file to allow interleaved
 *   writing with audio frames, which MediaMuxer requires on some devices.
 * - Audio is fully encoded into memory (AAC frames are small) before muxing.
 * - The muxer then writes audio and video frames in timestamp order.
 */
class VideoAudioMuxer(private val context: Context) {

    /**
     * Creates a new MP4 file that contains the video track from [sourceVideoUri]
     * and the audio from [audioWavFile] (PCM 16-bit).
     *
     * @param sourceVideoUri original video URI
     * @param audioWavFile WAV file containing PCM 16-bit audio
     * @param outputFile destination MP4 file
     * @param cancellationToken optional cancellation token
     * @return true on success
     */
    fun muxVideoWithPcmAudio(
        sourceVideoUri: Uri,
        audioWavFile: File,
        outputFile: File,
        cancellationToken: ProcessCancellationToken? = null
    ): Boolean {
        var extractor: MediaExtractor? = null
        var audioEncoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var pcmStream: FileInputStream? = null
        var videoTempFile: File? = null
        var success = false

        try {
            // 1. Set up video extractor
            extractor = MediaExtractor()
            context.contentResolver.openFileDescriptor(sourceVideoUri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: throw IllegalStateException("Cannot open video URI")

            val videoTrackIndex = (0 until extractor.trackCount).find { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: throw IllegalStateException("No video track found")

            val videoFormat = extractor.getTrackFormat(videoTrackIndex)
            val videoRotation = videoFormat.getInteger(MediaFormat.KEY_ROTATION, 0)
            extractor.selectTrack(videoTrackIndex)

            // 2. Copy video frames to a temporary file and collect frame metadata
            videoTempFile = File(context.cacheDir, "video_temp_${System.currentTimeMillis()}.tmp")
            val videoFrames = copyVideoFrames(extractor, videoTempFile)
            if (videoFrames.isEmpty()) {
                throw IllegalStateException("No video frames found")
            }

            // 3. Parse WAV header
            val wavHeader = readWavHeader(audioWavFile)
            val sampleRate = wavHeader.sampleRate
            val channels = wavHeader.channels.coerceAtLeast(1)
            val bytesPerFrame = channels * (wavHeader.bitDepth / 8)

            if (sampleRate <= 0 || bytesPerFrame <= 0) {
                throw IllegalStateException("Invalid WAV format: sr=$sampleRate, bpf=$bytesPerFrame")
            }

            // 4. Open PCM stream
            pcmStream = FileInputStream(audioWavFile)
            val actualSkip = pcmStream.skip(wavHeader.pcmOffset.toLong())
            if (actualSkip < wavHeader.pcmOffset) {
                throw IllegalStateException("Failed to skip WAV header: skipped $actualSkip / ${wavHeader.pcmOffset}")
            }

            // 5. Create and start AAC encoder
            audioEncoder = createAacEncoder(sampleRate, channels)
            audioEncoder.start()

            // 6. Prime encoder until output format is available
            val (audioFormat, initialPcmBytesFed) = primeEncoder(
                audioEncoder, pcmStream, sampleRate, bytesPerFrame
            ) ?: throw IllegalStateException("Failed to get AAC output format")

            // 7. Collect all remaining AAC frames into memory
            val audioFrames = mutableListOf<AacFrame>()
            collectAacFrames(
                audioEncoder, pcmStream, sampleRate, bytesPerFrame,
                initialPcmBytesFed, audioFrames, cancellationToken
            )

            if (audioFrames.isEmpty()) {
                throw IllegalStateException("No AAC frames generated")
            }

            // 8. Create muxer and add tracks
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            if (videoRotation in listOf(90, 180, 270)) {
                muxer.setOrientationHint(videoRotation)
            }
            muxer.start()

            // 9. Interleave write audio and video frames by timestamp
            val audioIter = audioFrames.iterator()
            val videoIter = videoFrames.iterator()
            var nextAudio = if (audioIter.hasNext()) audioIter.next() else null
            var nextVideo = if (videoIter.hasNext()) videoIter.next() else null

            FileInputStream(videoTempFile).use { videoStream ->
                var videoStreamPos = 0L

                while (nextAudio != null || nextVideo != null) {
                    cancellationToken?.throwIfCancelled()

                    val useAudio = nextAudio != null &&
                            (nextVideo == null || nextAudio.ptsUs <= nextVideo.ptsUs)

                    if (useAudio) {
                        val buf = ByteBuffer.wrap(nextAudio.data)
                        val info = MediaCodec.BufferInfo()
                        info.set(0, nextAudio.data.size, nextAudio.ptsUs, nextAudio.flags)
                        muxer.writeSampleData(muxerAudioTrack, buf, info)
                        nextAudio = if (audioIter.hasNext()) audioIter.next() else null
                    } else {
                        val videoFrame = nextVideo!!
                        val skip = videoFrame.offset - videoStreamPos
                        if (skip > 0) {
                            val skipped = videoStream.skip(skip)
                            videoStreamPos += skipped
                        }
                        val buf = ByteBuffer.allocate(videoFrame.size)
                        var totalRead = 0
                        while (totalRead < videoFrame.size) {
                            val read = videoStream.read(buf.array(), totalRead, videoFrame.size - totalRead)
                            if (read < 0) throw IllegalStateException("Unexpected EOF reading video frame")
                            totalRead += read
                        }
                        videoStreamPos += totalRead

                        val info = MediaCodec.BufferInfo()
                        info.set(0, videoFrame.size, videoFrame.ptsUs, videoFrame.flags)
                        muxer.writeSampleData(muxerVideoTrack, buf, info)
                        nextVideo = if (videoIter.hasNext()) videoIter.next() else null
                    }
                }
            }

            success = true
            ClarivoLogger.i(
                "VideoAudioMuxer: success ${outputFile.absolutePath}, " +
                        "audioFrames=${audioFrames.size}, videoFrames=${videoFrames.size}, sampleRate=$sampleRate"
            )
        } catch (e: Exception) {
            ClarivoLogger.e("VideoAudioMuxer: mux failed", e)
            success = false
        } finally {
            try { pcmStream?.close() } catch (_: Exception) {}
            try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            try { audioEncoder?.stop() } catch (_: Exception) {}
            try { audioEncoder?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            try { videoTempFile?.delete() } catch (_: Exception) {}
        }
        return success
    }

    private fun createAacEncoder(sampleRate: Int, channels: Int): MediaCodec {
        val codec = MediaCodec.createEncoderByType("audio/mp4a-latm")
        val format = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, channels)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, getRecommendedBitRate(sampleRate, channels))
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384 * channels)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return codec
    }

    private fun getRecommendedBitRate(sampleRate: Int, channels: Int): Int {
        return when {
            sampleRate >= 48000 -> 192000 * channels
            sampleRate >= 44100 -> 128000 * channels
            sampleRate >= 22050 -> 64000 * channels
            else -> 32000 * channels
        }
    }

    /**
     * Copies video frames from [extractor] into [tempFile] and returns a list of
     * frame metadata (offset, size, pts, flags).
     */
    private fun copyVideoFrames(extractor: MediaExtractor, tempFile: File): List<VideoFrame> {
        val frames = mutableListOf<VideoFrame>()
        val buffer = ByteBuffer.allocate(4 * 1024 * 1024) // 4MB buffer
        var offset = 0L

        FileOutputStream(tempFile).use { fos ->
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val sampleTime = extractor.sampleTime
                val flags = extractor.sampleFlags

                val data = ByteArray(sampleSize)
                buffer.get(data)
                fos.write(data)

                frames.add(VideoFrame(offset, sampleSize, sampleTime, flags))
                offset += sampleSize
                extractor.advance()
            }
        }
        return frames
    }

    /**
     * Feeds the encoder with PCM data until [MediaCodec.INFO_OUTPUT_FORMAT_CHANGED] is received.
     * Does **not** send END_OF_STREAM — that is left to [collectAacFrames].
     * Returns the output [MediaFormat] and the number of PCM bytes already fed,
     * or null on failure.
     */
    private fun primeEncoder(
        encoder: MediaCodec,
        pcmStream: FileInputStream,
        sampleRate: Int,
        bytesPerFrame: Int
    ): Pair<MediaFormat, Long>? {
        val bufferInfo = MediaCodec.BufferInfo()
        val inputBufferSize = (sampleRate * bytesPerFrame / 10).coerceAtMost(16384 * bytesPerFrame / 2)
        val readBuffer = ByteArray(inputBufferSize)
        var inputExhausted = false
        var outputFormat: MediaFormat? = null
        var pcmBytesFed = 0L
        val usPerByte = 1_000_000.0 / (sampleRate * bytesPerFrame)

        while (outputFormat == null) {
            if (!inputExhausted) {
                val inputId = encoder.dequeueInputBuffer(10000)
                if (inputId >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputId)!!
                    inputBuffer.clear()
                    val bytesRead = pcmStream.read(readBuffer)
                    if (bytesRead > 0) {
                        inputBuffer.put(readBuffer, 0, bytesRead)
                        val ptsUs = (pcmBytesFed * usPerByte).toLong()
                        encoder.queueInputBuffer(inputId, 0, bytesRead, ptsUs, 0)
                        pcmBytesFed += bytesRead
                    } else {
                        // Input exhausted but we don't send EOS here; let collectAacFrames handle it.
                        inputExhausted = true
                    }
                }
            }

            val outputId = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            when {
                outputId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    outputFormat = encoder.outputFormat
                }
                outputId == MediaCodec.INFO_TRY_AGAIN_LATER -> { /* continue */ }
                outputId >= 0 -> {
                    // Discard early output data; it will be re-generated by collectAacFrames.
                    encoder.releaseOutputBuffer(outputId, false)
                }
            }

            if (inputExhausted && outputFormat == null) {
                // Input is exhausted and we still don't have a format — something is wrong.
                return null
            }
        }
        return Pair(outputFormat, pcmBytesFed)
    }

    /**
     * Continues encoding the remaining PCM data and collects all AAC output frames.
     * Sends EOS when the PCM stream is exhausted.
     */
    private fun collectAacFrames(
        encoder: MediaCodec,
        pcmStream: FileInputStream,
        sampleRate: Int,
        bytesPerFrame: Int,
        initialPcmBytesFed: Long,
        frames: MutableList<AacFrame>,
        cancellationToken: ProcessCancellationToken?
    ) {
        val bufferInfo = MediaCodec.BufferInfo()
        val inputBufferSize = (sampleRate * bytesPerFrame / 10).coerceAtMost(16384 * bytesPerFrame / 2)
        val readBuffer = ByteArray(inputBufferSize)
        var inputDone = false
        var outputDone = false
        var pcmBytesFed = initialPcmBytesFed
        val usPerByte = 1_000_000.0 / (sampleRate * bytesPerFrame)

        while (!outputDone) {
            cancellationToken?.throwIfCancelled()

            if (!inputDone) {
                val inputId = encoder.dequeueInputBuffer(10000)
                if (inputId >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputId)!!
                    inputBuffer.clear()
                    val bytesRead = pcmStream.read(readBuffer)
                    if (bytesRead > 0) {
                        inputBuffer.put(readBuffer, 0, bytesRead)
                        val ptsUs = (pcmBytesFed * usPerByte).toLong()
                        encoder.queueInputBuffer(inputId, 0, bytesRead, ptsUs, 0)
                        pcmBytesFed += bytesRead
                    } else {
                        encoder.queueInputBuffer(
                            inputId, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                    }
                }
            }

            val outputId = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            when {
                outputId == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                outputId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                outputId >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputId)!!
                    if (bufferInfo.size > 0) {
                        val data = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.get(data)
                        frames.add(
                            AacFrame(
                                data = data,
                                ptsUs = bufferInfo.presentationTimeUs,
                                flags = bufferInfo.flags
                            )
                        )
                    }
                    encoder.releaseOutputBuffer(outputId, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
        }
    }

    private fun readWavHeader(file: File): WavHeader {
        val header = file.inputStream().use { stream ->
            ByteArray(44).also { stream.read(it) }
        }
        if (header.size < 44) {
            return WavHeader(sampleRate = 16000, channels = 1, bitDepth = 16, pcmOffset = 0)
        }
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val sampleRate = bb.getInt(24)
        val channels = bb.getShort(22).toInt()
        val bitDepth = bb.getShort(34).toInt()
        return WavHeader(sampleRate, channels, bitDepth, pcmOffset = 44)
    }

    private data class WavHeader(
        val sampleRate: Int,
        val channels: Int,
        val bitDepth: Int,
        val pcmOffset: Int
    )

    private data class VideoFrame(
        val offset: Long,
        val size: Int,
        val ptsUs: Long,
        val flags: Int
    )

    private data class AacFrame(
        val data: ByteArray,
        val ptsUs: Long,
        val flags: Int
    )
}
