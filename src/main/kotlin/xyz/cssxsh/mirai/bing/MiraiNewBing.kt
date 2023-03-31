package xyz.cssxsh.mirai.bing

import kotlinx.coroutines.*
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*

public object MiraiNewBing : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.new-bing",
        name = "mirai-new-bing",
        version = "0.4.1",
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
        logger.info { "当前代理设置 '${MiraiNewBingConfig.proxy}'" }

        MiraiNewBingListener.chat
        MiraiNewBingListener.reload
        logger.warning { "使用前请赋予权限！！！！！！！！！" }
        MiraiNewBingListener.registerTo(globalEventChannel())
    }

    override fun onDisable() {
        MiraiNewBingListener.cancel()
    }
}