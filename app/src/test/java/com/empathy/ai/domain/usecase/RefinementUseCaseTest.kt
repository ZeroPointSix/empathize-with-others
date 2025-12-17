package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.AiResult
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.FloatingWindowError
import com.empathy.ai.domain.model.PolishResult
import com.empathy.ai.domain.model.RefinementRequest
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RefinementUseCase 单元测试
 *
 * 测试微调重新生成用例的各种场景
 *
 * 【重要设计说明】
 * RefinementUseCase 直接调用 AiRepository，而不是原始的 UseCase。
 * 原因：原始 UseCase 会将用户输入保存到对话历史记录中。如果重新生成时
 * 再次调用这些 UseCase，会导致同一条输入被重复保存到历史记录中。
 */
class RefinementUseCaseTest {

    private lateinit var useCase: RefinementUseCase
    private lateinit var aiRepository: AiRepository
    private lateinit var aiProviderRepository: AiProviderRepository

    private val testContactId = "contact_123"

    private val testProvider = AiProvider(
        id = "provider_1",
        name = "TestProvider",
        baseUrl = "https://api.test.com",
        apiKey = "test_key",
        models = listOf(AiModel(id = "gpt-4", displayName = "GPT-4")),
        defaultModelId = "gpt-4",
        isDefault = true
    )

    private val testAnalysisResult = AnalysisResult(
        replySuggestion = "建议的回复内容",
        strategyAnalysis = "对话分析摘要",
        riskLevel = RiskLevel.SAFE
    )

    private val testPolishResult = PolishResult(
        polishedText = "润色后的文本",
        hasRisk = false,
        riskWarning = null
    )

    private val testReplyResult = ReplyResult(
        suggestedReply = "建议的回复内容",
        strategyNote = "策略说明"
    )

    @Before
    fun setup() {
        aiRepository = mockk()
        aiProviderRepository = mockk()

        useCase = RefinementUseCase(
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository
        )
    }

    // ==================== 直接重新生成测试（不保存历史记录） ====================

