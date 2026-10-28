package personal.focusflight.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import personal.focusflight.data.local.FlightSession
import personal.focusflight.data.preferences.AppPrefs
import personal.focusflight.domain.session.GeoPoint
import personal.focusflight.domain.session.SessionClock
import personal.focusflight.map.*
import personal.focusflight.platform.timer.TimerPlatform
import personal.focusflight.ui.components.*
import java.text.DateFormat
import java.util.Date

/** The renderer consumes the clock projection; it never advances the timer itself. */
@Composable
fun FlightDeck(
    flight: FlightSession,
    remainingMs: Long,
    prefs: AppPrefs,
    command: (String) -> Unit,
    changeMap: (MapMode?, Boolean?, Boolean?, Boolean?) -> Unit,
    changeAwake: (Boolean) -> Unit,
) {
    var window by rememberSaveableFlight(flight.id) { false }
    var overview by rememberSaveableFlight(flight.id) { false }
    var panel by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(MapStatus.LOADING) }
    val paused = flight.state == "PAUSED"
    val progress = SessionClock.progress(flight.durationMs, remainingMs)
    val from = GeoPoint(flight.departureLat, flight.departureLon)
    val to = GeoPoint(flight.arrivalLat, flight.arrivalLon)
    val hiddenDestination = flight.randomRoute
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().then(if (paused) Modifier.blur(16.dp) else Modifier)) {
            if (window) WindowFocusView(Modifier.fillMaxSize(), paused)
            else FlightMap(MapScene(from, to,
                airports = if (hiddenDestination) emptyList() else listOf(MapAirport(flight.departureCode, from), MapAirport(flight.arrivalCode, to)),
                progress = progress, mode = prefs.mapMode, labels = prefs.labels, terrain = prefs.terrain,
                buildings = prefs.buildings, follow = !overview, bottomInsetDp = 110f), Modifier.fillMaxSize()) { status = it }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.15f), Color.Transparent,
            Color.Transparent, Color.Black.copy(alpha=.7f)))))
        if (paused) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.52f)))
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=10.dp, vertical=10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                Column {
                    GlassIconButton(if(paused) Glyph.PLAY else Glyph.PAUSE, if(paused) "Resume" else "Pause",
                        { command(if(paused) "RESUME" else "PAUSE") })
                    GlassIconButton(Glyph.ROUTE, "Route overview", { overview = !overview; window = false }, selected=overview)
                    GlassIconButton(Glyph.SETTINGS, "Flight options", { panel = "options" })
                }
                Column {
                    GlassIconButton(Glyph.MAP, "Choose map style", { panel = "map" })
                    GlassIconButton(if(window) Glyph.GLOBE else Glyph.WINDOW, if(window) "Map view" else "Window view",
                        { window = !window }, selected=window)
                }
            }
            if (!window && status == MapStatus.UNAVAILABLE) Text("Map unavailable. Your timer is still running.", color=Paper,
                fontSize=11.sp, modifier=Modifier.padding(8.dp).clip(RoundedCornerShape(12.dp)).background(Glass).padding(12.dp))
            Spacer(Modifier.weight(1f))
            if (paused) Column(Modifier.fillMaxWidth().padding(bottom=24.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                Text("Paused", color=Paper.copy(alpha=.5f), fontSize=13.sp)
                Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(flight.anchorWallMs)), color=Paper,
                    fontSize=14.sp, fontWeight=FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=5.dp, vertical=16.dp), horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.Bottom) {
                Column {
                    Text("Time Remaining",color=Paper.copy(alpha=.5f),fontSize=10.sp)
                    Text(TimerPlatform.formatRemaining(remainingMs),color=Paper,fontSize=24.sp,fontWeight=FontWeight.SemiBold)
                }
                Column(horizontalAlignment=Alignment.End) {
                    Text("Distance Remaining",color=Paper.copy(alpha=.5f),fontSize=10.sp)
                    Text(distanceText((flight.distanceMeters*(1-progress)).toLong(),prefs.unit),color=Paper,
                        fontSize=24.sp,fontWeight=FontWeight.SemiBold)
                }
            }
        }
        when(panel) {
            "map" -> MapStylePanel(prefs, { panel=null }, changeMap)
            "options" -> FocusPanel("Flight options", { panel=null }) {
                SettingsToggle("Keep Screen Awake", prefs.keepAwake, changeAwake)
                Spacer(Modifier.height(10.dp))
                FlightCapsule("Give up", { panel="cancel" })
            }
            "cancel" -> FocusPanel("Give up this flight?", { panel=null }) {
                Text("This flight will be saved as canceled and earns no focus credit.",color=Paper.copy(alpha=.6f),fontSize=13.sp)
                Spacer(Modifier.height(24.dp))
                FlightCapsule("Keep focusing", { panel=null }, pastel=true)
                TextButton(onClick={ command("CANCEL");panel=null }, modifier=Modifier.fillMaxWidth()) { Text("Give up",color=Color(0xFFE9A39B)) }
            }
        }
    }
}

