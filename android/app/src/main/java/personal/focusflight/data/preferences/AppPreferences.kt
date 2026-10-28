package personal.focusflight.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import personal.focusflight.domain.session.DistanceUnit
import personal.focusflight.map.MapMode
import java.io.IOException

private val Context.focusPreferences by preferencesDataStore("focus_flight_preferences")

data class AppPrefs(
    val homeCode: String = "CDG",
    val airportChosen: Boolean = false,
    val introDone: Boolean = false,
    val mapMode: MapMode = MapMode.SATELLITE,
    val labels: Boolean = true,
    val terrain: Boolean = true,
    val buildings: Boolean = true,
    val unit: DistanceUnit = DistanceUnit.KILOMETERS,
    val keepAwake: Boolean = false,
    val soundEnabled: Boolean = false,
    val soundTrack: String = "cabin",
    val volume: Float = .5f,
    val windowScene: String = "clouds",
    val language: String = "",
    val blockingEnabled: Boolean = false,
    val dndEnabled: Boolean = false,
    val blockedApps: Set<String> = emptySet(),
    val blockedSites: Set<String> = emptySet(),
)

class PreferenceRepository(context: Context) {
    private val store = context.applicationContext.focusPreferences
    val flow = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { p ->
        AppPrefs(homeCode = p[stringPreferencesKey("home")] ?: "CDG",
            airportChosen = p[booleanPreferencesKey("airport_chosen")] ?: false,
            introDone = p[booleanPreferencesKey("intro_done")] ?: false,
            mapMode = MapMode.entries.firstOrNull { it.name == p[stringPreferencesKey("map_mode")] } ?: MapMode.SATELLITE,
            labels = p[booleanPreferencesKey("labels")] ?: true,
            terrain = p[booleanPreferencesKey("terrain")] ?: true,
            buildings = p[booleanPreferencesKey("buildings")] ?: true,
            unit = DistanceUnit.entries.firstOrNull { it.name == p[stringPreferencesKey("units")] } ?: DistanceUnit.KILOMETERS,
            keepAwake = p[booleanPreferencesKey("keep_awake")] ?: false,
            soundEnabled = p[booleanPreferencesKey("sound_enabled")] ?: false,
            soundTrack = p[stringPreferencesKey("sound_track")] ?: "cabin",
            volume = (p[floatPreferencesKey("volume")] ?: .5f).coerceIn(0f, 1f),
            windowScene = p[stringPreferencesKey("window_scene")] ?: "clouds",
            language = p[stringPreferencesKey("language")] ?: "",
            blockingEnabled = p[booleanPreferencesKey("blocking_enabled")] ?: false,
            dndEnabled = p[booleanPreferencesKey("dnd_enabled")] ?: false,
            blockedApps = p[stringSetPreferencesKey("blocked_apps")] ?: emptySet(),
            blockedSites = p[stringSetPreferencesKey("blocked_sites")] ?: emptySet())
    }

    suspend fun home(code: String) = store.edit { it[stringPreferencesKey("home")] = code; it[booleanPreferencesKey("airport_chosen")] = true }
    suspend fun finishIntro() = store.edit { it[booleanPreferencesKey("intro_done")] = true }
    suspend fun map(mode: MapMode? = null, labels: Boolean? = null, terrain: Boolean? = null, buildings: Boolean? = null) = store.edit { p ->
        mode?.let { p[stringPreferencesKey("map_mode")] = it.name }
        labels?.let { p[booleanPreferencesKey("labels")] = it }
        terrain?.let { p[booleanPreferencesKey("terrain")] = it }
        buildings?.let { p[booleanPreferencesKey("buildings")] = it }
    }
    suspend fun units(unit: DistanceUnit) = store.edit { it[stringPreferencesKey("units")] = unit.name }
    suspend fun awake(enabled: Boolean) = store.edit { it[booleanPreferencesKey("keep_awake")] = enabled }
    suspend fun sound(enabled: Boolean? = null, track: String? = null, volume: Float? = null) = store.edit { p ->
        enabled?.let { p[booleanPreferencesKey("sound_enabled")] = it }
        track?.let { p[stringPreferencesKey("sound_track")] = it }
        volume?.let { p[floatPreferencesKey("volume")] = it.coerceIn(0f, 1f) }
    }
    suspend fun window(scene: String) = store.edit { it[stringPreferencesKey("window_scene")] = scene }
    suspend fun language(tag: String) = store.edit { it[stringPreferencesKey("language")] = tag }
    suspend fun blocking(enabled: Boolean? = null, dnd: Boolean? = null, apps: Set<String>? = null, sites: Set<String>? = null) = store.edit { p ->
        enabled?.let { p[booleanPreferencesKey("blocking_enabled")] = it }
        dnd?.let { p[booleanPreferencesKey("dnd_enabled")] = it }
        apps?.let { p[stringSetPreferencesKey("blocked_apps")] = it }
        sites?.let { p[stringSetPreferencesKey("blocked_sites")] = it }
    }
}
