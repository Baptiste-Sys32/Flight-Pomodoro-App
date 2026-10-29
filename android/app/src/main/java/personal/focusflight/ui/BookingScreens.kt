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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
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
        },mode=prefs.mapMode,labels=prefs.labels,terrain=prefs.terrain,buildings=prefs.buildings,bottomInsetDp=300f,animateDirection=true),Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.12f),Color.Transparent,Color.Black.copy(alpha=.15f)))))
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                GlassIconButton(Glyph.BACK,"Back",back)
                Row {
                    GlassIconButton(Glyph.PALETTE,"Choose map style",{styles=true})
                    GlassIconButton(Glyph.SHUFFLE,"Random route",toggleRandom,selected=random)
                    GlassIconButton(Glyph.SEARCH,"Search destination",search)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().padding(horizontal=24.dp).clip(RoundedCornerShape(20.dp))
                .background(if(prefs.mapMode==MapMode.TERRA) Color(0xEE17201D) else Color(0xF5F6F7F1)).padding(top=10.dp,bottom=5.dp)) {
                DurationRuler(minutes,changeMinutes,if(prefs.mapMode==MapMode.TERRA) Paper else Ink)
            }
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
    var position by remember {mutableFloatStateOf(minutes.toFloat())}
    var dragging by remember {mutableStateOf(false)}
    val currentChange by rememberUpdatedState(change)
    LaunchedEffect(minutes) {if(!dragging) position=minutes.toFloat()}
    val displayed by animateFloatAsState(if(dragging) position else minutes.toFloat(),
        if(dragging) androidx.compose.animation.core.snap() else tween(180),label="Duration ruler position")
    Column(Modifier.fillMaxWidth().height(78.dp).semantics(mergeDescendants=true) {
        contentDescription="Focus duration"
        stateDescription="$minutes minutes"
        progressBarRangeInfo=ProgressBarRangeInfo(minutes.toFloat(),15f..180f,32)
        setProgress { change((kotlin.math.round(it/5)*5).toInt().coerceIn(15,180));true }
        customActions=listOf(CustomAccessibilityAction("Add five minutes") {change((minutes+5).coerceAtMost(180));true},
            CustomAccessibilityAction("Subtract five minutes") {change((minutes-5).coerceAtLeast(15));true})
    }.pointerInput(Unit) {
        detectHorizontalDragGestures(onDragStart={dragging=true},
            onDragEnd={dragging=false},onDragCancel={dragging=false}) {event,amount->
            event.consume()
            position=(position-amount/density/14f*5f).coerceIn(15f,180f)
            currentChange((kotlin.math.round(position/5)*5).toInt().coerceIn(15,180))
        }
    },horizontalAlignment=Alignment.CenterHorizontally) {
        Text("$minutes min",color=foreground,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val center=size.width/2
            val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color=foreground.copy(alpha=.65f).toArgb();textSize=10*density;textAlign=android.graphics.Paint.Align.CENTER
            }
            for(value in 15..180 step 5) {
                val relative=(value-displayed)/5
                val x=center+relative*14*density
                if(x < 0 || x > size.width) continue
                val alpha=(1-abs(x-center)/(size.width/2)).coerceIn(.15f,1f)
                val major=value%15==0
                val top=4*density+(relative*relative/256f*7*density)
                drawLine(foreground.copy(alpha=alpha*.8f),Offset(x,top),Offset(x,top+(if(major) 15 else 8)*density),density)
                if(major) drawContext.canvas.nativeCanvas.drawText(value.toString(),x,top+31*density,paint)
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
    Column(Modifier.fillMaxSize().background(Color(0xFF101211)).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) {
            GlassIconButton(Glyph.BACK,"Back",back)
            Text("Choose your seat",color=Paper,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(start=8.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().semantics {contentDescription="Aircraft cabin"}.verticalScroll(rememberScrollState()),contentAlignment=Alignment.TopCenter) {
            Box(Modifier.widthIn(max=360.dp).fillMaxWidth().height(940.dp)) {
                Canvas(Modifier.matchParentSize()) {
                    val w=size.width;val h=size.height
                    val hull=Path().apply {
                        moveTo(w*.5f,14.dp.toPx());cubicTo(w*.28f,30.dp.toPx(),w*.06f,100.dp.toPx(),w*.06f,230.dp.toPx())
                        lineTo(w*.06f,h-35.dp.toPx());quadraticTo(w*.5f,h+18.dp.toPx(),w*.94f,h-35.dp.toPx())
                        lineTo(w*.94f,230.dp.toPx());cubicTo(w*.94f,100.dp.toPx(),w*.72f,30.dp.toPx(),w*.5f,14.dp.toPx());close()
                    }
                    drawPath(hull,Color(0xFF252A28))
                    drawPath(hull,Color(0xFF414945),style=androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                    for(side in listOf(-1,1)) {
                        val glass=Path().apply {
                            moveTo(w*(.5f+side*.025f),70.dp.toPx())
                            cubicTo(w*(.5f+side*.12f),78.dp.toPx(),w*(.5f+side*.22f),112.dp.toPx(),w*(.5f+side*.25f),146.dp.toPx())
                            lineTo(w*(.5f+side*.025f),135.dp.toPx());close()
                        }
                        drawPath(glass,Color(0xFF101A18))
                    }
                    drawLine(Color(0xFF46514A),Offset(w*.2f,176.dp.toPx()),Offset(w*.8f,176.dp.toPx()),1.dp.toPx())
                    for(row in 0..7) {
                        val y=(257+row*73).dp.toPx()
                        for(x in listOf(w*.085f,w*.895f)) drawRoundRect(Color(0xFF89958D),Offset(x,y),androidx.compose.ui.geometry.Size(w*.02f,19.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    }
                }
                Column(Modifier.fillMaxWidth().padding(top=210.dp,start=47.dp,end=47.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("A","C","","D","F").forEachIndexed {i,label->
                            Text(label,color=Paper.copy(alpha=.5f),fontSize=11.sp,textAlign=TextAlign.Center,
                                modifier=Modifier.weight(if(i==2) .65f else 1f).padding(bottom=12.dp))
                        }
                    }
                    repeat(8) {row->
                        Row(Modifier.fillMaxWidth().height(73.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                            listOf('A','C',' ','D','F').forEachIndexed {col,letter->
                                val key="%02d%c".format(row+1,letter)
                                if(letter==' ') Text("%02d".format(row+1),color=Paper.copy(alpha=.4f),fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.weight(.65f))
                                else {
                                    val kind=SeatFocus[if(col>2) col-1 else col]
                                    Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(10.dp))
                                        .background(if(seat==key) kind.color else Color(0xFF39413C))
                                        .border(1.dp,if(seat==key) kind.color else Color(0xFF505A53),RoundedCornerShape(10.dp))
                                        .semantics {contentDescription="Seat $key, ${kind.name}";selected=seat==key}
                                        .clickable {select(key)},contentAlignment=Alignment.Center) {
                                        Box(Modifier.align(Alignment.BottomCenter).padding(bottom=5.dp).fillMaxWidth(.75f).height(4.dp)
                                            .background(if(seat==key) Ink.copy(alpha=.18f) else Color(0xFF202924),RoundedCornerShape(2.dp)))
                                        FlightIcon(kind.glyph,if(seat==key) Ink else Paper.copy(alpha=.55f),Modifier.size(22.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF101211)).padding(top=16.dp,bottom=20.dp)) {
            val kind=seatKind(seat)
            Row(Modifier.fillMaxWidth().padding(bottom=14.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                FlightIcon(kind.glyph,kind.color,Modifier.size(18.dp));Spacer(Modifier.width(8.dp))
                Text("${kind.name}  ·  $seat",color=Paper,fontSize=13.sp)
            }
            FlightCapsule(if(busy) "Saving boarding pass" else "Confirm",confirm,Modifier.padding(horizontal=30.dp),enabled=!busy)
        }
    }
}

fun FlightDraft.ticket()=TicketData(departureCode,departureCity,if(randomRoute) "???" else arrivalCode,
    if(randomRoute) "Surprise destination" else arrivalCity,(durationMs/60_000).toInt(),distanceMeters,seat,category,"Now",
    SimpleDateFormat("yyyy/MM/dd",Locale.getDefault()).format(Date(bookedWallMs)),id)

@Composable
fun BoardingScreen(draft: FlightDraft,prefs: AppPrefs,back: ()->Unit,board: ()->Unit,requestNotifications: ()->Unit) {
    var tearing by rememberSaveable(draft.id) { mutableStateOf(false) }
    var printed by rememberSaveable(draft.id) {mutableStateOf(false)}
    val paperFeed by animateFloatAsState(if(printed) 1f else 0f,tween(950),label="Ticket printer")
    LaunchedEffect(draft.id) {printed=true}
    val progress by animateFloatAsState(if(tearing) 1f else 0f,tween(420),label="boarding stub")
    LaunchedEffect(tearing) {if(tearing) {delay(450);board()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF111512)).safeDrawingPadding()) {
        GlassIconButton(Glyph.BACK,"Back",back,Modifier.padding(10.dp))
        Spacer(Modifier.weight(.35f))
        Column(Modifier.align(Alignment.CenterHorizontally).widthIn(max=464.dp).padding(horizontal=22.dp)) {
            Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF303A34)).padding(horizontal=7.dp,vertical=3.dp)) {
                Box(Modifier.fillMaxSize().background(Color.Black,RoundedCornerShape(2.dp)))
            }
            Spacer(Modifier.height(3.dp))
            FlightPass(draft.ticket(),prefs.unit,Modifier.drawWithContent {
                clipRect(bottom=size.height*paperFeed) {this@drawWithContent.drawContent()}
            },barcodeModifier=Modifier.graphicsLayer {translationX=progress*size.width*.65f;translationY=progress*50;rotationZ=-progress*12;alpha=1-progress})
        }
        Spacer(Modifier.weight(1f))
        FlightCapsule("Boarding",{requestNotifications();tearing=true},Modifier.padding(horizontal=30.dp),enabled=!tearing && paperFeed>=.99f)
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
fun TakeoffScreen(busy: Boolean,go: ()->Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF0F7D9),Color(0xFFE9F7F4),Color.White)))) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            FlightIcon(Glyph.PLANE,Ink,Modifier.align(Alignment.TopCenter).offset(y=maxHeight*.23f).size(40.dp))
            Text("Cabin doors closed.\nReady for takeoff.",color=Ink,textAlign=TextAlign.Center,fontSize=22.sp,fontWeight=FontWeight.SemiBold,
                modifier=Modifier.align(Alignment.TopCenter).offset(y=maxHeight*.47f))
            Box(Modifier.align(Alignment.TopCenter).offset(y=maxHeight*.73f).width(105.dp).height(44.dp).clip(CircleShape).background(Ink).clickable(enabled=!busy,onClick=go),contentAlignment=Alignment.Center) {
                Text(if(busy) "..." else "GO",color=Paper,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
            }
        }
    }
}
