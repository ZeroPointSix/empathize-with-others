package com.empathy.ai.domain.util

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * DebugLogger 单元测试
 *
 * 测试调试日志工具类的功能：
 * - Debug 模式下完整输出
 * - Release 模式下截取输出
 * - 超长文本分段输出
 *
 * @see SR-00001 模型列表自动获取与调试日志优化
 */
class DebugLoggerTest {

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== 短文本测试 ====================

    @Test
    fun `logFullPrompt should output complete content for short text in debug mode`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "This is a short test content"

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = true)

        // Then
        verify { android.util.Log.d(tag, match { it.contains("开始") && it.contains("总长度: ${content.length}") }) }
        verify { android.util.Log.d(tag, content) }
        verify { android.util.Log.d(tag, match { it.contains("结束") }) }
    }

    @Test
    fun `logFullPrompt should truncate content in release mode`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "A".repeat(1000) // 1000 字符

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = false)

        // Then
        verify { android.util.Log.d(tag, match { it.contains("前500字符") && it.length < 600 }) }
    }

    // ==================== 长文本分段测试 ====================

    @Test
    fun `logFullPrompt should split long content into segments in debug mode`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "A".repeat(10000) // 10000 字符，应分为 3 段

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = true)

        // Then
        verify { android.util.Log.d(tag, match { it.contains("开始") }) }
        verify { android.util.Log.d(tag, match { it.contains("第 1/3 段") }) }
        verify { android.util.Log.d(tag, match { it.contains("第 2/3 段") }) }
        verify { android.util.Log.d(tag, match { it.contains("第 3/3 段") }) }
        verify { android.util.Log.d(tag, match { it.contains("结束") }) }
    }

    @Test
    fun `logFullPrompt should handle exactly MAX_LOG_LENGTH content`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "A".repeat(DebugLogger.MAX_LOG_LENGTH) // 刚好 4000 字符

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = true)

        // Then
        // 刚好 4000 字符，不需要分段
        verify { android.util.Log.d(tag, content) }
        verify(exactly = 0) { android.util.Log.d(tag, match { it.contains("第 1/") }) }
    }

    @Test
    fun `logFullPrompt should split content exceeding MAX_LOG_LENGTH by one`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "A".repeat(DebugLogger.MAX_LOG_LENGTH + 1) // 4001 字符

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = true)

        // Then
        // 4001 字符，需要分为 2 段
        verify { android.util.Log.d(tag, match { it.contains("第 1/2 段") }) }
        verify { android.util.Log.d(tag, match { it.contains("第 2/2 段") }) }
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `logFullPrompt should handle empty content`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = ""

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = true)

        // Then
        verify { android.util.Log.d(tag, match { it.contains("总长度: 0") }) }
        verify { android.util.Log.d(tag, "") }
    }

    @Test
    fun `logFullPrompt should handle content with special characters`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "测试中文内容\n换行符\t制表符\r回车符"

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = true)

        // Then
        verify { android.util.Log.d(tag, content) }
    }

    @Test
    fun `logFullPrompt should handle content with unicode characters`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "🎉 Emoji 测试 🚀 日本語 한국어"

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = true)

        // Then
        verify { android.util.Log.d(tag, content) }
    }

    // ==================== Release 模式测试 ====================

    @Test
    fun `logFullPrompt should not output full content in release mode`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "A".repeat(10000)

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = false)

        // Then
        // Release 模式不应输出完整内容
        verify(exactly = 0) { android.util.Log.d(tag, match { it.contains("开始") }) }
        verify(exactly = 0) { android.util.Log.d(tag, match { it.contains("第 1/") }) }
        // 只应输出截取后的内容
        verify { android.util.Log.d(tag, match { it.contains("前500字符") }) }
    }

    @Test
    fun `logFullPrompt should truncate to exactly 500 characters in release mode`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "A".repeat(1000)

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = false)

        // Then
        verify { android.util.Log.d(tag, match { 
            it.contains("前500字符") && it.contains("A".repeat(500)) && !it.contains("A".repeat(501))
        }) }
    }

    // ==================== 标签格式测试 ====================

    @Test
    fun `logFullPrompt should include correct label in output`() {
        // Given
        val tag = "AiRepositoryImpl"
        val label = "PromptContext"
        val content = "Test content"

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = true)

        // Then
        verify { android.util.Log.d(tag, match { it.contains("PromptContext 开始") }) }
        verify { android.util.Log.d(tag, match { it.contains("PromptContext 结束") }) }
    }

    @Test
    fun `logFullPrompt should include total length in header`() {
        // Given
        val tag = "TestTag"
        val label = "TestLabel"
        val content = "A".repeat(12345)

        // When
        DebugLogger.logFullPrompt(tag, label, content, isDebugMode = true)

        // Then
        verify { android.util.Log.d(tag, match { it.contains("总长度: 12345") }) }
    }
}
