package personal.focusflight.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import personal.focusflight.data.local.FlightDraft
import personal.focusflight.data.preferences.AppPrefs
import personal.focusflight.domain.session.GeoPoint
import personal.focusflight.map.*
import personal.focusflight.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun BookingScreen(from: AirportEntry, options: List<FlightOption>, selected: FlightOption?, minutes: Int, random: Boolean,
                  prefs: AppPrefs, changeMinutes: (Int)->Unit, select: (FlightOption)->Unit,
                  toggleRandom: ()->Unit, search: ()->Unit, back: ()->Unit, book: ()->Unit,
                  changeMap: (MapMode?,Boolean?,Boolean?,Boolean?)->Unit) {
    var styles by remember { mutableStateOf(false) }
    val fromPoint=GeoPoint(from.airport.lat,from.airport.lon)
    val toPoint=selected?.destination?.airport?.let { GeoPoint(it.lat,it.lon) }?.takeUnless { random }
    Box(Modifier.fillMaxSize().background(Ink)) {
        FlightMap(MapScene(fromPoint,toPoint,buildList {
            add(MapAirport(from.airport.code,fromPoint,true))
            if(toPoint!=null && selected!=null) add(MapAirport(selected.destination.airport.code,toPoint))
        },mode=prefs.mapMode,labels=prefs.labels,terrain=prefs.terrain,buildings=prefs.buildings,bottomInsetDp=300f),Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.12f),Color.Transparent,Color.Black.copy(alpha=.15f)))))
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                GlassIconButton(Glyph.BACK,"Back",back)
                Row {
                    GlassIconButton(Glyph.MAP,"Choose map style",{styles=true})
                    GlassIconButton(Glyph.SHUFFLE,"Random route",toggleRandom,selected=random)
                    GlassIconButton(Glyph.SEARCH,"Search destination",search)
                }
            }
            Spacer(Modifier.weight(1f))
            DurationRuler(minutes,changeMinutes,if(prefs.mapMode==MapMode.TERRA) Paper else Ink)
            Spacer(Modifier.height(15.dp))
            if(random) Box(Modifier.fillMaxWidth().height(128.dp),contentAlignment=Alignment.Center) {
                Column(Modifier.width(132.dp).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Paper).padding(16.dp)) {
                    AirportBadge("???",true)
                    Spacer(Modifier.weight(1f))
                    Text("Somewhere new",color=Ink,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                    Text("$minutes min",color=Ink.copy(alpha=.5f),fontSize=12.sp)
                }
            } else DestinationCards(options,selected,minutes,select)
            Spacer(Modifier.height(16.dp))
            FlightCapsule("Book My Flight",book,Modifier.padding(horizontal=30.dp),enabled=selected!=null,pastel=true)
            Spacer(Modifier.height(25.dp))
        }
        if(styles) MapStylePanel(prefs,{styles=false},changeMap)
    }
}

@Composable
private fun DurationRuler(minutes: Int, change: (Int)->Unit,foreground: Color) {
    val density=LocalDensity.current.density
    var drag by remember { mutableFloatStateOf(0f) }
    val current by rememberUpdatedState(minutes)
    Column(Modifier.fillMaxWidth().height(56.dp).semantics(mergeDescendants=true) {
        contentDescription="Focus duration"
        stateDescription="$minutes minutes"
        progressBarRangeInfo=ProgressBarRangeInfo(minutes.toFloat(),15f..180f,32)
        setProgress { change((it.toInt()/5*5).coerceIn(15,180));true }
        customActions=listOf(CustomAccessibilityAction("Add five minutes") {change((current+5).coerceAtMost(180));true},
            CustomAccessibilityAction("Subtract five minutes") {change((current-5).coerceAtLeast(15));true})
    }.pointerInput(Unit) {
        detectHorizontalDragGestures(onDragStart={drag=0f}) { event,amount ->
            event.consume();drag+=amount/density
            while(abs(drag)>=14f) {
                val step=if(drag>0) -5 else 5
                change((current+step).coerceIn(15,180))
                drag+=if(drag>0) -14f else 14f
            }
        }
    },horizontalAlignment=Alignment.CenterHorizontally) {
        Text("$minutes min",color=foreground,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val center=size.width/2
            val gap=14*density
            for(i in -16..16) {
                val x=center+i*gap
                val curve=(i*i/256f)*12*density
                val height=if(i%3==0) 12*density else 6*density
                drawLine(foreground.copy(alpha=(1-abs(i)/17f)*.7f),Offset(x,curve+height),Offset(x,curve+height*2),density)
            }
            drawLine(FlightYellow,Offset(center,0f),Offset(center,22*density),2*density)
        }
    }
}

