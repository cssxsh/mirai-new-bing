package xyz.cssxsh.mirai.bing

import net.mamoe.mirai.console.data.*
import xyz.cssxsh.bing.*

public object MiraiNewBingConfig : ReadOnlyPluginConfig("bing"), NewBingConfig {
    @ValueName("proxy")
    override val proxy: String by value("")

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

    @ValueName("default_style")
    public val default: String by value("Balanced")

    @ValueName("reload_prefix")
    public val reload: String by value("bing-reload")

    @ValueName("balanced_prefix")
    public val balanced: String by value("bing-balanced")

    @ValueName("creative_prefix")
    public val creative: String by value("bing-creative")

    @ValueName("precise_prefix")
    public val precise: String by value("bing-precise")

    @ValueName("show_source_attributions")
    public val source: Boolean by value(true)
}