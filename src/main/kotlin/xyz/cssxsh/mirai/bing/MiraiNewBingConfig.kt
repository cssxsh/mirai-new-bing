package xyz.cssxsh.mirai.bing

import net.mamoe.mirai.console.data.*
import xyz.cssxsh.bing.*

public object MiraiNewBingConfig : ReadOnlyPluginConfig("openai"), NewBingConfig {
    @ValueName("proxy")
    override val proxy: String by value("socks://127.0.0.1:7890")

    @ValueName("doh")
    override val doh: String by value("https://public.dns.iij.jp/dns-query")

    @ValueName("ipv6")
    override val ipv6: Boolean by value(true)

    @ValueName("timeout")
    override val timeout: Long by value(30_000L)

    @ValueName("cookie")
    override val cookie: String by value("")

    @ValueName("device")
    override val device: String by value("azsdk-js-api-client-factory/1.0.0-beta.1 core-rest-pipeline/1.10.0 OS/MacIntel")

    @ValueName("options")
    override val options: List<String> by value(listOf(
        "deepleo",
        "nlu_direct_response_filter",
        "disable_emoji_spoken_text",
        "responsible_ai_policy_235",
        "enablemm",
        "dtappid",
        "rai253",
        "dv3sugg",
        "harmonyv3"
    ))

    @ValueName("allowed_message_types")
    override val allowed: List<String> by value(listOf(
        "Chat",
        "InternalSearchQuery"
    ))

    @ValueName("chat_prefix")
    public val prefix: String by value("bing")
}