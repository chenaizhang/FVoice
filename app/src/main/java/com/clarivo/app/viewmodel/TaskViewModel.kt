package com.clarivo.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarivo.app.core.model.ProcessTask
import com.clarivo.app.core.model.ProcessTaskStatus
import com.clarivo.app.core.task.ProcessTaskManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.clarivo.app.ClarivoApplication
import com.clarivo.app.R

data class TaskItem(
    val id: String,
    val fileName: String,
    val isRealtime: Boolean,
    val processMode: String,
    val duration: String,
    val status: ProcessTaskStatus,
    val completedAtMillis: Long,
    val processingOrder: Int = Int.MAX_VALUE,
)

data class TaskUiState(
    val tasks: List<TaskItem> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val searchQuery: String = ""
)

enum class TaskFilter {
    ALL, PROCESSING, COMPLETED
}

class TaskViewModel : ViewModel() {

    private val taskManager = ClarivoApplication.processTaskManager

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val historyChangeListener = { loadTasks() }

    init {
        loadTasks()
        taskManager.currentTask.onEach { _ ->
            loadTasks()
        }.launchIn(viewModelScope)
        taskManager.historyChanged.onEach {
            loadTasks()
        }.launchIn(viewModelScope)
        taskManager.addHistoryChangeListener(historyChangeListener)
    }

    fun refreshTasks() = loadTasks()

    override fun onCleared() {
        super.onCleared()
        taskManager.removeHistoryChangeListener(historyChangeListener)
    }

    private fun loadTasks() {
        val history = taskManager.loadHistory()
        val current = taskManager.currentTask.value
        val all = mutableListOf<ProcessTask>()
        current?.let { all.add(it) }
        all.addAll(history.filter { h -> h.id != current?.id })

        val items = all.map { task ->
            TaskItem(
                id = task.id,
                fileName = task.sourceFileName.ifBlank { ClarivoApplication.instance.getString(R.string.result_unknown_file) },
                isRealtime = task.isRealtime,
                processMode = task.type.labelKey,
                duration = "",
                status = task.status,
                completedAtMillis = if (task.status == ProcessTaskStatus.PROCESSING) 0 else task.completedAt,
                processingOrder = if (task.status == ProcessTaskStatus.PROCESSING) 0 else Int.MAX_VALUE
            )
        }
        _uiState.value = _uiState.value.copy(tasks = items)
    }

    fun setFilter(filter: TaskFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearHistory() {
        viewModelScope.launch {
            taskManager.clearHistory()
        }
    }
}
