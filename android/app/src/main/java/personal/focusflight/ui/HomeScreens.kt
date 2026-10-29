package personal.focusflight.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import personal.focusflight.data.local.FlightSession
import personal.focusflight.data.preferences.AppPrefs
import personal.focusflight.domain.session.GeoPoint
import personal.focusflight.map.*
import personal.focusflight.ui.components.*
import java.time.LocalTime

@Composable
fun HomeScreen(home: AirportEntry,prefs: AppPrefs,start: ()->Unit,airport: ()->Unit,open: (String)->Unit) {
    val point=GeoPoint(home.airport.lat,home.airport.lon)
    val headerColor=if(prefs.mapMode==MapMode.TERRA) Paper else Ink
    Box(Modifier.fillMaxSize().background(Ink)) {
        FlightMap(MapScene(point,airports=listOf(MapAirport(home.airport.code,point,true)),mode=prefs.mapMode,
            labels=prefs.labels,terrain=prefs.terrain,buildings=prefs.buildings,bottomInsetDp=110f),Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            if(headerColor==Ink) Color.White.copy(alpha=.25f) else Color.Black.copy(alpha=.3f),Color.Transparent,Color.Black.copy(alpha=.15f)))))
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top=20.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top) {
            Column(Modifier.padding(start=12.dp).weight(1f).clickable(onClick=airport)) {
                Text(when(LocalTime.now().hour) {in 5..11->"Good morning!";in 12..17->"Good afternoon!";else->"Good evening!"},
                    color=headerColor.copy(alpha=.65f),fontSize=16.sp,fontWeight=FontWeight.Medium)
                Text(home.displayCity,color=headerColor,fontSize=34.sp,fontWeight=FontWeight.Bold)
            }
            GlassIconButton(Glyph.PROFILE,"Profile and settings",{open("SETTINGS")})
            }
            Spacer(Modifier.weight(1f))
            FlightCapsule("Start Journey",start,Modifier.padding(horizontal=12.dp),pastel=true)
            Spacer(Modifier.height(44.dp))
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun AirportPicker(airports: List<AirportEntry>,home: AirportEntry,title: String,exclude: String?=null,close: (() -> Unit)?=null,mode: MapMode=MapMode.STANDARD,
                  selected: (AirportEntry)->Unit) {
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val results=remember(query,airports,exclude) {
        val eligible=airports.filter { it.airport.code!=exclude }
        if(query.isBlank()) {
            val featured=listOf(home.airport.code,"CDG","LHR","JFK","NRT","ICN","BER","SYD")
            featured.distinct().mapNotNull { code->eligible.firstOrNull {it.airport.code==code} }
        } else eligible.asSequence().filter {
            it.airport.code.contains(query,true) || it.airport.city.contains(query,true) || it.name.contains(query,true)
        }.sortedBy {if(it.airport.code.equals(query,true)) 0 else if(it.airport.code.startsWith(query,true)) 1 else 2}.take(60).toList()
    }
    Box(Modifier.fillMaxSize().background(Ink)) {
        FlightMap(MapScene(GeoPoint(home.airport.lat,home.airport.lon),globe=true,mode=mode,bottomInsetDp=480f),Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(18.dp)) {
            if(close!=null) GlassIconButton(Glyph.BACK,"Back",close)
            Spacer(Modifier.weight(1f))
            GlassPanel {
                Text(title,color=Paper,fontSize=20.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(bottom=16.dp))
                SearchField(query,{query=it},"Search city or airport")
                LazyColumn(Modifier.fillMaxWidth().heightIn(max=275.dp)) {
                    items(results,key={it.airport.code}) { entry ->
                        Row(Modifier.fillMaxWidth().semantics {contentDescription="Airport ${entry.airport.code}"}
                            .clickable {selected(entry)}.padding(vertical=13.dp),verticalAlignment=Alignment.CenterVertically) {
                            AirportBadge(entry.airport.code)
                            Column(Modifier.weight(1f).padding(start=12.dp)) {
                                Text(entry.displayCity,color=Paper,fontSize=14.sp,fontWeight=FontWeight.Medium)
                                Text(entry.name,color=Paper.copy(alpha=.45f),fontSize=10.sp,maxLines=1)
                            }
                        }
                    }
                    if(results.isEmpty()) item { Text("No airports found",color=Paper.copy(alpha=.5f),modifier=Modifier.padding(vertical=30.dp)) }
                }
                FlightCapsule("Random location",{airports.filter {it.airport.code!=exclude}.randomOrNull()?.let(selected)},pastel=true)
            }
        }
    }
}

