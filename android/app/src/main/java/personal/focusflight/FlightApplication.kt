package personal.focusflight

import android.app.Application
import android.os.SystemClock
import android.provider.Settings
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import personal.focusflight.data.local.FlightDatabase
import personal.focusflight.data.local.FlightRepository
import personal.focusflight.data.local.FlightMigrations
import personal.focusflight.domain.session.ClockReading

class FlightApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { Room.databaseBuilder(this, FlightDatabase::class.java, "focus-flight.db")
        .addMigrations(FlightMigrations.V1_TO_V2).build() }
    val repository by lazy { FlightRepository(database, ::clock) }
    fun clock() = ClockReading(SystemClock.elapsedRealtime(), System.currentTimeMillis(),
        Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, 0))
}
