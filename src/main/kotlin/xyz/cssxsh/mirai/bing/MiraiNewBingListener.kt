package xyz.cssxsh.mirai.bing

import io.ktor.client.plugins.*
import io.ktor.client.statement.*
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
    private val expiration: MutableMap<String, Instant> = java.util.concurrent.ConcurrentHashMap()
    private val contacts: MutableMap<String, Contact> = java.util.concurrent.ConcurrentHashMap()

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is CancellationException -> {
                // ...
            }
            is ExceptionInEventHandlerException -> {
                if (exception.cause is java.net.SocketException) {
                    logger.warning({ "当前代理设置: '${MiraiNewBingConfig.proxy}'" }, exception.cause)
                } else {
                    logger.warning({ "MiraiNewBingListener with ${exception.event}" }, exception.cause)
                }
            }
            is ClientRequestException -> {
                logger.warning { "当前代理设置: ${MiraiNewBingConfig.proxy.ifEmpty { "''" }}, 请求失败: ${exception.response.request.url}" }
            }
            else -> {
                logger.warning({ "MiraiNewBingListener with '${MiraiNewBingConfig.proxy}'" }, exception)
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

        with(MiraiNewBing) {
            MiraiNewBingConfig.reload()
            logger.warning { "配置已重新加载" }
        }
        launch {
            subject.sendMessage("配置已重新加载")
        }
    }

    @EventHandler(priority = EventPriority.HIGH, concurrency = ConcurrencyKind.CONCURRENT)
    fun MessageEvent.reset() {
        val commander = toCommandSender()
        if (commander.hasPermission(chat).not()) return
        val content = message.contentToString()
        if (content.startsWith(MiraiNewBingConfig.reset).not()) return
        intercept()

        val id = commander.permitteeId.asString()
        chats.remove(id)
        contacts.remove(id)
        launch {
            subject.sendMessage(MiraiNewBingConfig.newTopicGreet.random())
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
        if (cache == null || (expiration[id] ?: Instant.MIN) < now) {
            cache = client.create()
            expiration[id] = now.plusSeconds(MiraiNewBingConfig.expiresIn)
            logger.info { "new bing chat ${cache.uuid} - ${cache.clientId} - ${cache.conversationId}" }
        }
        chats[id] = cache
        contacts[id] = fromEvent.subject

        client.send(chat = cache, text = text, style = style)
    }
}