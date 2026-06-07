package com.clarivo.app

import android.app.Application

class ClarivoApplication : Application() {

    companion object {
        lateinit var instance: ClarivoApplication
            private set

        lateinit var processTaskManager: com.clarivo.app.core.task.ProcessTaskManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        com.clarivo.app.util.ClarivoLogger.init(this)
        com.clarivo.app.core.modelmanager.ModelManager.installBundledModels(this)
        processTaskManager = com.clarivo.app.core.task.ProcessTaskManager.getInstance(this)
    }
}
