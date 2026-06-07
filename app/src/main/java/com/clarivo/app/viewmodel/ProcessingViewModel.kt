package com.clarivo.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarivo.app.core.model.ProcessProgress
import com.clarivo.app.core.model.ProcessResult
import com.clarivo.app.core.model.ProcessTask
import com.clarivo.app.core.task.ProcessTaskManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.clarivo.app.ClarivoApplication

data class ProcessingUiState(
    val task: ProcessTask? = null,
    val progress: ProcessProgress = ProcessProgress(),
    val isCancelling: Boolean = false,
    val error: String? = null,
    val logs: List<String> = emptyList()
)

class ProcessingViewModel : ViewModel() {

    private val taskManager = ClarivoApplication.processTaskManager

    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    init {
        taskManager.currentTask.onEach { task ->
            _uiState.value = _uiState.value.copy(task = task)
        }.launchIn(viewModelScope)

        taskManager.progress.onEach { progress ->
            _uiState.value = _uiState.value.copy(progress = progress)
        }.launchIn(viewModelScope)

        taskManager.resultEvent.onEach { result ->
            when (result) {
                is ProcessResult.Failure -> {
                    _uiState.value = _uiState.value.copy(error = result.errorMessage, isCancelling = false)
                }
                else -> { /* handled by navigation */ }
            }
        }.launchIn(viewModelScope)

        // Poll recent logs every 2 seconds while processing
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(2000)
                val recent = com.clarivo.app.util.ClarivoLogger.getRecentLogs(50)
                _uiState.value = _uiState.value.copy(logs = recent)
            }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true)
            taskManager.cancelCurrent()
        }
    }
}
