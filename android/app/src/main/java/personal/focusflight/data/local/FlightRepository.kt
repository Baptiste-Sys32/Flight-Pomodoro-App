package personal.focusflight.data.local

import androidx.room.withTransaction
import personal.focusflight.domain.session.ClockReading
import personal.focusflight.domain.session.SessionClock
import java.util.UUID

data class Airport(val code: String, val city: String, val lat: Double, val lon: Double)

class FlightRepository(private val db: FlightDatabase, private val clock: () -> ClockReading) {
    val flights = db.flights().observeAll()
    val drafts = db.drafts().observeAll()
    suspend fun ongoing() = db.flights().ongoing()

    suspend fun start(from: Airport, to: Airport, durationMs: Long, distanceMeters: Long,
                      category: String, seat: String, random: Boolean = false, diagnostic: Boolean = false): FlightSession = db.withTransaction {
        require(durationMs > 0)
        require(distanceMeters >= 0)
        check(db.flights().ongoing() == null) { "A flight is already in progress" }
        val now = clock()
        val s = FlightSession(UUID.randomUUID().toString(), from.code, from.city, from.lat, from.lon,
            to.code, to.city, to.lat, to.lon, durationMs, distanceMeters, category, seat, random,
            remainingAtAnchorMs = durationMs, anchorElapsedMs = now.elapsedMs,
            anchorWallMs = now.wallMs, anchorBoot = now.boot, startedWallMs = now.wallMs, diagnostic = diagnostic)
        db.flights().insert(s)
        s
    }

    suspend fun saveDraft(draft: FlightDraft) = db.withTransaction {
        require(draft.durationMs in 15 * 60_000L..180 * 60_000L)
        require(draft.distanceMeters >= 0 && draft.departureCode != draft.arrivalCode)
        check(db.flights().get(draft.id)==null) { "This boarding pass has already been used" }
        db.drafts().save(draft)
    }

    suspend fun deleteDraft(id: String) = db.drafts().delete(id)

    /** Convert planning to focus exactly once. The visual takeoff does not own the clock. */
    suspend fun startDraft(id: String): FlightSession = db.withTransaction {
        db.flights().get(id)?.let { existing ->
            check(existing.isOngoing) { "This boarding pass has already been used" }
            return@withTransaction existing
        }
        check(db.flights().ongoing() == null) { "A flight is already in progress" }
        val d = checkNotNull(db.drafts().get(id)) { "Boarding pass not found" }
        val now = clock()
        val s = FlightSession(d.id, d.departureCode, d.departureCity, d.departureLat, d.departureLon,
            d.arrivalCode, d.arrivalCity, d.arrivalLat, d.arrivalLon, d.durationMs, d.distanceMeters,
            d.category, d.seat, d.randomRoute, remainingAtAnchorMs = d.durationMs,
            anchorElapsedMs = now.elapsedMs, anchorWallMs = now.wallMs, anchorBoot = now.boot,
            startedWallMs = now.wallMs)
        db.flights().insert(s)
        db.drafts().delete(id)
        s
    }

    suspend fun setPaused(paused: Boolean): FlightSession? = db.withTransaction {
        val s = db.flights().ongoing() ?: return@withTransaction null
        if ((s.state == "PAUSED") == paused) return@withTransaction s
        val now = clock()
        val remaining = SessionClock.remaining(s.anchor(), now)
        if (remaining == 0L) return@withTransaction completeLocked(s, now)
        s.copy(state = if (paused) "PAUSED" else "ACTIVE", remainingAtAnchorMs = remaining,
            anchorElapsedMs = now.elapsedMs, anchorWallMs = now.wallMs, anchorBoot = now.boot)
            .also { db.flights().update(it) }
    }

    /** Returns only a newly completed flight, so competing callbacks cannot award twice. */
    suspend fun completeDue(id: String? = null): FlightSession? = db.withTransaction {
        val s = if (id == null) db.flights().ongoing() else db.flights().get(id)
        if (s == null || s.state != "ACTIVE") return@withTransaction null
        val now = clock()
        if (SessionClock.remaining(s.anchor(), now) > 0) return@withTransaction null
        completeLocked(s, now)
    }

    private suspend fun completeLocked(s: FlightSession, now: ClockReading): FlightSession =
        s.copy(state = "COMPLETED", remainingAtAnchorMs = 0, endedWallMs = SessionClock.completionWallTime(s.anchor(), now),
            creditedFocusMs = s.durationMs, creditedDistanceMeters = s.distanceMeters)
            .also { db.flights().update(it) }

    suspend fun cancel(): FlightSession? = db.withTransaction {
        val s = db.flights().ongoing() ?: return@withTransaction null
        val now = clock()
        // A late cancel must not erase a session that has already reached its deadline.
        val remaining = SessionClock.remaining(s.anchor(), now)
        if (s.state == "ACTIVE" && remaining == 0L) return@withTransaction completeLocked(s, now)
        s.copy(state = "CANCELED", remainingAtAnchorMs = remaining, endedWallMs = now.wallMs)
            .also { db.flights().update(it) }
    }

    suspend fun note(id: String, text: String) = db.withTransaction {
        db.flights().get(id)?.let { db.flights().update(it.copy(note = text)) }
    }
}
