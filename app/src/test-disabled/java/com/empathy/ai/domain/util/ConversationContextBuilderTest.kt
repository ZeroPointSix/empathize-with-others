package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ConversationContextConfig
import com.empathy.ai.domain.model.MessageSender
import com.empathy.ai.domain.model.TimeFlowMarker
import com.empathy.ai.domain.model.TimestampedMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ConversationContextBuilder 单元测试
 *
 * 测试对话上下文构建器的核心功能
 */
class ConversationContextBuilderTest {

    private lateinit var builder: ConversationContextBuilder

    @Before
    fun setup() {
        builder = ConversationContextBuilder()
    }

    // ==================== buildHistoryContext 测试 ====================

    @Test
    fun `空消息列表返回空字符串`() {
        val result = builder.buildHistoryContext(emptyList())
        assertEquals("", result)
    }

    @Test
    fun `单条消息不插入时间标记`() {
        val messages = listOf(
            TimestampedMessage(
                content = "你好",
                timestamp = System.currentTimeMillis(),
                sender = MessageSender.ME
            )
        )

        val result = builder.buildHistoryContext(messages)

        assertTrue(result.contains("【历史对话】(最近1条)"))
        assertTrue(result.contains("[我]: 你好"))
        // 单条消息不应该有时间标记
        assertTrue(!result.contains("---"))
    }

    @Test
    fun `热对话不插入时间标记`() {
        val baseTime = System.currentTimeMillis()
        val messages = listOf(
            TimestampedMessage(
                content = "你好",
                timestamp = baseTime,
                sender = MessageSender.ME
            ),
            TimestampedMessage(
                content = "在吗",
                timestamp = baseTime + 5 * 60 * 1000, // 5分钟后
                sender = MessageSender.ME
            )
        )

        val result = builder.buildHistoryContext(messages)

        // 热对话（<10分钟）不应该有时间标记
        assertTrue(!result.contains("对话暂停了"))
        assertTrue(!result.contains("---"))
    }

    @Test
    fun `温对话插入轻标记`() {
        val baseTime = System.currentTimeMillis()
        val messages = listOf(
            TimestampedMessage(
                content = "你好",
                timestamp = baseTime,
                sender = MessageSender.ME
            ),
            TimestampedMessage(
                content = "在吗",
                timestamp = baseTime + 30 * 60 * 1000, // 30分钟后
                sender = MessageSender.ME
            )
        )

        val result = builder.buildHistoryContext(messages)

        // 温对话（10分钟~3小时）应该有轻标记
        assertTrue(result.contains("对话暂停了 30 分钟"))
    }

    @Test
    fun `温对话超过60分钟显示小时`() {
        val baseTime = System.currentTimeMillis()
        val messages = listOf(
            TimestampedMessage(
                content = "你好",
                timestamp = baseTime,
                sender = MessageSender.ME
            ),
            TimestampedMessage(
                content = "在吗",
                timestamp = baseTime + 90 * 60 * 1000, // 90分钟后
                sender = MessageSender.ME
            )
        )

        val result = builder.buildHistoryContext(messages)

        // 90分钟应该显示为1小时
        assertTrue(result.contains("对话暂停了 1 小时"))
    }

    @Test
    fun `冷对话插入强标记并提示情绪`() {
        val baseTime = System.currentTimeMillis()
        val messages = listOf(
            TimestampedMessage(
                content = "你好",
                timestamp = baseTime,
                sender = MessageSender.ME
            ),
            TimestampedMessage(
                content = "在吗",
                timestamp = baseTime + 6 * 60 * 60 * 1000, // 6小时后
                sender = MessageSender.ME
            )
        )

        val result = builder.buildHistoryContext(messages)

        // 冷对话（>3小时）应该有强标记并提示情绪
        assertTrue(result.contains("对话暂停了 6 小时"))
        assertTrue(result.contains("注意对方可能的情绪变化"))
    }

    // ==================== calculateTimeMarker 测试 ====================

    @Test
    fun `第一条消息返回None标记`() {
        val config = ConversationContextConfig()
        val marker = builder.calculateTimeMarker(
            previousTimestamp = null,
            currentTimestamp = System.currentTimeMillis(),
            previousDate = null,
            config = config
        )

        assertEquals(TimeFlowMarker.None, marker)
    }

    @Test
    fun `热对话间隔返回None标记`() {
        val config = ConversationContextConfig()
        val baseTime = System.currentTimeMillis()
        val marker = builder.calculateTimeMarker(
            previousTimestamp = baseTime,
            currentTimestamp = baseTime + 5 * 60 * 1000, // 5分钟
            previousDate = "2025-12-16",
            config = config
        )

        assertEquals(TimeFlowMarker.None, marker)
    }

    @Test
    fun `温对话间隔返回ShortGap标记`() {
        val config = ConversationContextConfig()
        val baseTime = System.currentTimeMillis()
        val marker = builder.calculateTimeMarker(
            previousTimestamp = baseTime,
            currentTimestamp = baseTime + 45 * 60 * 1000, // 45分钟
            previousDate = "2025-12-16",
            config = config
        )

        assertTrue(marker is TimeFlowMarker.ShortGap)
        assertEquals(45, (marker as TimeFlowMarker.ShortGap).minutes)
    }

    @Test
    fun `冷对话间隔返回LongGap标记`() {
        val config = ConversationContextConfig()
        val baseTime = System.currentTimeMillis()
        val marker = builder.calculateTimeMarker(
            previousTimestamp = baseTime,
            currentTimestamp = baseTime + 5 * 60 * 60 * 1000, // 5小时
            previousDate = "2025-12-16",
            config = config
        )

        assertTrue(marker is TimeFlowMarker.LongGap)
        assertEquals(5, (marker as TimeFlowMarker.LongGap).hours)
    }

    @Test
    fun `跨天返回DateChange标记`() {
        val config = ConversationContextConfig()
        val baseTime = System.currentTimeMillis()
        val marker = builder.calculateTimeMarker(
            previousTimestamp = baseTime,
            currentTimestamp = baseTime + 60 * 1000, // 1分钟后（但跨天）
            previousDate = "2025-12-15",
            config = config
        )

        // 跨天优先于时间间隔
        assertTrue(marker is TimeFlowMarker.DateChange)
    }

    // ==================== 自定义配置测试 ====================

    @Test
    fun `自定义热对话阈值生效`() {
        val config = ConversationContextConfig(
            historyCount = 5,
            hotSessionThreshold = 5 * 60 * 1000L, // 5分钟
            warmSessionThreshold = 60 * 60 * 1000L // 1小时
        )
        val baseTime = System.currentTimeMillis()

        // 6分钟应该是温对话
        val marker = builder.calculateTimeMarker(
            previousTimestamp = baseTime,
            currentTimestamp = baseTime + 6 * 60 * 1000,
            previousDate = "2025-12-16",
            config = config
        )

        assertTrue(marker is TimeFlowMarker.ShortGap)
    }
}
