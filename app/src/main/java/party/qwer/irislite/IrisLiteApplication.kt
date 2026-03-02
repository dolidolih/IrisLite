package party.qwer.irislite

import android.app.Application
import android.content.Intent
import party.qwer.irislite.service.IrisForegroundService

class IrisLiteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfig.init(this)

        if (AppConfig.isServiceEnabled) {
            val serviceIntent = Intent(this, IrisForegroundService::class.java)
            startForegroundService(serviceIntent)
        }
    }
}