@Composable
fun SearchField(value: String,change: (String)->Unit,hint: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color.Black.copy(alpha=.25f)).padding(14.dp),
        verticalAlignment=Alignment.CenterVertically) {
        FlightIcon(Glyph.SEARCH,Paper.copy(alpha=.5f),Modifier.size(17.dp));Spacer(Modifier.width(10.dp))
        BasicTextField(value,change,singleLine=true,textStyle=TextStyle(color=Paper,fontSize=14.sp,fontFamily=Inter),
            cursorBrush=Brush.linearGradient(listOf(Paper,Paper)),modifier=Modifier.weight(1f).semantics {contentDescription=hint},
            decorationBox={inner->Box {if(value.isEmpty()) Text(hint,color=Paper.copy(alpha=.4f),fontSize=14.sp);inner()}})
    }
}

@Composable
fun MenuRow(label: String,value: String="",glyph: Glyph?=null,click: ()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=49.dp).clickable(onClick=click).padding(horizontal=3.dp),verticalAlignment=Alignment.CenterVertically) {
        if(glyph!=null) {FlightIcon(glyph,Paper.copy(alpha=.7f),Modifier.size(17.dp));Spacer(Modifier.width(12.dp))}
        Text(label,color=Paper,fontSize=13.sp,modifier=Modifier.weight(1f))
        Text(value,color=Paper.copy(alpha=.4f),fontSize=12.sp)
        Spacer(Modifier.width(6.dp));FlightIcon(Glyph.CHEVRON,Paper.copy(alpha=.3f),Modifier.size(12.dp))
    }
}

@Composable
fun InitialMembershipCard(flights: List<FlightSession>,modifier: Modifier=Modifier) {
    val completed=flights.filter {it.state=="COMPLETED" && !it.diagnostic}
    val hours=completed.sumOf {it.creditedFocusMs}/3_600_000
    Box(modifier.fillMaxWidth().aspectRatio(1.62f).clip(RoundedCornerShape(19.dp))
        .background(Brush.linearGradient(listOf(Color(0xFFC7E1EC),Color(0xFF86ACBE),Color(0xFFDDEAF0))))) {
        Canvas(Modifier.matchParentSize()) {
            for(i in 0..60) drawLine(Color.White.copy(alpha=.09f),Offset(size.width*i/60,0f),Offset(size.width*i/60+size.height*.4f,size.height),1f)
            drawLine(Color(0xFF254A64).copy(alpha=.5f),Offset(size.width*.55f,8f),Offset(size.width*.55f,size.height-8f),2f)
        }
        Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Text("FOCUS FLIGHT\nA TIMER FOR GEEKS",color=Color(0xFF365C70).copy(alpha=.65f),fontSize=10.sp,letterSpacing=1.sp)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.SpaceBetween) {
                FlightIcon(Glyph.PLANE,Color(0xFF365C70).copy(alpha=.55f),Modifier.size(28.dp).rotate(30f))
                Column(horizontalAlignment=Alignment.End) {
                    Text("Airfield",color=Color(0xFFF4FAFC),fontSize=25.sp,fontWeight=FontWeight.SemiBold)
                    Text("$hours focus hours",color=Color(0xFF426A7E),fontSize=9.sp)
                }
            }
        }
    }
}
