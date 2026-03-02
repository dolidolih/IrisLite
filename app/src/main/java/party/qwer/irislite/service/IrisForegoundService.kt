package party.qwer.irislite.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import party.qwer.irislite.AppConfig

class IrisForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IrisLite::ServerWakeLock")
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification =
            Notification.Builder(this, "iris_service_channel")
                .setContentTitle("IrisLite Background Service")
                .setContentText("Ktor server and listeners are active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

        startForeground(1001, notification)

        AppConfig.isServiceEnabled = true

        IrisLiteServer.start(applicationContext)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        IrisLiteServer.stop()

        AppConfig.isServiceEnabled = false

        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) { e.printStackTrace() }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "iris_service_channel",
            "IrisLite Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}