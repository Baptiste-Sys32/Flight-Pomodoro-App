package personal.focusflight.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import personal.focusflight.domain.session.ClockAnchor

@Entity(tableName = "flights")
data class FlightSession(
    @PrimaryKey val id: String,
    val departureCode: String,
    val departureCity: String,
    val departureLat: Double,
    val departureLon: Double,
    val arrivalCode: String,
    val arrivalCity: String,
    val arrivalLat: Double,
    val arrivalLon: Double,
    val durationMs: Long,
    val distanceMeters: Long,
    val category: String,
    val seat: String,
    val randomRoute: Boolean = false,
    val state: String = "ACTIVE",
    val remainingAtAnchorMs: Long,
    val anchorElapsedMs: Long,
    val anchorWallMs: Long,
    val anchorBoot: Int,
    val startedWallMs: Long,
    val endedWallMs: Long? = null,
    val creditedFocusMs: Long = 0,
    val creditedDistanceMeters: Long = 0,
    val note: String = "",
    @ColumnInfo(defaultValue = "0") val diagnostic: Boolean = false,
) {
    fun anchor() = ClockAnchor(remainingAtAnchorMs, anchorElapsedMs, anchorWallMs, anchorBoot, state == "PAUSED")
    val isOngoing: Boolean get() = state == "ACTIVE" || state == "PAUSED"
}

@Dao
interface FlightDao {
    @Query("SELECT * FROM flights ORDER BY startedWallMs DESC")
    fun observeAll(): Flow<List<FlightSession>>

    @Query("SELECT * FROM flights WHERE state IN ('ACTIVE','PAUSED') ORDER BY startedWallMs DESC LIMIT 1")
    suspend fun ongoing(): FlightSession?

    @Query("SELECT * FROM flights WHERE id = :id")
    suspend fun get(id: String): FlightSession?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: FlightSession)

    @Update
    suspend fun update(session: FlightSession)
}

@Entity(tableName = "drafts")
data class FlightDraft(
    @PrimaryKey val id: String,
    val departureCode: String, val departureCity: String, val departureLat: Double, val departureLon: Double,
    val arrivalCode: String, val arrivalCity: String, val arrivalLat: Double, val arrivalLon: Double,
    val durationMs: Long, val distanceMeters: Long, val category: String, val seat: String,
    val randomRoute: Boolean, val bookedWallMs: Long,
)

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts ORDER BY bookedWallMs DESC") fun observeAll(): Flow<List<FlightDraft>>
    @Query("SELECT * FROM drafts WHERE id = :id") suspend fun get(id: String): FlightDraft?
    @Upsert suspend fun save(draft: FlightDraft)
    @Query("DELETE FROM drafts WHERE id = :id") suspend fun delete(id: String)
}

@Database(entities = [FlightSession::class, FlightDraft::class], version = 2, exportSchema = true)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun flights(): FlightDao
    abstract fun drafts(): DraftDao
}
