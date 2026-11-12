package com.pankobyte.launcher

import android.app.Application

class LauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
