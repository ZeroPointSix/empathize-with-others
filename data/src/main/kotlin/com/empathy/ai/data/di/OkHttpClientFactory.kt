package com.empathy.ai.data.di

import android.util.Log
import com.empathy.ai.data.local.ProxyPreferences
import com.empathy.ai.domain.model.ProxyConfig
import com.empathy.ai.domain.model.ProxyType
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttpClient工厂类
 *
 * 支持动态代理配置切换，无需重启应用
 *
 * 设计规格（TD-00025）:
 * - 支持HTTP/HTTPS/SOCKS4/SOCKS5代理
 * - 支持代理认证
 * - 支持动态重建客户端
 * - 线程安全
 *
 * @see TD-00025 Phase 5: 网络代理实现
 */
@Singleton
class OkHttpClientFactory @Inject constructor(
    private val proxyPreferences: ProxyPreferences
) {
    @Volatile
    private var currentClient: OkHttpClient? = null

    @Volatile
    private var currentProxyConfig: ProxyConfig? = null

    private val lock = Any()

    /**
     * 获取OkHttpClient实例
     *
     * 如果代理配置发生变化，会自动重建客户端
     *
     * @return OkHttpClient实例
     */
    fun getClient(): OkHttpClient {
        val proxyConfig = proxyPreferences.getProxyConfig()

        synchronized(lock) {
            // 检查是否需要重建客户端
            if (currentClient == null || proxyConfig != currentProxyConfig) {
                currentClient = buildClient(proxyConfig)
                currentProxyConfig = proxyConfig
                Log.d(TAG, "OkHttpClient rebuilt with proxy config: $proxyConfig")
            }
            return currentClient!!
        }
    }

    /**
     * 强制重建客户端
     *
     * 当代理配置更新后调用此方法
     */
    fun rebuild() {
        synchronized(lock) {
            val proxyConfig = proxyPreferences.getProxyConfig()
            currentClient = buildClient(proxyConfig)
            currentProxyConfig = proxyConfig
            Log.d(TAG, "OkHttpClient force rebuilt with proxy config: $proxyConfig")
        }
    }

    /**
     * 使用指定配置构建客户端
     *
     * 用于测试代理连接
     *
     * @param proxyConfig 代理配置
     * @return OkHttpClient实例
     */
    fun buildClientWithConfig(proxyConfig: ProxyConfig): OkHttpClient {
        return buildClient(proxyConfig)
    }

    /**
     * 构建OkHttpClient
     */
    private fun buildClient(proxyConfig: ProxyConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()

        // 超时设置
        builder
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // 日志拦截器
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        builder.addInterceptor(loggingInterceptor)

        // 代理配置
        if (proxyConfig.enabled && proxyConfig.isValid()) {
            configureProxy(builder, proxyConfig)
        }

        return builder.build()
    }

    /**
     * 配置代理
     */
    private fun configureProxy(builder: OkHttpClient.Builder, config: ProxyConfig) {
        val proxyType = when (config.type) {
            ProxyType.HTTP, ProxyType.HTTPS -> Proxy.Type.HTTP
            ProxyType.SOCKS4, ProxyType.SOCKS5 -> Proxy.Type.SOCKS
        }

        val proxy = Proxy(
            proxyType,
            InetSocketAddress(config.host, config.port)
        )
        builder.proxy(proxy)

        Log.d(TAG, "Proxy configured: ${config.type} ${config.host}:${config.port}")

        // 代理认证
        if (config.requiresAuth()) {
            val authenticator = Authenticator { _, response ->
                if (response.request.header("Proxy-Authorization") != null) {
                    // 已经尝试过认证，避免无限循环
                    return@Authenticator null
                }

                val credential = Credentials.basic(config.username, config.password)
                response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
            builder.proxyAuthenticator(authenticator)
            Log.d(TAG, "Proxy authentication configured for user: ${config.username}")
        }
    }

    companion object {
        private const val TAG = "OkHttpClientFactory"

        /** 连接超时（秒） */
        private const val CONNECT_TIMEOUT_SECONDS = 15L

        /** 读取超时（秒） */
        private const val READ_TIMEOUT_SECONDS = 45L

        /** 写入超时（秒） */
        private const val WRITE_TIMEOUT_SECONDS = 15L
    }
}
