package personal.focusflight

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import personal.focusflight.data.local.Airport
import personal.focusflight.domain.session.SessionClock
import personal.focusflight.platform.timer.FlightTimerService
import personal.focusflight.platform.timer.TimerPlatform

class MainActivity : ComponentActivity() {
    private val app get() = application as FlightApplication
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val flights by app.repository.flights.collectAsStateWithLifecycle(emptyList())
            val flight = flights.firstOrNull { it.isOngoing }
            var now by remember { mutableStateOf(app.clock()) }
            LaunchedEffect(Unit) { while (true) { now = app.clock(); delay(250) } }
            val remaining = flight?.let { SessionClock.remaining(it.anchor(), now) } ?: 25 * 60_000L
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFF4E582), background = Color(0xFF101312))) {
                Column(Modifier.fillMaxSize().background(Color(0xFF101312)).safeDrawingPadding().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FOCUS FLIGHT", fontSize = 12.sp, letterSpacing = 4.sp, color = Color.White.copy(alpha = .6f))
                    Text("A timer for geeks", fontSize = 13.sp, color = Color.White.copy(alpha = .4f))
                    Spacer(Modifier.weight(1f))
                    Text(if (flight == null) "Ready for departure" else "${flight.departureCode}  →  ${flight.arrivalCode}", color = Color.White)
                    Spacer(Modifier.height(20.dp))
                    Text(TimerPlatform.formatRemaining(remaining), fontSize = 64.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                    Text(if (flight?.state == "PAUSED") "Paused" else flight?.category ?: "Study", color = Color.White.copy(alpha = .55f))
                    Spacer(Modifier.weight(1f))
                    if (flight == null) {
                        Button(onClick = { start(25 * 60_000L) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50)) { Text("Start focus flight") }
                        if (BuildConfig.DEBUG) TextButton(onClick = { start(15_000) }) { Text("15-second test flight") }
                    } else {
                        Button(onClick = { command(if (flight.state == "PAUSED") "RESUME" else "PAUSE") }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (flight.state == "PAUSED") "Resume" else "Pause")
                        }
                        TextButton(onClick = { command("CANCEL") }) { Text("Give up") }
                    }
                    Text("Native timer milestone", fontSize = 11.sp, color = Color.White.copy(alpha = .3f))
                }
            }
        }
        lifecycleScope.launch {
            app.repository.completeDue()?.let { TimerPlatform.arrival(app, it) }
            app.repository.ongoing()?.let { command("SYNC") }
        }
    }

    private fun start(duration: Long) {
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        lifecycleScope.launch {
            if (app.repository.ongoing() == null) {
                app.repository.start(Airport("CDG", "Paris", 49.0097, 2.5479), Airport("LHR", "London", 51.47, -.4543),
                    duration, 348_000, "Study", "1A")
                command("SYNC")
            }
        }
    }

    private fun command(action: String) = ContextCompat.startForegroundService(this,
        Intent(this, FlightTimerService::class.java).setAction(action))
}
