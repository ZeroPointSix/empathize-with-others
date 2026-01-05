package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * AiStreamChunk流式数据块模型测试
 *
 * 测试流式响应的各种数据块类型。
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
class AiStreamChunkTest {

    @Test
    fun `Started应为单例对象`() {
        val started1 = AiStreamChunk.Started
        val started2 = AiStreamChunk.Started
        assertTrue(started1 === started2)
    }

    @Test
    fun `TextDelta应正确存储文本内容`() {
        val delta = AiStreamChunk.TextDelta("Hello")
        assertEquals("Hello", delta.text)
    }

    @Test
    fun `ThinkingDelta应正确存储思考内容和耗时`() {
        val delta = AiStreamChunk.ThinkingDelta("Let me think...", 500L)
        assertEquals("Let me think...", delta.text)
        assertEquals(500L, delta.thinkingMs)
    }

    @Test
    fun `ThinkingDelta耗时可为空`() {
        val delta = AiStreamChunk.ThinkingDelta("Thinking...")
        assertEquals("Thinking...", delta.text)
        assertNull(delta.thinkingMs)
    }

    @Test
    fun `ThinkingComplete应正确存储完整思考内容和总耗时`() {
        val complete = AiStreamChunk.ThinkingComplete("Full thinking process", 2000L)
        assertEquals("Full thinking process", complete.fullThinking)
        assertEquals(2000L, complete.totalMs)
    }

    @Test
    fun `Complete应正确存储完整文本和Token使用统计`() {
        val usage = TokenUsage(100, 50, 150)
        val complete = AiStreamChunk.Complete("Full response", usage)
        assertEquals("Full response", complete.fullText)
        assertEquals(usage, complete.usage)
    }

    @Test
    fun `Complete的Token使用统计可为空`() {
        val complete = AiStreamChunk.Complete("Response", null)
        assertEquals("Response", complete.fullText)
        assertNull(complete.usage)
    }

    @Test
    fun `Error应正确存储异常信息`() {
        val exception = IOException("Network error")
        val error = AiStreamChunk.Error(exception)
        assertEquals(exception, error.error)
        assertEquals("Network error", error.error.message)
    }

    @Test
    fun `TokenUsage应正确计算总Token数`() {
        val usage = TokenUsage(100, 50, 150)
        assertEquals(100, usage.promptTokens)
        assertEquals(50, usage.completionTokens)
        assertEquals(150, usage.totalTokens)
    }

    @Test
    fun `所有AiStreamChunk子类应为密封类成员`() {
        val chunks: List<AiStreamChunk> = listOf(
            AiStreamChunk.Started,
            AiStreamChunk.TextDelta("text"),
            AiStreamChunk.ThinkingDelta("thinking"),
            AiStreamChunk.ThinkingComplete("complete", 1000L),
            AiStreamChunk.Complete("full", null),
            AiStreamChunk.Error(Exception("error"))
        )

        chunks.forEach { chunk ->
            when (chunk) {
                is AiStreamChunk.Started -> assertTrue(true)
                is AiStreamChunk.TextDelta -> assertTrue(true)
                is AiStreamChunk.ThinkingDelta -> assertTrue(true)
                is AiStreamChunk.ThinkingComplete -> assertTrue(true)
                is AiStreamChunk.Complete -> assertTrue(true)
                is AiStreamChunk.Error -> assertTrue(true)
            }
        }
    }
}