@Composable
private fun rememberSaveableFlight(key: String, initial: () -> Boolean) =
    androidx.compose.runtime.saveable.rememberSaveable(key) { mutableStateOf(initial()) }

@Composable
fun FocusPanel(title: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        GlassPanel(Modifier.fillMaxWidth().padding(20.dp).widthIn(max=430.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Text(title,fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=Paper,modifier=Modifier.weight(1f))
                GlassIconButton(Glyph.CLOSE,"Close",close)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,color=Paper,fontSize=13.sp,modifier=Modifier.weight(1f))
        Switch(checked,onCheckedChange=change)
    }
}

@Composable
fun MapStylePanel(prefs: AppPrefs, close: () -> Unit, change: (MapMode?, Boolean?, Boolean?, Boolean?) -> Unit) {
    FocusPanel("Choose Map Style",close) {
        MapMode.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach { mode ->
                    Box(Modifier.weight(1f).height(102.dp).clip(RoundedCornerShape(20.dp))
                        .border(if(prefs.mapMode==mode) 2.dp else 0.dp,Paper,RoundedCornerShape(20.dp))
                        .clickable { change(mode,null,null,null) }) {
                        MapStyleSwatch(mode,Modifier.fillMaxSize())
                        Text(mode.title,color=if(mode==MapMode.MONOCHROME || mode==MapMode.STANDARD) Ink else Paper,
                            fontSize=12.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.align(Alignment.BottomStart).padding(14.dp))
                        if(prefs.mapMode==mode) FlightIcon(Glyph.CHECK,Paper,Modifier.align(Alignment.TopEnd).padding(10.dp).size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        SettingsToggle("Labels",prefs.labels) { change(null,it,null,null) }
        SettingsToggle("Terrain",prefs.terrain) { change(null,null,it,null) }
        SettingsToggle("3D buildings",prefs.buildings) { change(null,null,null,it) }
    }
}

/** Original illustrative swatches, not screenshots or borrowed map tiles. */
@Composable
private fun MapStyleSwatch(mode: MapMode, modifier: Modifier) {
    Canvas(modifier) {
        val sea = when(mode) { MapMode.MONOCHROME -> Color(0xFFC7CACA); MapMode.TERRA -> Color(0xFF101715)
            MapMode.STANDARD -> Color(0xFF8EC9D2); MapMode.SATELLITE -> Color(0xFF153B46) }
        val land = when(mode) { MapMode.MONOCHROME -> Color(0xFFE6E7E3); MapMode.TERRA -> Color(0xFF303A34)
            MapMode.STANDARD -> Color(0xFFB4CB97); MapMode.SATELLITE -> Color(0xFF416247) }
        drawRect(sea)
        val p=Path().apply { moveTo(0f,0f);lineTo(size.width*.8f,0f);cubicTo(size.width*.2f,size.height*.3f,size.width*.9f,size.height*.5f,size.width*.45f,size.height*.7f);lineTo(size.width*.3f,size.height);lineTo(0f,size.height);close() }
        drawPath(p,land)
        for(i in 1..7) drawLine(land.copy(alpha=.35f),Offset(0f,size.height*i/8),Offset(size.width*.55f,size.height*(i+.8f)/8),2f)
    }
}
