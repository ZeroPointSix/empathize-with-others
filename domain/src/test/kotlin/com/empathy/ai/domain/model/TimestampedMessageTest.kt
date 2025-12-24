package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TimestampedMessage 单元测试
 *
 * 测试带时间戳消息模型的功能
 */
class TimestampedMessageTest {

    // ==================== 构造函数验证测试 ====================

    @Test
    fun `正常创建消息成功`() {
        val message = TimestampedMessage(
            content = "你好",
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.ME
        )

        assertEquals("你好", message.content)
        assertEquals(MessageSender.ME, message.sender)
    }

    @Test
    fun `空内容抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimestampedMessage(
                content = "",
                timestamp = System.currentTimeMillis(),
                sender = MessageSender.ME
            )
        }
    }

    @Test
    fun `空白内容抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimestampedMessage(
                content = "   ",
                timestamp = System.currentTimeMillis(),
                sender = MessageSender.ME
            )
        }
    }

    @Test
    fun `时间戳为0抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimestampedMessage(
                content = "你好",
                timestamp = 0,
                sender = MessageSender.ME
            )
        }
    }

    @Test
    fun `负数时间戳抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimestampedMessage(
                content = "你好",
                timestamp = -1,
                sender = MessageSender.ME
            )
        }
    }

    // ==================== toDisplayString 测试 ====================

    @Test
    fun `toDisplayString包含发送者前缀`() {
        val message = TimestampedMessage(
            content = "你好",
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.ME
        )

        val result = message.toDisplayString()

        assertTrue(result.contains("[我]"))
    }

    @Test
    fun `toDisplayString包含消息内容`() {
        val content = "这是一条测试消息"
        val message = TimestampedMessage(
            content = content,
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.ME
        )

        val result = message.toDisplayString()

        assertTrue(result.contains(content))
    }

    @Test
    fun `toDisplayString默认包含时间`() {
        val message = TimestampedMessage(
            content = "你好",
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.ME
        )

        val result = message.toDisplayString()

        // 应该包含括号（时间格式）
        assertTrue(result.contains("("))
        assertTrue(result.contains(")"))
    }

    @Test
    fun `toDisplayString不显示时间`() {
        val message = TimestampedMessage(
            content = "你好",
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.ME
        )

        val result = message.toDisplayString(showTime = false)

        // 不应该包含括号
        assertTrue(!result.contains("("))
        assertTrue(!result.contains(")"))
        assertEquals("[我]: 你好", result)
    }

    @Test
    fun `toDisplayString格式正确`() {
        val message = TimestampedMessage(
            content = "你好",
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.ME
        )

        val result = message.toDisplayString(showTime = false)

        assertEquals("[我]: 你好", result)
    }

    // ==================== getFormattedTime 测试 ====================

    @Test
    fun `getFormattedTime返回HH_mm格式`() {
        // 使用固定时间戳：2025-12-16 10:30:00
        val message = TimestampedMessage(
            content = "你好",
            timestamp = 1734319800000L, // 这个时间戳对应的时间取决于时区
            sender = MessageSender.ME
        )

        val result = message.getFormattedTime()

        // 验证格式为 HH:mm
        assertTrue(result.matches(Regex("\\d{2}:\\d{2}")))
    }

    // ==================== getFormattedDate 测试 ====================

    @Test
    fun `getFormattedDate返回yyyy_MM_dd格式`() {
        val message = TimestampedMessage(
            content = "你好",
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.ME
        )

        val result = message.getFormattedDate()

        // 验证格式为 yyyy-MM-dd
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    // ==================== MessageSender 测试 ====================

    @Test
    fun `ME发送者前缀正确`() {
        val message = TimestampedMessage(
            content = "你好",
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.ME
        )

        assertTrue(message.toDisplayString().startsWith("[我]"))
    }

    @Test
    fun `THEM发送者前缀正确`() {
        val message = TimestampedMessage(
            content = "你好",
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.THEM
        )

        assertTrue(message.toDisplayString().startsWith("[对方]"))
    }
}
