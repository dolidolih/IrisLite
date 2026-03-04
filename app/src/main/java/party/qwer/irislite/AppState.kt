package party.qwer.irislite

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableSharedFlow
import party.qwer.irislite.models.NotificationEvent

object AppConfig {
    private const val PREFS_NAME = "IrisLitePrefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var isServiceEnabled: Boolean = false

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

data class StoredRoom(val name: String, val id: String)

object AppColors {
    val DarkBg = Color(0xFF131318)
    val CardBg = Color(0xFF272738)
    val InputBg = Color(0xFF161622)
    val PrimaryAccent = Color(0xFF815AF4)
    val TextMain = Color(0xFFFFFFFF)
    val TextSub = Color(0xFF9E9EA8)
    val SuccessVivid = Color(0xFF00E676)
    val ErrorVivid = Color(0xFFFF4B55)
    val BottomNavBg = Color(0xFF161622)
}

object AppState {
    val notificationHistory = mutableStateListOf<NotificationEvent>()
    val storedRooms = mutableStateListOf<StoredRoom>()
    val wsBroadcastFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
}