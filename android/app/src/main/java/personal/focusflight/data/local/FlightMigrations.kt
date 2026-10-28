package personal.focusflight.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object FlightMigrations {
    val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE flights ADD COLUMN diagnostic INTEGER NOT NULL DEFAULT 0")
            // This exact short fixture was only exposed by the initial debug screen.
            db.execSQL("UPDATE flights SET diagnostic = 1 WHERE durationMs = 15000 AND departureCode = 'CDG' AND arrivalCode = 'LHR' AND category = 'Study' AND seat = '1A'")
            db.execSQL("CREATE TABLE IF NOT EXISTS drafts (id TEXT NOT NULL PRIMARY KEY, departureCode TEXT NOT NULL, departureCity TEXT NOT NULL, departureLat REAL NOT NULL, departureLon REAL NOT NULL, arrivalCode TEXT NOT NULL, arrivalCity TEXT NOT NULL, arrivalLat REAL NOT NULL, arrivalLon REAL NOT NULL, durationMs INTEGER NOT NULL, distanceMeters INTEGER NOT NULL, category TEXT NOT NULL, seat TEXT NOT NULL, randomRoute INTEGER NOT NULL, bookedWallMs INTEGER NOT NULL)")
        }
    }
}
