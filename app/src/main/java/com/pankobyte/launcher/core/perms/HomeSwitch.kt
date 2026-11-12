package com.pankobyte.launcher.core.perms

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * The app is a home app only while Kid mode runs. The HOME entry lives on the `KidHome`
 * activity-alias in the manifest, which starts switched off. Starting Kid mode switches it on
 * and asks Android to make us the default home. Leaving Kid mode switches it off again, which
 * takes us out of the home app list, and hands the phone back to the parent's own launcher.
 */
object HomeSwitch {
    private fun alias(context: Context) = ComponentName(context, "com.pankobyte.launcher.KidHome")

    fun setEnabled(context: Context, on: Boolean) {
        val state = if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        runCatching {
            context.packageManager.setComponentEnabledSetting(alias(context), state, PackageManager.DONT_KILL_APP)
        }
    }

    fun isEnabled(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(alias(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    /**
     * Package of the current default home app, or null when there is none and Android would
     * ask. Android answers "no default" with its own chooser screen, which lives in "android".
     */
    fun currentHome(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val pkg = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return pkg?.takeIf { it != "android" }
    }

    /**
     * Where to send the parent after Kid mode ends and the alias is off. When Android has
     * already fallen back to the launcher they had before, go straight to it. Otherwise open
     * the system "Default home app" page so they can pick it. Android has no pop-up that lets
     * one app make a different app the default home.
     */
    fun handBackIntent(context: Context, previous: String?): Intent {
        val current = currentHome(context)
        val intent = if (previous != null && previous != context.packageName && current == previous) {
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        } else {
            Intent(Settings.ACTION_HOME_SETTINGS)
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
