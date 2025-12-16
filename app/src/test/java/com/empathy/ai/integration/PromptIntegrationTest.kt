package com.empathy.ai.integration

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ScenePromptConfig
import com.empathy.ai.domain.repository.PromptRepository
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.PromptVariableResolver
import com.empathy.ai.domain.util.SystemPrompts
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 提示词系统集成测试
 *
 * 验证PromptBuilder与PromptRepository的集成
 */
class PromptIntegrationTest {

    private lateinit var promptRepository: PromptRepository
    private lateinit var variableResolver: PromptVariableResolver
    private lateinit var promptBuilder: PromptBuilder

    @Before
    fun setup() {
        promptRepository = mockk()
        variableResolver = PromptVariableResolver()
        promptBuilder = PromptBuilder(promptRepository, variableResolver)
    }

    @Test
    fun `buildSystemInstruction should include user custom prompt`() = runTest {
        // Given
        val customPrompt = "请特别注意对方的情绪变化"
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns 
            Result.success(customPrompt)
        coEvery { promptRepository.getContactPrompt(any()) } returns 
            Result.success(null)

        val context = PromptContext(
            contactName = "小明",
            relationshipStatus = "朋友",
            factsCount = 5
        )

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            contactId = "contact_1",
            context = context
        )

        // Then
        assertTrue("应包含用户自定义指令", result.contains("【用户自定义指令】"))
        assertTrue("应包含自定义提示词内容", result.contains(customPrompt))
        assertTrue("应包含系统Header", result.contains(SystemPrompts.getHeader(PromptScene.ANALYZE)))
        assertTrue("应包含系统Footer", result.contains(SystemPrompts.getFooter(PromptScene.ANALYZE)))
    }

    @Test
    fun `buildSystemInstruction should include contact specific prompt`() = runTest {
        // Given
        val contactPrompt = "这个人比较敏感，说话要委婉"
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns 
            Result.success("")
        coEvery { promptRepository.getContactPrompt("contact_1") } returns 
            Result.success(contactPrompt)

        val context = PromptContext(contactName = "小红")

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            contactId = "contact_1",
            context = context
        )

        // Then
        assertTrue("应包含联系人专属指令", result.contains("【针对此联系人的特殊指令】"))
        assertTrue("应包含联系人提示词内容", result.contains(contactPrompt))
    }

    @Test
    fun `buildSystemInstruction should resolve variables`() = runTest {
        // Given
        val customPrompt = "分析与{{contact_name}}的对话，当前关系：{{relationship_status}}"
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns 
            Result.success(customPrompt)
        coEvery { promptRepository.getContactPrompt(any()) } returns 
            Result.success(null)

        val context = PromptContext(
            contactName = "小明",
            relationshipStatus = "好友"
        )

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            contactId = "contact_1",
            context = context
        )

        // Then
        assertTrue("应替换contact_name变量", result.contains("小明"))
        assertTrue("应替换relationship_status变量", result.contains("好友"))
        assertFalse("不应包含未替换的变量", result.contains("{{contact_name}}"))
    }

    @Test
    fun `buildSystemInstruction should handle repository errors gracefully`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns 
            Result.failure(Exception("存储错误"))
        coEvery { promptRepository.getContactPrompt(any()) } returns 
            Result.failure(Exception("数据库错误"))

        val context = PromptContext(contactName = "测试")

        // When
        val result = promptBuilder.buildSystemInstruction(
            scene = PromptScene.ANALYZE,
            contactId = "contact_1",
            context = context
        )

        // Then - 应该正常返回，只包含系统指令
        assertTrue("应包含系统Header", result.contains(SystemPrompts.getHeader(PromptScene.ANALYZE)))
        assertTrue("应包含系统Footer", result.contains(SystemPrompts.getFooter(PromptScene.ANALYZE)))
    }

    @Test
    fun `injectContextData should replace placeholder`() {
        // Given
        val instruction = "系统指令\n【上下文数据】\n${PromptBuilder.CONTEXT_PLACEHOLDER}\n结束"
        val contextData = "这是实际的上下文数据"

        // When
        val result = promptBuilder.injectContextData(instruction, contextData)

        // Then
        assertTrue("应包含上下文数据", result.contains(contextData))
        assertFalse("不应包含占位符", result.contains(PromptBuilder.CONTEXT_PLACEHOLDER))
    }

    @Test
    fun `PromptContext fromContact should extract tags correctly`() {
        // Given
        val profile = ContactProfile(
            id = "test_id",
            name = "测试联系人",
            targetGoal = "成为好朋友",
            facts = listOf(
                Fact("雷区", "不要提前任", System.currentTimeMillis(), FactSource.USER_INPUT),
                Fact("策略", "多聊共同爱好", System.currentTimeMillis(), FactSource.USER_INPUT),
                Fact("生日", "1月1日", System.currentTimeMillis(), FactSource.USER_INPUT)
            ),
            relationshipScore = 60
        )

        // When
        val context = PromptContext.fromContact(profile)

        // Then
        assertTrue("应包含雷区标签", context.riskTags.contains("不要提前任"))
        assertTrue("应包含策略标签", context.strategyTags.contains("多聊共同爱好"))
        assertTrue("factsCount应为3", context.factsCount == 3)
    }

    @Test
    fun `buildSimpleInstruction should not include context placeholder`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.CHECK) } returns 
            Result.success("")
        coEvery { promptRepository.getContactPrompt(any()) } returns 
            Result.success(null)

        val context = PromptContext(contactName = "测试")

        // When
        val result = promptBuilder.buildSimpleInstruction(
            scene = PromptScene.CHECK,
            contactId = "contact_1",
            context = context
        )

        // Then
        assertFalse("不应包含上下文占位符", result.contains(PromptBuilder.CONTEXT_PLACEHOLDER))
        assertFalse("不应包含上下文数据标题", result.contains("【上下文数据】"))
    }

    @Test
    fun `different scenes should have different system prompts`() = runTest {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("")
        coEvery { promptRepository.getContactPrompt(any()) } returns Result.success(null)
        val context = PromptContext()

        // When
        val analyzeResult = promptBuilder.buildSimpleInstruction(
            scene = PromptScene.ANALYZE,
            context = context
        )
        val checkResult = promptBuilder.buildSimpleInstruction(
            scene = PromptScene.CHECK,
            context = context
        )
        val summaryResult = promptBuilder.buildSimpleInstruction(
            scene = PromptScene.SUMMARY,
            context = context
        )

        // Then
        assertTrue("ANALYZE场景应有对应Header", 
            analyzeResult.contains(SystemPrompts.getHeader(PromptScene.ANALYZE)))
        assertTrue("CHECK场景应有对应Header", 
            checkResult.contains(SystemPrompts.getHeader(PromptScene.CHECK)))
        assertTrue("SUMMARY场景应有对应Header", 
            summaryResult.contains(SystemPrompts.getHeader(PromptScene.SUMMARY)))
    }
}
