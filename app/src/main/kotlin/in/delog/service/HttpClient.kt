package `in`.delog.service

import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Duration

object HttpClient {
    private var proxy: Proxy? = null
    private var useProxy: Boolean = false

    var proxyChangeListeners = ArrayList<() -> Unit>()

    fun start() {
    }

    fun getHttpClient(): OkHttpClient {
        val seconds = if (useProxy) 40L else 10L
        val duration = Duration.ofSeconds(seconds)
        proxy = initProxy(false,"127.0.0.1", 9090)
        return OkHttpClient.Builder()
            .proxy(proxy)
            .readTimeout(duration)
            .connectTimeout(duration)
            .writeTimeout(duration)
            .build()
    }

    fun getProxy(): Proxy? {
        return proxy
    }

    fun initProxy(useProxy: Boolean, hostname: String, port: Int): Proxy? {
        return if (useProxy) Proxy(Proxy.Type.SOCKS, InetSocketAddress(hostname, port)) else null
    }
}