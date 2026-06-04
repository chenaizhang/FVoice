package com.fvoice.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.fvoice.app.MainActivity
import com.fvoice.app.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val PROGRESS_CHANNEL_ID = "process_progress"
        const val RESULT_CHANNEL_ID = "process_result"
        const val PROGRESS_NOTIFICATION_ID = 1001
        const val RESULT_NOTIFICATION_ID = 1002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        val progressChannel = NotificationChannel(
            PROGRESS_CHANNEL_ID,
            context.getString(R.string.notification_channel_progress),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_progress_desc)
        }

        val resultChannel = NotificationChannel(
            RESULT_CHANNEL_ID,
            context.getString(R.string.notification_channel_result),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_result_desc)
        }

        notificationManager.createNotificationChannels(listOf(progressChannel, resultChannel))
    }

    fun buildProgressNotification(title: String, progress: Int, stage: String): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(stage)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun showProgressNotification(title: String, progress: Int, stage: String) {
        notificationManager.notify(
            PROGRESS_NOTIFICATION_ID,
            buildProgressNotification(title, progress, stage)
        )
    }

    fun showCompleteNotification(title: String, success: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (success) {
            context.getString(R.string.notification_process_complete)
        } else {
            context.getString(R.string.notification_process_failed)
        }

        val notification = NotificationCompat.Builder(context, RESULT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(RESULT_NOTIFICATION_ID, notification)
    }

    fun showErrorNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, RESULT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(RESULT_NOTIFICATION_ID, notification)
    }

    fun cancelProgressNotification() {
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
    }
}
