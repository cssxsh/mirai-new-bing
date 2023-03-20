package xyz.cssxsh.mirai.bing

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.bing.*
import java.time.Instant
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
    internal val reload: Permission by MiraiBingPermissions
    private val chats: MutableMap<String, NewBingChat> = java.util.concurrent.ConcurrentHashMap()
    private val chatExpirationTimes: MutableMap<String, Instant> = java.util.concurrent.ConcurrentHashMap()
    private val contacts: MutableMap<String, Contact> = java.util.concurrent.ConcurrentHashMap()

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is CancellationException -> {
                // ...
            }
            is ExceptionInEventHandlerException -> {
                if (exception.cause is java.net.SocketException) {
                    logger.warning({ "当前代理设置: ${MiraiNewBingConfig.proxy.ifEmpty { "<empty>" }}" }, exception.cause)
                } else {
                    logger.warning({ "MiraiNewBingListener with ${exception.event}" }, exception.cause)
                }
            }
            else -> {
                logger.warning({ "MiraiNewBingListener with ${exception.event}" }, exception)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, concurrency = ConcurrencyKind.CONCURRENT)
    fun MessageEvent.reload() {
        val commander = toCommandSender()
        if (commander.hasPermission(reload).not()) return
        val content = message.contentToString()
        if (content.startsWith(MiraiNewBingConfig.reload).not()) return
        intercept()

        launch {
            with(MiraiNewBing) {
                MiraiNewBingConfig.reload()
                logger.warning { "配置已重新加载" }
            }
            subject.sendMessage("配置已重新加载")
        }
    }

    private val newTopicGreet = arrayOf(
        "谢谢你帮我理清头绪! 我现在能帮你做什么?",
        "谢谢你! 知道你什么时候准备好继续前进总是很有帮助的。我现在能为你回答什么问题?",
        "重新开始总是很棒。问我任何问题!",
        "当然，我很乐意重新开始。我现在可以为你提供哪些帮助?",
        "好了，我已经为新的对话重置了我的大脑。你现在想聊些什么?",
        "没问题，很高兴你喜欢上一次对话。让我们转到一个新主题。你想要了解有关哪些内容的详细信息?",
        "当然，我已准备好进行新的挑战。我现在可以为你做什么?",
        "好的，我已清理好板子，可以重新开始了。我可以帮助你探索什么?",
        "明白了，我已经抹去了过去，专注于现在。我们现在应该探索什么?",
        "很好，让我们来更改主题。你在想什么?",
        "好了，我已经为新的对话擦拭干净板子了。现在我可以和你聊些什么呢?",
        "不用担心，我很高兴尝试一些新内容。我现在可以为你回答什么问题?",
    )

    @EventHandler(priority = EventPriority.HIGH, concurrency = ConcurrencyKind.CONCURRENT)
    fun MessageEvent.reset() {
        val commander = toCommandSender()
        if (commander.hasPermission(chat).not()) return
        val content = message.contentToString()
        if (content.startsWith(MiraiNewBingConfig.reset).not()) return
        intercept()

        launch {
            val id = commander.permitteeId.asString()
            chats.remove(id)
            contacts.remove(id)
            subject.sendMessage(newTopicGreet.random())
        }
    }

    @EventHandler(concurrency = ConcurrencyKind.CONCURRENT)
    suspend fun MessageEvent.chat() {
        val commander = toCommandSender()
        if (commander.hasPermission(chat).not()) return

        val content = message.contentToString()
        val (test, style) = when {
            content.startsWith(MiraiNewBingConfig.creative) -> {
                content.removePrefix(MiraiNewBingConfig.creative) to "Creative"
            }
            content.startsWith(MiraiNewBingConfig.balanced) -> {
                content.removePrefix(MiraiNewBingConfig.balanced) to "Balanced"
            }
            content.startsWith(MiraiNewBingConfig.precise) -> {
                content.removePrefix(MiraiNewBingConfig.precise) to "Precise"
            }
            content.startsWith(MiraiNewBingConfig.prefix) -> {
                content.removePrefix(MiraiNewBingConfig.prefix) to MiraiNewBingConfig.default
            }
            else -> return
        }

        launch {
            commander.send(text = test, style = style)
        }
    }

    private suspend fun CommandSenderOnMessage<*>.send(text: String, style: String) {
        val id = permitteeId.asString()
        var cache = chats[id]
        val now = Instant.now()
        if (cache == null || chatExpirationTimes[id]?.isAfter(now) == true) {
            cache = client.create()
            chatExpirationTimes[id] = now.plusSeconds(MiraiNewBingConfig.expiration)
        }
        chats[id] = cache
        contacts[id] = fromEvent.subject

        client.send(chat = cache, text = text, style = style)
    }
}