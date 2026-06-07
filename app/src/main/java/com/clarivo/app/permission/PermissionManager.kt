package com.clarivo.app.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionManager(private val context: Context) {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    val permissions: PermissionState
        get() = state.value

    fun refresh() {
        _state.value = readState()
    }

    fun notificationRuntimePermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }

    fun microphonePermission(): String = Manifest.permission.RECORD_AUDIO

    fun legacyStoragePermission(): String? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Manifest.permission.READ_EXTERNAL_STORAGE
        } else {
            null
        }
    }

    fun storageSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${appContext.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${appContext.packageName}")
            }
        }
    }

    fun notificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
        }
    }

    fun batteryWhitelistIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${appContext.packageName}")
        }
    }

    fun overlaySettingsIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${appContext.packageName}")
        }
    }

    private fun readState(): PermissionState {
        return PermissionState(
            storage = checkStorage(),
            notification = checkNotification(),
            microphone = checkMicrophone(),
            batteryWhitelist = checkBatteryWhitelist(),
            overlay = checkOverlay()
        )
    }

    private fun checkStorage(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun checkNotification(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        }
    }

    private fun checkMicrophone(): Boolean = hasPermission(Manifest.permission.RECORD_AUDIO)

    private fun checkBatteryWhitelist(): Boolean {
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
    }

    private fun checkOverlay(): Boolean = Settings.canDrawOverlays(appContext)

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) ==
            PermissionChecker.PERMISSION_GRANTED
    }
}
