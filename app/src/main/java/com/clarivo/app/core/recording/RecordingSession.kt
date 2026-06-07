package com.clarivo.app.core.recording

import android.content.Context
import com.clarivo.app.core.media.OutputNameGenerator
import com.clarivo.app.core.model.ProcessTaskType
import com.clarivo.app.core.task.ProcessCancellationToken
import com.clarivo.app.util.ClarivoLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class RecordingSession(context: Context) {

    private val appContext = context.applicationContext
    private val recorder = AudioRecorder(appContext)
    private val sessionDir = File(appContext.cacheDir, "recordings").apply { mkdirs() }

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var currentPcmFile: File? = null
    private var currentWavFile: File? = null
    private var sessionId: String = ""

    fun prepare(): Boolean {
        return recorder.prepare()
    }

    suspend fun start(
        taskType: ProcessTaskType = ProcessTaskType.REALTIME_RECORD,
        cancellationToken: ProcessCancellationToken? = null,
        onAmplitude: (Float) -> Unit = {}
    ) {
        if (_state.value is RecordingState.Recording) {
            ClarivoLogger.w("Recording already in progress")
            return
        }

        sessionId = UUID.randomUUID().toString()
        val pcm = File(sessionDir, "${sessionId}.pcm")
        currentPcmFile = pcm
        currentWavFile = File(sessionDir, OutputNameGenerator.generate(
            "recording",
            OutputNameGenerator.ProcessType.DENOISED,
            "wav"
        ))

        _state.value = RecordingState.Recording(
            sessionId = sessionId,
            startTimeMs = System.currentTimeMillis()
        )

        try {
            recorder.startRecording(pcm, cancellationToken, onAmplitude)
        } catch (e: com.clarivo.app.core.exception.ClarivoException.CancelledByUser) {
            _state.value = RecordingState.Cancelled(sessionId)
            throw e
        } catch (e: Exception) {
            _state.value = RecordingState.Error(sessionId, e.message ?: "Recording failed")
            throw e
        }
    }

    fun stop(): File? {
        recorder.stopRecording()
        val pcm = currentPcmFile ?: return null
        val wav = currentWavFile ?: return null

        return try {
            recorder.writeWavHeader(pcm, wav)
            pcm.delete()
            _state.value = RecordingState.Completed(sessionId, wav)
            ClarivoLogger.i("Recording saved: ${wav.absolutePath}")
            wav
        } catch (e: Exception) {
            ClarivoLogger.e("Failed to finalize recording", e)
            _state.value = RecordingState.Error(sessionId, e.message ?: "Finalize failed")
            null
        }
    }

    fun release() {
        recorder.release()
        _state.value = RecordingState.Idle
    }

    sealed class RecordingState {
        data object Idle : RecordingState()
        data class Recording(val sessionId: String, val startTimeMs: Long) : RecordingState()
        data class Completed(val sessionId: String, val file: File) : RecordingState()
        data class Cancelled(val sessionId: String) : RecordingState()
        data class Error(val sessionId: String, val message: String) : RecordingState()
    }
}
