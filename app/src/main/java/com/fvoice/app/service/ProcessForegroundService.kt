package com.fvoice.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.fvoice.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProcessForegroundService : Service() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ProcessForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProcessForegroundService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationHelper.buildProgressNotification(
            getString(com.fvoice.app.R.string.service_process_title),
            0,
            getString(com.fvoice.app.R.string.service_process_preparing)
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

        // Simulate processing for skeleton
        serviceScope.launch {
            for (i in 0..100 step 5) {
                notificationHelper.showProgressNotification(
                    getString(com.fvoice.app.R.string.service_process_title),
                    i,
                    getString(com.fvoice.app.R.string.service_process_running)
                )
                delay(1000)
            }
            notificationHelper.showCompleteNotification(
                getString(com.fvoice.app.R.string.service_process_title),
                true
            )
            stopSelf()
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
