package com.pankobyte.launcher.kid.session

import android.content.Context
import android.os.SystemClock
import com.pankobyte.launcher.core.data.Keys
import com.pankobyte.launcher.core.data.KidSession
import com.pankobyte.launcher.core.data.LauncherDb
import com.pankobyte.launcher.core.data.SettingsStore
import com.pankobyte.launcher.core.perms.HomeSwitch
import com.pankobyte.launcher.guard.DndControl
import com.pankobyte.launcher.guard.EmergencyWindow
import com.pankobyte.launcher.guard.KidSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The live Kid session. [totalMs] is the length the play-time bar is measured against. */
data class KidSnapshot(
    val state: KidSessionState = KidSessionState.OFF,
    val remainingMs: Long = NO_LIMIT,
    val totalMs: Long = NO_LIMIT,
    val profileId: Long = 0,
    val allowedPackages: Set<String> = emptySet(),
)

/**
 * Owns the Kid session state. Every transition is written to DataStore before the
 * in-memory state changes, so a restart always resumes from what was saved.
 */
class KidSessionManager(
    private val context: Context,
    private val settings: SettingsStore,
    private val db: LauncherDb,
    scope: CoroutineScope,
) {
    private val _snapshot = MutableStateFlow<KidSnapshot?>(null)

    /** Null until the saved state has been read. */
    val snapshot: StateFlow<KidSnapshot?> = _snapshot

    private val mutex = Mutex()
    private var unsavedMs = 0L

    /** Open while the Emergency link's dialer is allowed. Uses device uptime. */
    @Volatile
    var emergency: EmergencyWindow? = null

    init {
        scope.launch { mutex.withLock { restore() } }
    }

    private suspend fun restore() {
        val p = settings.snapshot()
        if (p[Keys.kidActive] != true) {
            // Kid mode is off, so we must not be a home app. Covers a crash between leaving
            // Kid mode and switching the home entry off.
            HomeSwitch.setEnabled(context, false)
            _snapshot.value = KidSnapshot()
            return
        }
        val profileId = p[Keys.kidProfileId] ?: 0
        val remaining = p[Keys.kidRemainingMs] ?: NO_LIMIT
        _snapshot.value = KidSnapshot(
            state = restoredState(true, remaining),
            remainingMs = remaining,
            totalMs = maxOf(limitMs(profileId), remaining),
            profileId = profileId,
            allowedPackages = allowedPackages(profileId),
        )
    }

    private suspend fun limitMs(profileId: Long): Long {
        val minutes = db.kidDao().profile(profileId)?.timeLimitMin ?: -1
        return if (minutes < 0) NO_LIMIT else minutes * 60_000L
    }

    private suspend fun allowedPackages(profileId: Long): Set<String> {
        return db.kidDao().apps(profileId).map { it.packageName }.toSet()
    }

    /** Starts a session. The caller has already verified the start checks. */
    suspend fun start(profileId: Long) = mutex.withLock {
        val current = _snapshot.value ?: return@withLock
        if (transition(current.state, KidEvent.START) != KidSessionState.RUNNING) return@withLock
        val limit = limitMs(profileId)
        val allowed = allowedPackages(profileId)
        val sessionId = db.kidDao().insertSession(KidSession(profileId = profileId, startedAt = System.currentTimeMillis()))
        // Do Not Disturb is an optional extra. Skipped when the parent switched it off.
        val dnd = if (settings.snapshot()[Keys.useDnd] != false) DndControl.enable(context) else null
        settings.edit {
            it[Keys.kidActive] = true
            it[Keys.kidProfileId] = profileId
            it[Keys.kidRemainingMs] = limit
            it[Keys.kidSessionId] = sessionId
            if (dnd != null) {
                it[Keys.kidPrevDndFilter] = dnd.filter
                dnd.policy?.let { policy -> it[Keys.kidPrevDndPolicy] = policy }
            } else {
                it.remove(Keys.kidPrevDndFilter)
                it.remove(Keys.kidPrevDndPolicy)
            }
        }
        unsavedMs = 0
        _snapshot.value = KidSnapshot(KidSessionState.RUNNING, limit, limit, profileId, allowed)
        KidSessionService.start(context)
    }

    /** Leaves Kid mode. Called only after a correct PIN. */
    suspend fun exit() = mutex.withLock {
        val current = _snapshot.value ?: return@withLock
        if (current.state == KidSessionState.OFF) return@withLock
        val p = settings.snapshot()
        settings.edit {
            it[Keys.kidActive] = false
            it.remove(Keys.kidRemainingMs)
            it.remove(Keys.kidSessionId)
            it.remove(Keys.kidPrevDndFilter)
            it.remove(Keys.kidPrevDndPolicy)
        }
        p[Keys.kidSessionId]?.let { db.kidDao().endSession(it, System.currentTimeMillis()) }
        p[Keys.kidPrevDndFilter]?.let { DndControl.restore(context, DndControl.Saved(it, p[Keys.kidPrevDndPolicy])) }
        emergency = null
        _snapshot.value = KidSnapshot()
        KidSessionService.stop(context)
    }

    /** Adds 15 minutes after time ran out. Called only after a correct PIN. */
    suspend fun extend() = mutex.withLock {
        val current = _snapshot.value ?: return@withLock
        if (transition(current.state, KidEvent.EXTEND) != KidSessionState.RUNNING) return@withLock
        settings.set(Keys.kidRemainingMs, EXTENSION_MS)
        unsavedMs = 0
        _snapshot.value = current.copy(state = KidSessionState.RUNNING, remainingMs = EXTENSION_MS, totalMs = EXTENSION_MS)
    }

    /** Advances the timer by [deltaMs] of device uptime. Returns true when time just ran out. */
    suspend fun tick(deltaMs: Long): Boolean = mutex.withLock {
        val current = _snapshot.value ?: return@withLock false
        if (current.state != KidSessionState.RUNNING || current.remainingMs == NO_LIMIT) return@withLock false
        when (val result = advance(current.remainingMs, deltaMs)) {
            is TimerResult.Running -> {
                unsavedMs += deltaMs
                if (unsavedMs >= SAVE_EVERY_MS) {
                    settings.set(Keys.kidRemainingMs, result.remainingMs)
                    unsavedMs = 0
                }
                _snapshot.value = current.copy(remainingMs = result.remainingMs)
                false
            }
            TimerResult.TimesUp -> {
                settings.set(Keys.kidRemainingMs, 0L)
                unsavedMs = 0
                _snapshot.value = current.copy(state = transition(current.state, KidEvent.TIME_ZERO), remainingMs = 0)
                true
            }
        }
    }

    /** Saves the time left right now, for when the timer is about to pause. */
    suspend fun saveNow() = mutex.withLock {
        val current = _snapshot.value ?: return@withLock
        if (current.state == KidSessionState.RUNNING && current.remainingMs != NO_LIMIT) {
            settings.set(Keys.kidRemainingMs, current.remainingMs)
            unsavedMs = 0
        }
    }

    fun openEmergencyWindow() {
        emergency = EmergencyWindow(openedAtMs = SystemClock.elapsedRealtime())
    }

    private companion object {
        const val SAVE_EVERY_MS = 30_000L
    }
}