@Composable
private fun DestinationCards(options: List<FlightOption>, selected: FlightOption?, minutes: Int, select: (FlightOption)->Unit) {
    val state=rememberLazyListState()
    val scope=rememberCoroutineScope()
    LaunchedEffect(options) { if(options.isNotEmpty()) state.scrollToItem(0) }
    LaunchedEffect(options,state) {
        snapshotFlow {
            if(state.isScrollInProgress) null else state.layoutInfo.visibleItemsInfo.minByOrNull {
                abs(it.offset+it.size/2-(state.layoutInfo.viewportStartOffset+state.layoutInfo.viewportEndOffset)/2)
            }?.index
        }.distinctUntilChanged().collect { index -> options.getOrNull(index ?: -1)?.let(select) }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(128.dp)) {
        LazyRow(state=state,flingBehavior=rememberSnapFlingBehavior(state),
            contentPadding=PaddingValues(horizontal=(maxWidth-112.dp)/2),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            itemsIndexed(options,key={_,item->item.destination.airport.code}) { index,item ->
                val chosen=item.destination.airport.code==selected?.destination?.airport?.code
                Column(Modifier.width(112.dp).fillMaxHeight().clip(RoundedCornerShape(19.dp))
                    .background(if(chosen) Paper else Color(0xEC101712))
                    .clickable {select(item);scope.launch {state.animateScrollToItem(index)}}.padding(14.dp)) {
                    AirportBadge(item.destination.airport.code,chosen)
                    Spacer(Modifier.weight(1f))
                    Text(item.destination.displayCity,color=if(chosen) Ink else Paper,fontSize=12.sp,fontWeight=FontWeight.SemiBold,
                        maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text(if(chosen) "$minutes min" else "~${item.minutes} min",color=(if(chosen) Ink else Paper).copy(alpha=.5f),fontSize=12.sp)
                }
            }
        }
    }
}

@Composable
fun AirportBadge(code: String, selected: Boolean=false) {
    Text(code,color=if(selected) Ink else FlightYellow,fontWeight=FontWeight.Bold,fontSize=12.sp,
        modifier=Modifier.clip(RoundedCornerShape(5.dp)).background(if(selected) FlightYellow else Color.Transparent)
            .border(1.dp,if(selected) Ink.copy(alpha=.6f) else FlightYellow,RoundedCornerShape(5.dp)).padding(horizontal=8.dp,vertical=5.dp))
}

private val SeatFocus=listOf(FocusKinds[0],FocusKinds[2],FocusKinds[3],FocusKinds[1])
fun seatKind(seat: String)=SeatFocus[listOf('A','C','D','F').indexOf(seat.lastOrNull()).coerceAtLeast(0)]

@Composable
fun SeatScreen(seat: String, select: (String)->Unit, back: ()->Unit, confirm: ()->Unit, busy: Boolean) {
    Box(Modifier.fillMaxSize().background(Color(0xFF101211))) {
        Box(Modifier.fillMaxSize().padding(horizontal=14.dp).background(Color(0xFF202321),RoundedCornerShape(topStart=180.dp,topEnd=180.dp)))
        Canvas(Modifier.fillMaxWidth().height(160.dp).padding(horizontal=95.dp,vertical=35.dp)) {
            val p=Path().apply {moveTo(size.width*.46f,0f);lineTo(size.width*.46f,size.height*.45f);lineTo(0f,size.height*.75f);quadraticTo(0f,size.height*.2f,size.width*.46f,0f)}
            drawPath(p,Color(0xFF343835))
            val q=Path().apply {moveTo(size.width*.54f,0f);lineTo(size.width*.54f,size.height*.45f);lineTo(size.width,size.height*.75f);quadraticTo(size.width,size.height*.2f,size.width*.54f,0f)}
            drawPath(q,Color(0xFF343835))
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            GlassIconButton(Glyph.BACK,"Back",back,Modifier.padding(10.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(top=185.dp,bottom=25.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                item {
                    Row(Modifier.widthIn(max=350.dp).fillMaxWidth().padding(horizontal=25.dp),verticalAlignment=Alignment.CenterVertically) {
                        listOf("A","C","","D","F").forEachIndexed { index,label ->
                            Text(label,color=Paper.copy(alpha=.4f),fontSize=12.sp,textAlign=TextAlign.Center,
                                modifier=Modifier.weight(if(index==2) .7f else 1f).padding(bottom=13.dp))
                        }
                    }
                }
                items(8) { row ->
                    Row(Modifier.widthIn(max=350.dp).fillMaxWidth().padding(horizontal=25.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf('A','C',' ','D','F').forEachIndexed { col,letter ->
                            val key="%02d%c".format(row+1,letter)
                            if(letter==' ') Text("%02d".format(row+1),color=Paper.copy(alpha=.35f),fontSize=11.sp,textAlign=TextAlign.Center,modifier=Modifier.weight(.7f))
                            else {
                                val kind=SeatFocus[if(col>2) col-1 else col]
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if(seat==key) kind.color else Color(0xFF303431))
                                    .semantics {contentDescription="Seat $key, ${kind.name}";selected=seat==key}
                                    .clickable {select(key)},contentAlignment=Alignment.Center) {
                                    if(seat==key) FlightIcon(kind.glyph,Ink,Modifier.size(25.dp))
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                val kind=seatKind(seat)
                FlightIcon(kind.glyph,kind.color,Modifier.size(16.dp));Spacer(Modifier.width(8.dp))
                Text("${kind.name}  ·  $seat",color=Paper.copy(alpha=.7f),fontSize=12.sp)
            }
            FlightCapsule(if(busy) "Saving boarding pass" else "Confirm",confirm,Modifier.padding(horizontal=30.dp),enabled=!busy)
            Spacer(Modifier.height(25.dp))
        }
    }
}

fun FlightDraft.ticket()=TicketData(departureCode,departureCity,if(randomRoute) "???" else arrivalCode,
    if(randomRoute) "Surprise destination" else arrivalCity,(durationMs/60_000).toInt(),distanceMeters,seat,category,"Now",
    SimpleDateFormat("yyyy/MM/dd",Locale.getDefault()).format(Date(bookedWallMs)),id)

@Composable
fun BoardingScreen(draft: FlightDraft,prefs: AppPrefs,back: ()->Unit,board: ()->Unit,requestNotifications: ()->Unit) {
    var tearing by rememberSaveable(draft.id) { mutableStateOf(false) }
    val progress by animateFloatAsState(if(tearing) 1f else 0f,tween(420),label="boarding stub")
    LaunchedEffect(tearing) {if(tearing) {delay(450);board()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF111512)).safeDrawingPadding()) {
        GlassIconButton(Glyph.BACK,"Back",back,Modifier.padding(10.dp))
        Spacer(Modifier.weight(.35f))
        FlightPass(draft.ticket(),prefs.unit,Modifier.align(Alignment.CenterHorizontally).padding(horizontal=22.dp),
            barcodeModifier=Modifier.graphicsLayer {translationX=progress*size.width*.65f;translationY=progress*50;rotationZ=-progress*12;alpha=1-progress})
        Spacer(Modifier.weight(1f))
        FlightCapsule("Boarding",{requestNotifications();tearing=true},Modifier.padding(horizontal=30.dp),enabled=!tearing)
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
fun TakeoffScreen(busy: Boolean,go: ()->Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF0F7D9),Color(0xFFE9F7F4),Color.White)))) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            FlightIcon(Glyph.PLANE,Ink,Modifier.align(Alignment.TopCenter).offset(y=maxHeight*.23f).size(35.dp).rotate(60f))
            Text("Cabin doors closed.\nReady for takeoff.",color=Ink,textAlign=TextAlign.Center,fontSize=22.sp,fontWeight=FontWeight.SemiBold,
                modifier=Modifier.align(Alignment.TopCenter).offset(y=maxHeight*.47f))
            Box(Modifier.align(Alignment.TopCenter).offset(y=maxHeight*.73f).width(105.dp).height(44.dp).clip(CircleShape).background(Ink).clickable(enabled=!busy,onClick=go),contentAlignment=Alignment.Center) {
                Text(if(busy) "..." else "GO",color=Paper,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
            }
        }
    }
}
