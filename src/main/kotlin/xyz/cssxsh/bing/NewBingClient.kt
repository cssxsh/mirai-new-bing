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
import kotlin.random.*

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
    internal val log: Logger = LoggerFactory.getLogger(this::class.java)

    public open suspend fun create(): NewBingChat {
        val uuid: UUID = UUID.randomUUID()
        val ip = Random(uuid.hashCode()).run { "13.${nextInt(104, 107)}.${nextInt(0, 255)}.${nextInt(0, 255)}" }
        val response = http.get("https://www.bing.com/turing/conversation/create") {
            url {
                if (config.mirror.isNotEmpty()) {
                    takeFrom(config.mirror)
                }
            }
            header("x-ms-client-request-id", uuid)
            header("x-ms-useragent", config.device)
            header("accept-language", config.language)
            header("x-forwarded-for", ip)
            header("cookie", config.cookie)
        }

        log.debug(response.toString())

        if (response.contentLength() == 0L) {
            throw ServerResponseException(response, "<empty>")
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
                    log.warn("Frame.Binary")
                }
                is Frame.Text -> {
                    for (text in frame.readText().splitToSequence(RS)) {
                        if (text.isEmpty()) continue
                        val item = format.decodeFromString(JsonObject.serializer(), text)
                        when (item["type"]?.jsonPrimitive?.int) {
                            1 -> {
                                if (log.isTraceEnabled) log.trace(item.toString())
                            }
                            2 -> {
                                if (log.isDebugEnabled) log.debug(item.toString())
                                launch {
                                    shared.emit(chat.uuid to item)
                                }
                            }
                            3 -> {
                                if (log.isDebugEnabled) log.debug(item.toString())
                                return
                            }
                            6 -> {
                                if (log.isDebugEnabled) log.debug(item.toString())
                                // echo
                                launch {
                                    sendJson {
                                        put("type", 6)
                                    }
                                }
                                continue
                            }
                            else -> {
                                if (item.isEmpty().not()) log.warn(item.toString())
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
                            else -> log.warn("Unknown Style: '$style'")
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

    protected open suspend fun websocket(uuid: String, block: suspend DefaultClientWebSocketSession.() -> Unit) {
        http.wss("wss://sydney.bing.com/sydney/ChatHub", {
            header("accept-language", config.language)
            header("x-ms-client-request-id", uuid)
            header("x-ms-useragent", config.device)
        }, block)
    }

    /**
     * @param style Balanced, Creative, Precise
     */
    public suspend fun send(chat: NewBingChat, text: String, style: String) {
        websocket(uuid = chat.uuid) {
            bind(chat = chat)
            message(chat = chat, text = text, style = style)
            handle(chat = chat)
        }
    }
}