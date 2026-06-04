package com.fvoice.app.viewmodel

import androidx.lifecycle.ViewModel
import com.fvoice.app.data.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Placeholder task model for UI skeleton
data class RecentTask(
    val id: String,
    val fileName: String,
    val type: String,
    val duration: String,
    val status: TaskStatus,
    val processType: String
)

data class HomeUiState(
    val recentTasks: List<RecentTask> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            recentTasks = listOf(
                RecentTask(
                    id = "1",
                    fileName = "meeting_interview.mp4",
                    type = "视频",
                    duration = "12:43",
                    status = TaskStatus.COMPLETED,
                    processType = "降噪"
                ),
                RecentTask(
                    id = "2",
                    fileName = "class_recording.wav",
                    type = "音频",
                    duration = "58:20",
                    status = TaskStatus.PROCESSING,
                    processType = "转写"
                )
            )
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}
