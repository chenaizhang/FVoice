package com.fvoice.app.core.task

import android.content.Context
import android.net.Uri
import com.fvoice.app.core.model.OutputFileInfo
import com.fvoice.app.core.model.OutputFileType
import com.fvoice.app.core.model.ProcessTask
import com.fvoice.app.core.model.ProcessTaskStatus
import com.fvoice.app.core.model.TranscriptResult
import com.fvoice.app.util.FVoiceLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProcessStateStore(context: Context) {

    private val storeDir = File(context.filesDir, "task_state").apply { mkdirs() }
    private val currentTaskFile = File(storeDir, "current_task.json")
    private val taskHistoryDir = File(storeDir, "history").apply { mkdirs() }
    private val resultHistoryDir = File(storeDir, "results").apply { mkdirs() }
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun saveCurrentTask(task: ProcessTask) {
        try {
            currentTaskFile.writeText(json.encodeToString(task))
        } catch (e: Exception) {
            FVoiceLogger.e("Failed to save current task", e)
        }
    }

    fun loadCurrentTask(): ProcessTask? {
        return try {
            if (!currentTaskFile.exists()) return null
            json.decodeFromString<ProcessTask>(currentTaskFile.readText())
        } catch (e: Exception) {
            FVoiceLogger.e("Failed to load current task", e)
            null
        }
    }

    fun clearCurrentTask() {
        currentTaskFile.delete()
    }

    fun archiveTask(task: ProcessTask) {
        try {
            val file = File(taskHistoryDir, "${task.id}.json")
            file.writeText(json.encodeToString(task))
        } catch (e: Exception) {
            FVoiceLogger.e("Failed to archive task", e)
        }
    }

    fun loadAllTasks(): List<ProcessTask> {
        return taskHistoryDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull {
                try {
                    json.decodeFromString<ProcessTask>(it.readText())
                } catch (_: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun saveResult(
        taskId: String,
        outputFiles: List<OutputFileInfo>,
        transcriptResult: TranscriptResult?
    ) {
        try {
            val file = File(resultHistoryDir, "$taskId.json")
            val stored = StoredProcessResult(
                taskId = taskId,
                outputFiles = outputFiles.map { it.toStored() },
                transcriptResult = transcriptResult
            )
            file.writeText(json.encodeToString(stored))
        } catch (e: Exception) {
            FVoiceLogger.e("Failed to save task result", e)
        }
    }

    fun loadResult(taskId: String): StoredProcessResult? {
        return try {
            val file = File(resultHistoryDir, "$taskId.json")
            if (!file.exists()) return null
            json.decodeFromString<StoredProcessResult>(file.readText())
        } catch (e: Exception) {
            FVoiceLogger.e("Failed to load task result", e)
            null
        }
    }

    fun cleanupOrphanedTempFiles(context: Context) {
        val tempDir = File(context.cacheDir, "process_temp")
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000L) {
                    file.deleteRecursively()
                }
            }
        }
    }

    fun markInterruptedAsFailed() {
        val task = loadCurrentTask() ?: return
        if (task.status == ProcessTaskStatus.PROCESSING || task.status == ProcessTaskStatus.PENDING) {
            val failed = task.copy(
                status = ProcessTaskStatus.FAILED,
                errorCode = "interrupted",
                errorMessage = "Task was interrupted unexpectedly",
                completedAt = System.currentTimeMillis()
            )
            saveCurrentTask(failed)
            archiveTask(failed)
            clearCurrentTask()
        }
    }

    fun deleteTask(taskId: String) {
        try {
            File(taskHistoryDir, "$taskId.json").delete()
            File(resultHistoryDir, "$taskId.json").delete()
            FVoiceLogger.i("Task deleted: $taskId")
        } catch (e: Exception) {
            FVoiceLogger.e("Failed to delete task: $taskId", e)
        }
    }

    fun clearHistory() {
        try {
            taskHistoryDir.listFiles()?.forEach { file ->
                if (file.extension == "json") {
                    file.delete()
                }
            }
            resultHistoryDir.listFiles()?.forEach { file ->
                if (file.extension == "json") {
                    file.delete()
                }
            }
            FVoiceLogger.i("History cleared")
        } catch (e: Exception) {
            FVoiceLogger.e("Failed to clear history", e)
        }
    }

    private fun OutputFileInfo.toStored(): StoredOutputFileInfo {
        return StoredOutputFileInfo(
            uri = uri.toString(),
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            type = type
        )
    }
}

@Serializable
data class StoredProcessResult(
    val taskId: String,
    val outputFiles: List<StoredOutputFileInfo> = emptyList(),
    val transcriptResult: TranscriptResult? = null
) {
    fun toOutputFiles(): List<OutputFileInfo> {
        return outputFiles.map { it.toOutputFileInfo() }
    }
}

@Serializable
data class StoredOutputFileInfo(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long = 0,
    val type: OutputFileType
) {
    fun toOutputFileInfo(): OutputFileInfo {
        return OutputFileInfo(
            uri = Uri.parse(uri),
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            type = type
        )
    }
}
