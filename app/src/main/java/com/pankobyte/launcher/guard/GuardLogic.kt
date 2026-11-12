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
    /**
     * Every app that has an icon in the app drawer, which covers all apps the user installed.
     * Null while the list is still loading: until then anything not allowed is blocked.
     */
    val iconPackages: Set<String>?,
)

/**
 * Apps that are blocked even though they have no icon in the app drawer, because a child could
 * install, delete or change things from them. Checked on real devices as they are tested.
 */
val ALWAYS_BLOCKED_PACKAGES = setOf(
    // Installers: adding or removing apps.
    "com.android.packageinstaller",
    "com.google.android.packageinstaller",
    "com.samsung.android.packageinstaller",
    "com.miui.packageinstaller",
    // Settings and its search screen.
    "com.android.settings",
    "com.android.settings.intelligence",
    "com.google.android.settings.intelligence",
    // Stores.
    "com.android.vending",
    "com.sec.android.app.samsungapps",
    // File browser: files can be deleted from it.
    "com.android.documentsui",
    "com.google.android.documentsui",
    // Voice assistant.
    "com.samsung.android.bixby.agent",
)

/**
 * Decides what the guard does when [pkg] comes to the front. First matching rule wins.
 *
 * An app with an icon that the parent did not allow is sent home. Parts of the phone without an
 * icon (permission pop-ups, Google sign-in, crash dialogs, each brand's game and gesture helpers)
 * are left alone, so an allowed app keeps working on every brand. [ALWAYS_BLOCKED_PACKAGES]
 * lists the icon-less parts that are still blocked.
 */
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
    // The call screen has no icon on some phones. With no call going on it stays blocked.
    pkg in ctx.dialerPackages -> GuardAction.GO_HOME
    pkg in ALWAYS_BLOCKED_PACKAGES -> GuardAction.GO_HOME
    ctx.iconPackages == null || pkg in ctx.iconPackages -> GuardAction.GO_HOME
    else -> GuardAction.ALLOW
}

/**
 * True when a window event comes from a floating overlay and not from an app screen.
 *
 * An app screen reports its Activity class. Overlays drawn over the app in front (Samsung edge
 * panel, Game Tools, side gesture handles, Samsung Pay swipe handle) report a plain framework
 * view class. They do not put their app in front, so the allowlist must not judge them.
 * Unknown or missing class names count as an app screen, which keeps the guard strict.
 */
fun isOverlayWindow(className: String?): Boolean =
    className != null && (className.startsWith("android.widget.") || className.startsWith("android.view."))

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