    @Test
    fun `when regenerate ANALYZE without instruction, should call aiRepository refineAnalysis`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.ANALYZE,
            originalInput = "原始对话内容",
            lastAiResponse = "上次分析结果",
            refinementInstruction = null,
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refineAnalysis(any(), any()) } returns Result.success(testAnalysisResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val aiResult = result.getOrNull()
        assertTrue(aiResult is AiResult.Analysis)
        assertEquals(testAnalysisResult, (aiResult as AiResult.Analysis).result)
        // 验证直接调用 AiRepository，而不是原始 UseCase（避免重复保存历史）
        coVerify { aiRepository.refineAnalysis(testProvider, any()) }
    }

    @Test
    fun `when regenerate POLISH without instruction, should call aiRepository refinePolish`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.POLISH,
            originalInput = "原始草稿",
            lastAiResponse = "上次润色结果",
            refinementInstruction = null,
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refinePolish(any(), any()) } returns Result.success(testPolishResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val aiResult = result.getOrNull()
        assertTrue(aiResult is AiResult.Polish)
        assertEquals(testPolishResult, (aiResult as AiResult.Polish).result)
        coVerify { aiRepository.refinePolish(testProvider, any()) }
    }

    @Test
    fun `when regenerate REPLY without instruction, should call aiRepository refineReply`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.REPLY,
            originalInput = "对方消息",
            lastAiResponse = "上次回复建议",
            refinementInstruction = null,
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refineReply(any(), any()) } returns Result.success(testReplyResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val aiResult = result.getOrNull()
        assertTrue(aiResult is AiResult.Reply)
        assertEquals(testReplyResult, (aiResult as AiResult.Reply).result)
        coVerify { aiRepository.refineReply(testProvider, any()) }
    }

    // ==================== 带微调指令测试 ====================

    @Test
    fun `when refine ANALYZE with instruction, should call aiRepository refineAnalysis`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.ANALYZE,
            originalInput = "原始对话内容",
            lastAiResponse = "上次分析结果",
            refinementInstruction = "更详细一些",
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refineAnalysis(any(), any()) } returns Result.success(testAnalysisResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val aiResult = result.getOrNull()
        assertTrue(aiResult is AiResult.Analysis)
        coVerify { aiRepository.refineAnalysis(testProvider, any()) }
    }

    @Test
    fun `when refine POLISH with instruction, should call aiRepository refinePolish`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.POLISH,
            originalInput = "原始草稿",
            lastAiResponse = "上次润色结果",
            refinementInstruction = "更正式一些",
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refinePolish(any(), any()) } returns Result.success(testPolishResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val aiResult = result.getOrNull()
        assertTrue(aiResult is AiResult.Polish)
        coVerify { aiRepository.refinePolish(testProvider, any()) }
    }

    @Test
    fun `when refine REPLY with instruction, should call aiRepository refineReply`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.REPLY,
            originalInput = "对方消息",
            lastAiResponse = "上次回复建议",
            refinementInstruction = "更幽默一些",
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refineReply(any(), any()) } returns Result.success(testReplyResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val aiResult = result.getOrNull()
        assertTrue(aiResult is AiResult.Reply)
        coVerify { aiRepository.refineReply(testProvider, any()) }
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `when no provider configured for refinement, should return NoProviderConfigured error`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.ANALYZE,
            originalInput = "原始内容",
            lastAiResponse = "上次结果",
            refinementInstruction = "微调指令",
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(null)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FloatingWindowError.NoProviderConfigured)
    }

    @Test
    fun `when no provider configured for regenerate, should return NoProviderConfigured error`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.ANALYZE,
            originalInput = "原始内容",
            lastAiResponse = "上次结果",
            refinementInstruction = null,  // 无微调指令，走 regenerateDirectly
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(null)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FloatingWindowError.NoProviderConfigured)
    }

    @Test
    fun `when exception thrown, should return FloatingWindowError`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.ANALYZE,
            originalInput = "原始内容",
            lastAiResponse = "上次结果",
            refinementInstruction = "微调指令",
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } throws RuntimeException("网络超时")

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FloatingWindowError)
    }

    @Test
    fun `when AI call fails, should return failure`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.POLISH,
            originalInput = "原始草稿",
            lastAiResponse = "上次结果",
            refinementInstruction = "微调指令",
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refinePolish(any(), any()) } returns Result.failure(
            RuntimeException("AI调用失败")
        )

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `when instruction is empty string, should treat as no instruction and call refine directly`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.POLISH,
            originalInput = "原始草稿",
            lastAiResponse = "上次结果",
            refinementInstruction = "",
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refinePolish(any(), any()) } returns Result.success(testPolishResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        coVerify { aiRepository.refinePolish(testProvider, any()) }
    }

    @Test
    fun `when instruction is blank, should treat as no instruction and call refine directly`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.REPLY,
            originalInput = "对方消息",
            lastAiResponse = "上次结果",
            refinementInstruction = "   ",
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refineReply(any(), any()) } returns Result.success(testReplyResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        coVerify { aiRepository.refineReply(testProvider, any()) }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `when CHECK action type without instruction, should use POLISH as fallback`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.CHECK,
            originalInput = "原始草稿",
            lastAiResponse = "上次结果",
            refinementInstruction = null,
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refinePolish(any(), any()) } returns Result.success(testPolishResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val aiResult = result.getOrNull()
        assertTrue(aiResult is AiResult.Polish)
        coVerify { aiRepository.refinePolish(testProvider, any()) }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `when CHECK action type with instruction, should use POLISH as fallback`() = runTest {
        // Given
        val request = RefinementRequest(
            originalTask = ActionType.CHECK,
            originalInput = "原始草稿",
            lastAiResponse = "上次结果",
            refinementInstruction = "更正式一些",
            contactId = testContactId
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { aiRepository.refinePolish(any(), any()) } returns Result.success(testPolishResult)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val aiResult = result.getOrNull()
        assertTrue(aiResult is AiResult.Polish)
        coVerify { aiRepository.refinePolish(testProvider, any()) }
    }
}
