package personal.focusflight.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import personal.focusflight.domain.session.GeoPoint

@Composable
fun RendererPreview(initialScene: String = "globe", onBack: () -> Unit) {
    if(initialScene in listOf("window-ground","window-cruise","window-descent")) {
        val progress=when(initialScene) {"window-cruise"->.5;"window-descent"->.98;else->0.0}
        WindowFocusView(Modifier.fillMaxSize(),scene=MapScene(GeoPoint(0.0,0.0),progress=progress))
        return
    }
    var mode by remember { mutableStateOf(MapMode.STANDARD) }
    var globe by remember { mutableStateOf(initialScene == "globe") }
    var city by remember { mutableStateOf(initialScene == "city") }
    var status by remember { mutableStateOf(MapStatus.LOADING) }
    var window by remember { mutableStateOf(initialScene == "window") }
    val paris = GeoPoint(48.8591, 2.2917)
    val london = GeoPoint(51.5072, -.1276)
    Box(Modifier.fillMaxSize().background(Color(0xFF111C1C))) {
        if (window) WindowFocusView(Modifier.fillMaxSize(),scene=MapScene(paris,london,progress=.02,mode=mode))
        else FlightMap(MapScene(paris, if (city) null else london, listOf(MapAirport("PAR", paris), MapAirport("LON", london)),
            progress = .25, mode = mode, globe = globe, zoomOverride = if (city) 16.0 else null,
            bottomInsetDp = 180f), Modifier.fillMaxSize()) { status = it }
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PreviewPill("Back", onBack)
                PreviewPill(if (globe) "City" else "Globe") { globe = !globe; city = false; window = false }
            }
            if (!window && status != MapStatus.READY) Text(if (status == MapStatus.LOADING) "Loading map" else "Map unavailable", color = Color.White,
                modifier = Modifier.padding(top = 12.dp).background(Color.Black.copy(alpha = .6f), RoundedCornerShape(10.dp)).padding(10.dp))
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PreviewPill(if (city) "Route overview" else "3D buildings") { city = !city; globe = false; window = false }
                PreviewPill(if (window) "Map view" else "Window view") { window = !window }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MapMode.entries.forEach { item ->
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (mode == item) Color(0xFFF2F1DC) else Color(0xCC111714))
                        .clickable { mode = item }.padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                        Text(item.title, color = if (mode == item) Color.Black else Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPill(text: String, onClick: () -> Unit) {
    Text(text, color = Color.White, fontSize = 13.sp,
        modifier = Modifier.clip(CircleShape).background(Color(0xBF18201C)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp))
}
