package com.fvoice.app.viewmodel

import androidx.lifecycle.ViewModel
import com.fvoice.app.data.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TaskItem(
    val id: String,
    val fileName: String,
    val type: String,
    val processMode: String,
    val duration: String,
    val status: TaskStatus,
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

    private val _uiState = MutableStateFlow(
        TaskUiState(
            tasks = listOf(
                TaskItem(
                    id = "1",
                    fileName = "meeting_interview.mp4",
                    type = "视频",
                    processMode = "降噪并转写",
                    duration = "12:43",
                    status = TaskStatus.COMPLETED,
                    completedAtMillis = 1_786_092_600_000,
                ),
                TaskItem(
                    id = "2",
                    fileName = "class_recording.wav",
                    type = "音频",
                    processMode = "仅转写",
                    duration = "58:20",
                    status = TaskStatus.PROCESSING,
                    completedAtMillis = 0,
                    processingOrder = 0,
                ),
                TaskItem(
                    id = "3",
                    fileName = "street_voice.m4a",
                    type = "音频",
                    processMode = "强力降噪",
                    duration = "03:18",
                    status = TaskStatus.FAILED,
                    completedAtMillis = 1_785_828_000_000,
                ),
                TaskItem(
                    id = "4",
                    fileName = "demo_clip.mov",
                    type = "视频",
                    processMode = "替换降噪音轨",
                    duration = "01:42",
                    status = TaskStatus.COMPLETED,
                    completedAtMillis = 1_786_172_400_000,
                )
            )
        )
    )
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    fun setFilter(filter: TaskFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}
