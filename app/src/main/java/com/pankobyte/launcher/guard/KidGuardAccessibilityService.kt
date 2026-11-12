package com.pankobyte.launcher.guard

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.pankobyte.launcher.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Primary Kid mode guard. Sees which app's window came to the front and sends anything
 * that is not allowed back to the kid home. It never reads window content.
 */
class KidGuardAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
        _connected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return
        val pkg = event.packageName?.toString() ?: return
        // Overlays from apps running in the background are not the app in front. System UI is the
        // exception: its shade is an overlay and still has to be closed.
        if (pkg != SYSTEM_UI_PACKAGE && isOverlayWindow(event.className?.toString())) return
        when (AppContainer.guard.evaluate(pkg)) {
            GuardAction.ALLOW -> Unit
            GuardAction.GO_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            GuardAction.DISMISS_SHADE -> handleSystemUi()
        }
    }

    /**
     * System UI raises events for the notification shade, the volume panel, the status bar and
     * the back gesture. Only the shade must close. Sending Home or Back here would throw the
     * child out of their app whenever they press a volume key or go back one screen.
     */
    // The broadcast needs no permission below Android 12, the only place it is sent.
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun handleSystemUi() {
        if (Build.VERSION.SDK_INT >= 31) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            // Before Android 12 this broadcast closes the shade and leaves the app in front alone.
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        disconnected()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnected()
        super.onDestroy()
    }

    private fun disconnected() {
        if (instance === this) {
            instance = null
            _connected.value = false
        }
    }

    companion object {
        @Volatile
        var instance: KidGuardAccessibilityService? = null
            private set

        private val _connected = MutableStateFlow(false)

        /** True while Android has the service running and is sending it events. */
        val connected: StateFlow<Boolean> = _connected

        /** Sends the phone to the home screen through the service. False when the service is not running. */
        fun goHome(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
    }
}
