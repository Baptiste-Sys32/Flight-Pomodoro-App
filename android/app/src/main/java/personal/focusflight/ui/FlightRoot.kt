package personal.focusflight.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import personal.focusflight.BuildConfig
import personal.focusflight.data.local.*
import personal.focusflight.data.preferences.AppPrefs
import personal.focusflight.domain.session.DistanceUnit
import personal.focusflight.domain.session.GeoPoint
import personal.focusflight.domain.session.SessionClock
import personal.focusflight.map.*
import personal.focusflight.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FlightRoot(vm: FlightViewModel,requestNotifications: ()->Unit,renderer: ()->Unit) {
    val uriHandler=androidx.compose.ui.platform.LocalUriHandler.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val flights by vm.flights.collectAsStateWithLifecycle()
    val drafts by vm.drafts.collectAsStateWithLifecycle()
    val airports by vm.airports.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val starting by vm.starting.collectAsStateWithLifecycle()
    val booking by vm.booking.collectAsStateWithLifecycle()
    var screen by rememberSaveable {mutableStateOf("HOME")}
    var minutes by rememberSaveable {mutableIntStateOf(25)}
    var selectedCode by rememberSaveable {mutableStateOf<String?>(null)}
    var random by rememberSaveable {mutableStateOf(false)}
    var seat by rememberSaveable {mutableStateOf("03A")}
    var draftId by rememberSaveable {mutableStateOf<String?>(null)}
    var passReturn by rememberSaveable {mutableStateOf("SEATS")}
    var recordId by rememberSaveable {mutableStateOf<String?>(null)}
    FocusTheme {
        val p=prefs
        val saved=flights
        val planned=drafts
        if(p==null || saved==null || planned==null || airports.isEmpty()) {
            Box(Modifier.fillMaxSize().background(Ink),contentAlignment=Alignment.Center) {Text("Preparing your flight",color=Paper.copy(alpha=.6f))}
            if(error!=null) FocusPanel("Unable to load",vm::clearError) {Text(error.orEmpty(),color=Paper)}
            return@FocusTheme
        }
        val home=airports.firstOrNull {it.airport.code==p.homeCode} ?: airports.first {it.airport.code=="CDG"}
        val current=saved.firstOrNull {it.isOngoing}
        val view=LocalView.current
        DisposableEffect(current?.id,p.keepAwake) {
            view.keepScreenOn=current!=null && p.keepAwake
            onDispose {view.keepScreenOn=false}
        }
        var now by remember {mutableStateOf(vm.clock())}
        val owner=LocalLifecycleOwner.current
        LaunchedEffect(current?.id,current?.state,owner) {
            now=vm.clock()
            if(current?.state=="ACTIVE") owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while(true) {now=vm.clock();delay(250)}
            }
        }
        val mapChange:(MapMode?,Boolean?,Boolean?,Boolean?)->Unit={m,l,t,b->vm.changePreferences {map(m,l,t,b)}}
        if(current!=null) {
            FlightDeck(current,SessionClock.remaining(current.anchor(),now),p,vm::command,mapChange,
                {enabled->vm.changePreferences {awake(enabled)}})
        } else {
            val options by produceState<List<FlightOption>>(emptyList(),home.airport.code,minutes) {
                value=withContext(Dispatchers.Default) {vm.options(home,minutes)}
            }
            val selected=selectedCode?.let {code->airports.firstOrNull {it.airport.code==code}?.let {vm.option(home,it)}} ?: options.firstOrNull()
            val draft=planned.firstOrNull {it.id==draftId}
            fun back() {
                screen=when(screen) {"DESTINATION"->"BOOK";"SEATS"->"BOOK";"PASS"->passReturn;"TAKEOFF"->"PASS";"RECORD"->"LOG";else->"HOME"}
            }
            BackHandler(enabled=screen!="HOME") {if(!starting && !booking) back()}
            if(!p.airportChosen || screen=="ORIGIN") {
                AirportPicker(airports,home,"Choose your departure airport",close=if(p.airportChosen) ({screen="HOME"}) else null,mode=p.mapMode) {
                    entry->vm.changePreferences {home(entry.airport.code)};selectedCode=null;screen="HOME"
                }
            } else when(screen) {
                "BOOK"->BookingScreen(home,options,selected,minutes,random,p,
                    {minutes=it;selectedCode=null},{selectedCode=it.destination.airport.code},{random=!random},
                    {screen="DESTINATION"},{screen="HOME"},{
                        if(random) selectedCode=options.randomOrNull()?.destination?.airport?.code
                        screen="SEATS"
                    },mapChange)
                "DESTINATION"->AirportPicker(airports,home,"Choose your destination",home.airport.code,{screen="BOOK"},mode=p.mapMode) {
                    selectedCode=it.airport.code;random=false;screen="BOOK"
                }
                "SEATS"->SeatScreen(seat,{seat=it},{screen="BOOK"},{
                    selected?.let {route->vm.book(home,route,minutes,seatKind(seat).name,seat,random,draftId) {
                        draftId=it;passReturn="SEATS";screen="PASS"
                    }}
                },booking)
                "PASS"->if(draft!=null) BoardingScreen(draft,p,::back,{screen="TAKEOFF"},requestNotifications)
                    else LoadingPass()
                "TAKEOFF"->TakeoffScreen(starting) {draftId?.let {vm.startDraft(it) {screen="HOME"}}}
                "LOG"->FlightLogScreen(saved,planned,p,{
                    screen="HOME"
                },{id->draftId=id;passReturn="LOG";screen="PASS"},{id->recordId=id;screen="RECORD"})
                "RECORD"->saved.firstOrNull {it.id==recordId}?.let {flight->RecordScreen(flight,p,{screen="LOG"}) {vm.saveNote(flight.id,it)}}
                    ?: LoadingPass()
                "WORLD"->WorldScreen(home,saved,p,{screen="HOME"})
                else->{
                    HomeScreen(home,p,{
                        draftId=null;seat="03A";selectedCode=null;screen="BOOK"
                    },{screen="ORIGIN"},{screen=it.uppercase(Locale.ROOT)})
                    when(screen) {
                        "MINE"->FocusPanel("Mine",{screen="HOME"}) {
                            InitialMembershipCard(saved)
                            Spacer(Modifier.height(15.dp))
                            MenuRow("FlightLog",glyph=Glyph.NOTE) {screen="LOG"}
                            MenuRow("Home Map Style",p.mapMode.title,Glyph.MAP) {screen="MAP"}
                            MenuRow("Settings",glyph=Glyph.SETTINGS) {screen="SETTINGS"}
                        }
                        "MAP"->MapStylePanel(p,{screen="SETTINGS"},mapChange)
                        "SETTINGS"->FocusPanel("Profile and settings",{screen="HOME"}) {
                            MenuRow("FlightLog",glyph=Glyph.NOTE) {screen="LOG"}
                            MenuRow("Departure airport",p.homeCode) {screen="ORIGIN"}
                            MenuRow("Map style",p.mapMode.title) {screen="MAP"}
                            MenuRow("Asset credits") {screen="CREDITS"}
                            MenuRow("Units",p.unit.label) {vm.changePreferences {units(if(p.unit==DistanceUnit.KILOMETERS) DistanceUnit.NAUTICAL_MILES else DistanceUnit.KILOMETERS)}}
                            SettingsToggle("Keep Screen Awake",p.keepAwake) {enabled->vm.changePreferences {awake(enabled)}}
                        }
                        "CREDITS"->FocusPanel("Asset credits",{screen="SETTINGS"}) {
                            Text("A320 model by amvlab (2026), CC BY 4.0. Unbranded model; display scale and orientation configured by this app.",color=Paper.copy(alpha=.7f),fontSize=12.sp)
                            MenuRow("amvlab aircraft models") {uriHandler.openUri("https://github.com/amvlab/aircraft-models")}
                            MenuRow("Creative Commons BY 4.0") {uriHandler.openUri("https://creativecommons.org/licenses/by/4.0/")}
                            MenuRow("City scenery: Kenney", "CC0") {uriHandler.openUri("https://kenney.nl/assets/city-kit-commercial")}
                            Text("UI icons: Lucide (ISC/MIT). Font: Inter (SIL Open Font License). License notices are bundled with the app.",color=Paper.copy(alpha=.7f),fontSize=12.sp)
                        }
                        "TRENDS"->TrendsPanel(saved,{screen="HOME"})
                    }
                }
            }
        }
        if(error!=null) FocusPanel("Flight update",vm::clearError) {Text(error.orEmpty(),color=Paper.copy(alpha=.7f))}
    }
}

