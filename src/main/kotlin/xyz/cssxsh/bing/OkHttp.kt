package xyz.cssxsh.bing

import io.ktor.http.*
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.*

internal fun OkHttpClient.Builder.apply(config: NewBingConfig) {
    dns(object : Dns {
        private val doh = DnsOverHttps.Builder()
            .client(OkHttpClient())
            .url(config.doh.toHttpUrl())
            .includeIPv6(config.ipv6)
            .build()

        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                doh.lookup(hostname)
            } catch (_: UnknownHostException) {
                Dns.SYSTEM.lookup(hostname)
            } catch (cause: Exception) {
                throw UnknownHostException(hostname).initCause(cause)
            }
        }
    })
    proxy(config.proxy.ifEmpty {
        val system = System.getProperty("java.net.useSystemProxies", "false")
        try {
            System.setProperty("java.net.useSystemProxies", "true")
            proxySelector(ProxySelector.getDefault())
        } finally {
            System.setProperty("java.net.useSystemProxies", system)
        }
        null
    }?.let { urlString ->
        val url = Url(urlString)
        when (url.protocol) {
            URLProtocol.HTTP -> Proxy(Proxy.Type.HTTP, InetSocketAddress(url.host, url.port))
            URLProtocol.SOCKS -> Proxy(Proxy.Type.SOCKS, InetSocketAddress(url.host, url.port))
            else -> throw IllegalArgumentException("Illegal Proxy: $urlString")
        }
    })
}