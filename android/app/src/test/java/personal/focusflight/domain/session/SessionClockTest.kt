package personal.focusflight.domain.session

import org.junit.Assert.*
import org.junit.Test

class SessionClockTest {
    private val active = ClockAnchor(60_000, 100_000, 1_000_000, 7, false)
    @Test fun deepSleepUsesMonotonicElapsedTime() {
        assertEquals(15_000, SessionClock.remaining(active, ClockReading(145_000, 1_045_000, 7)))
    }
    @Test fun changingWallClockDoesNotChangeSameBootCountdown() {
        assertEquals(30_000, SessionClock.remaining(active, ClockReading(130_000, 8_000_000, 7)))
        assertEquals(30_000, SessionClock.remaining(active, ClockReading(130_000, 1, 7)))
    }
    @Test fun pauseDoesNotEarnTimeDuringSleepOrReboot() {
        assertEquals(60_000, SessionClock.remaining(active.copy(paused = true), ClockReading(1, 8_000_000, 8)))
    }
    @Test fun rebootUsesUtcRecoveryAndNeverCreatesNegativeRemainingTime() {
        assertEquals(40_000, SessionClock.remaining(active, ClockReading(2000, 1_020_000, 8)))
        assertEquals(0, SessionClock.remaining(active, ClockReading(2000, 1_080_000, 8)))
        assertEquals(60_000, SessionClock.remaining(active, ClockReading(2000, 900_000, 8)))
    }
    @Test fun completionAndProgressClampAtDeadline() {
        assertEquals(0, SessionClock.remaining(active, ClockReading(900_000, 0, 7)))
        assertEquals(1.0, SessionClock.progress(60_000, 0), 0.00001)
        assertEquals(0.0, SessionClock.progress(60_000, 90_000), 0.00001)
    }
    @Test fun recoveryRecordsDeadlineRatherThanDeliveryTime() {
        assertEquals(1_060_000L, SessionClock.completionWallTime(active, ClockReading(900_000, 1_800_000, 7)))
        assertEquals(1_060_000L, SessionClock.completionWallTime(active, ClockReading(2000, 1_800_000, 8)))
    }
}
