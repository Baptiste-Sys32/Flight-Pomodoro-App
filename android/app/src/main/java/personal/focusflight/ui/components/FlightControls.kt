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
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Inter = FontFamily(*listOf(FontWeight.Normal,FontWeight.Medium,FontWeight.SemiBold,FontWeight.Bold).map {weight->
    Font(R.font.inter_variable,weight,variationSettings=FontVariation.Settings(FontVariation.weight(weight.weight)))
}.toTypedArray())

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
    LOCATION, SETTINGS, BOOK, LAPTOP, PALETTE, NOTE, CHART, CARD, FILTER, PLUS, CHECK, CHEVRON, TRASH, STUDY, PROFILE }

@Composable
fun FlightIcon(glyph: Glyph, color: Color = Color.White, modifier: Modifier = Modifier.size(20.dp)) {
    val asset=when(glyph) {
        Glyph.BACK -> R.drawable.ic_lucide_back
        Glyph.CLOSE -> R.drawable.ic_lucide_close
        Glyph.PAUSE -> R.drawable.ic_lucide_pause
        Glyph.PLAY -> R.drawable.ic_lucide_play
        Glyph.PLANE -> R.drawable.ic_lucide_plane
        Glyph.SOUND -> R.drawable.ic_lucide_sound
        Glyph.GLOBE -> R.drawable.ic_lucide_globe
        Glyph.MAP -> R.drawable.ic_lucide_map
        Glyph.ROUTE -> R.drawable.ic_lucide_route
        Glyph.SEARCH -> R.drawable.ic_lucide_search
        Glyph.SHUFFLE -> R.drawable.ic_lucide_shuffle
        Glyph.LOCATION -> R.drawable.ic_lucide_location
        Glyph.SETTINGS -> R.drawable.ic_lucide_settings
        Glyph.BOOK -> R.drawable.ic_lucide_book
        Glyph.LAPTOP -> R.drawable.ic_lucide_laptop
        Glyph.PALETTE -> R.drawable.ic_lucide_palette
        Glyph.NOTE -> R.drawable.ic_lucide_note
        Glyph.CHART -> R.drawable.ic_lucide_chart
        Glyph.CARD -> R.drawable.ic_lucide_card
        Glyph.FILTER -> R.drawable.ic_lucide_filter
        Glyph.PLUS -> R.drawable.ic_lucide_plus
        Glyph.CHECK -> R.drawable.ic_lucide_check
        Glyph.CHEVRON -> R.drawable.ic_lucide_chevron
        Glyph.TRASH -> R.drawable.ic_lucide_trash
        Glyph.STUDY -> R.drawable.ic_lucide_study
        Glyph.PROFILE -> R.drawable.ic_lucide_profile
        Glyph.WINDOW -> null
    }
    if(asset!=null) androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(asset),null,modifier,tint=color)
    else Canvas(modifier) {
        val stroke=Stroke(1.6f*size.width/24)
        drawRoundRect(color,Offset(size.width*.2f,size.height*.08f),Size(size.width*.6f,size.height*.84f),CornerRadius(size.width*.22f),style=stroke)
        drawRoundRect(color,Offset(size.width*.32f,size.height*.2f),Size(size.width*.36f,size.height*.6f),CornerRadius(size.width*.14f),style=stroke)
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
val FocusKinds = listOf(FocusKind("Study",Glyph.STUDY,Color(0xFF9DBED5)), FocusKind("Work",Glyph.LAPTOP,Color(0xFFB1A1DD)),
    FocusKind("Reading",Glyph.BOOK,Color(0xFF95CEAE)),FocusKind("Creative",Glyph.PALETTE,Color(0xFFDDC58D)))

fun focusKind(name: String) = FocusKinds.firstOrNull { it.name.equals(name,true) } ?: FocusKind(name,Glyph.PLUS,Color(0xFF9DBED5))
