package com.pankobyte.launcher.core.apps

import android.content.Intent

/** System intents for jobs the app hands to the phone's own apps. */
object DefaultApps {
    fun phone() = Intent(Intent.ACTION_DIAL)
}
