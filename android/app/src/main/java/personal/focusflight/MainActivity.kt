package personal.focusflight

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import personal.focusflight.map.RendererPreview
import personal.focusflight.platform.timer.FlightTimerService
import personal.focusflight.platform.timer.TimerPlatform
import personal.focusflight.ui.FlightRoot
import personal.focusflight.ui.FlightViewModel

class MainActivity : ComponentActivity() {
    private val app get()=application as FlightApplication
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            var preview by remember {mutableStateOf(BuildConfig.DEBUG && intent.getBooleanExtra("renderer_preview",false))}
            BackHandler(enabled=preview) {preview=false}
            if(preview) RendererPreview(intent.getStringExtra("renderer_scene") ?: "globe") {preview=false}
            else FlightRoot(viewModel<FlightViewModel>(),{
                if(android.os.Build.VERSION.SDK_INT>=33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            },{preview=true})
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            app.repository.completeDue()?.let {TimerPlatform.arrival(app,it)}
            app.repository.ongoing()?.let {
                ContextCompat.startForegroundService(this@MainActivity,
                    Intent(this@MainActivity,FlightTimerService::class.java).setAction("SYNC"))
            }
        }
    }
}
