package personal.focusflight.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import personal.focusflight.FlightApplication
import personal.focusflight.data.local.Airport
import personal.focusflight.data.local.FlightSession
import personal.focusflight.data.local.FlightDraft
import personal.focusflight.data.preferences.AppPrefs
import personal.focusflight.data.preferences.PreferenceRepository
import personal.focusflight.domain.session.FlightRoute
import personal.focusflight.domain.session.GeoPoint
import personal.focusflight.platform.timer.FlightTimerService
import kotlin.math.abs
import kotlin.math.ceil
import java.io.IOException
import java.util.UUID

data class AirportEntry(val airport: Airport, val name: String, val country: String) {
    val displayCity get() = airport.city.substringBefore(" (")
}
data class FlightOption(val destination: AirportEntry, val distanceMeters: Long, val minutes: Int)

class FlightViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FlightApplication
    val preferences = PreferenceRepository(app)
    val prefs: StateFlow<AppPrefs?> = preferences.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val flights: StateFlow<List<FlightSession>?> = app.repository.flights.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val drafts: StateFlow<List<FlightDraft>?> = app.repository.drafts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _airports = MutableStateFlow<List<AirportEntry>>(emptyList())
    val airports = _airports.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _starting = MutableStateFlow(false)
    val starting = _starting.asStateFlow()
    private val _booking=MutableStateFlow(false)
    val booking=_booking.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _airports.value = withContext(Dispatchers.IO) {
                    val json = app.assets.open("airports.json").bufferedReader().use { JSONArray(it.readText()) }
                    (0 until json.length()).map { i ->
                        val a = json.getJSONObject(i)
                        AirportEntry(Airport(a.getString("code"), a.getString("city"), a.getDouble("lat"), a.getDouble("lon")),
                            a.getString("name"), a.getString("country"))
                    }
                }
            } catch (_: IOException) { _error.value = "The airport catalog could not be loaded." }
        }
    }

    fun clock() = app.clock()
    fun clearError() { _error.value = null }
    fun changePreferences(block: suspend PreferenceRepository.() -> Unit) { viewModelScope.launch { preferences.block() } }

    fun option(from: AirportEntry, to: AirportEntry): FlightOption {
        val meters = FlightRoute.distance(GeoPoint(from.airport.lat, from.airport.lon), GeoPoint(to.airport.lat, to.airport.lon))
        // An explicit local route estimate, not a recovered iOS formula.
        val minutes = ceil(meters / 1000.0 / 800.0 * 60 + 15).toInt().coerceAtLeast(15)
        return FlightOption(to, meters.toLong(), minutes)
    }

    fun options(from: AirportEntry, minutes: Int): List<FlightOption> = _airports.value.asSequence()
        .filter { it.airport.code != from.airport.code }
        .map { option(from, it) }
        .sortedBy { abs(it.minutes - minutes) }
        .take(14).toList()

    fun book(from: AirportEntry, option: FlightOption, minutes: Int, category: String, seat: String, random: Boolean,
             id: String?=null,booked: (String) -> Unit) {
        if(_booking.value) return
        _booking.value=true
        viewModelScope.launch {
            val to=option.destination.airport
            val d=FlightDraft(id ?: UUID.randomUUID().toString(),from.airport.code,from.displayCity,from.airport.lat,from.airport.lon,
                to.code,option.destination.displayCity,to.lat,to.lon,minutes*60_000L,option.distanceMeters,category,seat,random,clock().wallMs)
            try { app.repository.saveDraft(d);booked(d.id) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _error.value="Your boarding pass could not be saved." }
            finally {_booking.value=false}
        }
    }

    fun startDraft(id: String, onStarted: () -> Unit) {
        if (_starting.value) return
        _starting.value = true
        app.appScope.launch {
            var created = false
            try {
                val session=app.repository.startDraft(id)
                created = true
                personal.focusflight.platform.timer.TimerPlatform.schedule(app,session)
                withContext(Dispatchers.Main) {
                    ContextCompat.startForegroundService(app, Intent(app, FlightTimerService::class.java).setAction("SYNC"))
                    onStarted()
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _error.value = if(created) "Your timer has started. Reopen the app to restore its flight notification."
                    else "Your flight could not start. Please try again."
            } finally { _starting.value = false }
        }
    }

    fun command(action: String) { ContextCompat.startForegroundService(app, Intent(app, FlightTimerService::class.java).setAction(action)) }
    fun saveNote(id: String, note: String) { viewModelScope.launch { app.repository.note(id, note) } }
}
