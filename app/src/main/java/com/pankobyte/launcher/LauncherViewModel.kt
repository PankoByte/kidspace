package com.pankobyte.launcher

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pankobyte.launcher.core.apps.AppEntry
import com.pankobyte.launcher.core.apps.DefaultApps
import com.pankobyte.launcher.core.data.Keys
import com.pankobyte.launcher.core.data.KidProfile
import com.pankobyte.launcher.core.data.LauncherSettings
import com.pankobyte.launcher.core.perms.GuardChecks
import com.pankobyte.launcher.core.perms.HomeSwitch
import com.pankobyte.launcher.core.perms.Perms
import com.pankobyte.launcher.guard.KidGuardAccessibilityService
import com.pankobyte.launcher.kid.pin.PinHasher
import com.pankobyte.launcher.kid.pin.lockoutMs
import com.pankobyte.launcher.kid.session.KidSessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Parent-side screens once first-run setup is finished. [Home] is the main screen. */
sealed interface Route {
    data object Home : Route
    /** Change which apps the child may open. */
    data object Apps : Route
    /** Change the play time. */
    data object Time : Route
    data object Settings : Route
    /** Turn the Kid mode guard (Accessibility service) on. */
    data object Guard : Route
    /** PIN check before changing the PIN. */
    data object PinGate : Route
    data object PinChange : Route
}

/** First-run setup steps, in order. The PIN is last: once it exists, setup is finished. */
object Onboard {
    const val WELCOME = 0
    const val GUARD = 1
    const val APPS = 2
    const val TIME = 3
    const val PIN = 4
    const val COUNT = 5

    // Keep stored IDs stable for installations that started the previous setup flow.
    val steps = listOf(WELCOME, APPS, TIME, GUARD, PIN)
    fun position(step: Int) = steps.indexOf(step).coerceAtLeast(0)
    fun previous(step: Int) = steps[(position(step) - 1).coerceAtLeast(0)]
}

/** Screens shown while Kid mode is on. */
sealed interface KidRoute {
    data object Home : KidRoute
    data object Pin : KidRoute
    /** After a correct PIN on the time's-up screen: more time, or leave. */
    data object ParentChoice : KidRoute
}

sealed interface PinResult {
    data object Ok : PinResult
    data object Wrong : PinResult
    data object Locked : PinResult
}

data class PinLock(val failCount: Int = 0, val lockedUntil: Long = 0)

/** How long Start waits for Android to record us as the home app after the pop-up. */
private const val HOME_WAIT_MS = 3_000L
private const val HOME_POLL_MS = 100L

/**
 * Set while the home app pop-up is open. Process-wide on purpose: when the parent picks us,
 * Android also presses Home, which opens a second copy of [MainActivity] with its own view model.
 * Either copy may come up first, so whichever does finishes the start, and only once.
 */
private val startPending = java.util.concurrent.atomic.AtomicBoolean(false)

/**
 * True when Start sent the parent to the system home settings page rather than the pop-up.
 * That page never reports what was chosen, so its answer has to be waited for.
 */
@Volatile private var startViaSettingsPage = false

