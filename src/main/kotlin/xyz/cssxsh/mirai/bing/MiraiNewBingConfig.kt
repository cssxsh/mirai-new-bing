package xyz.cssxsh.mirai.bing

import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import xyz.cssxsh.bing.*
import java.io.File

public object MiraiNewBingConfig : ReadOnlyPluginConfig("bing"), NewBingConfig {

    override val cookie: String get() = cache.readText()

    @PublishedApi
    internal var cache: File = File("./cookie.txt")

    @ConsoleExperimentalApi
    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
        val plugin = owner as? JvmPlugin ?: return
        cache = plugin.resolveDataFile("cookie.txt").apply {
            if (exists().not()) createNewFile()
        }
    }

    @ValueName("proxy")
    @ValueDescription("代理，配置时请注意单引号")
    override val proxy: String by value("")

    @ValueName("mirror")
    @ValueDescription("镜像，配置时请注意单引号")
    override val mirror: String by value("")

    @ValueName("doh")
    override val doh: String by value("https://public.dns.iij.jp/dns-query")

    @ValueName("ipv6")
    override val ipv6: Boolean by value(true)

    @ValueName("timeout")
    override val timeout: Long by value(30_000L)

    @ValueName("user_agent")
    override val ua: String by value(NewBingClient.USER_AGENT)

    @ValueName("device")
    override val device: String by value(NewBingClient.DEVICE)

    @ValueName("accept-language")
    override val language: String by value("zh-CN,zh;q=0.9")

    @ValueName("options")
    override val options: List<String> by value(listOf(
        "deepleo",
        "enable_debug_commands",
        "disable_emoji_spoken_text",
        "enablemm"
    ))

    @ValueName("allowed_message_types")
    override val allowed: List<String> by value(listOf(
        "Chat"
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

    @ValueName("reset_prefix")
    public val reset: String by value("bing-reset")

    @ValueName("chat_expires_in")
    public val expiresIn: Long by value(300L)

    @ValueName("show_source_attributions")
    public val source: Boolean by value(true)

    @ValueName("new_topic_greet")
    public val newTopicGreet: List<String> by value(listOf(
        "谢谢你帮我理清头绪! 我现在能帮你做什么?",
        "谢谢你! 知道你什么时候准备好继续前进总是很有帮助的。我现在能为你回答什么问题?",
        "重新开始总是很棒。问我任何问题!",
        "当然，我很乐意重新开始。我现在可以为你提供哪些帮助?",
        "好了，我已经为新的对话重置了我的大脑。你现在想聊些什么?",
        "没问题，很高兴你喜欢上一次对话。让我们转到一个新主题。你想要了解有关哪些内容的详细信息?",
        "当然，我已准备好进行新的挑战。我现在可以为你做什么?",
        "好的，我已清理好板子，可以重新开始了。我可以帮助你探索什么?",
        "明白了，我已经抹去了过去，专注于现在。我们现在应该探索什么?",
        "很好，让我们来更改主题。你在想什么?",
        "好了，我已经为新的对话擦拭干净板子了。现在我可以和你聊些什么呢?",
        "不用担心，我很高兴尝试一些新内容。我现在可以为你回答什么问题?",
    ))
}