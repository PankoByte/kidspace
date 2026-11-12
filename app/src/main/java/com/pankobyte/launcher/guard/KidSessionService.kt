package com.pankobyte.launcher.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import com.pankobyte.launcher.AppContainer
import com.pankobyte.launcher.MainActivity
import com.pankobyte.launcher.R
import com.pankobyte.launcher.core.data.Keys
import com.pankobyte.launcher.core.perms.Perms
import com.pankobyte.launcher.kid.session.KidSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs for the length of a Kid session. Owns the play timer and, when the parent switched the
 * backup guard on, reads the app in front from Usage Access once a second and applies the allowlist.
 */
class KidSessionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private var lastPackage: String? = null
    private var lastQueryMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
        running = true
        if (!started) {
            started = true
            scope.launch { loop() }
        }
        return START_STICKY
    }

    private suspend fun loop() {
        val kid = AppContainer.kid
        // The backup guard is an optional extra. It can only change outside Kid mode, so read it once.
        val useBackup = AppContainer.settings.snapshot()[Keys.useBackupGuard] != false
        var last = SystemClock.elapsedRealtime()
        while (scope.isActive) {
            delay(TICK_MS)
            val snapshot = kid.snapshot.value ?: continue
            if (snapshot.state == KidSessionState.OFF) break
            val now = SystemClock.elapsedRealtime()
            val timeJustRanOut = kid.tick(now - last)
            last = now
            backupGuard(useBackup, force = timeJustRanOut)
        }
        stopSelf()
    }

    /** Applies the allowlist to the app Usage Access reports in front. */
    private fun backupGuard(enabled: Boolean, force: Boolean) {
        val pkg = if (enabled) foregroundPackage() else null
        if (pkg == null) {
            // Without the backup guard we cannot tell what is in front. When time runs out, go home anyway.
            if (force) goHome()
            return
        }
        if (AppContainer.guard.evaluate(pkg) == GuardAction.GO_HOME) goHome()
    }

    private fun foregroundPackage(): String? {
        if (!Perms.hasUsageAccess(this)) return null
        val usage = getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val from = if (lastQueryMs == 0L) now - FIRST_LOOKBACK_MS else lastQueryMs - 500
        lastQueryMs = now
        val events = runCatching { usage.queryEvents(from, now) }.getOrNull() ?: return lastPackage
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) lastPackage = event.packageName
        }
        return lastPackage
    }

    private fun goHome() {
        if (KidGuardAccessibilityService.goHome()) return
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        runCatching { startActivity(intent) }
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.kid_notification_channel), NotificationManager.IMPORTANCE_LOW),
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kid_notification)
            .setContentTitle(getString(R.string.kid_notification_title))
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        running = false
        scope.launch(Dispatchers.Main.immediate) { AppContainer.kid.saveNow() }.invokeOnCompletion { scope.cancel() }
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "kid_session"
        private const val NOTIFICATION_ID = 1
        private const val TICK_MS = 1_000L
        private const val FIRST_LOOKBACK_MS = 60_000L

        @Volatile
        var running = false
            private set

        fun start(context: Context) {
            runCatching { context.startForegroundService(Intent(context, KidSessionService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KidSessionService::class.java))
        }
    }
}
