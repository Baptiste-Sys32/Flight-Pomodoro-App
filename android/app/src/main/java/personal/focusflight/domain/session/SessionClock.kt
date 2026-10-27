package personal.focusflight.domain.session

/** Clock projection is shared by the service, UI, widgets and map. */
data class ClockReading(val elapsedMs: Long, val wallMs: Long, val boot: Int)

data class ClockAnchor(
    val remainingMs: Long,
    val elapsedMs: Long,
    val wallMs: Long,
    val boot: Int,
    val paused: Boolean,
)

object SessionClock {
    fun remaining(anchor: ClockAnchor, now: ClockReading): Long {
        if (anchor.paused) return anchor.remainingMs.coerceAtLeast(0)
        // Across a reboot, elapsedRealtime resets. UTC is a recovery fallback only.
        val delta = elapsed(anchor, now)
        return (anchor.remainingMs - delta).coerceIn(0, anchor.remainingMs)
    }

    private fun elapsed(anchor: ClockAnchor, now: ClockReading): Long =
        if (anchor.boot == now.boot && now.elapsedMs >= anchor.elapsedMs) {
            now.elapsedMs - anchor.elapsedMs
        } else {
            (now.wallMs - anchor.wallMs).coerceAtLeast(0)
        }

    fun completionWallTime(anchor: ClockAnchor, now: ClockReading): Long =
        now.wallMs - (elapsed(anchor, now) - anchor.remainingMs).coerceAtLeast(0)

    fun progress(durationMs: Long, remainingMs: Long): Double =
        if (durationMs <= 0) 1.0 else (1.0 - remainingMs.toDouble() / durationMs).coerceIn(0.0, 1.0)
}
