package com.pankobyte.launcher.core.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object Keys {
    val kidActive = booleanPreferencesKey("kid_active")
    val kidProfileId = longPreferencesKey("kid_profile_id")
    val kidSessionId = longPreferencesKey("kid_session_id")
    val kidRemainingMs = longPreferencesKey("kid_remaining_ms")
    val kidPrevDndFilter = intPreferencesKey("kid_prev_dnd_filter")
    val kidPrevDndPolicy = stringPreferencesKey("kid_prev_dnd_policy")
    val kidDisclosureAgreed = booleanPreferencesKey("kid_disclosure_agreed")

    /** The parent's own home app when Kid mode started, handed back when it ends. */
    val prevHomePackage = stringPreferencesKey("prev_home_package")

    /** How far the first-run setup got, so a restart carries on from the same step. */
    val onboardingStep = intPreferencesKey("onboarding_step")

    /** Optional extras, on unless the parent switched them off in Settings. */
    val useBackupGuard = booleanPreferencesKey("use_backup_guard")
    val useDnd = booleanPreferencesKey("use_dnd")

    val pinHash = stringPreferencesKey("pin_hash")
    val pinSalt = stringPreferencesKey("pin_salt")
    val pinFailCount = intPreferencesKey("pin_fail_count")
    val pinLockedUntil = longPreferencesKey("pin_locked_until")
}

/** Parent-side settings the UI observes. Kid session keys are read by the session manager. */
data class LauncherSettings(
    val hasPin: Boolean = false,
    val useBackupGuard: Boolean = true,
    val useDnd: Boolean = true,
    val onboardingStep: Int = 0,
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(context: Context) {
    private val store = context.dataStore

    val prefs: Flow<Preferences> = store.data

    val settings: Flow<LauncherSettings> = store.data.map { p ->
        LauncherSettings(
            hasPin = p[Keys.pinHash] != null,
            useBackupGuard = p[Keys.useBackupGuard] != false,
            useDnd = p[Keys.useDnd] != false,
            onboardingStep = p[Keys.onboardingStep] ?: 0,
        )
    }

    suspend fun snapshot(): Preferences = store.data.first()

    suspend fun edit(block: (MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) = edit { it[key] = value }
}
