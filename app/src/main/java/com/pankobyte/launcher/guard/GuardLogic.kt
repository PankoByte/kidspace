package com.pankobyte.launcher.guard

import com.pankobyte.launcher.kid.session.KidSessionState

enum class GuardAction { ALLOW, GO_HOME, DISMISS_SHADE }

const val SYSTEM_UI_PACKAGE = "com.android.systemui"

/** Everything the allowlist decision needs besides the package and the session state. */
data class AllowContext(
    val ownPackage: String,
    val imePackage: String?,
    /** Default dialer plus the system in-call screens. */
    val dialerPackages: Set<String>,
    val defaultDialer: String?,
    val callActive: Boolean,
    val emergencyOpen: Boolean,
    /** Apps the parent allowed for the active profile. */
    val allowedPackages: Set<String>,
)

/** Decides what the guard does when [pkg] comes to the front. First matching rule wins. */
fun decide(pkg: String, state: KidSessionState, ctx: AllowContext): GuardAction = when {
    state == KidSessionState.OFF -> GuardAction.ALLOW
    pkg == ctx.ownPackage -> GuardAction.ALLOW
    ctx.imePackage != null && pkg == ctx.imePackage -> GuardAction.ALLOW
    ctx.callActive && pkg in ctx.dialerPackages -> GuardAction.ALLOW
    ctx.emergencyOpen && ctx.defaultDialer != null && pkg == ctx.defaultDialer -> GuardAction.ALLOW
    // System UI (status bar, volume panel, shade) only ever has its shade closed. Pressing Home
    // for it would also close the PIN screen a parent just opened from the time's-up screen.
    pkg == SYSTEM_UI_PACKAGE -> GuardAction.DISMISS_SHADE
    state == KidSessionState.TIMES_UP -> GuardAction.GO_HOME
    pkg in ctx.allowedPackages -> GuardAction.ALLOW
    else -> GuardAction.GO_HOME
}

const val EMERGENCY_CAP_MS = 3 * 60_000L

/**
 * The emergency window opened from the PIN screen.
 * It stays open until the dialer has been seen in front and then left, or [EMERGENCY_CAP_MS] passes.
 */
data class EmergencyWindow(val openedAtMs: Long, val dialerSeen: Boolean = false) {
    fun isOpen(nowMs: Long): Boolean = nowMs - openedAtMs in 0 until EMERGENCY_CAP_MS

    /** Returns the window after [pkg] came to the front, or null when the window has closed. */
    fun onForeground(pkg: String, nowMs: Long, defaultDialer: String?, passThrough: Set<String>): EmergencyWindow? = when {
        !isOpen(nowMs) -> null
        pkg == defaultDialer -> copy(dialerSeen = true)
        pkg in passThrough -> this
        dialerSeen -> null
        else -> this
    }
}
