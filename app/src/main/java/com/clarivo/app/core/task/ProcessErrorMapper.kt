package com.clarivo.app.core.task

import android.content.Context
import com.clarivo.app.R
import com.clarivo.app.core.exception.ClarivoException

object ProcessErrorMapper {

    fun map(context: Context, throwable: Throwable): Pair<String, String> {
        return when (throwable) {
            is ClarivoException.TaskAlreadyRunning -> "task_already_running" to context.getString(R.string.error_task_already_running)
            is ClarivoException.InvalidInputFile -> "invalid_input" to context.getString(R.string.error_invalid_input)
            is ClarivoException.MediaProbeFailed -> "media_probe_failed" to context.getString(R.string.error_media_probe_failed)
            is ClarivoException.AudioDecodeFailed -> "audio_decode_failed" to context.getString(R.string.error_audio_decode_failed)
            is ClarivoException.AudioEncodeFailed -> "audio_encode_failed" to context.getString(R.string.error_audio_encode_failed)
            is ClarivoException.DenoiseFailed -> "denoise_failed" to withDetail(context.getString(R.string.error_denoise_failed), throwable)
            is ClarivoException.AsrFailed -> "asr_failed" to withDetail(context.getString(R.string.error_asr_failed), throwable)
            is ClarivoException.ExportFailed -> "export_failed" to context.getString(R.string.error_export_failed)
            is ClarivoException.ModelNotAvailable -> "model_not_available" to withDetail(context.getString(R.string.error_model_not_available), throwable)
            is ClarivoException.CancelledByUser -> "cancelled" to context.getString(R.string.error_cancelled_by_user)
            is ClarivoException.StorageAccessDenied -> "storage_denied" to context.getString(R.string.error_storage_denied)
            is ClarivoException.LargeFileWarning -> "large_file" to throwable.message.orEmpty()
            is ClarivoException.ServiceBindFailed -> "service_bind_failed" to context.getString(R.string.error_service_bind_failed)
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
