@file:Suppress("PropertyName")

package party.qwer.irislite.models

import android.app.PendingIntent
import android.app.RemoteInput
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ReplyRequest(
    val room: String,
    val type: String,
    val data: JsonElement
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class NotificationPayload(
    val msg: String,
    val room: String,
    val sender: String,
    val is_lite: Boolean = true,
    val is_group_chat: Boolean,
    val profile_image: String?,
    val json: IrisJsonData
)

@Serializable
data class IrisJsonData(
    val _id: String? = null,
    val id: String?,
    val type: String? = null,
    @Suppress("PropertyName") val chat_id: String?,
    val scope: String? = null,
    @Suppress("PropertyName") val user_id: String?,
    val message: String,
    val attachment: String? = null,
    val created_at: String? = null,
    val deleted_at: String? = null,
    val client_message_id: String? = null,
    val prev_id: String? = null,
    val referer: String? = null,
    val supplement: String? = null,
    val v: String? = null
)

data class NotificationEvent(
    val timestamp: Long,
    val room: String,
    val roomId: String,
    val senderName: String,
    val senderId: String,
    val isGroupChat: Boolean,
    val text: String,
    val rawDump: String
)

data class ReplyAction(
    val pendingIntent: PendingIntent,
    val remoteInput: RemoteInput
)