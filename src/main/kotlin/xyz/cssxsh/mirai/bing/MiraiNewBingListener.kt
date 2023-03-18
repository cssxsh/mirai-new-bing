package xyz.cssxsh.mirai.bing

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.bing.*
import kotlin.coroutines.*

@PublishedApi
internal object MiraiNewBingListener : SimpleListenerHost() {
    private val client = object : NewBingClient(config = MiraiNewBingConfig) {
        init {
            launch {
                shared.collect { (clientId, data) ->
                    val item = data["item"] as? JsonObject ?: return@collect
                    val messages = item["messages"] as? JsonArray ?: return@collect
                    for (message in messages) {
                        message as? JsonObject ?: continue
                        val author = message["author"]?.jsonPrimitive?.content ?: continue
                        if ("bot" != author) continue
                        val (id, _) = chats.entries.find { it.value.clientId == clientId } ?: continue
                        val subject = contacts[id] ?: continue
                        val text = message["text"]?.jsonPrimitive?.content ?: continue

                        launch {
                            subject.sendMessage(text)
                        }
                    }
                }
            }
        }
    }
    private val logger = MiraiLogger.Factory.create(this::class)
    internal val chat: Permission by MiraiBingPermissions
    private val chats: MutableMap<String, NewBingChat> = java.util.concurrent.ConcurrentHashMap()
    private val contacts: MutableMap<String, Contact> = java.util.concurrent.ConcurrentHashMap()

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is CancellationException -> {
                // ...
            }
            is ExceptionInEventHandlerException -> {
                logger.warning({ "MiraiNewBingListener with ${exception.event}" }, exception.cause)
            }
            else -> Unit
        }
    }

    @EventHandler(concurrency = ConcurrencyKind.CONCURRENT)
    suspend fun MessageEvent.handle() {
        val content = message.contentToString()
        if (content.startsWith(MiraiNewBingConfig.prefix).not()) return

        val id = toCommandSender().permitteeId.asString()
        val cache = chats[id] ?: client.create()
        chats[id] = cache
        contacts[id] = subject
        launch {
            client.send(cache, content.removePrefix(MiraiNewBingConfig.prefix))
        }
    }
}