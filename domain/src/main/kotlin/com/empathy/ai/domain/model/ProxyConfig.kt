package com.empathy.ai.domain.model

/**
 * 代理配置
 *
 * 网络代理的完整配置信息
 *
 * @property enabled 是否启用代理
 * @property type 代理类型
 * @property host 代理服务器地址
 * @property port 代理服务器端口
 * @property username 认证用户名（可选）
 * @property password 认证密码（可选）
 */
data class ProxyConfig(
    val enabled: Boolean = false,
    val type: ProxyType = ProxyType.HTTP,
    val host: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = ""
) {
    companion object {
        /** 默认 HTTP 代理端口 */
        const val DEFAULT_HTTP_PORT = 8080
        /** 默认 SOCKS 代理端口 */
        const val DEFAULT_SOCKS_PORT = 1080
        /** 端口最小值 */
        const val PORT_MIN = 1
        /** 端口最大值 */
        const val PORT_MAX = 65535
    }

    /**
     * 验证配置是否有效
     */
    fun isValid(): Boolean {
        if (!enabled) return true
        return host.isNotBlank() && port in PORT_MIN..PORT_MAX
    }

    /**
     * 是否需要认证
     */
    fun requiresAuth(): Boolean {
        return username.isNotBlank() && password.isNotBlank()
    }

    /**
     * 获取代理地址（host:port 格式）
     */
    fun getProxyAddress(): String {
        return if (host.isNotBlank() && port > 0) {
            "$host:$port"
        } else {
            ""
        }
    }

    /**
     * 获取完整的代理 URL
     */
    fun getProxyUrl(): String {
        if (!isValid()) return ""
        val scheme = when (type) {
            ProxyType.HTTP, ProxyType.HTTPS -> "http"
            ProxyType.SOCKS4, ProxyType.SOCKS5 -> "socks"
        }
        return if (requiresAuth()) {
            "$scheme://$username:$password@$host:$port"
        } else {
            "$scheme://$host:$port"
        }
    }
}

/**
 * 代理类型
 */
enum class ProxyType {
    /** HTTP 代理 */
    HTTP,
    /** HTTPS 代理 */
    HTTPS,
    /** SOCKS4 代理 */
    SOCKS4,
    /** SOCKS5 代理 */
    SOCKS5;

    /**
     * 获取显示名称
     */
    fun getDisplayName(): String {
        return when (this) {
            HTTP -> "HTTP"
            HTTPS -> "HTTPS"
            SOCKS4 -> "SOCKS4"
            SOCKS5 -> "SOCKS5"
        }
    }

    /**
     * 是否为 SOCKS 类型
     */
    fun isSocks(): Boolean {
        return this == SOCKS4 || this == SOCKS5
    }
}
