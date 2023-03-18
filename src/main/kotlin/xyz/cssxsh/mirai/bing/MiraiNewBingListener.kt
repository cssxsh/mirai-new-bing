package xyz.cssxsh.mirai.bing

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.bing.*
import kotlin.coroutines.*

@PublishedApi
internal object MiraiNewBingListener : SimpleListenerHost() {
    private val client = object : NewBingClient(config = MiraiNewBingConfig) {
        init {
            launch {
                shared.collect { (uuid, data) ->
                    val item = data["item"] as? JsonObject ?: return@collect
                    val messages = item["messages"] as? JsonArray ?: return@collect
                    for (element in messages) {
                        val message = format.decodeFromJsonElement(NewBingMessage.serializer(), element)
                        if ("bot" != message.author) continue
                        if ("InternalSearchQuery" == message.messageType) continue
                        val (id, _) = chats.entries.find { it.value.uuid == uuid } ?: continue
                        val subject = contacts[id] ?: continue

                        launch {
                            subject.sendMessage(buildMessageChain {
                                appendLine(message.text)
                                if (MiraiNewBingConfig.source && message.sourceAttributions.isEmpty().not()) {
                                    appendLine()
                                    var index = 1
                                    for (attribution in message.sourceAttributions) {
                                        appendLine("[^${index++}^] ${attribution.providerDisplayName} ${attribution.seeMoreUrl}")
                                    }
                                }
                            })
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
        val commander = toCommandSender()
        if (commander.hasPermission(chat).not()) return

        val id = commander.permitteeId.asString()
        val cache = chats[id] ?: client.create()
        chats[id] = cache
        contacts[id] = subject
        launch {
            client.send(chat = cache, text = content.removePrefix(MiraiNewBingConfig.prefix))
        }
    }
}