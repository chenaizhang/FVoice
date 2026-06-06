package com.fvoice.app.core.exception

sealed class FVoiceException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class TaskAlreadyRunning(message: String = "Another task is already running") : FVoiceException(message)

    class InvalidInputFile(message: String, cause: Throwable? = null) : FVoiceException(message, cause)

    class MediaProbeFailed(message: String, cause: Throwable? = null) : FVoiceException(message, cause)

    class AudioDecodeFailed(message: String, cause: Throwable? = null) : FVoiceException(message, cause)

    class AudioEncodeFailed(message: String, cause: Throwable? = null) : FVoiceException(message, cause)

    class DenoiseFailed(message: String, cause: Throwable? = null) : FVoiceException(message, cause)

    class AsrFailed(message: String, cause: Throwable? = null) : FVoiceException(message, cause)

    class ExportFailed(message: String, cause: Throwable? = null) : FVoiceException(message, cause)

    class ModelNotAvailable(message: String) : FVoiceException(message)

    class CancelledByUser(message: String = "Cancelled by user") : FVoiceException(message)

    class StorageAccessDenied(message: String = "Storage access denied") : FVoiceException(message)

    class LargeFileWarning(message: String = "File is very large, processing may take a long time") : FVoiceException(message)

    class ServiceBindFailed(message: String) : FVoiceException(message)
}
