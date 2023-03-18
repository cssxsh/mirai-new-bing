package xyz.cssxsh.mirai.bing

import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.utils.*

public object MiraiNewBing : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.new-bing",
        name = "mirai-new-bing",
        version = "0.1.0",
    ) {
        author("cssxsh")
    }
) {
    override fun onEnable() {
        logger.info { "Plugin loaded" }
    }
}