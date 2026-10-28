package personal.focusflight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun FlightPass(ticket: TicketData, unit: DistanceUnit, modifier: Modifier = Modifier, paper: Boolean = false,
               barcodeModifier: Modifier = Modifier, showBarcode: Boolean = true) {
    val foreground = if (paper) Ink else Paper
    val background = if (paper) Color(0xFFF1F1EA) else Color(0xFF202522)
    val muted = foreground.copy(alpha = .45f)
    Column(modifier.widthIn(max = 420.dp)) {
        Box(Modifier.fillMaxWidth().background(background, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp,
            bottomStart = if(showBarcode) 0.dp else 10.dp,bottomEnd = if(showBarcode) 0.dp else 10.dp))) {
            Canvas(Modifier.matchParentSize()) {
                val tint = foreground.copy(alpha = .035f)
                drawOval(tint, Offset(size.width*.23f,size.height*.07f), Size(size.width*.55f,size.height*.85f),style=Stroke(size.width*.11f))
                for(i in 0..4) drawLine(tint,Offset(size.width*.15f,size.height*(i/5f)),Offset(size.width*.9f,size.height*(i/5f)+35),8f)
            }
            Column(Modifier.padding(20.dp)) {
                if(ticket.state.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Text(ticket.date,color=muted,fontSize=9.sp)
                        Text(ticket.state,color=if(ticket.state=="LANDED") Color(0xFF79C59B) else muted,fontSize=9.sp,fontWeight=FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(13.dp))
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ticket.fromCode,color=foreground,fontSize=38.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
                        Text(ticket.fromCity,color=muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(horizontal=9.dp)) {
                        FlightIcon(Glyph.PLANE,muted,Modifier.size(16.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(shortDuration(ticket.minutes),color=muted,fontSize=10.sp)
                    }
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.End) {
                        Text(ticket.toCode,color=foreground,fontSize=38.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
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
                var x = 8f
                while(x < size.width-8) { drawLine(muted.copy(alpha=.4f),Offset(x,0f),Offset(x+4f,0f),1f);x+=8f }
            }
            Box(barcodeModifier.fillMaxWidth().background(background,RoundedCornerShape(bottomStart=10.dp,bottomEnd=10.dp)).padding(20.dp)) {
                TicketBarcode(ticket.id,Modifier.fillMaxWidth().height(51.dp))
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

/** Decorative ticket pattern, not a claim to encode a scannable airline boarding pass. */
@Composable
fun TicketBarcode(value: String,modifier: Modifier=Modifier) {
    Canvas(modifier) {
        drawRect(Color.White)
        val unit=size.width/228f
        val rowHeight=size.height/8f
        var seed=value.hashCode()
        for(row in 0..7) {
            var column=8
            while(column<220) {
                seed=seed*1664525+1013904223
                val width=1+((seed ushr 20) and 3)
                if((seed ushr 28) and 1 == 1) drawRect(Color.Black,Offset(column*unit,row*rowHeight),Size(width*unit,rowHeight))
                column+=width
            }
        }
        drawRect(Color.Black,Offset(unit,0f),Size(4*unit,size.height))
        drawRect(Color.Black,Offset(222*unit,0f),Size(2*unit,size.height))
    }
}
