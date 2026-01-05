package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * StreamingState流式状态测试
 *
 * 测试ViewModel和UI之间的流式状态传递。
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
class StreamingStateTest {

    @Test
    fun `Started应正确存储消息ID`() {
        val state = StreamingState.Started("msg-123")
        assertEquals("msg-123", state.messageId)
    }

    @Test
    fun `ThinkingUpdate应正确存储思考内容和耗时`() {
        val state = StreamingState.ThinkingUpdate("Thinking...", 1500L)
        assertEquals("Thinking...", state.content)
        assertEquals(1500L, state.elapsedMs)
    }

    @Test
    fun `TextUpdate应正确存储文本内容`() {
        val state = StreamingState.TextUpdate("Hello World")
        assertEquals("Hello World", state.content)
    }

    @Test
    fun `Completed应正确存储完整文本和Token使用统计`() {
        val usage = TokenUsage(100, 50, 150)
        val state = StreamingState.Completed("Full response", usage)
        assertEquals("Full response", state.fullText)
        assertEquals(usage, state.usage)
    }

    @Test
    fun `Completed的Token使用统计可为空`() {
        val state = StreamingState.Completed("Response", null)
        assertEquals("Response", state.fullText)
        assertNull(state.usage)
    }

    @Test
    fun `Error应正确存储异常信息`() {
        val exception = RuntimeException("Test error")
        val state = StreamingState.Error(exception)
        assertEquals(exception, state.error)
        assertEquals("Test error", state.error.message)
    }

    @Test
    fun `所有StreamingState子类应为密封类成员`() {
        val states: List<StreamingState> = listOf(
            StreamingState.Started("msg-1"),
            StreamingState.ThinkingUpdate("thinking", 1000L),
            StreamingState.TextUpdate("text"),
            StreamingState.Completed("full", null),
            StreamingState.Error(Exception("error"))
        )

        states.forEach { state ->
            when (state) {
                is StreamingState.Started -> assertTrue(true)
                is StreamingState.ThinkingUpdate -> assertTrue(true)
                is StreamingState.TextUpdate -> assertTrue(true)
                is StreamingState.Completed -> assertTrue(true)
                is StreamingState.Error -> assertTrue(true)
            }
        }
    }

    @Test
    fun `ThinkingUpdate的elapsedMs应为非负数`() {
        val state = StreamingState.ThinkingUpdate("content", 0L)
        assertTrue(state.elapsedMs >= 0)
    }

    @Test
    fun `TextUpdate应支持空字符串`() {
        val state = StreamingState.TextUpdate("")
        assertEquals("", state.content)
    }

    @Test
    fun `Completed应支持空字符串`() {
        val state = StreamingState.Completed("", null)
        assertEquals("", state.fullText)
    }
}
