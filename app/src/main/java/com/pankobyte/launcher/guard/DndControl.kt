package com.pankobyte.launcher.guard

import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Turns Do Not Disturb on for a Kid session (calls only) and puts the earlier setting back on exit. */
object DndControl {
    /** What Do Not Disturb looked like before the session, so it can be restored. */
    data class Saved(val filter: Int, val policy: String?)

    fun enable(context: Context): Saved? {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return null
        return runCatching {
            val old = nm.notificationPolicy
            val saved = Saved(
                filter = nm.currentInterruptionFilter,
                policy = "${old.priorityCategories},${old.priorityCallSenders},${old.priorityMessageSenders}",
            )
            // Calls ring and the child's app keeps its sound. Everything else is silent.
            var categories = NotificationManager.Policy.PRIORITY_CATEGORY_CALLS
            if (Build.VERSION.SDK_INT >= 28) {
                categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM
            }
            nm.notificationPolicy = NotificationManager.Policy(
                categories,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
            )
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            saved
        }.getOrNull()
    }

    fun restore(context: Context, saved: Saved) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return
        runCatching {
            saved.policy?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 }?.let { (cats, calls, messages) ->
                nm.notificationPolicy = NotificationManager.Policy(cats, calls, messages)
            }
            val filter = saved.filter.takeIf { it != NotificationManager.INTERRUPTION_FILTER_UNKNOWN }
                ?: NotificationManager.INTERRUPTION_FILTER_ALL
            nm.setInterruptionFilter(filter)
        }
    }
}
