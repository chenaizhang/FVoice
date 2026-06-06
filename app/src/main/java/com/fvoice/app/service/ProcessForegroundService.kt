package com.fvoice.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.fvoice.app.R
import com.fvoice.app.core.task.ProcessTaskManager
import com.fvoice.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProcessForegroundService : Service() {

    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val ACTION_CANCEL = "com.fvoice.app.CANCEL_TASK"

        fun start(context: Context, taskId: String) {
            val intent = Intent(context, ProcessForegroundService::class.java).apply {
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProcessForegroundService::class.java))
        }

        fun cancelIntent(context: Context): Intent {
            return Intent(context, ProcessForegroundService::class.java).apply {
                action = ACTION_CANCEL
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var taskManager: ProcessTaskManager
    private var currentTaskId: String? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        taskManager = ProcessTaskManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            serviceScope.launch {
                taskManager.cancelCurrent()
                stopSelf()
            }
            return START_NOT_STICKY
        }

        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        if (taskId.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        currentTaskId = taskId

        val notification = notificationHelper.buildProgressNotification(
            title = getString(R.string.service_process_title),
            progress = 0,
            stage = getString(R.string.service_process_preparing),
            taskId = taskId
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.PROGRESS_NOTIFICATION_ID, notification)
        }

        // Observe progress
        taskManager.progress.onEach { progress ->
            val task = taskManager.currentTask.value
            val fileName = task?.sourceFileName ?: ""
            notificationHelper.showProgressNotification(
                title = getString(R.string.service_process_title),
                progress = progress.percent,
                stage = if (fileName.isNotBlank()) "${progress.currentStage} · $fileName" else progress.currentStage,
                taskId = taskId
            )
        }.launchIn(serviceScope)

        // Observe result
        serviceScope.launch {
            taskManager.resultEvent.collect { result ->
                if (result.taskId != taskId) return@collect
                when (result) {
                    is com.fvoice.app.core.model.ProcessResult.Success -> {
                        notificationHelper.showCompleteNotification(
                            title = getString(R.string.service_process_title),
                            message = getString(R.string.notification_process_complete_with_file,
                                taskManager.currentTask.value?.sourceFileName ?: "")
                        )
                        stopSelf()
                    }
                    is com.fvoice.app.core.model.ProcessResult.Failure -> {
                        notificationHelper.showErrorNotification(
                            title = getString(R.string.service_process_title),
                            message = getString(R.string.notification_process_failed_reason, result.errorMessage)
                        )
                        stopSelf()
                    }
                    is com.fvoice.app.core.model.ProcessResult.Cancelled -> {
                        notificationHelper.cancelProgressNotification()
                        stopSelf()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        notificationHelper.cancelProgressNotification()
        super.onDestroy()
    }
}