@Composable private fun LoadingPass() {
    Box(Modifier.fillMaxSize().background(Ink),contentAlignment=Alignment.Center) {Text("Preparing boarding pass",color=Paper.copy(alpha=.5f))}
}

@Composable
fun FlightLogScreen(flights: List<FlightSession>,drafts: List<FlightDraft>,prefs: AppPrefs,back: ()->Unit,
                    openDraft: (String)->Unit,openRecord: (String)->Unit) {
    var status by rememberSaveable {mutableStateOf("All")}
    var scenario by rememberSaveable {mutableStateOf("All")}
    var newest by rememberSaveable {mutableStateOf(true)}
    var menu by remember {mutableStateOf<String?>(null)}
    val statuses=listOf("All","Planning","In progress","Landed","Canceled")
    val categories=listOf("All")+flights.map {it.category}.plus(drafts.map {it.category}).distinct()
    val visible=flights.filter { !it.diagnostic && (scenario=="All" || it.category==scenario) && when(status) {
        "Planning"->false;"In progress"->it.isOngoing;"Landed"->it.state=="COMPLETED";"Canceled"->it.state=="CANCELED";else->true
    }}.let {if(newest) it else it.reversed()}
    val planning=drafts.filter {(status=="All" || status=="Planning") && (scenario=="All" || it.category==scenario)}.let {if(newest) it else it.reversed()}
    val rows=(planning.map {LogItem(it.bookedWallMs,"plan-${it.id}",draft=it)}+
        visible.map {LogItem(it.startedWallMs,"flight-${it.id}",flight=it)})
        .sortedWith(compareBy<LogItem> {it.time}.thenBy {it.key}).let {if(newest) it.reversed() else it}
    Column(Modifier.fillMaxSize().background(Ink).safeDrawingPadding()) {
        ScreenHeader("FlightLog",back)
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterPill(if(status=="All") "Status" else status,Modifier.weight(1f)) {menu="Status"}
            FilterPill(if(scenario=="All") "Scenario" else scenario,Modifier.weight(1f)) {menu="Scenario"}
            FilterPill("Sort By",Modifier.weight(.7f)) {menu="Sort By"}
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            items(rows,key={it.key}) {row->
                row.draft?.let {d->FlightPass(d.ticket().copy(boarding="Planned",state="PLANNING"),prefs.unit,
                    Modifier.fillMaxWidth().clickable {openDraft(d.id)},showBarcode=false)}
                row.flight?.let {f->FlightPass(f.ticket(),prefs.unit,Modifier.fillMaxWidth().clickable {openRecord(f.id)},showBarcode=false)}
            }
            if(visible.isEmpty() && planning.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical=65.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    FlightIcon(Glyph.NOTE,Paper.copy(alpha=.15f),Modifier.size(80.dp))
                    Text("No flights here yet",color=Paper.copy(alpha=.4f),modifier=Modifier.padding(18.dp))
                }
            }
        }
    }
    if(menu!=null) FocusPanel(menu.orEmpty(),{menu=null}) {
        val choices=when(menu) {"Status"->statuses;"Scenario"->categories;else->listOf("Newest first","Oldest first")}
        choices.forEach {choice->MenuRow(choice) {
            when(menu) {"Status"->status=choice;"Scenario"->scenario=choice;else->newest=choice=="Newest first"}
            menu=null
        }}
    }
}

