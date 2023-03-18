package xyz.cssxsh.bing

import kotlinx.serialization.*

@Serializable
public data class NewBingChat(
    @SerialName("clientId")
    val clientId: String = "",
    @SerialName("conversationId")
    val conversationId: String = "",
    @SerialName("conversationSignature")
    val conversationSignature: String = "",
    @SerialName("index")
    var index: Int
)