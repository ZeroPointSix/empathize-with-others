package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ConversationContextConfig 单元测试
 *
 * 测试对话上下文配置类的验证逻辑
 */
class ConversationContextConfigTest {

    // ==================== 默认值测试 ====================

    @Test
    fun `默认历史条数为5`() {
        val config = ConversationContextConfig()
        assertEquals(5, config.historyCount)
    }

    @Test
    fun `默认热对话阈值为10分钟`() {
        val config = ConversationContextConfig()
        assertEquals(10 * 60 * 1000L, config.hotSessionThreshold)
    }

    @Test
    fun `默认温对话阈值为3小时`() {
        val config = ConversationContextConfig()
        assertEquals(3 * 60 * 60 * 1000L, config.warmSessionThreshold)
    }

    @Test
    fun `常量DEFAULT_HISTORY_COUNT正确`() {
        assertEquals(5, ConversationContextConfig.DEFAULT_HISTORY_COUNT)
    }

    @Test
    fun `常量DEFAULT_HOT_SESSION_THRESHOLD正确`() {
        assertEquals(10 * 60 * 1000L, ConversationContextConfig.DEFAULT_HOT_SESSION_THRESHOLD)
    }

    @Test
    fun `常量DEFAULT_WARM_SESSION_THRESHOLD正确`() {
        assertEquals(3 * 60 * 60 * 1000L, ConversationContextConfig.DEFAULT_WARM_SESSION_THRESHOLD)
    }

    // ==================== 历史条数选项测试 ====================

    @Test
    fun `HISTORY_COUNT_OPTIONS包含0_5_10`() {
        val options = ConversationContextConfig.HISTORY_COUNT_OPTIONS
        assertEquals(listOf(0, 5, 10), options)
    }

    @Test
    fun `historyCount为0时创建成功`() {
        val config = ConversationContextConfig(historyCount = 0)
        assertEquals(0, config.historyCount)
    }

    @Test
    fun `historyCount为5时创建成功`() {
        val config = ConversationContextConfig(historyCount = 5)
        assertEquals(5, config.historyCount)
    }

    @Test
    fun `historyCount为10时创建成功`() {
        val config = ConversationContextConfig(historyCount = 10)
        assertEquals(10, config.historyCount)
    }

    // ==================== 验证逻辑测试 ====================

    @Test
    fun `无效historyCount抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversationContextConfig(historyCount = 3)
        }
    }

    @Test
    fun `负数historyCount抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversationContextConfig(historyCount = -1)
        }
    }

    @Test
    fun `historyCount为1抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversationContextConfig(historyCount = 1)
        }
    }

    @Test
    fun `historyCount为15抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversationContextConfig(historyCount = 15)
        }
    }

    @Test
    fun `热对话阈值为0抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversationContextConfig(hotSessionThreshold = 0)
        }
    }

    @Test
    fun `热对话阈值为负数抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversationContextConfig(hotSessionThreshold = -1)
        }
    }

    @Test
    fun `温对话阈值小于等于热对话阈值抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversationContextConfig(
                hotSessionThreshold = 10 * 60 * 1000L,
                warmSessionThreshold = 10 * 60 * 1000L // 等于热对话阈值
            )
        }
    }

    @Test
    fun `温对话阈值小于热对话阈值抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversationContextConfig(
                hotSessionThreshold = 10 * 60 * 1000L,
                warmSessionThreshold = 5 * 60 * 1000L // 小于热对话阈值
            )
        }
    }

    // ==================== 自定义配置测试 ====================

    @Test
    fun `自定义阈值创建成功`() {
        val config = ConversationContextConfig(
            historyCount = 10,
            hotSessionThreshold = 5 * 60 * 1000L,
            warmSessionThreshold = 60 * 60 * 1000L
        )

        assertEquals(10, config.historyCount)
        assertEquals(5 * 60 * 1000L, config.hotSessionThreshold)
        assertEquals(60 * 60 * 1000L, config.warmSessionThreshold)
    }

    @Test
    fun `异常消息包含有效选项`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            ConversationContextConfig(historyCount = 7)
        }

        assertTrue(exception.message?.contains("0") == true)
        assertTrue(exception.message?.contains("5") == true)
        assertTrue(exception.message?.contains("10") == true)
    }
}
