package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.repository.PromptRepository
import com.empathy.ai.testutil.PromptTestDataFactory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PromptBuilder单元测试
 *
 * 测试覆盖：
 * - 系统指令构建
 * - 变量注入
 * - 联系人提示词优先级
 * - 上下文占位符
 */
class PromptBuilderTest {

    private lateinit var promptBuilder: PromptBuilder
    private lateinit var promptRepository: PromptRepository
    private lateinit var variableResolver: PromptVariableResolver

    @Before
    fun setup() {
        promptRepository = mockk()
        variableResolver = PromptVariableResolver()
        promptBuilder = PromptBuilder(promptRepository, variableResolver)
    }

    // ========== buildSystemInstruction() 测试 ==========

    @Test
    fun `buildSystemInstruction should include system header`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("用户提示词")
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            context = context
        )

        // Then
        assertTrue(result.contains("共情AI助手"))
    }

    @Test
    fun `buildSystemInstruction should include system footer`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("用户提示词")
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            context = context
        )

        // Then
        assertTrue(result.contains("JSON"))
    }

    @Test
    fun `buildSystemInstruction should include user prompt`() = runTest {
        // Given
        val userPrompt = "这是用户自定义的提示词"
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success(userPrompt)
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            context = context
        )

        // Then
        assertTrue(result.contains(userPrompt))
        assertTrue(result.contains("【用户自定义指令】"))
    }

    @Test
    fun `buildSystemInstruction should resolve variables in user prompt`() = runTest {
        // Given
        val userPrompt = "分析与{{contact_name}}的对话"
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success(userPrompt)
        val context = PromptTestDataFactory.createPromptContext(contactName = "小明")

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            context = context
        )

        // Then
        assertTrue(result.contains("分析与小明的对话"))
        assertFalse(result.contains("{{contact_name}}"))
    }

    @Test
    fun `buildSystemInstruction should include context placeholder`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("提示词")
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            context = context
        )

        // Then
        assertTrue(result.contains(PromptBuilder.CONTEXT_PLACEHOLDER))
        assertTrue(result.contains("【上下文数据】"))
    }

    @Test
    fun `buildSystemInstruction should include contact prompt when provided`() = runTest {
        // Given
        val contactPrompt = "这是联系人专属提示词"
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("全局提示词")
        coEvery { promptRepository.getContactPrompt("contact123") } returns Result.success(contactPrompt)
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            contactId = "contact123",
            context = context
        )

        // Then
        assertTrue(result.contains(contactPrompt))
        assertTrue(result.contains("【针对此联系人的特殊指令】"))
    }

    @Test
    fun `buildSystemInstruction should handle null contact prompt`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("全局提示词")
        coEvery { promptRepository.getContactPrompt("contact123") } returns Result.success(null)
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            contactId = "contact123",
            context = context
        )

        // Then
        assertFalse(result.contains("【针对此联系人的特殊指令】"))
    }

    @Test
    fun `buildSystemInstruction should handle repository failure gracefully`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.failure(Exception("Error"))
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            context = context
        )

        // Then - 应该仍然包含系统头部和尾部
        assertTrue(result.contains("共情AI助手"))
        assertTrue(result.contains("JSON"))
    }

    // ========== buildSimpleInstruction() 测试 ==========

    @Test
    fun `buildSimpleInstruction should not include context placeholder`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("提示词")
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSimpleInstruction(
            scene = PromptScene.ANALYZE,
            context = context
        )

        // Then
        assertFalse(result.contains(PromptBuilder.CONTEXT_PLACEHOLDER))
        assertFalse(result.contains("【上下文数据】"))
    }

    @Test
    fun `buildSimpleInstruction should include system header and footer`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("提示词")
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSimpleInstruction(
            scene = PromptScene.ANALYZE,
            context = context
        )

        // Then
        assertTrue(result.contains("共情AI助手"))
        assertTrue(result.contains("JSON"))
    }

    // ========== injectContextData() 测试 ==========

    @Test
    fun `injectContextData should replace placeholder with actual data`() {
        // Given
        val instruction = "Header\n${PromptBuilder.CONTEXT_PLACEHOLDER}\nFooter"
        val contextData = "这是实际的上下文数据"

        // When
        val result = promptBuilder.injectContextData(instruction, contextData)

        // Then
        assertTrue(result.contains(contextData))
        assertFalse(result.contains(PromptBuilder.CONTEXT_PLACEHOLDER))
        assertTrue(result.contains("Header"))
        assertTrue(result.contains("Footer"))
    }

    @Test
    fun `injectContextData should handle empty context data`() {
        // Given
        val instruction = "Header\n${PromptBuilder.CONTEXT_PLACEHOLDER}\nFooter"
        val contextData = ""

        // When
        val result = promptBuilder.injectContextData(instruction, contextData)

        // Then
        assertFalse(result.contains(PromptBuilder.CONTEXT_PLACEHOLDER))
        assertTrue(result.contains("Header"))
        assertTrue(result.contains("Footer"))
    }

    @Test
    fun `injectContextData should handle instruction without placeholder`() {
        // Given
        val instruction = "Header\nFooter"
        val contextData = "上下文数据"

        // When
        val result = promptBuilder.injectContextData(instruction, contextData)

        // Then
        // 没有占位符，原样返回
        assertFalse(result.contains(contextData))
        assertEquals("Header\nFooter", result)
    }

    // ========== 合并顺序测试 ==========

    @Test
    fun `buildSystemInstruction should follow correct merge order`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("用户指令")
        coEvery { promptRepository.getContactPrompt("contact123") } returns Result.success("联系人指令")
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            contactId = "contact123",
            context = context
        )

        // Then - 验证顺序：Header -> 用户指令 -> 联系人指令 -> 上下文 -> Footer
        val headerIndex = result.indexOf("共情AI助手")
        val userIndex = result.indexOf("用户指令")
        val contactIndex = result.indexOf("联系人指令")
        val contextIndex = result.indexOf(PromptBuilder.CONTEXT_PLACEHOLDER)
        val footerIndex = result.indexOf("JSON")

        assertTrue(headerIndex < userIndex)
        assertTrue(userIndex < contactIndex)
        assertTrue(contactIndex < contextIndex)
        assertTrue(contextIndex < footerIndex)
    }

    // ========== 不同场景测试 ==========

    @Test
    fun `buildSystemInstruction should work for CHECK scene`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.CHECK) } returns Result.success("检查提示词")
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.CHECK,
            context = context
        )

        // Then
        assertTrue(result.contains("检查提示词"))
    }

    @Test
    fun `buildSystemInstruction should work for SUMMARY scene`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.SUMMARY) } returns Result.success("总结提示词")
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.SUMMARY,
            context = context
        )

        // Then
        assertTrue(result.contains("总结提示词"))
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
