package com.pankobyte.launcher.core.perms

import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.pankobyte.launcher.guard.KidGuardAccessibilityService

/** Permission state checks and the intents that open the matching system settings page. */
object Perms {
    /**
     * On Android 10 and up the home role is the source of truth. It changes the moment the
     * parent picks us in the pop-up, while the resolved HOME activity can lag behind it.
     */
    fun isDefaultHome(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && roles.isRoleHeld(RoleManager.ROLE_HOME)) return true
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, 0)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun isAccessibilityOn(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val mine = ComponentName(context, KidGuardAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }

    @Suppress("DEPRECATION")
    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasDndAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    /**
     * Intent for the system "make this your home app" pop-up, or the home settings page on
     * phones older than Android 10 and when Android will not show the pop-up any more.
     */
    fun homeIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun accessibilityIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    fun usageAccessIntent() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    fun dndIntent() = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}

/**
 * What the phone currently lets Kid mode do. Only [accessibility] is needed to start. The home
 * app is asked for with a system pop-up when Start is tapped. [usageAccess] and [dndAccess] are
 * optional extras the parent switches on in Settings.
 */
data class GuardChecks(
    val defaultHome: Boolean,
    val accessibility: Boolean,
    val usageAccess: Boolean,
    val dndAccess: Boolean,
) {
    companion object {
        fun read(context: Context) = GuardChecks(
            defaultHome = Perms.isDefaultHome(context),
            // Switched on in settings is not enough: after an app update Android can leave the
            // service unbound until it is switched off and on again.
            accessibility = Perms.isAccessibilityOn(context) && KidGuardAccessibilityService.connected.value,
            usageAccess = Perms.hasUsageAccess(context),
            dndAccess = Perms.hasDndAccess(context),
        )
    }
}