/** Holds every screen's state. Navigation is plain Compose state, with no navigation library. */
@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModel : ViewModel() {
    private val c = AppContainer
    private val kidDao = c.db.kidDao()

    val settings: StateFlow<LauncherSettings?> =
        c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val apps: StateFlow<List<AppEntry>> = c.apps.apps
    val kid = c.kid.snapshot

    val pinLock: StateFlow<PinLock> = c.settings.prefs
        .map { PinLock(it[Keys.pinFailCount] ?: 0, it[Keys.pinLockedUntil] ?: 0) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PinLock())

    private val profileId = MutableStateFlow<Long?>(null)
    val profile: StateFlow<KidProfile?> = profileId.filterNotNull()
        .flatMapLatest { kidDao.observeProfile(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Packages the child may open. Null until the saved list has been read. */
    val allowed: StateFlow<Set<String>?> = profileId.filterNotNull()
        .flatMapLatest { kidDao.observeApps(it) }
        .map { list -> list.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var route by mutableStateOf<Route>(Route.Home)
    var kidRoute by mutableStateOf<KidRoute>(KidRoute.Home)

    /** The setup step picked in this run. Null until the parent moves, then the saved step is used. */
    var onboardingStep by mutableStateOf<Int?>(null)
        private set

    /** Set when the parent closed the home app pop-up without choosing us. Shown on the home screen. */
    var homeRefused by mutableStateOf(false)

    /** Bumped on every activity resume so permission state is read again. */
    var resumeTick by mutableIntStateOf(0)
        private set

    init {
        viewModelScope.launch { ensureProfile() }
        viewModelScope.launch {
            // Re-read the start checks when Android connects or drops the Kid mode guard.
            KidGuardAccessibilityService.connected.collect { resumeTick++ }
        }
        viewModelScope.launch {
            kid.collect { snapshot ->
                if (snapshot?.state == KidSessionState.OFF) kidRoute = KidRoute.Home
            }
        }
    }

    fun onResume() {
        resumeTick++
        // Picked as home while the pop-up was open: this may be the copy Android opened with Home.
        if (startPending.get() && Perms.isDefaultHome(c.appContext)) finishStart(accepted = true)
    }

    /** Home button or gesture while we are in front. Only matters in Kid mode, where we are the home app. */
    fun onHomePressed() {
        val state = kid.value?.state
        if (state == KidSessionState.OFF) return
        // Once time is up the guard itself presses Home to pull the child out of other apps.
        // That must not close the PIN screen a parent is typing on.
        if (state == KidSessionState.TIMES_UP && kidRoute == KidRoute.Pin) return
        kidRoute = KidRoute.Home
    }

    // ---- Parent side ------------------------------------------------------------------------

    private suspend fun ensureProfile() {
        if (profileId.value != null) return
        profileId.value = kidDao.firstProfile()?.id
            ?: kidDao.insertProfile(KidProfile(name = "", colour = 0, timeLimitMin = 30))
    }

    /** Moves first-run setup to [step] and saves it, so a restart carries on from there. */
    fun setOnboardingStep(step: Int) {
        onboardingStep = step
        viewModelScope.launch { c.settings.set(Keys.onboardingStep, step) }
    }

    fun createPin(pin: String) = viewModelScope.launch {
        val salt = PinHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(pin, salt) }
        c.settings.edit {
            it[Keys.pinSalt] = PinHasher.encode(salt)
            it[Keys.pinHash] = PinHasher.encode(hash)
            it[Keys.pinFailCount] = 0
            it[Keys.pinLockedUntil] = 0
        }
        route = Route.Home
    }

    /** Checks a typed PIN and keeps the wrong-try count and lock time up to date. */
    suspend fun checkPin(pin: String): PinResult {
        val prefs = c.settings.snapshot()
        if (System.currentTimeMillis() < (prefs[Keys.pinLockedUntil] ?: 0)) return PinResult.Locked
        val salt = prefs[Keys.pinSalt] ?: return PinResult.Wrong
        val hash = prefs[Keys.pinHash] ?: return PinResult.Wrong
        val ok = withContext(Dispatchers.Default) {
            PinHasher.verify(pin, PinHasher.decode(salt), PinHasher.decode(hash))
        }
        if (ok) {
            c.settings.edit {
                it[Keys.pinFailCount] = 0
                it[Keys.pinLockedUntil] = 0
            }
            return PinResult.Ok
        }
        val fails = (prefs[Keys.pinFailCount] ?: 0) + 1
        c.settings.edit {
            it[Keys.pinFailCount] = fails
            it[Keys.pinLockedUntil] = System.currentTimeMillis() + lockoutMs(fails)
        }
        return PinResult.Wrong
    }

    /** Recorded when the parent agrees to the Accessibility disclosure, before settings open. */
    fun agreeToDisclosure() = viewModelScope.launch { c.settings.set(Keys.kidDisclosureAgreed, true) }

    fun setAllowedApps(packages: Set<String>) = viewModelScope.launch {
        val id = profileId.filterNotNull().first()
        kidDao.setApps(id, packages)
    }

    fun setTimeLimit(minutes: Int) = viewModelScope.launch {
        profile.value?.let { kidDao.updateProfile(it.copy(timeLimitMin = minutes)) }
    }

    /** Optional extra: the once-a-second backup check. Needs Usage access to do anything. */
    fun setUseBackupGuard(on: Boolean) = viewModelScope.launch { c.settings.set(Keys.useBackupGuard, on) }

    /** Optional extra: silence notifications during Kid mode. Needs Do Not Disturb access to do anything. */
    fun setUseDnd(on: Boolean) = viewModelScope.launch { c.settings.set(Keys.useDnd, on) }

    // ---- Start and leave --------------------------------------------------------------------

    /**
     * First half of Start. Saves the parent's own launcher, switches our home entry on, and
     * returns the intent for the system "make this your home app" pop-up. Returns null when we
     * are already the home app, in which case Kid mode starts at once.
     */
    fun beginStart(): Intent? {
        val context = c.appContext
        if (settings.value?.hasPin != true || allowed.value.isNullOrEmpty()) return null
        if (!GuardChecks.read(context).accessibility) return null
        homeRefused = false
        if (!HomeSwitch.isEnabled(context)) {
            val previous = HomeSwitch.currentHome(context)
            viewModelScope.launch {
                c.settings.edit {
                    if (previous != null) it[Keys.prevHomePackage] = previous else it.remove(Keys.prevHomePackage)
                }
            }
            HomeSwitch.setEnabled(context, true)
        }
        if (Perms.isDefaultHome(context)) {
            startKidMode()
            return null
        }
        val intent = Perms.homeIntent(context)
        startViaSettingsPage = intent.action == Settings.ACTION_HOME_SETTINGS
        startPending.set(true)
        return intent
    }

    /**
     * Second half of Start, after the home app pop-up or the home settings page closes.
     * [accepted] is true when the pop-up reported that the parent picked us. Android can take
     * a moment to record the new home app, so wait for it briefly instead of failing at once.
     */
    fun finishStart(accepted: Boolean = false) {
        // The other copy of the screen already finished this start.
        if (!startPending.getAndSet(false)) return
        finishStartChecked(accepted)
    }

    private fun finishStartChecked(accepted: Boolean) = viewModelScope.launch {
        val context = c.appContext
        var isHome = Perms.isDefaultHome(context)
        var waited = 0L
        // The pop-up's "no" is final. Only the settings page needs time to show its answer.
        val maxWait = if (startViaSettingsPage) HOME_WAIT_MS else 0L
        while (!isHome && waited < maxWait) {
            delay(HOME_POLL_MS)
            waited += HOME_POLL_MS
            isHome = Perms.isDefaultHome(context)
        }
        if (isHome || accepted) {
            startKidMode()
        } else {
            // The parent did not pick us. Take the home entry away again so it does not linger.
            HomeSwitch.setEnabled(context, false)
            homeRefused = true
        }
    }

    /**
     * The home check was done by the caller, so only the other start checks are repeated here.
     * Reads the database directly: this may run in a view model created a moment ago, before its
     * profile and app flows have loaded.
     */
    private fun startKidMode() = viewModelScope.launch {
        val id = profileId.filterNotNull().first()
        if (!GuardChecks.read(c.appContext).accessibility) return@launch
        if (c.settings.snapshot()[Keys.pinHash] == null) return@launch
        if (kidDao.apps(id).isEmpty()) return@launch
        kidRoute = KidRoute.Home
        c.kid.start(id)
    }

    /** Ends Kid mode and hands the phone back to the parent's own launcher. Called only after a correct PIN. */
    fun leaveKidMode() = viewModelScope.launch {
        c.kid.exit()
        route = Route.Home
        val context = c.appContext
        val previous = c.settings.snapshot()[Keys.prevHomePackage]
        HomeSwitch.setEnabled(context, false)
        c.settings.edit { it.remove(Keys.prevHomePackage) }
        runCatching { context.startActivity(HomeSwitch.handBackIntent(context, previous)) }
    }

    // ---- Kid side ---------------------------------------------------------------------------

    fun launchKidApp(entry: AppEntry) {
        if (entry.packageName in (kid.value?.allowedPackages ?: emptySet())) c.apps.launch(entry)
    }

    fun addKidTime() = viewModelScope.launch {
        c.kid.extend()
        kidRoute = KidRoute.Home
    }

    /** Emergency link on the PIN screen: opens the dialer and lets it stay in front for a while. */
    fun emergencyCall() {
        c.kid.openEmergencyWindow()
        runCatching {
            c.appContext.startActivity(DefaultApps.phone().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
