package com.pankobyte.launcher.guard

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.provider.Settings
import android.telecom.TelecomManager
import com.pankobyte.launcher.core.apps.AppEntry
import com.pankobyte.launcher.core.apps.AppRepository
import com.pankobyte.launcher.kid.session.KidSessionManager
import com.pankobyte.launcher.kid.session.KidSessionState

/** Reads the live device facts the allowlist needs and applies [decide]. Shared by both guards. */
class GuardRuntime(
    private val context: Context,
    private val kid: KidSessionManager,
    private val apps: AppRepository,
) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val telecom = context.getSystemService(TelecomManager::class.java)

    // The icon set is rebuilt only when the app list itself is replaced.
    @Volatile private var iconSource: List<AppEntry>? = null
    @Volatile private var iconSet: Set<String> = emptySet()

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
            iconPackages = iconPackages(),
        )
        return decide(pkg, snapshot.state, ctx)
    }

    /** Packages with an app drawer icon, or null until the app list has been read once. */
    private fun iconPackages(): Set<String>? {
        if (!apps.loaded.value) return null
        val list = apps.apps.value
        if (list !== iconSource) {
            iconSet = list.mapTo(HashSet()) { it.packageName }
            iconSource = list
        }
        return iconSet
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
