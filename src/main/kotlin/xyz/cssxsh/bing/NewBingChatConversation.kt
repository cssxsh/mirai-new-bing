package xyz.cssxsh.bing

import kotlinx.serialization.*

@Serializable
public data class NewBingChatConversation(
    @SerialName("clientId")
    val clientId: String = "",
    @SerialName("conversationId")
    val conversationId: String = "",
    @SerialName("conversationSignature")
    val conversationSignature: String = "",
    @SerialName("result")
    val result: NewBingChatResult
)