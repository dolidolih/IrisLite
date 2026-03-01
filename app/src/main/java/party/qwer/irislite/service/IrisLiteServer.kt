package party.qwer.irislite.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.send
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.*
import party.qwer.irislite.AppConfig
import party.qwer.irislite.AppState
import party.qwer.irislite.models.ApiResponse
import party.qwer.irislite.models.ReplyRequest

object IrisLiteServer {
    private var activeEngine: ApplicationEngine? = null

    private val wsBroadcastFlow = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val sharedFlow = wsBroadcastFlow.asSharedFlow()

    fun broadcastToClients(message: String) {
        wsBroadcastFlow.tryEmit(message)
    }

    fun start(context: Context) {
        if (activeEngine != null) return
        try {
            val lenientJson = Json { ignoreUnknownKeys = true }

            embeddedServer(Netty, port = AppConfig.serverPort) {
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(lenientJson)
                }
                install(ContentNegotiation) { json(lenientJson) }
                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse(
                                success = false,
                                message = cause.message ?: "unknown error"
                            )
                        )
                    }
                }

                routing {
                    webSocket("/ws") {
                        sharedFlow.collect { msg ->
                            send(msg)
                        }
                    }

                    post("/reply") {
                        val req = call.receive<ReplyRequest>()
                        val roomId = req.room

                        when (req.type) {
                            "text" -> {
                                val text = req.data.jsonPrimitive.content
                                handleTextReply(context, roomId, text)
                            }
                            "image_multiple", "image" -> {
                                val images = if (req.data is JsonArray) {
                                    req.data.jsonArray.map { it.jsonPrimitive.content }
                                } else {
                                    listOf(req.data.jsonPrimitive.content)
                                }
                                handleImageReply(context, roomId, images)
                            }
                        }
                        call.respond(ApiResponse(success = true, message = "Enqueued"))
                    }
                }
            }.start(wait = false).also { activeEngine = it.engine }

            ReplyManager.startQueue()
        } catch (e: Exception) {
            e.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Server Start Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            activeEngine = null
        }
    }

    fun stop() {
        activeEngine?.stop(1000, 2000)
        activeEngine = null
    }

    private fun handleTextReply(context: Context, room: String, text: String) {
        ReplyManager.enqueue {
            val action = ReplyManager.replyActions[room]
            if (action != null) {
                val intent = Intent()
                val bundle = Bundle()

                bundle.putCharSequence(action.remoteInput.resultKey, text)
                android.app.RemoteInput.addResultsToIntent(arrayOf(action.remoteInput), intent, bundle)

                try {
                    action.pendingIntent.send(context, 0, intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to send reply: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No ReplyAction stored for room: $room", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleImageReply(context: Context, room: String, base64Images: List<String>) {
        ReplyManager.enqueue {
            val uris = base64Images.mapNotNull { base64 ->
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "iris_img_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    }
                    uri
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (uris.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    setPackage("com.kakao.talk")
                    type = "image/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    putExtra("key_id", room)
                    putExtra("key_type", 1)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                try {
                    context.startActivity(intent)

                    delay(1500)
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}