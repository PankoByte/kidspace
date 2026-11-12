package com.pankobyte.launcher.kid.session

enum class KidSessionState { OFF, RUNNING, TIMES_UP }

enum class KidEvent { START, TIME_ZERO, EXTEND, EXIT }

const val NO_LIMIT = -1L
const val EXTENSION_MS = 15 * 60_000L
const val WARNING_MS = 5 * 60_000L

/** The Kid mode state machine. An event that is not valid in the current state changes nothing. */
fun transition(state: KidSessionState, event: KidEvent): KidSessionState = when (state) {
    KidSessionState.OFF -> if (event == KidEvent.START) KidSessionState.RUNNING else state
    KidSessionState.RUNNING -> when (event) {
        KidEvent.TIME_ZERO -> KidSessionState.TIMES_UP
        KidEvent.EXIT -> KidSessionState.OFF
        else -> state
    }
    KidSessionState.TIMES_UP -> when (event) {
        KidEvent.EXTEND -> KidSessionState.RUNNING
        KidEvent.EXIT -> KidSessionState.OFF
        else -> state
    }
}

sealed interface TimerResult {
    data class Running(val remainingMs: Long) : TimerResult
    data object TimesUp : TimerResult
}

/**
 * Moves the play timer forward by [deltaMs] of device uptime.
 * [remainingMs] of [NO_LIMIT] never runs out. A negative delta is ignored.
 */
fun advance(remainingMs: Long, deltaMs: Long): TimerResult {
    if (remainingMs == NO_LIMIT) return TimerResult.Running(NO_LIMIT)
    val left = remainingMs - deltaMs.coerceAtLeast(0)
    return if (left <= 0) TimerResult.TimesUp else TimerResult.Running(left)
}

/** Whole minutes left, rounded up, for display. */
fun minutesLeft(remainingMs: Long): Int =
    if (remainingMs <= 0) 0 else ((remainingMs + 59_999) / 60_000).toInt()

/** State to restore after a restart from the two persisted values. */
fun restoredState(kidActive: Boolean, remainingMs: Long): KidSessionState = when {
    !kidActive -> KidSessionState.OFF
    remainingMs == NO_LIMIT || remainingMs > 0 -> KidSessionState.RUNNING
    else -> KidSessionState.TIMES_UP
}
