package personal.focusflight

import android.content.Intent
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import personal.focusflight.data.local.Airport
import personal.focusflight.platform.timer.FlightTimerService
import personal.focusflight.platform.timer.TimerPlatform

@RunWith(AndroidJUnit4::class)
class TimerServiceTest {
    @Test fun userStartedTimerPausesResumesAndPersistsArrival() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FlightApplication
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val s = runBlocking {
            app.repository.start(Airport("CDG", "Paris", 49.0, 2.5), Airport("LHR", "London", 51.47, -.45),
                15_000, 348_000, "Study", "1A", diagnostic = true)
        }
        fun command(action: String) = scenario.onActivity { activity ->
            ContextCompat.startForegroundService(activity, Intent(activity, FlightTimerService::class.java).setAction(action))
        }
        command("SYNC")
        command("PAUSE")
        waitFor(5_000) { runBlocking { app.repository.ongoing()?.state == "PAUSED" } }
        val paused = runBlocking { app.repository.ongoing()!! }
        Thread.sleep(1200)
        val stillPaused = runBlocking { app.repository.ongoing()!! }
        assertEquals(paused.remainingAtAnchorMs, stillPaused.remainingAtAnchorMs)
        command("RESUME")
        waitFor(5_000) { runBlocking { app.repository.ongoing()?.state == "ACTIVE" } }
        val notifications = app.getSystemService(NotificationManager::class.java)
        waitFor(5_000) {
            notifications.activeNotifications.firstOrNull { it.id == TimerPlatform.SESSION_NOTIFICATION }
                ?.notification?.extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true
        }
        val initialProgress = notifications.activeNotifications.first { it.id == TimerPlatform.SESSION_NOTIFICATION }
            .notification.extras.getInt(Notification.EXTRA_PROGRESS)
        waitFor(6_000) {
            val value = notifications.activeNotifications.firstOrNull { it.id == TimerPlatform.SESSION_NOTIFICATION }
                ?.notification?.extras?.getInt(Notification.EXTRA_PROGRESS) ?: 0
            value in (initialProgress + 21)..999
        }
        waitFor(25_000) { runBlocking { app.database.flights().get(s.id)?.state == "COMPLETED" } }
        val saved = runBlocking { app.database.flights().get(s.id)!! }
        assertEquals(15_000L, saved.creditedFocusMs)
        assertEquals(348_000L, saved.creditedDistanceMeters)
        assertNull(runBlocking { app.repository.ongoing() })
        scenario.close()
    }

    private fun waitFor(timeout: Long, condition: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + timeout
        while (!condition()) {
            if (android.os.SystemClock.elapsedRealtime() >= deadline) fail("Timed out waiting for timer state")
            Thread.sleep(50)
        }
    }
}
