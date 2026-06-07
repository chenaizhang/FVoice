package com.clarivo.app.core.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.clarivo.app.core.model.OutputFileInfo
import com.clarivo.app.core.model.OutputFileType
import com.clarivo.app.util.ClarivoLogger
import java.io.File
import java.io.FileInputStream

/**
 * TODO: Planned enhancements:
 * - Video remuxing: mux processed audio back into original video container (requires ffmpeg)
 * - More export formats: MP4/M4A audio encoding (requires MediaCodec or ffmpeg)
 * - Custom output path: allow user-selected directory via SAF with persistent URI permission
 */
class MediaExportManager(private val context: Context) {

    fun exportToDownloads(
        sourceFile: File,
        displayName: String,
        mimeType: String,
        fileType: OutputFileType
    ): OutputFileInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportViaMediaStore(sourceFile, displayName, mimeType, fileType)
        } else {
            exportToLegacyDir(sourceFile, displayName, mimeType, fileType)
        }
    }

    private fun exportViaMediaStore(
        sourceFile: File,
        displayName: String,
        mimeType: String,
        fileType: OutputFileType
    ): OutputFileInfo? {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Clarivo")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(collection, values) ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                FileInputStream(sourceFile).use { fis ->
                    fis.copyTo(os)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }

            ClarivoLogger.i("Exported via MediaStore: $displayName -> $uri")
            return OutputFileInfo(
                uri = uri,
                fileName = displayName,
                mimeType = mimeType,
                fileSize = sourceFile.length(),
                type = fileType
            )
        } catch (e: Exception) {
            ClarivoLogger.e("Export failed: $displayName", e)
            context.contentResolver.delete(uri, null, null)
            return null
        }
    }

    private fun exportToLegacyDir(
        sourceFile: File,
        displayName: String,
        mimeType: String,
        fileType: OutputFileType
    ): OutputFileInfo? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fvDir = File(downloadsDir, "Clarivo").apply { mkdirs() }
        val destFile = File(fvDir, displayName)

        return try {
            sourceFile.copyTo(destFile, overwrite = true)
            ClarivoLogger.i("Exported to legacy dir: ${destFile.absolutePath}")
            OutputFileInfo(
                uri = Uri.fromFile(destFile),
                fileName = displayName,
                mimeType = mimeType,
                fileSize = destFile.length(),
                type = fileType
            )
        } catch (e: Exception) {
            ClarivoLogger.e("Legacy export failed: $displayName", e)
            null
        }
    }
}
