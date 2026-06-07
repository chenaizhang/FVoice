package com.clarivo.app.core.task

import com.clarivo.app.core.model.ProcessTask
import com.clarivo.app.core.model.ProcessTaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ProcessQueue {

    private val _currentTask = MutableStateFlow<ProcessTask?>(null)
    val currentTask: StateFlow<ProcessTask?> = _currentTask.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    @Volatile
    private var activeToken: ProcessCancellationToken? = null

    fun enqueue(
        type: com.clarivo.app.core.model.ProcessTaskType,
        sourceUri: String,
        sourceFileName: String,
        settings: com.clarivo.app.core.model.TaskSettings = com.clarivo.app.core.model.TaskSettings()
    ): ProcessTask {
        if (_isRunning.value) {
            throw com.clarivo.app.core.exception.ClarivoException.TaskAlreadyRunning()
        }
        val task = ProcessTask(
            id = UUID.randomUUID().toString(),
            type = type,
            status = ProcessTaskStatus.PENDING,
            sourceUri = sourceUri,
            sourceFileName = sourceFileName,
            settings = settings
        )
        _currentTask.value = task
        return task
    }

    fun startTask(taskId: String): ProcessCancellationToken {
        val task = _currentTask.value
        if (task == null || task.id != taskId) {
            throw IllegalStateException("Task not found: $taskId")
        }
        if (_isRunning.value) {
            throw com.clarivo.app.core.exception.ClarivoException.TaskAlreadyRunning()
        }
        _isRunning.value = true
        val token = ProcessCancellationToken()
        activeToken = token
        _currentTask.value = task.copy(status = ProcessTaskStatus.PROCESSING, startedAt = System.currentTimeMillis())
        return token
    }

    fun updateProgress(taskId: String, progress: Int, stage: String) {
        val task = _currentTask.value ?: return
        if (task.id != taskId) return
        _currentTask.value = task.copy(progress = progress, currentStage = stage)
    }

    fun completeTask(taskId: String) {
        val task = _currentTask.value ?: return
        if (task.id != taskId) return
        _currentTask.value = task.copy(
            status = ProcessTaskStatus.COMPLETED,
            progress = 100,
            completedAt = System.currentTimeMillis()
        )
        _isRunning.value = false
        activeToken = null
    }

    fun failTask(taskId: String, errorCode: String, errorMessage: String) {
        val task = _currentTask.value ?: return
        if (task.id != taskId) return
        _currentTask.value = task.copy(
            status = ProcessTaskStatus.FAILED,
            errorCode = errorCode,
            errorMessage = errorMessage,
            completedAt = System.currentTimeMillis()
        )
        _isRunning.value = false
        activeToken = null
    }

    fun cancelCurrent() {
        activeToken?.cancel()
        val task = _currentTask.value ?: return
        _currentTask.value = task.copy(
            status = ProcessTaskStatus.CANCELLED,
            completedAt = System.currentTimeMillis()
        )
        _isRunning.value = false
        activeToken = null
    }

    fun removeCurrentTask(taskId: String) {
        if (_currentTask.value?.id == taskId) {
            _currentTask.value = null
            _isRunning.value = false
            activeToken = null
        }
    }

    fun clear() {
        activeToken?.cancel()
        _currentTask.value = null
        _isRunning.value = false
        activeToken = null
    }
}
