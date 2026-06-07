package com.clarivo.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarivo.app.ClarivoApplication
import com.clarivo.app.R
import com.clarivo.app.core.model.ProcessTaskStatus
import com.clarivo.app.core.task.ProcessTaskManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class RecentTask(
    val id: String,
    val fileName: String,
    val isRealtime: Boolean,
    val duration: String,
    val status: ProcessTaskStatus,
    val processType: String,
    val completedAt: Long = 0
)

data class HomeUiState(
    val recentTasks: List<RecentTask> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val taskManager = com.clarivo.app.ClarivoApplication.processTaskManager

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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

    override fun onCleared() {
        super.onCleared()
        taskManager.removeHistoryChangeListener(historyChangeListener)
    }

    fun refreshTasks() = loadTasks()

    private fun loadTasks() {
        val history = taskManager.loadHistory()
        val current = taskManager.currentTask.value
        val all = mutableListOf<com.clarivo.app.core.model.ProcessTask>()
        current?.let { all.add(it) }
        all.addAll(history.filter { it.id != current?.id })

        val items = all.take(3).map { task ->
            RecentTask(
                id = task.id,
                fileName = task.sourceFileName.ifBlank { ClarivoApplication.instance.getString(R.string.result_unknown_file) },
                isRealtime = task.isRealtime,
                duration = "",
                status = task.status,
                processType = task.type.name.lowercase().replaceFirstChar { it.uppercase() },
                completedAt = task.completedAt
            )
        }
        _uiState.value = _uiState.value.copy(recentTasks = items)
    }
}
