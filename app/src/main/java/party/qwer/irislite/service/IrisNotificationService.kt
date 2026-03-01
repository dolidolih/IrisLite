//notification parser : https://github.com/mooner1022/StarLight/blob/08181968ab72eec6c1f9378247beb5a606dc0ef8/app/src/main/java/dev/mooner/starlight/listener/specs/AndroidRParserSpec.kt

package party.qwer.irislite.service

import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import party.qwer.irislite.AppConfig
import party.qwer.irislite.AppState
import party.qwer.irislite.models.IrisJsonData
import party.qwer.irislite.models.NotificationEvent
import party.qwer.irislite.models.NotificationPayload
import party.qwer.irislite.models.ReplyAction
import java.util.Arrays

class IrisNotificationService : NotificationListenerService() {
    private val httpClient = OkHttpClient()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!AppConfig.isServiceEnabled || sbn.packageName != "com.kakao.talk") return

        val notification = sbn.notification
        val extras = notification.extras ?: return

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        if (rawTitle == null && rawText == null) return

        val senderName = rawTitle ?: ""
        val text = rawText ?: ""

        val subText = extras.getString(Notification.EXTRA_SUB_TEXT)
        val summaryText = extras.getString(Notification.EXTRA_SUMMARY_TEXT)
        val room = subText ?: summaryText ?: senderName

        val isGroupChat = room != senderName
        val roomId = sbn.tag ?: ""
        val chatLogId = extras.getLong("chatLogId", 0L)

        var senderId = ""
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                if (!messages.isNullOrEmpty()) {
                    val messageBundle = messages[0] as? Bundle
                    val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        messageBundle?.getParcelable("sender_person", Person::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        messageBundle?.getParcelable("sender_person") as? Person
                    }
                    if (person != null) senderId = person.key ?: ""
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val rawDump = dumpBundle(extras).toString()

        val event = NotificationEvent(
            timestamp = System.currentTimeMillis(),
            room = room,
            roomId = roomId,
            senderName = senderName,
            senderId = senderId,
            isGroupChat = isGroupChat,
            text = text,
            rawDump = rawDump
        )

        extractAndStoreReplyAction(notification, room)
        if (roomId.isNotEmpty()) {
            extractAndStoreReplyAction(notification, roomId)
        }

        coroutineScope.launch(Dispatchers.Main) {
            AppState.notificationHistory.add(0, event)
            while (AppState.notificationHistory.size > 10) {
                AppState.notificationHistory.removeAt(AppState.notificationHistory.lastIndex)
            }
        }

        coroutineScope.launch {
            val innerJson = IrisJsonData(
                id = chatLogId.toString(),
                chat_id = roomId,
                user_id = senderId,
                message = text
            )

            val payload = NotificationPayload(
                msg = text,
                room = room,
                sender = senderName,
                is_lite = true,
                json = innerJson
            )

            val json = Json {
                encodeDefaults = true
            }

            val jsonPayload = json.encodeToString(payload)

            IrisLiteServer.broadcastToClients(jsonPayload)

            val endpoint = AppConfig.webEndpoint
            if (endpoint.isNotBlank()) {
                try {
                    val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder().url(endpoint).post(body).build()
                    httpClient.newCall(request).execute().close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun extractAndStoreReplyAction(notification: Notification, roomKey: String) {
        val actions = notification.actions ?: return
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (remoteInput.allowFreeFormInput) {
                    ReplyManager.replyActions[roomKey] = ReplyAction(action.actionIntent, remoteInput)
                    coroutineScope.launch(Dispatchers.Main) {
                        if (!AppState.storedRooms.contains(roomKey)) {
                            AppState.storedRooms.add(roomKey)
                        }
                    }
                    return
                }
            }
        }
    }

    private fun dumpBundle(bundle: Bundle?): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        if (bundle == null) return map
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            if (value is Bundle) {
                map[key] = dumpBundle(value)
            } else if (value != null && value.javaClass.isArray) {
                map[key] = Arrays.toString(value as Array<*>)
            } else {
                map[key] = value?.toString()
            }
        }
        return map
    }
}