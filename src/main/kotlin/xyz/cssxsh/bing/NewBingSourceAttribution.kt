package xyz.cssxsh.bing

import kotlinx.serialization.*

@Serializable
public data class NewBingSourceAttribution(
    @SerialName("providerDisplayName")
    val providerDisplayName: String = "",
    @SerialName("searchQuery")
    val searchQuery: String = "",
    @SerialName("seeMoreUrl")
    val seeMoreUrl: String = ""
)