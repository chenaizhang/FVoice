package com.fvoice.app

import android.app.Application

class FVoiceApplication : Application() {

    companion object {
        lateinit var instance: FVoiceApplication
            private set

        lateinit var processTaskManager: com.fvoice.app.core.task.ProcessTaskManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        com.fvoice.app.util.FVoiceLogger.init(this)
        com.fvoice.app.core.modelmanager.ModelManager.installBundledModels(this)
        processTaskManager = com.fvoice.app.core.task.ProcessTaskManager.getInstance(this)
    }
}
