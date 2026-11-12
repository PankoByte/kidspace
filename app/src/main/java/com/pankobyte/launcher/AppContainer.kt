package com.pankobyte.launcher

import android.content.Context
import com.pankobyte.launcher.core.apps.AppRepository
import com.pankobyte.launcher.core.data.LauncherDb
import com.pankobyte.launcher.core.data.SettingsStore
import com.pankobyte.launcher.guard.GuardRuntime
import com.pankobyte.launcher.kid.session.KidSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container. Created once in [LauncherApp]. */
object AppContainer {
    lateinit var appContext: Context
        private set
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settings: SettingsStore by lazy { SettingsStore(appContext) }
    val db: LauncherDb by lazy { LauncherDb.create(appContext) }
    val apps: AppRepository by lazy { AppRepository(appContext, scope) }
    val kid: KidSessionManager by lazy { KidSessionManager(appContext, settings, db, scope) }
    val guard: GuardRuntime by lazy { GuardRuntime(appContext, kid, apps) }

    fun init(context: Context) {
        appContext = context.applicationContext
        // Read the saved Kid mode state at once, so a restart lands on the kid home.
        kid
        // The guard needs the app list. Start reading it now, so it is ready before the first check.
        apps
    }
}