private data class LogItem(val time: Long,val key: String,val draft: FlightDraft?=null,val flight: FlightSession?=null)

@Composable
fun FilterPill(label: String,modifier: Modifier=Modifier,selected: Boolean=false,click: ()->Unit) {
    Text(label,color=Paper.copy(alpha=.7f),fontSize=10.sp,maxLines=1,
        modifier=modifier.clip(RoundedCornerShape(20.dp)).background(if(selected) Color(0xFF555B56) else Color(0xFF292D2A)).clickable(onClick=click).padding(horizontal=11.dp,vertical=12.dp))
}

@Composable fun ScreenHeader(title: String,back: ()->Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
        GlassIconButton(Glyph.BACK,"Back",back)
        Text(title,color=Paper,fontSize=19.sp,fontWeight=FontWeight.SemiBold)
    }
}

fun FlightSession.ticket():TicketData {
    val hidden=randomRoute && state!="COMPLETED"
    return TicketData(departureCode,departureCity,if(hidden) "???" else arrivalCode,if(hidden) "Surprise destination" else arrivalCity,
        (durationMs/60_000).toInt(),distanceMeters,seat,category,SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date(startedWallMs)),
        SimpleDateFormat("yyyy/MM/dd",Locale.getDefault()).format(Date(startedWallMs)),id,
        when(state) {"COMPLETED"->"LANDED";"CANCELED"->"CANCELED";else->"IN PROGRESS"})
}

