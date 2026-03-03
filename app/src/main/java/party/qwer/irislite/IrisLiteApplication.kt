package party.qwer.irislite

import android.app.Application
import android.content.Intent
import party.qwer.irislite.service.IrisForegroundService

class IrisLiteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfig.init(this)

        val serviceIntent = Intent(this, IrisForegroundService::class.java).apply {
            if (AppConfig.isServiceEnabled) action = "party.qwer.irislite.START"
        }
        startForegroundService(serviceIntent)
    }
}