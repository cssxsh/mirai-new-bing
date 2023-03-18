package xyz.cssxsh.mirai.bing

import kotlinx.coroutines.*
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.event.*

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
        MiraiNewBingConfig.reload()
        if (MiraiNewBingConfig.cookie.isEmpty()) {
            val token = runBlocking { ConsoleInput.requestInput(hint = "请输入 New Bing Cookie") }

            @OptIn(ConsoleExperimentalApi::class)
            @Suppress("UNCHECKED_CAST")
            val value = MiraiNewBingConfig.findBackingFieldValue<String>("cookie") as Value<String>
            value.value = token

        }
        MiraiNewBingConfig.save()

        MiraiNewBingListener.chat
        MiraiNewBingListener.registerTo(globalEventChannel())
    }

    override fun onDisable() {
        MiraiNewBingListener.cancel()
    }
}