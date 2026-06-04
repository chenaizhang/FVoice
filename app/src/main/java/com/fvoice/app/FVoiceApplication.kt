package com.fvoice.app

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass

class FVoiceApplication : Application() {

    companion object {
        lateinit var instance: FVoiceApplication
            private set

        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
            runCatching {
                val method = ApplicationInfo::class.java
                    .getDeclaredMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(appInfo, enable)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        com.fvoice.app.util.FVoiceLogger.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val enable = prefs.getBoolean("enable_predictive_back", true)
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback"
            )
            setEnableOnBackInvokedCallback(applicationInfo, enable)
        }
    }
}
