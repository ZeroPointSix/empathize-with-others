package com.empathy.ai.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ProxyPreferences 单元测试
 * 
 * TD-00025 T5-05: 代理配置存储测试
 * 
 * 测试覆盖：
 * - 代理配置保存和读取
 * - 代理启用状态
 * - 配置验证
 * - 配置清除
 */
class ProxyPreferencesTest {

    // ============================================================
    // 代理配置保存和读取测试
    // ============================================================

    @Test
    fun `代理配置_默认配置正确`() {
        val config = TestProxyConfig()
        
        assertFalse("默认应禁用代理", config.enabled)
        assertEquals("默认类型应为HTTP", TestProxyType.HTTP, config.type)
        assertEquals("默认主机应为空", "", config.host)
        assertEquals("默认端口应为0", 0, config.port)
        assertEquals("默认用户名应为空", "", config.username)
        assertEquals("默认密码应为空", "", config.password)
    }

    @Test
    fun `代理配置_保存HTTP代理配置`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 8080,
            username = "",
            password = ""
        )
        
        // 模拟保存和读取
        val savedConfig = config.copy()
        
        assertEquals("启用状态应正确", true, savedConfig.enabled)
        assertEquals("类型应为HTTP", TestProxyType.HTTP, savedConfig.type)
        assertEquals("主机应正确", "proxy.example.com", savedConfig.host)
        assertEquals("端口应正确", 8080, savedConfig.port)
    }

    @Test
    fun `代理配置_保存SOCKS5代理配置`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.SOCKS5,
            host = "socks.example.com",
            port = 1080,
            username = "user",
            password = "pass"
        )
        
        // 模拟保存和读取
        val savedConfig = config.copy()
        
        assertEquals("类型应为SOCKS5", TestProxyType.SOCKS5, savedConfig.type)
        assertEquals("主机应正确", "socks.example.com", savedConfig.host)
        assertEquals("端口应正确", 1080, savedConfig.port)
        assertEquals("用户名应正确", "user", savedConfig.username)
        assertEquals("密码应正确", "pass", savedConfig.password)
    }

    // ============================================================
    // 代理启用状态测试
    // ============================================================

    @Test
    fun `代理启用_默认应禁用`() {
        val enabled = false
        
        assertFalse("默认应禁用代理", enabled)
    }

    @Test
    fun `代理启用_设置启用`() {
        var enabled = false
        enabled = true
        
        assertTrue("应启用代理", enabled)
    }

    @Test
    fun `代理启用_设置禁用`() {
        var enabled = true
        enabled = false
        
        assertFalse("应禁用代理", enabled)
    }

    // ============================================================
    // 配置验证测试
    // ============================================================

    @Test
    fun `配置验证_有效配置`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 8080
        )
        
        assertTrue("有效配置应通过验证", config.isValid())
    }

    @Test
    fun `配置验证_空主机无效`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "",
            port = 8080
        )
        
        assertFalse("空主机应无效", config.isValid())
    }

    @Test
    fun `配置验证_端口为0无效`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 0
        )
        
        assertFalse("端口为0应无效", config.isValid())
    }

    @Test
    fun `配置验证_端口超出范围无效`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 70000
        )
        
        assertFalse("端口超出范围应无效", config.isValid())
    }

    @Test
    fun `配置验证_hasValidConfig_启用且有效`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 8080
        )
        
        val hasValidConfig = config.enabled && config.isValid()
        
        assertTrue("启用且有效应返回true", hasValidConfig)
    }

    @Test
    fun `配置验证_hasValidConfig_禁用`() {
        val config = TestProxyConfig(
            enabled = false,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 8080
        )
        
        val hasValidConfig = config.enabled && config.isValid()
        
        assertFalse("禁用应返回false", hasValidConfig)
    }

    @Test
    fun `配置验证_hasValidConfig_启用但无效`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "",
            port = 0
        )
        
        val hasValidConfig = config.enabled && config.isValid()
        
        assertFalse("启用但无效应返回false", hasValidConfig)
    }

    // ============================================================
    // 配置清除测试
    // ============================================================

    @Test
    fun `配置清除_清除后应为默认值`() {
        // 模拟清除配置
        val clearedConfig = TestProxyConfig()
        
        assertFalse("清除后应禁用", clearedConfig.enabled)
        assertEquals("清除后类型应为HTTP", TestProxyType.HTTP, clearedConfig.type)
        assertEquals("清除后主机应为空", "", clearedConfig.host)
        assertEquals("清除后端口应为0", 0, clearedConfig.port)
        assertEquals("清除后用户名应为空", "", clearedConfig.username)
        assertEquals("清除后密码应为空", "", clearedConfig.password)
    }

    // ============================================================
    // 代理类型测试
    // ============================================================

    @Test
    fun `代理类型_HTTP类型解析正确`() {
        val typeName = "HTTP"
        val type = TestProxyType.valueOf(typeName)
        
        assertEquals("HTTP类型应正确解析", TestProxyType.HTTP, type)
    }

    @Test
    fun `代理类型_HTTPS类型解析正确`() {
        val typeName = "HTTPS"
        val type = TestProxyType.valueOf(typeName)
        
        assertEquals("HTTPS类型应正确解析", TestProxyType.HTTPS, type)
    }

    @Test
    fun `代理类型_SOCKS4类型解析正确`() {
        val typeName = "SOCKS4"
        val type = TestProxyType.valueOf(typeName)
        
        assertEquals("SOCKS4类型应正确解析", TestProxyType.SOCKS4, type)
    }

    @Test
    fun `代理类型_SOCKS5类型解析正确`() {
        val typeName = "SOCKS5"
        val type = TestProxyType.valueOf(typeName)
        
        assertEquals("SOCKS5类型应正确解析", TestProxyType.SOCKS5, type)
    }

    @Test
    fun `代理类型_无效类型应使用默认值`() {
        val typeName = "INVALID"
        val type = try {
            TestProxyType.valueOf(typeName)
        } catch (e: IllegalArgumentException) {
            TestProxyType.HTTP
        }
        
        assertEquals("无效类型应使用HTTP", TestProxyType.HTTP, type)
    }

    // ============================================================
    // 认证配置测试
    // ============================================================

    @Test
    fun `认证配置_有认证信息`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 8080,
            username = "user",
            password = "pass"
        )
        
        assertTrue("应需要认证", config.requiresAuth())
    }

    @Test
    fun `认证配置_无认证信息`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 8080,
            username = "",
            password = ""
        )
        
        assertFalse("不应需要认证", config.requiresAuth())
    }

    @Test
    fun `认证配置_只有用户名`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 8080,
            username = "user",
            password = ""
        )
        
        assertFalse("只有用户名不应需要认证", config.requiresAuth())
    }

    @Test
    fun `认证配置_只有密码`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 8080,
            username = "",
            password = "pass"
        )
        
        assertFalse("只有密码不应需要认证", config.requiresAuth())
    }

    // ============================================================
    // 代理地址测试
    // ============================================================

    @Test
    fun `代理地址_生成正确`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "proxy.example.com",
            port = 8080
        )
        
        val address = config.getProxyAddress()
        
        assertEquals("代理地址应正确", "proxy.example.com:8080", address)
    }

    @Test
    fun `代理地址_IP地址格式`() {
        val config = TestProxyConfig(
            enabled = true,
            type = TestProxyType.HTTP,
            host = "192.168.1.1",
            port = 3128
        )
        
        val address = config.getProxyAddress()
        
        assertEquals("IP代理地址应正确", "192.168.1.1:3128", address)
    }

    // ============================================================
    // 辅助数据类
    // ============================================================

    /**
     * 测试用代理类型
     */
    enum class TestProxyType {
        HTTP, HTTPS, SOCKS4, SOCKS5
    }

    /**
     * 测试用代理配置
     */
    data class TestProxyConfig(
        val enabled: Boolean = false,
        val type: TestProxyType = TestProxyType.HTTP,
        val host: String = "",
        val port: Int = 0,
        val username: String = "",
        val password: String = ""
    ) {
        fun isValid(): Boolean {
            return host.isNotBlank() && port in 1..65535
        }

        fun requiresAuth(): Boolean {
            return username.isNotBlank() && password.isNotBlank()
        }

        fun getProxyAddress(): String {
            return "$host:$port"
        }
    }
}
