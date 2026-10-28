package personal.focusflight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import personal.focusflight.R

val Ink = Color(0xFF141716)
val Paper = Color(0xFFF2F2EA)
val FlightYellow = Color(0xFFF0D95D)
val Glass = Color(0xDA1B201E)
val Inter = FontFamily(Font(R.font.inter_variable, FontWeight.Normal), Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold), Font(R.font.inter_variable, FontWeight.Bold))

@Composable
fun FocusTheme(content: @Composable () -> Unit) {
    val typography = Typography(
        bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontSize = 15.sp),
        bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontSize = 13.sp),
        bodySmall = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontSize = 11.sp),
        titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
        titleMedium = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp))
    MaterialTheme(colorScheme = darkColorScheme(primary = Paper, onPrimary = Ink, background = Ink,
        surface = Color(0xFF1C201F), onSurface = Paper, secondary = FlightYellow), typography = typography, content = content)
}

enum class Glyph { BACK, CLOSE, PAUSE, PLAY, PLANE, SOUND, WINDOW, GLOBE, MAP, ROUTE, SEARCH, SHUFFLE,
    LOCATION, SETTINGS, BOOK, LAPTOP, PALETTE, NOTE, CHART, CARD, FILTER, PLUS, CHECK, CHEVRON, TRASH }

@Composable
fun FlightIcon(glyph: Glyph, color: Color = Color.White, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier) {
        scale(size.width / 24, size.height / 24, Offset.Zero) {
            val stroke = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(x: Float, y: Float, a: Float, b: Float) = drawLine(color, Offset(x,y), Offset(a,b), 1.6f, StrokeCap.Round)
            fun path(vararg points: Pair<Float,Float>, close: Boolean = false, fill: Boolean = false) {
                val p = Path(); points.firstOrNull()?.let { p.moveTo(it.first,it.second) }
                points.drop(1).forEach { p.lineTo(it.first,it.second) }; if (close) p.close()
                if (fill) drawPath(p,color) else drawPath(p,color,style=stroke)
            }
            when (glyph) {
                Glyph.BACK -> path(15f to 5f,8f to 12f,15f to 19f)
                Glyph.CHEVRON -> path(9f to 6f,15f to 12f,9f to 18f)
                Glyph.CLOSE -> { line(6f,6f,18f,18f);line(18f,6f,6f,18f) }
                Glyph.PAUSE -> { drawRoundRect(color,Offset(7f,5f),Size(3.5f,14f),CornerRadius(1f));drawRoundRect(color,Offset(14f,5f),Size(3.5f,14f),CornerRadius(1f)) }
                Glyph.PLAY -> path(8f to 5f,19f to 12f,8f to 19f,close=true,fill=true)
                Glyph.PLANE -> path(11f to 2f,13f to 2f,14f to 9f,22f to 15f,22f to 17f,13.5f to 14f,
                    13f to 20f,16f to 22f,12f to 21f,8f to 22f,11f to 20f,10.5f to 14f,2f to 17f,2f to 15f,10f to 9f,close=true,fill=true)
                Glyph.SOUND -> { path(4f to 9f,8f to 9f,13f to 5f,13f to 19f,8f to 15f,4f to 15f,close=true);drawArc(color,-55f,110f,false,Offset(10f,7f),Size(10f,10f),style=stroke);drawArc(color,-55f,110f,false,Offset(7f,3f),Size(18f,18f),style=stroke) }
                Glyph.WINDOW -> { drawRoundRect(color,Offset(5f,2f),Size(14f,20f),CornerRadius(5f),style=stroke);drawRoundRect(color,Offset(8f,5f),Size(8f,14f),CornerRadius(3f),style=stroke) }
                Glyph.GLOBE -> { drawCircle(color,9f,Offset(12f,12f),style=stroke);drawOval(color,Offset(8f,3f),Size(8f,18f),style=stroke);line(3f,12f,21f,12f) }
                Glyph.MAP -> { path(3f to 5f,9f to 3f,15f to 5f,21f to 3f,21f to 19f,15f to 21f,9f to 19f,3f to 21f,close=true);line(9f,3f,9f,19f);line(15f,5f,15f,21f) }
                Glyph.ROUTE -> { drawCircle(color,2.3f,Offset(5f,18f),style=stroke);drawCircle(color,2.3f,Offset(19f,6f),style=stroke);path(5f to 15f,5f to 8f,14f to 8f,14f to 17f,19f to 17f,19f to 9f) }
                Glyph.SEARCH -> { drawCircle(color,6.5f,Offset(10f,10f),style=stroke);line(15f,15f,21f,21f) }
                Glyph.SHUFFLE -> { path(3f to 6f,7f to 6f,17f to 18f,21f to 18f);path(3f to 18f,7f to 18f,17f to 6f,21f to 6f);path(18f to 3f,21f to 6f,18f to 9f);path(18f to 15f,21f to 18f,18f to 21f) }
                Glyph.LOCATION -> { drawCircle(color,7f,Offset(12f,12f),style=stroke);drawCircle(color,2.5f,Offset(12f,12f));line(12f,1f,12f,5f);line(12f,19f,12f,23f);line(1f,12f,5f,12f);line(19f,12f,23f,12f) }
                Glyph.SETTINGS -> { drawCircle(color,6.8f,Offset(12f,12f),style=stroke);drawCircle(color,2.8f,Offset(12f,12f),style=stroke);for(i in 0..7) { val a=i*Math.PI/4;line((12+7*kotlin.math.cos(a)).toFloat(),(12+7*kotlin.math.sin(a)).toFloat(),(12+10*kotlin.math.cos(a)).toFloat(),(12+10*kotlin.math.sin(a)).toFloat()) } }
                Glyph.BOOK -> { path(3f to 4f,9f to 4f,12f to 6f,15f to 4f,21f to 4f,21f to 20f,15f to 20f,12f to 22f,9f to 20f,3f to 20f,close=true);line(12f,6f,12f,22f) }
                Glyph.LAPTOP -> { drawRoundRect(color,Offset(4f,4f),Size(16f,13f),CornerRadius(1f),style=stroke);path(4f to 17f,1f to 21f,23f to 21f,20f to 17f,close=true) }
                Glyph.PALETTE -> { drawOval(color,Offset(3f,3f),Size(18f,17f),style=stroke);drawCircle(color,1.3f,Offset(8f,8f));drawCircle(color,1.3f,Offset(14f,7f));drawCircle(color,1.3f,Offset(17f,12f));drawCircle(color,2.5f,Offset(10f,16f),style=stroke) }
                Glyph.NOTE -> { drawRoundRect(color,Offset(4f,3f),Size(16f,19f),CornerRadius(2f),style=stroke);line(8f,8f,16f,8f);line(8f,12f,16f,12f);line(8f,16f,13f,16f) }
                Glyph.CHART -> { line(4f,20f,4f,12f);line(9f,20f,9f,6f);line(14f,20f,14f,10f);line(19f,20f,19f,3f) }
                Glyph.CARD -> { drawRoundRect(color,Offset(2f,5f),Size(20f,14f),CornerRadius(3f),style=stroke);line(2f,10f,22f,10f);line(5f,15f,10f,15f) }
                Glyph.FILTER -> { line(3f,6f,21f,6f);line(6f,12f,18f,12f);line(9f,18f,15f,18f) }
                Glyph.PLUS -> { line(12f,4f,12f,20f);line(4f,12f,20f,12f) }
                Glyph.CHECK -> path(4f to 12f,10f to 18f,21f to 6f)
                Glyph.TRASH -> { path(6f to 7f,7f to 21f,17f to 21f,18f to 7f);line(4f,6f,20f,6f);path(9f to 6f,9f to 3f,15f to 3f,15f to 6f) }
            }
        }
    }
}

