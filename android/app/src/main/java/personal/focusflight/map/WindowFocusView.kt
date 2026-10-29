package personal.focusflight.map

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One native frame, one blind, one offline 3D scene. No photographic blind or map layer. */
@Composable
fun WindowFocusView(modifier: Modifier=Modifier,paused: Boolean=false,scene: MapScene?=null,durationMs: Long=1_500_000) {
    var shade by rememberSaveable {mutableFloatStateOf(0f)}
    var dragging by remember {mutableStateOf(false)}
    val closed by animateFloatAsState(shade,if(dragging) snap() else tween(320),label="Window blind")
    Box(modifier.background(Brush.radialGradient(listOf(Color(0xFF1A2526),Color(0xFF080C0D)),radius=900f)),contentAlignment=Alignment.Center) {
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth(.83f).widthIn(max=345.dp).aspectRatio(.66f)
                .clip(RoundedCornerShape(44)).background(Brush.linearGradient(listOf(Color(0xFF4A5655),Color(0xFF152021),Color(0xFF6F7B77)))).padding(7.dp)) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(44))
                    .background(Brush.linearGradient(listOf(Color(0xFFC9D0C8),Color(0xFF7E8B84),Color(0xFFD9DED6)))).padding(13.dp)) {
                    BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(43)).background(Color(0xFF7BB2CD))) {
                        WindowScenery(scene?.progress ?: .02,durationMs,paused,closed>=.995f,Modifier.fillMaxSize())
                        val opening=maxHeight
                        val blindHeight=18.dp+(opening-18.dp)*closed
                        Box(Modifier.fillMaxWidth().height(blindHeight)
                            .background(Brush.horizontalGradient(listOf(Color(0xFFA6B0A7),Color(0xFFD1D6CB),Color(0xFFADB9AF)))))
                        Box(Modifier.align(Alignment.TopCenter).offset(y=(blindHeight-30.dp).coerceAtLeast(0.dp))
                            .fillMaxWidth().height(48.dp).semantics {
                                contentDescription="Window blind"
                                stateDescription=if(shade>=.5f) "Closed" else "Open"
                                progressBarRangeInfo=ProgressBarRangeInfo(shade,0f..1f)
                                setProgress {shade=it.coerceIn(0f,1f);true}
                                onClick(if(shade>.5f) "Open blind" else "Close blind") {shade=if(shade>.5f) 0f else 1f;true}
                            }.pointerInput(opening) {
                                detectVerticalDragGestures(onDragStart={dragging=true},
                                    onDragEnd={dragging=false;shade=if(shade>.5f) 1f else 0f},onDragCancel={dragging=false}) {event,dy->
                                    event.consume();shade=(shade+dy/(opening.toPx()-18.dp.toPx())).coerceIn(0f,1f)
                                }
                            },contentAlignment=Alignment.TopCenter) {
                            Box(Modifier.padding(top=13.dp).width(44.dp).height(5.dp).background(Color(0xFF55645C),RoundedCornerShape(3.dp)))
                        }
                    }
                }
            }
            Text(if(shade>.5f) "Open blind" else "Close blind",color=Color(0xFFB8C3BA),fontSize=12.sp,
                modifier=Modifier.clip(RoundedCornerShape(24.dp)).clickable {shade=if(shade>.5f) 0f else 1f}.padding(horizontal=24.dp,vertical=14.dp))
        }
    }
}
