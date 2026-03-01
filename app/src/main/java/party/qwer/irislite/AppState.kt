package party.qwer.irislite

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import party.qwer.irislite.models.NotificationEvent

object AppConfig {
    private const val PREFS_NAME = "IrisLitePrefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean("isServiceEnabled", false)
        set(value) = prefs.edit().putBoolean("isServiceEnabled", value).apply()

    var webEndpoint: String
        get() = prefs.getString("webEndpoint", "") ?: ""
        set(value) = prefs.edit().putString("webEndpoint", value).apply()

    var sendRate: Long
        get() = prefs.getLong("sendRate", 500L)
        set(value) = prefs.edit().putLong("sendRate", value).apply()

    var serverPort: Int
        get() = prefs.getInt("serverPort", 3000)
        set(value) = prefs.edit().putInt("serverPort", value).apply()
}

object AppState {
    val notificationHistory = mutableStateListOf<NotificationEvent>()
    val storedRooms = mutableStateListOf<String>()
    val wsBroadcastFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
}