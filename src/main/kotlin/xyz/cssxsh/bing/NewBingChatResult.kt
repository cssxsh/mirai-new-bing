package xyz.cssxsh.bing

import kotlinx.serialization.*

@Serializable
public data class NewBingChatResult(
    @SerialName("message")
    val message: String? = "Link Failure",
    @SerialName("value")
    val value: String = ""
)