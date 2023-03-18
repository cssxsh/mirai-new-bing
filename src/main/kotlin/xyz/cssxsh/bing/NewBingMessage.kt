package xyz.cssxsh.bing

import kotlinx.serialization.*

@Serializable
public data class NewBingMessage(
    @SerialName("author")
    val author: String = "",
    @SerialName("contentOrigin")
    val contentOrigin: String = "",
    @SerialName("createdAt")
    val createdAt: String = "",
    @SerialName("messageId")
    val messageId: String = "",
    @SerialName("offense")
    val offense: String = "",
    @SerialName("privacy")
    val privacy: String? = null,
    @SerialName("requestId")
    val requestId: String = "",
    @SerialName("sourceAttributions")
    val sourceAttributions: List<NewBingSourceAttribution> = emptyList(),
    @SerialName("text")
    val text: String = "",
    @SerialName("timestamp")
    val timestamp: String = "",
    @SerialName("messageType")
    val messageType: String = "",
)