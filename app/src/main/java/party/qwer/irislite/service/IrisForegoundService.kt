package party.qwer.irislite.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import party.qwer.irislite.AppConfig
import party.qwer.irislite.R

class IrisForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "iris_service_channel"

    companion object {
        const val ACTION_START_SERVICE = "party.qwer.irislite.START"
        const val ACTION_STOP_SERVICE = "party.qwer.irislite.STOP"
        const val ACTION_EXIT_SERVICE = "party.qwer.irislite.EXIT"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IrisLite::ServerWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        when (intent?.action) {
            ACTION_EXIT_SERVICE -> {
                cleanup()
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_SERVICE -> startIrisLogic()
            ACTION_STOP_SERVICE -> stopIrisLogic()
            else -> {
                if (AppConfig.isServiceEnabled) {
                    startIrisLogic()
                } else {
                    stopIrisLogic()
                }
            }
        }

        val notification = buildServiceNotification(AppConfig.isServiceEnabled)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun startIrisLogic() {
        if (AppConfig.isServiceEnabled && wakeLock?.isHeld == true) return

        wakeLock?.let {
            if (!it.isHeld) it.acquire()
        }

        AppConfig.isServiceEnabled = true
        IrisLiteServer.start(applicationContext)
        scheduleHeartbeat()
    }

    private fun stopIrisLogic() {
        cleanup()
    }

    private fun buildServiceNotification(isRunning: Boolean): Notification {
        val title = if (isRunning) "IrisLite가 작동 중입니다." else "IrisLite가 작동 중이지 않습니다."
        val buttonText = if (isRunning) "중지" else "시작"
        val actionType = if (isRunning) ACTION_STOP_SERVICE else ACTION_START_SERVICE

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val actionIcon = Icon.createWithResource(this, R.mipmap.ic_launcher)

        val actionIntent = Intent(this, IrisForegroundService::class.java).apply {
            action = actionType
        }
        val pendingIntent = PendingIntent.getService(
            this,
            if (isRunning) 1 else 2,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val actionBuilder = Notification.Action.Builder(
            actionIcon,
            buttonText,
            pendingIntent
        ).build()

        val exitIntent = Intent(this, IrisForegroundService::class.java).apply {
            action = ACTION_EXIT_SERVICE
        }
        val exitPendingIntent = PendingIntent.getService(
            this,
            3,
            exitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val exitAction = Notification.Action.Builder(
            actionIcon,
            "종료",
            exitPendingIntent
        ).build()

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("IrisLite Background Service")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.btn_star_big_off)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .addAction(actionBuilder)
            .addAction(exitAction)

        contentPendingIntent?.let {
            builder.setContentIntent(it)
        }

        return builder.build()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        cleanup()
        stopSelf()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        IrisLiteServer.stop()
        AppConfig.isServiceEnabled = false
        cancelHeartbeat()
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleHeartbeat() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WatchdogReceiver::class.java).apply {
            action = "party.qwer.irislite.HEARTBEAT"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val interval = 15 * 60 * 1000L
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + interval,
            interval,
            pendingIntent
        )
    }

    private fun cancelHeartbeat() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WatchdogReceiver::class.java).apply {
            action = "party.qwer.irislite.HEARTBEAT"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "IrisLite Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}