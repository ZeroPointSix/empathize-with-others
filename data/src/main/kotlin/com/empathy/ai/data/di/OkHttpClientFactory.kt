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
 * 【核心功能】动态代理支持（TD-00025）
 * - 无需重启应用即可切换代理配置
 * - 自动重建 OkHttpClient 以应用新配置
 * - 线程安全的单例模式
 *
 * 业务背景:
 *   - FD: FD-00025 [AI配置功能完善]
 *   - TDD: TDD-00025 [AI配置功能完善技术设计]
 *
 * 【设计决策】为什么需要动态重建客户端？
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ OkHttpClient 设计原则                                               │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │ 1. OkHttpClient 被设计为可复用实例                                  │
 * │    - 连接池、线程池、拦截器链都是重量级对象                         │
 * │    - 频繁创建销毁会导致性能问题                                     │
 * │                                                                     │
 * │ 2. 代理配置只能通过 Builder 在创建时指定                            │
 * │    - 没有 setProxy() 方法可以动态修改                               │
 * │    - 必须创建新实例                                                 │
 * │                                                                     │
 * │ 3. 解决方案：缓存 + 比较策略                                        │
 * │    - 缓存当前 Client 实例                                           │
 * │    - 每次获取时比较配置                                             │
 * │    - 配置变化时才重建                                               │
 * └─────────────────────────────────────────────────────────────────────┘
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
     * 获取 OkHttpClient 实例
     *
     * 【懒加载 + 缓存】模式
     * - 首次调用时创建实例
     * - 后续调用直接返回缓存
     * - 代理配置变化时自动重建
     *
     * 【线程安全】
     * - 使用 synchronized 保护客户端重建过程
     * - Volatile 保证可见性
     * - 双重检查锁定优化性能
     *
     * @return OkHttpClient 实例
     */
    fun getClient(): OkHttpClient {
        val proxyConfig = proxyPreferences.getProxyConfig()

        synchronized(lock) {
            // 检查是否需要重建客户端
            // 【比较策略】使用 data class 的 equals 比较所有字段
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
     * 【使用场景】代理配置更新后确保立即生效
     * - 用户在设置页面更新代理配置
     * - 需要立即使用新配置发起请求
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
     * 用于测试代理连接，不影响缓存配置
     *
     * @param proxyConfig 代理配置
     * @return OkHttpClient实例
     */
    fun buildClientWithConfig(proxyConfig: ProxyConfig): OkHttpClient {
        return buildClient(proxyConfig)
    }

    /**
     * 构建 OkHttpClient
     *
     * 【配置顺序】拦截器 → 代理 → 超时
     * - 日志拦截器最先添加，在最外层
     * - 代理配置在最后应用
     * - 超时在 Builder 阶段设置
     *
     * 【日志级别】BASIC vs BODY
     * - BASIC: 请求/响应行 + 耗时（推荐，避免日志过大）
     * - BODY: 完整请求/响应体（仅调试时开启，注意隐私）
     *
     * @param proxyConfig 代理配置（可为 null 表示直连）
     * @return 配置好的 OkHttpClient 实例
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
            // 【生产建议】DEBUG 模式下可改为 BODY
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
     *
     * 【代理类型映射】
     * - HTTP/HTTPS → Proxy.Type.HTTP
     * - SOCKS4/SOCKS5 → Proxy.Type.SOCKS
     *
     * 【认证流程】
     * 1. 首次请求不带 Proxy-Authorization
     * 2. 服务端返回 407 Proxy Authentication Required
     * 3. Authenticator 被调用，添加认证头后重试
     * 4. 避免无限循环：检查是否已添加过认证头
     *
     * @param builder OkHttpClient.Builder
     * @param config 代理配置
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
