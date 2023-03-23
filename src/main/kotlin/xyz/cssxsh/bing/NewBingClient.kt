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

public open class NewBingClient(@PublishedApi internal val config: NewBingConfig) {
    public companion object {
        public const val RS: String = "\u001E"
        public const val USER_AGENT: String =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.44"
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
        install(UserAgent) {
            agent = USER_AGENT
        }
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
    protected open val format: Json = Json {
        ignoreUnknownKeys = true
    }
    protected val shared: MutableSharedFlow<Pair<String, JsonObject>> = MutableSharedFlow()

    @PublishedApi
    internal val logger: Logger = LoggerFactory.getLogger(this::class.java)

    public open suspend fun create(): NewBingChat {
        val uuid: UUID = UUID.randomUUID()
        val response = http.get("https://edgeservices.bing.com/edgesvc/turing/conversation/create") {
            header("x-ms-client-request-id", uuid)
            header("x-ms-useragent", config.device)
            header("accept-language", config.language)

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

        logger.debug(response.toString())

        return NewBingChat(
            clientId = conversation.clientId,
            conversationId = conversation.conversationId,
            conversationSignature = conversation.conversationSignature,
            uuid = uuid.toString(),
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
                    logger.warn("Frame.Binary")
                }
                is Frame.Text -> {
                    for (text in frame.readText().splitToSequence(RS)) {
                        if (text.isEmpty()) continue
                        val item = format.decodeFromString(JsonObject.serializer(), text)
                        when (item["type"]?.jsonPrimitive?.int) {
                            1 -> {
                                if (logger.isTraceEnabled) logger.trace(item.toString())
                            }
                            2 -> {
                                if (logger.isDebugEnabled) logger.debug(item.toString())
                                launch {
                                    shared.emit(chat.uuid to item)
                                }
                            }
                            3 -> {
                                if (logger.isDebugEnabled) logger.debug(item.toString())
                                return
                            }
                            6 -> {
                                if (logger.isDebugEnabled) logger.debug(item.toString())
                                // echo
                                launch {
                                    sendJson {
                                        put("type", 6)
                                    }
                                }
                                continue
                            }
                            else -> {
                                if (item.isEmpty().not()) logger.warn(item.toString())
                                continue
                            }
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

    protected open suspend fun DefaultClientWebSocketSession.message(chat: NewBingChat, text: String, style: String) {
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
                        when (style) {
                            "Balanced" -> {
                                add("galileo")
                            }
                            "Creative" -> {
                                add("h3imaginative")
                            }
                            "Precise" -> {
                                add("h3precise")
                            }
                            else -> logger.warn("Unknown Style: $style")
                        }
                    }
                    putJsonArray("allowedMessageTypes") {
                        for (type in config.allowed) {
                            add(type)
                        }
                    }
                    putJsonArray("sliceIds") {
                        for (id in config.sliceIds) {
                            add(id)
                        }
                    }
                    // XXX: traceId
                    put("isStartOfSession", chat.index == 0)
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
            header("accept-language", config.language)
        }, block)
    }

    /**
     * @param style Balanced, Creative, Precise
     */
    public suspend fun send(chat: NewBingChat, text: String, style: String) {
        websocket {
            bind(chat = chat)
            message(chat = chat, text = text, style = style)
            handle(chat = chat)
        }
    }
}