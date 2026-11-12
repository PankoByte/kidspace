package com.pankobyte.launcher.guard

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.provider.Settings
import android.telecom.TelecomManager
import com.pankobyte.launcher.kid.session.KidSessionManager
import com.pankobyte.launcher.kid.session.KidSessionState

/** Reads the live device facts the allowlist needs and applies [decide]. Shared by both guards. */
class GuardRuntime(private val context: Context, private val kid: KidSessionManager) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val telecom = context.getSystemService(TelecomManager::class.java)

    fun evaluate(pkg: String): GuardAction {
        val snapshot = kid.snapshot.value ?: return GuardAction.ALLOW
        if (snapshot.state == KidSessionState.OFF) return GuardAction.ALLOW

        val ime = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.let { ComponentName.unflattenFromString(it)?.packageName }
        val dialer = runCatching { telecom.defaultDialerPackage }.getOrNull()
        val now = SystemClock.elapsedRealtime()
        val window = kid.emergency?.onForeground(pkg, now, dialer, setOfNotNull(ime, SYSTEM_UI_PACKAGE))
        kid.emergency = window

        val ctx = AllowContext(
            ownPackage = context.packageName,
            imePackage = ime,
            dialerPackages = IN_CALL_PACKAGES + setOfNotNull(dialer),
            defaultDialer = dialer,
            callActive = audio.mode == AudioManager.MODE_RINGTONE || audio.mode == AudioManager.MODE_IN_CALL,
            emergencyOpen = window?.isOpen(now) == true,
            allowedPackages = snapshot.allowedPackages,
        )
        return decide(pkg, snapshot.state, ctx)
    }

    private companion object {
        /** System in-call screens that are not always the default dialer. */
        val IN_CALL_PACKAGES = setOf(
            "com.android.incallui",
            "com.android.server.telecom",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.incallui",
            "com.samsung.android.dialer",
        )
    }
}
