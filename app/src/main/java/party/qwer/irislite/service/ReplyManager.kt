package party.qwer.irislite.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import party.qwer.irislite.AppConfig
import party.qwer.irislite.models.ReplyAction
import java.util.concurrent.ConcurrentHashMap

object ReplyManager {
    val replyActions = ConcurrentHashMap<String, ReplyAction>()

    private val messageChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    private var isQueueRunning = false

    fun startQueue() {
        if (isQueueRunning) return
        isQueueRunning = true
        coroutineScope.launch {
            for (action in messageChannel) {
                mutex.withLock {
                    try {
                        action.invoke()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(AppConfig.sendRate)
                }
            }
        }
    }

    fun enqueue(action: suspend () -> Unit) {
        coroutineScope.launch { messageChannel.send(action) }
    }
}