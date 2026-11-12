package com.pankobyte.launcher.kid

import com.pankobyte.launcher.kid.pin.MASTER_PIN
import com.pankobyte.launcher.kid.pin.PIN_LENGTH
import com.pankobyte.launcher.kid.pin.PinHasher
import com.pankobyte.launcher.kid.pin.isMasterPin
import com.pankobyte.launcher.kid.pin.lockoutMs
import com.pankobyte.launcher.kid.session.KidEvent
import com.pankobyte.launcher.kid.session.KidSessionState.OFF
import com.pankobyte.launcher.kid.session.KidSessionState.RUNNING
import com.pankobyte.launcher.kid.session.KidSessionState.TIMES_UP
import com.pankobyte.launcher.kid.session.NO_LIMIT
import com.pankobyte.launcher.kid.session.TimerResult
import com.pankobyte.launcher.kid.session.advance
import com.pankobyte.launcher.kid.session.minutesLeft
import com.pankobyte.launcher.kid.session.restoredState
import com.pankobyte.launcher.kid.session.transition
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidLogicTest {
    @Test fun stateMachineFollowsTheDiagram() {
        assertEquals(RUNNING, transition(OFF, KidEvent.START))
        assertEquals(TIMES_UP, transition(RUNNING, KidEvent.TIME_ZERO))
        assertEquals(OFF, transition(RUNNING, KidEvent.EXIT))
        assertEquals(RUNNING, transition(TIMES_UP, KidEvent.EXTEND))
        assertEquals(OFF, transition(TIMES_UP, KidEvent.EXIT))
    }

    @Test fun invalidEventsChangeNothing() {
        assertEquals(OFF, transition(OFF, KidEvent.EXIT))
        assertEquals(OFF, transition(OFF, KidEvent.TIME_ZERO))
        assertEquals(OFF, transition(OFF, KidEvent.EXTEND))
        assertEquals(RUNNING, transition(RUNNING, KidEvent.START))
        assertEquals(RUNNING, transition(RUNNING, KidEvent.EXTEND))
        assertEquals(TIMES_UP, transition(TIMES_UP, KidEvent.START))
        assertEquals(TIMES_UP, transition(TIMES_UP, KidEvent.TIME_ZERO))
    }

    @Test fun advanceCountsDown() {
        assertEquals(TimerResult.Running(59_000), advance(60_000, 1_000))
    }

    @Test fun advanceReachesTimesUpAtZeroAndBelow() {
        assertEquals(TimerResult.TimesUp, advance(1_000, 1_000))
        assertEquals(TimerResult.TimesUp, advance(1_000, 5_000))
        assertEquals(TimerResult.TimesUp, advance(0, 0))
    }

    @Test fun advanceNeverEndsWithNoLimit() {
        assertEquals(TimerResult.Running(NO_LIMIT), advance(NO_LIMIT, 10_000_000))
    }

    @Test fun advanceIgnoresNegativeDelta() {
        assertEquals(TimerResult.Running(60_000), advance(60_000, -5_000))
    }

    @Test fun minutesRoundUp() {
        assertEquals(0, minutesLeft(0))
        assertEquals(1, minutesLeft(1))
        assertEquals(1, minutesLeft(60_000))
        assertEquals(2, minutesLeft(60_001))
        assertEquals(15, minutesLeft(15 * 60_000))
    }

    @Test fun restoredStateAfterRestart() {
        assertEquals(OFF, restoredState(false, 60_000))
        assertEquals(RUNNING, restoredState(true, 60_000))
        assertEquals(RUNNING, restoredState(true, NO_LIMIT))
        assertEquals(TIMES_UP, restoredState(true, 0))
    }

    @Test fun firstFourWrongTriesDoNotLock() {
        for (n in 0..4) assertEquals(0, lockoutMs(n))
    }

    @Test fun lockDoublesFromThirtySecondsUpToFifteenMinutes() {
        assertEquals(30_000, lockoutMs(5))
        assertEquals(60_000, lockoutMs(6))
        assertEquals(120_000, lockoutMs(7))
        assertEquals(240_000, lockoutMs(8))
        assertEquals(480_000, lockoutMs(9))
        assertEquals(900_000, lockoutMs(10))
        assertEquals(900_000, lockoutMs(11))
        assertEquals(900_000, lockoutMs(500))
    }

    @Test fun pinHashVerifiesOnlyTheRightPin() {
        val salt = ByteArray(16) { it.toByte() }
        val hash = PinHasher.hash("1234", salt)
        assertEquals(32, hash.size)
        assertTrue(PinHasher.verify("1234", salt, hash))
        assertFalse(PinHasher.verify("1235", salt, hash))
        assertFalse(PinHasher.verify("1234", ByteArray(16), hash))
    }

    @Test fun masterPinIsFourDigitsAndOnlyItMatches() {
        assertEquals(PIN_LENGTH, MASTER_PIN.length)
        assertTrue(MASTER_PIN.all { it.isDigit() })
        assertTrue(isMasterPin(MASTER_PIN))
        assertFalse(isMasterPin("0000"))
        assertFalse(isMasterPin(""))
    }

    @Test fun pinHashSurvivesBase64RoundTrip() {
        val salt = PinHasher.newSalt()
        assertEquals(16, salt.size)
        val hash = PinHasher.hash("0000", salt)
        assertArrayEquals(hash, PinHasher.decode(PinHasher.encode(hash)))
    }
}
