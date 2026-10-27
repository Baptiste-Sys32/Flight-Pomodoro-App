package personal.focusflight

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import personal.focusflight.data.local.*
import personal.focusflight.domain.session.ClockReading
import personal.focusflight.domain.session.SessionClock

@RunWith(AndroidJUnit4::class)
class SessionPersistenceTest {
    private lateinit var db: FlightDatabase
    private lateinit var repo: FlightRepository
    private var now = ClockReading(100_000, 1_000_000, 2)
    private val paris = Airport("CDG", "Paris", 49.0097, 2.5479)
    private val london = Airport("LHR", "London", 51.47, -.4543)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, FlightDatabase::class.java).build()
        repo = FlightRepository(db) { now }
    }
    @After fun close() { db.close() }
    private suspend fun start() = repo.start(paris, london, 60_000, 348_000, "Study", "1A")

    @Test fun pauseSurvivesRepositoryRecreationAndDoesNotConsumeTime() = runBlocking {
        start()
        now = now.copy(elapsedMs = 120_000, wallMs = 1_020_000)
        repo.setPaused(true)
        now = now.copy(elapsedMs = 800_000, wallMs = 1_700_000)
        val recovered = FlightRepository(db) { now }
        val paused = recovered.ongoing()!!
        assertEquals("PAUSED", paused.state)
        assertEquals(40_000L, SessionClock.remaining(paused.anchor(), now))
        val resumed = recovered.setPaused(false)!!
        now = now.copy(elapsedMs = 810_000, wallMs = 1_710_000)
        assertEquals(30_000L, SessionClock.remaining(resumed.anchor(), now))
    }

    @Test fun racingCompletionCallbacksAwardOnlyOnce() = runBlocking {
        val s = start()
        now = now.copy(elapsedMs = 200_000, wallMs = 1_100_000)
        val results = coroutineScope { List(10) { async(Dispatchers.Default) { repo.completeDue(s.id) } }.awaitAll() }
        assertEquals(1, results.count { it != null })
        val saved = db.flights().get(s.id)!!
        assertEquals("COMPLETED", saved.state)
        assertEquals(60_000L, saved.creditedFocusMs)
        assertEquals(348_000L, saved.creditedDistanceMeters)
        assertEquals(1_060_000L, saved.endedWallMs)
        assertNull(repo.ongoing())
    }

    @Test fun cancellationAndOldAlarmCannotCreditANewFlight() = runBlocking {
        val old = start()
        repo.cancel()
        val next = start()
        now = now.copy(elapsedMs = 200_000, wallMs = 1_100_000)
        assertNull(repo.completeDue(old.id))
        assertEquals("CANCELED", db.flights().get(old.id)!!.state)
        assertEquals(0L, db.flights().get(old.id)!!.creditedFocusMs)
        assertEquals(next.id, repo.ongoing()!!.id)
    }

    @Test fun onlyOneFlightMayBeActive() = runBlocking {
        start()
        try { start(); fail("Second active flight accepted") } catch (_: IllegalStateException) { }
        assertNotNull(repo.ongoing())
    }
}
