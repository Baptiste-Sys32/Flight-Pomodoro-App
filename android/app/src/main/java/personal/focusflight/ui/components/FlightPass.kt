package personal.focusflight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import personal.focusflight.domain.session.DistanceUnit
import java.text.NumberFormat

data class TicketData(
    val fromCode: String, val fromCity: String, val toCode: String, val toCity: String,
    val minutes: Int, val distanceMeters: Long, val seat: String, val focus: String,
    val boarding: String, val date: String, val id: String, val state: String = "",
)

fun distanceText(meters: Long, unit: DistanceUnit): String =
    "${NumberFormat.getIntegerInstance().format(unit.value(meters.toDouble()).toLong())} ${unit.label}"

fun shortDuration(minutes: Int): String = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"

@Composable
fun FlightPass(ticket: TicketData, unit: DistanceUnit, modifier: Modifier = Modifier, paper: Boolean = true,
               barcodeModifier: Modifier = Modifier, showBarcode: Boolean = true) {
    val foreground = if (paper) Ink else Paper
    val background = if (paper) Color(0xFFF6F5EF) else Color(0xFF202522)
    val muted = foreground.copy(alpha = .63f)
    Column(modifier.widthIn(max = 420.dp)) {
        Box(Modifier.fillMaxWidth().background(background, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp,
            bottomStart = if(showBarcode) 0.dp else 10.dp,bottomEnd = if(showBarcode) 0.dp else 10.dp))) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("FOCUS FLIGHT",color=foreground,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp)
                    Text("BOARDING PASS",color=muted,fontSize=9.sp,letterSpacing=1.sp)
                }
                Spacer(Modifier.height(24.dp))
                if(ticket.state.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Text(ticket.date,color=muted,fontSize=9.sp)
                        Text(ticket.state,color=if(ticket.state=="LANDED") Color(0xFF79C59B) else muted,fontSize=9.sp,fontWeight=FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(13.dp))
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ticket.fromCode,color=foreground,fontSize=40.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
                        Text(ticket.fromCity,color=muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(horizontal=9.dp)) {
                        FlightIcon(Glyph.PLANE,foreground.copy(alpha=.75f),Modifier.size(21.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(shortDuration(ticket.minutes),color=muted,fontSize=10.sp)
                    }
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.End) {
                        Text(ticket.toCode,color=foreground,fontSize=40.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
                        Text(ticket.toCity,color=muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    PassField("Seat",ticket.seat,foreground)
                    PassField("Distance",distanceText(ticket.distanceMeters,unit),foreground,true)
                }
                Spacer(Modifier.height(11.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    PassField("Boarding",ticket.boarding,foreground)
                    PassField("Date",ticket.date,foreground,true)
                }
            }
        }
        if(showBarcode) {
            Canvas(Modifier.fillMaxWidth().height(1.dp).background(background)) {
                var x = 16.dp.toPx()
                while(x < size.width-16.dp.toPx()) { drawLine(muted.copy(alpha=.35f),Offset(x,0f),Offset(x+4.dp.toPx(),0f),1.dp.toPx());x+=8.dp.toPx() }
            }
            Box(barcodeModifier.fillMaxWidth().background(background,RoundedCornerShape(bottomStart=10.dp,bottomEnd=10.dp)).padding(20.dp)) {
                Column {
                    TicketBarcode(ticket.id,Modifier.fillMaxWidth().height(65.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("${ticket.focus.uppercase()}  /  ${ticket.id.take(8).uppercase()}",color=muted,fontSize=8.sp,letterSpacing=1.sp)
                }
            }
        }
    }
}

@Composable
private fun PassField(label: String,value: String,color: Color,end: Boolean=false) {
    Column(horizontalAlignment=if(end) Alignment.End else Alignment.Start) {
        Text(label,color=color.copy(alpha=.4f),fontSize=11.sp)
        Text(value,color=color,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
    }
}

/** A real PDF417 encoding of a local reference, never an airline or travel credential. */
@Composable
fun TicketBarcode(value: String,modifier: Modifier=Modifier) {
    val bitmap=remember(value) {
        val matrix=personal.focusflight.domain.ticket.PassBarcode.encode(value)
        android.graphics.Bitmap.createBitmap(matrix.width,matrix.height,android.graphics.Bitmap.Config.ARGB_8888).apply {
            val pixels=IntArray(matrix.width*matrix.height) {i->if(matrix[i%matrix.width,i/matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE}
            setPixels(pixels,0,matrix.width,0,0,matrix.width,matrix.height)
        }.asImageBitmap()
    }
    Image(bitmap,"Boarding pass barcode",modifier.background(Color.White),contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
