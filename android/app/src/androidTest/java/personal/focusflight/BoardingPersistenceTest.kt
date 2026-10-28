package personal.focusflight

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import personal.focusflight.data.local.*
import personal.focusflight.domain.session.ClockReading
import java.util.UUID
import android.app.Notification
import personal.focusflight.platform.timer.TimerPlatform

@RunWith(AndroidJUnit4::class)
class BoardingPersistenceTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext

    @Test fun usedPassCannotStartOrCreditTwice()= runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,FlightDatabase::class.java).build()
        try {
            var now=ClockReading(1000,1_000_000,2)
            var repo=FlightRepository(db) { now }
            val d=FlightDraft("booked","CDG","Paris",49.0,2.0,"LHR","London",51.0,-.4,
                25*60_000L,348_000,"Study","03A",true,now.wallMs)
            repo.saveDraft(d)
            repo=FlightRepository(db) { now }
            assertEquals(d,repo.drafts.first().single())
            val started=coroutineScope { List(8) { async(Dispatchers.IO) { repo.startDraft(d.id) } }.awaitAll() }
            assertEquals(1,started.map { it.id }.distinct().size)
            assertTrue(repo.drafts.first().isEmpty())
            now=now.copy(elapsedMs=1_501_000,wallMs=2_500_000)
            assertNotNull(repo.completeDue(d.id))
            assertNull(repo.completeDue(d.id))
            assertEquals(1,repo.flights.first().size)
            assertTrue(db.flights().get(d.id)!!.randomRoute)
            val title=TimerPlatform.notification(context.applicationContext as FlightApplication,started.first())
                .extras.getCharSequence(Notification.EXTRA_TITLE).toString()
            assertFalse("Random destination leaked into the notification",title.contains("LHR"))
            assertTrue(title.contains("?"))
            try { repo.startDraft(d.id);fail("Used pass was accepted") } catch (_: IllegalStateException) { }
            try { repo.saveDraft(d);fail("Used pass returned to planning") } catch (_: IllegalStateException) { }
        } finally { db.close() }
    }

    @Test fun migrationPreservesExistingClockAndNotesAndMarksOnlyDebugFixture()=runBlocking {
        val name="migration-${UUID.randomUUID()}.db"
        val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        val schema=instrumentation.context.assets.open("personal.focusflight.data.local.FlightDatabase/1.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(path,null).use { old ->
            old.execSQL(schema.getJSONArray("entities").getJSONObject(0).getString("createSql").replace("\u0024{TABLE_NAME}","flights"))
            val setup=schema.getJSONArray("setupQueries")
            for(i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO flights VALUES ('existing','CDG','Paris',49,2,'LHR','London',51,-0.4,1500000,348000,'Study','1A',0,'PAUSED',900000,1000,2000,3,1000,NULL,0,0,'Keep this note')")
            old.execSQL("INSERT INTO flights SELECT 'debug',departureCode,departureCity,departureLat,departureLon,arrivalCode,arrivalCity,arrivalLat,arrivalLon,15000,distanceMeters,category,seat,randomRoute,'COMPLETED',0,anchorElapsedMs,anchorWallMs,anchorBoot,startedWallMs,16000,15000,348000,'' FROM flights WHERE id='existing'")
            old.version=1
        }
        val migrated=Room.databaseBuilder(context,FlightDatabase::class.java,name).addMigrations(FlightMigrations.V1_TO_V2).build()
        try {
            val s=migrated.flights().get("existing")!!
            assertEquals("Keep this note",s.note)
            assertEquals("PAUSED",s.state)
            assertEquals(900000L,s.remainingAtAnchorMs)
            assertEquals(1000L,s.anchorElapsedMs)
            assertFalse(s.diagnostic)
            assertTrue(migrated.flights().get("debug")!!.diagnostic)
            assertTrue(migrated.drafts().observeAll().first().isEmpty())
        } finally { migrated.close();context.deleteDatabase(name) }
    }
}
