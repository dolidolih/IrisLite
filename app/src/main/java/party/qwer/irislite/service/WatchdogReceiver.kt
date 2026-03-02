package party.qwer.irislite.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import party.qwer.irislite.AppConfig

class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "party.qwer.irislite.HEARTBEAT") {
            if (AppConfig.isServiceEnabled) {
                val serviceIntent = Intent(context, IrisForegroundService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}