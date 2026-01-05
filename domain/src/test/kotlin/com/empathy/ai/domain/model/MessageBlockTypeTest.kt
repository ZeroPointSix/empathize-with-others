package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MessageBlockType消息块类型枚举测试
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
class MessageBlockTypeTest {

    @Test
    fun `应包含三种消息块类型`() {
        val types = MessageBlockType.entries
        assertEquals(3, types.size)
    }

    @Test
    fun `MAIN_TEXT类型应存在`() {
        val type = MessageBlockType.MAIN_TEXT
        assertEquals("MAIN_TEXT", type.name)
    }

    @Test
    fun `THINKING类型应存在`() {
        val type = MessageBlockType.THINKING
        assertEquals("THINKING", type.name)
    }

    @Test
    fun `ERROR类型应存在`() {
        val type = MessageBlockType.ERROR
        assertEquals("ERROR", type.name)
    }

    @Test
    fun `valueOf应正确解析字符串`() {
        assertEquals(MessageBlockType.MAIN_TEXT, MessageBlockType.valueOf("MAIN_TEXT"))
        assertEquals(MessageBlockType.THINKING, MessageBlockType.valueOf("THINKING"))
        assertEquals(MessageBlockType.ERROR, MessageBlockType.valueOf("ERROR"))
    }

    @Test
    fun `ordinal应按定义顺序排列`() {
        assertEquals(0, MessageBlockType.MAIN_TEXT.ordinal)
        assertEquals(1, MessageBlockType.THINKING.ordinal)
        assertEquals(2, MessageBlockType.ERROR.ordinal)
    }
}

/**
 * MessageBlockStatus消息块状态枚举测试
 */
class MessageBlockStatusTest {

    @Test
    fun `应包含四种消息块状态`() {
        val statuses = MessageBlockStatus.entries
        assertEquals(4, statuses.size)
    }

    @Test
    fun `PENDING状态应存在`() {
        val status = MessageBlockStatus.PENDING
        assertEquals("PENDING", status.name)
    }

    @Test
    fun `STREAMING状态应存在`() {
        val status = MessageBlockStatus.STREAMING
        assertEquals("STREAMING", status.name)
    }

    @Test
    fun `SUCCESS状态应存在`() {
        val status = MessageBlockStatus.SUCCESS
        assertEquals("SUCCESS", status.name)
    }

    @Test
    fun `ERROR状态应存在`() {
        val status = MessageBlockStatus.ERROR
        assertEquals("ERROR", status.name)
    }

    @Test
    fun `valueOf应正确解析字符串`() {
        assertEquals(MessageBlockStatus.PENDING, MessageBlockStatus.valueOf("PENDING"))
        assertEquals(MessageBlockStatus.STREAMING, MessageBlockStatus.valueOf("STREAMING"))
        assertEquals(MessageBlockStatus.SUCCESS, MessageBlockStatus.valueOf("SUCCESS"))
        assertEquals(MessageBlockStatus.ERROR, MessageBlockStatus.valueOf("ERROR"))
    }
}
