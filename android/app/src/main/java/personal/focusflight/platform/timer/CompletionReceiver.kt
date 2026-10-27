package personal.focusflight.platform.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import personal.focusflight.FlightApplication

class CompletionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as FlightApplication
        app.appScope.launch {
            try {
                app.repository.completeDue(intent.getStringExtra("session_id"))?.let { TimerPlatform.arrival(app, it) }
                val active = app.repository.ongoing()
                TimerPlatform.schedule(app, active)
                if (active == null) context.stopService(Intent(context, FlightTimerService::class.java))
            } finally { pending.finish() }
        }
    }
}

class RecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as FlightApplication
        app.appScope.launch {
            try {
                app.repository.completeDue()?.let { TimerPlatform.arrival(app, it) }
                // Boot receivers do not start a service. The persisted deadline/alarm is sufficient.
                TimerPlatform.schedule(app, app.repository.ongoing())
            } finally { pending.finish() }
        }
    }
}
