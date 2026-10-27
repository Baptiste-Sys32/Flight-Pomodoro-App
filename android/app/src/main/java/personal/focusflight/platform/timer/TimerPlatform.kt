package personal.focusflight.platform.timer

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import personal.focusflight.FlightApplication
import personal.focusflight.MainActivity
import personal.focusflight.R
import personal.focusflight.data.local.FlightSession
import personal.focusflight.domain.session.SessionClock
import kotlin.math.ceil

object TimerPlatform {
    const val SESSION_NOTIFICATION = 40
    private const val ARRIVAL_NOTIFICATION = 41
    private const val CHANNEL = "flight_session"
    private const val ARRIVALS = "flight_arrivals"

    fun channels(context: Context) {
        context.getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.timer_channel), NotificationManager.IMPORTANCE_LOW))
            createNotificationChannel(NotificationChannel(ARRIVALS, context.getString(R.string.arrival_channel), NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    private fun open(context: Context): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun notification(app: FlightApplication, s: FlightSession?): Notification {
        channels(app)
        val builder = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_plane).setContentIntent(open(app))
            .setOngoing(true).setOnlyAlertOnce(true).setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (s == null) return builder.setContentTitle(app.getString(R.string.app_name)).setContentText("Preparing your flight").build()
        val remaining = SessionClock.remaining(s.anchor(), app.clock())
        val arrival = if (s.randomRoute) "?" else s.arrivalCode
        builder.setContentTitle("${s.departureCode}  →  $arrival · ${s.category}")
            .setContentText(if (s.state == "PAUSED") "Paused · ${formatRemaining(remaining)}" else "${formatRemaining(remaining)} remaining")
            .setProgress(1000, (SessionClock.progress(s.durationMs, remaining) * 1000).toInt(), false)
        if (s.state == "ACTIVE") builder.setWhen(System.currentTimeMillis() + remaining)
            .setUsesChronometer(true).setChronometerCountDown(true)
        else builder.setShowWhen(false)
        val action = if (s.state == "PAUSED") "RESUME" else "PAUSE"
        val pending = PendingIntent.getService(app, 11, Intent(app, FlightTimerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        builder.addAction(0, app.getString(if (s.state == "PAUSED") R.string.resume else R.string.pause), pending)
        return builder.build()
    }

    private fun alarmIntent(context: Context, id: String? = null) = PendingIntent.getBroadcast(context, 20,
        Intent(context, CompletionReceiver::class.java).putExtra("session_id", id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun schedule(app: FlightApplication, s: FlightSession?) {
        val manager = app.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent(app, s?.id)
        manager.cancel(pending)
        if (s == null || s.state != "ACTIVE") return
        val trigger = android.os.SystemClock.elapsedRealtime() + SessionClock.remaining(s.anchor(), app.clock())
        try {
            if (manager.canScheduleExactAlarms()) manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
            else manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
        } catch (_: SecurityException) {
            // Access may be revoked between the check and scheduling.
            manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
        }
    }

    fun arrival(app: FlightApplication, session: FlightSession) {
        channels(app)
        if (app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(app).notify(ARRIVAL_NOTIFICATION,
                NotificationCompat.Builder(app, ARRIVALS).setSmallIcon(R.drawable.ic_notification_plane)
                    .setContentTitle(app.getString(R.string.flight_complete))
                    .setContentText("${session.arrivalCity} · ${app.getString(R.string.flight_complete_body)}")
                    .setContentIntent(open(app)).setAutoCancel(true).build())
        }
    }

    fun formatRemaining(ms: Long): String {
        val seconds = ceil(ms / 1000.0).toLong().coerceAtLeast(0)
        return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
        else "%02d:%02d".format(seconds / 60, seconds % 60)
    }
}