@Composable
fun RecordScreen(flight: FlightSession,prefs: AppPrefs,back: ()->Unit,save: (String)->Unit) {
    var note by rememberSaveable(flight.id) {mutableStateOf(flight.note)}
    var editing by remember {mutableStateOf(false)}
    val from=GeoPoint(flight.departureLat,flight.departureLon)
    val to=GeoPoint(flight.arrivalLat,flight.arrivalLon)
    Box(Modifier.fillMaxSize().background(Ink)) {
        FlightMap(MapScene(from,to,mode=prefs.mapMode,labels=prefs.labels,bottomInsetDp=420f,showRoute=!flight.randomRoute || flight.state=="COMPLETED"),Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            GlassIconButton(Glyph.BACK,"Back",back,Modifier.padding(10.dp))
            Spacer(Modifier.weight(1f))
            Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()).padding(20.dp)) {
                FlightPass(flight.ticket(),prefs.unit,Modifier.fillMaxWidth(),paper=true)
                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Text("Notes",color=Paper,fontWeight=FontWeight.SemiBold)
                        TextButton(onClick={if(editing) save(note);editing=!editing}) {Text(if(editing) "Save" else "Edit",color=Paper)}
                    }
                    if(editing) androidx.compose.foundation.text.BasicTextField(note,{note=it.take(20000)},
                        textStyle=androidx.compose.ui.text.TextStyle(color=Paper,fontFamily=Inter,fontSize=14.sp),modifier=Modifier.fillMaxWidth().heightIn(min=80.dp))
                    else Text(note.ifBlank {"No notes"},color=Paper.copy(alpha=.6f),fontSize=13.sp)
                }
            }
        }
    }
}

@Composable
fun TrendsPanel(flights: List<FlightSession>,close: ()->Unit) {
    var period by remember {mutableStateOf("Total")}
    val date=java.time.LocalDate.now()
    val zone=java.time.ZoneId.systemDefault()
    val start=when(period) {"Year"->date.withDayOfYear(1);"Month"->date.withDayOfMonth(1);"Week"->date.with(java.time.DayOfWeek.MONDAY);else->date}
    val records=flights.filter {it.state=="COMPLETED" && !it.diagnostic && (period=="Total" || (it.endedWallMs ?: 0)>=start.atStartOfDay(zone).toInstant().toEpochMilli())}
    FocusPanel("Trends",close) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            listOf("Total","Year","Month","Week","Day").forEach {label->FilterPill(label,Modifier.weight(1f),selected=period==label) {period=label}}
        }
        Spacer(Modifier.height(25.dp))
        Text("${records.sumOf {it.creditedFocusMs}/60_000} min",color=Paper,fontSize=35.sp,fontWeight=FontWeight.SemiBold)
        Text("Completed focus time · ${records.size} flights",color=Paper.copy(alpha=.5f),fontSize=12.sp)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun WorldScreen(home: AirportEntry,flights: List<FlightSession>,prefs: AppPrefs,back: ()->Unit) {
    val completed=flights.filter {it.state=="COMPLETED" && !it.diagnostic}
    val distance=completed.sumOf {it.creditedDistanceMeters}
    val visited=completed.flatMap {listOf(it.departureCode,it.arrivalCode)}.toSet().size
    Box(Modifier.fillMaxSize().background(Ink)) {
        FlightMap(MapScene(GeoPoint(home.airport.lat,home.airport.lon),globe=true,mode=prefs.mapMode,bottomInsetDp=220f),Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(18.dp)) {
            GlassIconButton(Glyph.BACK,"Back",back)
            Spacer(Modifier.weight(1f))
            GlassPanel(Modifier.fillMaxWidth()) {
                Text("AROUND EARTH",color=Paper.copy(alpha=.4f),fontSize=10.sp)
                Text("%.2fx".format(distance/40_075_016.686),color=Paper,fontSize=36.sp,fontWeight=FontWeight.SemiBold)
                Text("${distanceText(distance,prefs.unit)} simulated",color=Paper.copy(alpha=.6f),fontSize=12.sp)
                Spacer(Modifier.height(18.dp))
                Text("$visited airports visited",color=Paper,fontSize=14.sp)
            }
        }
    }
}
