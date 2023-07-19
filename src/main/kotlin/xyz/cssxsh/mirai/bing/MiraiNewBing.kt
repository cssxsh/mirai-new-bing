package xyz.cssxsh.mirai.bing

import kotlinx.coroutines.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*

public object MiraiNewBing : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.new-bing",
        name = "mirai-new-bing",
        version = "0.5.0",
    ) {
        author("cssxsh")
    }
) {
    override fun onEnable() {
        with(MiraiNewBingConfig) {
            reload()
            if (cookie.isEmpty()) {
                logger.warning("请将 Cookie 写入 ${cache.toPath().toUri()}")
            }
            if (proxy == "http" || proxy == "socks") {
                logger.error { "当前代理设置 '${proxy}'" }
            } else {
                logger.info { "当前代理设置 '${proxy}'" }
            }
        }

        MiraiNewBingListener.chat
        MiraiNewBingListener.reload
        logger.warning { "使用前请赋予权限！！！！！！！！！" }
        MiraiNewBingListener.registerTo(globalEventChannel())
    }

    override fun onDisable() {
        MiraiNewBingListener.cancel()
    }
}