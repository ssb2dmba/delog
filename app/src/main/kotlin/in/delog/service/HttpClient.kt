package `in`.delog.service

import `in`.delog.MainApplication
import `in`.delog.db.SettingStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Duration

object HttpClient {
    private var proxy: Proxy? = null

    fun getHttpClient(): OkHttpClient {
        val seconds = 40L
        val duration = Duration.ofSeconds(seconds)
        proxy = getProxy()
        return OkHttpClient.Builder()
            .proxy(proxy)
            .readTimeout(duration)
            .connectTimeout(duration)
            .writeTimeout(duration)
            .build()
    }

    fun getProxy(): Proxy? {
        val context = MainApplication.applicationContext()
        val store = SettingStore(context)
        val alwaysTorProxy = runBlocking { store.getData(SettingStore.ALWAYS_TOR_PROXY).first() }
        val torProxyPort = runBlocking { store.getData(SettingStore.TOR_SOCK_PROXY_PORT).first() }
        val port = torProxyPort?.toInt() ?: 9050
        return initProxy(alwaysTorProxy== "1","127.0.0.1",  port)
    }

    fun getTorProxy(): Proxy? {
        val context = MainApplication.applicationContext()
        val store = SettingStore(context)
        val torProxyPort = runBlocking { store.getData(SettingStore.TOR_SOCK_PROXY_PORT).first() }
        val port = torProxyPort?.toInt() ?: 9050
        return initProxy(true,"127.0.0.1",  port)
    }

    private fun initProxy(useProxy: Boolean, hostname: String, port: Int): Proxy? {
        return if (useProxy) Proxy(Proxy.Type.SOCKS, InetSocketAddress(hostname, port)) else null
    }
}