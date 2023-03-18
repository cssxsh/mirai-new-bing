package xyz.cssxsh.bing

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.slf4j.*
import java.util.*
import kotlin.coroutines.*

public open class NewBingClient(@PublishedApi internal val config: NewBingConfig) : CoroutineScope {
    public companion object {
        public const val RS: String = "\u001E"
    }

    public open val http: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json = Json)
        }
        install(HttpTimeout) {
            socketTimeoutMillis = config.timeout
            connectTimeoutMillis = config.timeout
            requestTimeoutMillis = null
        }
        BrowserUserAgent()
        ContentEncoding()
        WebSockets {
            //
        }
        expectSuccess = true
        engine {
            config {
                apply(config = config)
            }
        }
    }
    public override val coroutineContext: CoroutineContext = EmptyCoroutineContext
    protected open val format: Json = Json
    protected val shared: MutableSharedFlow<Pair<String, JsonObject>> = MutableSharedFlow()
    @PublishedApi internal val uuid: UUID = UUID.randomUUID()
    @PublishedApi internal val logger: Logger = LoggerFactory.getLogger(this::class.java)

    public open suspend fun create(): NewBingChat {
        val response = http.get("https://www.bing.com/turing/conversation/create") {
            header("x-ms-client-request-id", uuid)
            header("x-ms-useragent", config.device)
            header("accept-language", "en-US,en;q=0.9")

            if ("=" in config.cookie) {
                header("cookie", config.cookie)
            } else {
                cookie("_U", config.cookie)
            }
        }

        if (response.contentType() == ContentType.Text.Html) {
            throw ServerResponseException(response, response.body())
        }

        val conversation = response.body<NewBingChatConversation>()

        check(conversation.result.value == "Success") {
            conversation.result.message ?: conversation.result.value
        }

        return NewBingChat(
            clientId = conversation.clientId,
            conversationId = conversation.conversationId,
            conversationSignature = conversation.conversationSignature,
            index = 0
        )
    }

    protected open suspend fun DefaultClientWebSocketSession.sendJson(builderAction: JsonObjectBuilder.() -> Unit) {
        send(content = format.encodeToString(buildJsonObject(builderAction)) + RS)
    }

    protected open suspend fun DefaultClientWebSocketSession.handle(chat: NewBingChat) {
        while (isActive) {
            when (val frame = incoming.receive()) {
                is Frame.Binary -> {
                    // TODO
                }
                is Frame.Text -> {
                    for (text in frame.readText().splitToSequence(RS)) {
                        if (text.isEmpty()) continue
                        val item = format.decodeFromString(JsonObject.serializer(), text)
                        if (logger.isTraceEnabled) logger.trace(item.toString())
                        when (item["type"]?.jsonPrimitive?.int) {
                            1 -> {
                                // TODO
                            }
                            2 -> {
                                launch {
                                    shared.emit(chat.clientId to item)
                                }
                            }
                            3 -> return
                            null -> Unit
                        }
                    }
                }
                is Frame.Close -> break
                is Frame.Ping -> continue
                is Frame.Pong -> continue
            }
        }
    }

    protected open suspend fun DefaultClientWebSocketSession.bind(chat: NewBingChat) {
        sendJson {
            put("protocol", "json")
            put("version", 1)
        }
        sendJson {
            put("type", 6)
        }
    }

    protected open suspend fun DefaultClientWebSocketSession.message(chat: NewBingChat, text: String) {
        sendJson {
            put("type", 4)
            //
            putJsonArray("arguments") {
                addJsonObject {
                    put("source", "cib")
                    putJsonArray("optionsSets") {
                        for (option in config.options) {
                            add(option)
                        }
                        listOf(
                            "deepleo",
                            "nlu_direct_response_filter",
                            "disable_emoji_spoken_text",
                            "responsible_ai_policy_235",
                            "enablemm",
                            "dtappid",
                            "rai253",
                            "dv3sugg",
                            "harmonyv3"
                        ).forEach {
                            add(it)
                        }
                    }
                    putJsonArray("allowedMessageTypes") {
                        for (type in config.allowed) {
                            add(type)
                        }
                        listOf(
                            "Chat",
                            "InternalSearchQuery"
                        ).forEach {
                            add(it)
                        }
                    }
                    putJsonArray("sliceIds") {
                        // TODO
                    }
                    // XXX: traceId
                    put("isStartOfSession", true)
                    put("conversationId", chat.conversationId)
                    put("conversationSignature", chat.conversationSignature)
                    putJsonObject("participant") {
                        put("id", chat.clientId)
                    }

                    putJsonObject("message") {
                        put("author", "user")
                        put("inputMethod", "Keyboard")
                        put("text", text)
                        put("messageType", "Chat")
                    }
                }
            }
            put("invocationId", "${chat.index++}")
            put("target", "chat")
        }
    }

    protected open suspend fun websocket(block: suspend DefaultClientWebSocketSession.() -> Unit) {
        http.wss("wss://sydney.bing.com/sydney/ChatHub", {
            header("accept-language", "en-US,en;q=0.9")
        }, block)
    }

    public suspend fun send(chat: NewBingChat, text: String): String {
        websocket {
            bind(chat = chat)
            message(chat = chat, text = text)
            handle(chat = chat)
        }

        return chat.clientId
    }
}