@Composable
fun GlassIconButton(glyph: Glyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                    selected: Boolean = false, tint: Color = Color.White, background: Color = Color.Black.copy(alpha=.35f)) {
    Box(modifier.size(48.dp).semantics { contentDescription = label }.clickable(onClick = onClick), contentAlignment=Alignment.Center) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(if(selected) Paper else background), contentAlignment=Alignment.Center) {
            FlightIcon(glyph,if(selected) Ink else tint,Modifier.size(16.dp))
        }
    }
}

@Composable
fun FlightCapsule(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, pastel: Boolean = false) {
    val brush = if(pastel) Brush.horizontalGradient(listOf(Color(0xFFF0F9DA),Color(0xFFE3F3FC),Color(0xFFF5F8D9))) else Brush.linearGradient(listOf(Paper,Paper))
    Box(modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(if(enabled) brush else Brush.linearGradient(listOf(Paper.copy(alpha=.3f),Paper.copy(alpha=.3f))))
        .clickable(enabled=enabled,onClick=onClick),contentAlignment=Alignment.Center) {
        Text(label,color=Ink,fontSize=13.sp,fontWeight=FontWeight.SemiBold,fontFamily=Inter)
    }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(Glass).border(.5.dp,Color.White.copy(alpha=.06f),RoundedCornerShape(22.dp)).padding(14.dp),content=content)
}

data class FocusKind(val name: String,val glyph: Glyph,val color: Color)
val FocusKinds = listOf(FocusKind("Study",Glyph.BOOK,Color(0xFF9CC7AA)), FocusKind("Work",Glyph.LAPTOP,Color(0xFFB1A1DD)),
    FocusKind("Reading",Glyph.BOOK,Color(0xFF95CEAE)),FocusKind("Creative",Glyph.PALETTE,Color(0xFFDDC58D)))

fun focusKind(name: String) = FocusKinds.firstOrNull { it.name.equals(name,true) } ?: FocusKind(name,Glyph.PLUS,Color(0xFF9DBED5))
