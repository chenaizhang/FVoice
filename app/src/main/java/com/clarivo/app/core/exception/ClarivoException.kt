package com.clarivo.app.core.exception

sealed class ClarivoException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class TaskAlreadyRunning(message: String = "Another task is already running") : ClarivoException(message)

    class InvalidInputFile(message: String, cause: Throwable? = null) : ClarivoException(message, cause)

    class MediaProbeFailed(message: String, cause: Throwable? = null) : ClarivoException(message, cause)

    class AudioDecodeFailed(message: String, cause: Throwable? = null) : ClarivoException(message, cause)

    class AudioEncodeFailed(message: String, cause: Throwable? = null) : ClarivoException(message, cause)

    class DenoiseFailed(message: String, cause: Throwable? = null) : ClarivoException(message, cause)

    class AsrFailed(message: String, cause: Throwable? = null) : ClarivoException(message, cause)

    class ExportFailed(message: String, cause: Throwable? = null) : ClarivoException(message, cause)

    class ModelNotAvailable(message: String) : ClarivoException(message)

    class CancelledByUser(message: String = "Cancelled by user") : ClarivoException(message)

    class StorageAccessDenied(message: String = "Storage access denied") : ClarivoException(message)

    class LargeFileWarning(message: String = "File is very large, processing may take a long time") : ClarivoException(message)

    class ServiceBindFailed(message: String) : ClarivoException(message)
}
