package personal.focusflight.platform.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import personal.focusflight.FlightApplication

class FlightTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app get() = application as FlightApplication
    private var ticker: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(TimerPlatform.SESSION_NOTIFICATION, TimerPlatform.notification(app, null))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            when (intent?.action) {
                "PAUSE" -> app.repository.setPaused(true)?.takeIf { it.state == "COMPLETED" }?.let { TimerPlatform.arrival(app, it) }
                "RESUME" -> app.repository.setPaused(false)?.takeIf { it.state == "COMPLETED" }?.let { TimerPlatform.arrival(app, it) }
                "CANCEL" -> app.repository.cancel()?.takeIf { it.state == "COMPLETED" }?.let { TimerPlatform.arrival(app, it) }
            }
            val current = app.repository.ongoing()
            TimerPlatform.schedule(app, current)
            if (current != null) NotificationManagerCompat.from(app).notify(TimerPlatform.SESSION_NOTIFICATION, TimerPlatform.notification(app, current))
            if (ticker?.isActive != true) ticker = scope.launch { tick() }
        }
        return START_STICKY
    }

    private suspend fun tick() {
        var lastUpdate = 0L
        while (currentCoroutineContext().isActive) {
            app.repository.completeDue()?.let { TimerPlatform.arrival(app, it) }
            val s = app.repository.ongoing()
            if (s == null) {
                TimerPlatform.schedule(app, null)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastUpdate >= 15_000) {
                NotificationManagerCompat.from(app).notify(TimerPlatform.SESSION_NOTIFICATION, TimerPlatform.notification(app, s))
                lastUpdate = now
            }
            delay(1000)
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
