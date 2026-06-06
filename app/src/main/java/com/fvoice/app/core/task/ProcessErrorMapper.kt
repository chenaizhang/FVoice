package com.fvoice.app.core.task

import android.content.Context
import com.fvoice.app.R
import com.fvoice.app.core.exception.FVoiceException

object ProcessErrorMapper {

    fun map(context: Context, throwable: Throwable): Pair<String, String> {
        return when (throwable) {
            is FVoiceException.TaskAlreadyRunning -> "task_already_running" to context.getString(R.string.error_task_already_running)
            is FVoiceException.InvalidInputFile -> "invalid_input" to context.getString(R.string.error_invalid_input)
            is FVoiceException.MediaProbeFailed -> "media_probe_failed" to context.getString(R.string.error_media_probe_failed)
            is FVoiceException.AudioDecodeFailed -> "audio_decode_failed" to context.getString(R.string.error_audio_decode_failed)
            is FVoiceException.AudioEncodeFailed -> "audio_encode_failed" to context.getString(R.string.error_audio_encode_failed)
            is FVoiceException.DenoiseFailed -> "denoise_failed" to withDetail(context.getString(R.string.error_denoise_failed), throwable)
            is FVoiceException.AsrFailed -> "asr_failed" to withDetail(context.getString(R.string.error_asr_failed), throwable)
            is FVoiceException.ExportFailed -> "export_failed" to context.getString(R.string.error_export_failed)
            is FVoiceException.ModelNotAvailable -> "model_not_available" to withDetail(context.getString(R.string.error_model_not_available), throwable)
            is FVoiceException.CancelledByUser -> "cancelled" to context.getString(R.string.error_cancelled_by_user)
            is FVoiceException.StorageAccessDenied -> "storage_denied" to context.getString(R.string.error_storage_denied)
            is FVoiceException.LargeFileWarning -> "large_file" to throwable.message.orEmpty()
            is FVoiceException.ServiceBindFailed -> "service_bind_failed" to context.getString(R.string.error_service_bind_failed)
            else -> "unknown" to (throwable.message ?: context.getString(R.string.error_unknown))
        }
    }

    private fun withDetail(base: String, throwable: Throwable): String {
        val detail = throwable.message
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != base }
            ?.take(240)
            ?: return base
        return "$base: $detail"
    }
}
