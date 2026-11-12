package com.pankobyte.launcher.guard

import com.pankobyte.launcher.kid.session.KidSessionState.OFF
import com.pankobyte.launcher.kid.session.KidSessionState.RUNNING
import com.pankobyte.launcher.kid.session.KidSessionState.TIMES_UP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardLogicTest {
    private val own = "com.pankobyte.kidspace"
    private val ime = "com.keyboard"
    private val dialer = "com.dialer"
    private val inCall = "com.android.incallui"
    private val game = "com.game"
    private val settings = "com.android.settings"
    private val chrome = "com.android.chrome"
    private val permissions = "com.google.android.permissioncontroller"
    private val gameTools = "com.samsung.android.game.gametools"

    private val ctx = AllowContext(
        ownPackage = own,
        imePackage = ime,
        dialerPackages = setOf(dialer, inCall),
        defaultDialer = dialer,
        callActive = false,
        emergencyOpen = false,
        allowedPackages = setOf(game),
        iconPackages = setOf(game, settings, dialer, chrome),
    )

    @Test fun rule1_offAllowsEverything() {
        assertEquals(GuardAction.ALLOW, decide(settings, OFF, ctx))
        assertEquals(GuardAction.ALLOW, decide(SYSTEM_UI_PACKAGE, OFF, ctx))
    }

    @Test fun rule2_ownPackageAllowedInEveryState() {
        assertEquals(GuardAction.ALLOW, decide(own, RUNNING, ctx))
        assertEquals(GuardAction.ALLOW, decide(own, TIMES_UP, ctx))
    }

    @Test fun rule3_keyboardAllowedInEveryState() {
        assertEquals(GuardAction.ALLOW, decide(ime, RUNNING, ctx))
        assertEquals(GuardAction.ALLOW, decide(ime, TIMES_UP, ctx))
    }

    @Test fun rule4_callScreenAllowedOnlyDuringACall() {
        val calling = ctx.copy(callActive = true)
        assertEquals(GuardAction.ALLOW, decide(dialer, RUNNING, calling))
        assertEquals(GuardAction.ALLOW, decide(inCall, RUNNING, calling))
        assertEquals(GuardAction.ALLOW, decide(inCall, TIMES_UP, calling))
        assertEquals(GuardAction.GO_HOME, decide(dialer, RUNNING, ctx))
        assertEquals(GuardAction.GO_HOME, decide(inCall, RUNNING, ctx))
    }

    @Test fun rule5_emergencyWindowAllowsOnlyTheDefaultDialer() {
        val emergency = ctx.copy(emergencyOpen = true)
        assertEquals(GuardAction.ALLOW, decide(dialer, RUNNING, emergency))
        assertEquals(GuardAction.ALLOW, decide(dialer, TIMES_UP, emergency))
        assertEquals(GuardAction.GO_HOME, decide(inCall, RUNNING, emergency))
        assertEquals(GuardAction.GO_HOME, decide(settings, RUNNING, emergency))
    }

    @Test fun rule6_timesUpSendsEverythingElseHome() {
        assertEquals(GuardAction.GO_HOME, decide(game, TIMES_UP, ctx))
        assertEquals(GuardAction.DISMISS_SHADE, decide(SYSTEM_UI_PACKAGE, TIMES_UP, ctx))
        assertEquals(GuardAction.GO_HOME, decide(settings, TIMES_UP, ctx))
    }

    @Test fun rule7_systemUiDismissesTheShadeAndNeverGoesHome() {
        assertEquals(GuardAction.DISMISS_SHADE, decide(SYSTEM_UI_PACKAGE, RUNNING, ctx))
    }

    @Test fun rule8_approvedAppAllowed() {
        assertEquals(GuardAction.ALLOW, decide(game, RUNNING, ctx))
    }

    @Test fun rule9_appWithAnIconThatIsNotAllowedGoesHome() {
        assertEquals(GuardAction.GO_HOME, decide(settings, RUNNING, ctx))
        assertEquals(GuardAction.GO_HOME, decide(chrome, RUNNING, ctx))
    }

    @Test fun rule10_alwaysBlockedGoesHomeEvenWithoutAnIcon() {
        val noIcons = ctx.copy(iconPackages = emptySet())
        assertEquals(GuardAction.GO_HOME, decide("com.android.vending", RUNNING, noIcons))
        assertEquals(GuardAction.GO_HOME, decide("com.google.android.packageinstaller", RUNNING, noIcons))
        assertEquals(GuardAction.GO_HOME, decide(settings, RUNNING, noIcons))
    }

    @Test fun rule11_partsOfThePhoneWithoutAnIconAreLeftAlone() {
        // Permission pop-up, Google sign-in, crash dialog, a brand's game helper.
        assertEquals(GuardAction.ALLOW, decide(permissions, RUNNING, ctx))
        assertEquals(GuardAction.ALLOW, decide("com.google.android.gms", RUNNING, ctx))
        assertEquals(GuardAction.ALLOW, decide("android", RUNNING, ctx))
        assertEquals(GuardAction.ALLOW, decide(gameTools, RUNNING, ctx))
    }

    @Test fun timesUpStillSendsIconlessPartsHome() {
        assertEquals(GuardAction.GO_HOME, decide(permissions, TIMES_UP, ctx))
    }

    @Test fun everythingIsBlockedUntilTheAppListIsKnown() {
        val loading = ctx.copy(iconPackages = null)
        assertEquals(GuardAction.GO_HOME, decide(permissions, RUNNING, loading))
        assertEquals(GuardAction.GO_HOME, decide(gameTools, RUNNING, loading))
        assertEquals(GuardAction.ALLOW, decide(game, RUNNING, loading))
    }

    @Test fun noKeyboardSetDoesNotAllowAnything() {
        assertEquals(GuardAction.GO_HOME, decide(settings, RUNNING, ctx.copy(imePackage = null)))
    }

    @Test fun emergencyWindowClosesAfterDialerLeaves() {
        val pass = setOf(ime)
        var w: EmergencyWindow? = EmergencyWindow(openedAtMs = 0)
        // Our own PIN screen is still in front before the dialer appears.
        w = w!!.onForeground(own, 100, dialer, pass)
        assertTrue(w!!.isOpen(100))
        w = w.onForeground(dialer, 500, dialer, pass)
        assertTrue(w!!.dialerSeen)
        w = w.onForeground(ime, 800, dialer, pass)
        assertTrue(w!!.dialerSeen)
        // Any other app after the dialer closes the window, our own home screen included.
        assertNull(w.onForeground(settings, 1_000, dialer, pass))
        assertNull(w.onForeground(own, 1_000, dialer, pass))
    }

    @Test fun overlaysAreToldApartFromAppScreens() {
        // Samsung edge panel, Game Tools and gesture handles report plain view classes.
        assertTrue(isOverlayWindow("android.widget.FrameLayout"))
        assertTrue(isOverlayWindow("android.view.ViewGroup"))
        // App screens and dialogs are judged by the allowlist. So is anything unknown.
        assertFalse(isOverlayWindow("com.unity3d.player.UnityPlayerActivity"))
        assertFalse(isOverlayWindow("android.app.AlertDialog"))
        assertFalse(isOverlayWindow(null))
    }

    @Test fun emergencyWindowHasThreeMinuteCap() {
        val w = EmergencyWindow(openedAtMs = 1_000)
        assertTrue(w.isOpen(1_000))
        assertTrue(w.isOpen(1_000 + EMERGENCY_CAP_MS - 1))
        assertFalse(w.isOpen(1_000 + EMERGENCY_CAP_MS))
        assertNull(w.onForeground(dialer, 1_000 + EMERGENCY_CAP_MS, dialer, emptySet()))
    }
}
