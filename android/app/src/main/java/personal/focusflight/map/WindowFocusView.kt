package personal.focusflight.map

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import personal.focusflight.R

@Composable
fun WindowFocusView(modifier: Modifier = Modifier, paused: Boolean = false) {
    val drift = remember { Animatable(-.025f) }
    LaunchedEffect(paused) {
        if (!paused) drift.animateTo(.025f,
            infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Reverse))
    }
    Box(modifier.background(Color(0xFF030506)), contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.fillMaxWidth(.62f).widthIn(max = 300.dp).aspectRatio(2f / 3f)) {
            val width = maxWidth
            val height = maxHeight
            Box(Modifier.fillMaxSize().padding(horizontal = width * .1f, vertical = height * .14f)
                .clip(RoundedCornerShape(42))) {
                Image(painterResource(R.drawable.cloud_sky), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 1.12f; scaleY = 1.12f
                        translationX = size.width * drift.value
                    })
            }
            Image(painterResource(R.drawable.window_frame), "Airplane window", modifier = Modifier.fillMaxSize())
        }
    }
}
