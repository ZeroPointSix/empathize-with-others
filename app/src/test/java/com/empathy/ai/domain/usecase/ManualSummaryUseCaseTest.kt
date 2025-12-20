package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ConflictResolution
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.model.GenerationSource
import com.empathy.ai.domain.model.SummaryError
import com.empathy.ai.domain.model.SummaryType
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.util.ContextBuilder
import com.empathy.ai.domain.util.DateRangeValidator
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.SummaryConflictChecker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ManualSummaryUseCase 单元测试
 */
class ManualSummaryUseCaseTest {

    private lateinit var conversationRepository: ConversationRepository
    private lateinit var dailySummaryRepository: DailySummaryRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var aiProviderRepository: AiProviderRepository
    private lateinit var dateRangeValidator: DateRangeValidator
    private lateinit var conflictChecker: SummaryConflictChecker
    private lateinit var contextBuilder: ContextBuilder
    private lateinit var promptBuilder: PromptBuilder
    private lateinit var moshi: Moshi
    private lateinit var useCase: ManualSummaryUseCase

    private val testContactId = "contact-123"
    private val testStartDate = "2025-12-01"
    private val testEndDate = "2025-12-07"

    @Before
    fun setup() {
        conversationRepository = mockk()
        dailySummaryRepository = mockk()
        contactRepository = mockk()
        brainTagRepository = mockk()
        aiRepository = mockk()
        aiProviderRepository = mockk()
        dateRangeValidator = mockk()
        conflictChecker = mockk()
        contextBuilder = mockk()
        promptBuilder = mockk()
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        useCase = ManualSummaryUseCase(
            conversationRepository = conversationRepository,
            dailySummaryRepository = dailySummaryRepository,
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository,
            dateRangeValidator = dateRangeValidator,
            conflictChecker = conflictChecker,
            contextBuilder = contextBuilder,
            promptBuilder = promptBuilder,
            moshi = moshi,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    // ==================== 成功场景测试 ====================

    @Test
    fun `成功执行手动总结返回SummaryResult`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        setupSuccessScenario()

        // When
        val result = useCase(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        val summaryResult = result.getOrNull()!!
        assertEquals(testContactId, summaryResult.summary.contactId)
        assertEquals(SummaryType.CUSTOM_RANGE, summaryResult.summary.summaryType)
        assertEquals(GenerationSource.MANUAL, summaryResult.summary.generationSource)
        assertEquals(testStartDate, summaryResult.summary.startDate)
        assertEquals(testEndDate, summaryResult.summary.endDate)
    }

    @Test
    fun `进度回调被正确调用`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        setupSuccessScenario()
        val progressSteps = mutableListOf<Pair<Float, String>>()

        // When
        useCase(testContactId, dateRange) { progress, step ->
            progressSteps.add(progress to step)
        }

        // Then
        assertTrue(progressSteps.isNotEmpty())
        assertTrue(progressSteps.any { it.first == 1f })
    }

    // ==================== 验证失败测试 ====================

    @Test
    fun `日期范围验证失败返回错误`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery { dateRangeValidator.validate(dateRange, testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Invalid("开始日期不能晚于结束日期"))

        // When
        val result = useCase(testContactId, dateRange)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is SummaryException)
    }

    @Test
    fun `联系人不存在返回错误`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery { dateRangeValidator.validate(dateRange, testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)

        // When
        val result = useCase(testContactId, dateRange)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is SummaryException)
    }

    // ==================== 无对话数据测试 ====================

    @Test
    fun `无对话数据返回NoConversations错误`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery { dateRangeValidator.validate(dateRange, testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { contactRepository.getProfile(testContactId) } returns
            Result.success(createTestProfile())
        coEvery { conversationRepository.getLogsByContactAndDate(testContactId, any()) } returns
            Result.success(emptyList())

        // When
        val result = useCase(testContactId, dateRange)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as SummaryException
        assertEquals(SummaryError.NoConversations, exception.error)
    }

    // ==================== 冲突处理测试 ====================

    @Test
    fun `覆盖模式会删除已有总结`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        setupSuccessScenario()
        coEvery {
            dailySummaryRepository.deleteSummariesInRange(testContactId, testStartDate, testEndDate)
        } returns Result.success(3)

        // When
        useCase(testContactId, dateRange, ConflictResolution.OVERWRITE)

        // Then
        coVerify {
            dailySummaryRepository.deleteSummariesInRange(testContactId, testStartDate, testEndDate)
        }
    }

    @Test
    fun `仅补充模式会过滤已有总结的日期`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery { dateRangeValidator.validate(dateRange, testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { contactRepository.getProfile(testContactId) } returns
            Result.success(createTestProfile())
        coEvery {
            dailySummaryRepository.getSummarizedDatesInRange(testContactId, testStartDate, testEndDate)
        } returns Result.success(listOf("2025-12-03", "2025-12-05"))
        
        // 只有非已总结日期会被查询
        coEvery { conversationRepository.getLogsByContactAndDate(testContactId, "2025-12-01") } returns
            Result.success(listOf(createTestConversation()))
        coEvery { conversationRepository.getLogsByContactAndDate(testContactId, "2025-12-02") } returns
            Result.success(listOf(createTestConversation()))
        coEvery { conversationRepository.getLogsByContactAndDate(testContactId, "2025-12-04") } returns
            Result.success(listOf(createTestConversation()))
        coEvery { conversationRepository.getLogsByContactAndDate(testContactId, "2025-12-06") } returns
            Result.success(listOf(createTestConversation()))
        coEvery { conversationRepository.getLogsByContactAndDate(testContactId, "2025-12-07") } returns
            Result.success(listOf(createTestConversation()))
        
        setupAiMocks()
        coEvery { dailySummaryRepository.saveSummary(any()) } returns Result.success(1L)
        coEvery { conversationRepository.markAsSummarized(any()) } returns Result.success(Unit)
        // Mock data sync operations
        coEvery { contactRepository.updateFacts(any(), any()) } returns Result.success(Unit)
        coEvery { contactRepository.updateRelationshipScore(any(), any()) } returns Result.success(Unit)
        coEvery { brainTagRepository.saveTag(any()) } returns Result.success(1L)

        // When
        val result = useCase(testContactId, dateRange, ConflictResolution.FILL_GAPS)

        // Then
        assertTrue(result.isSuccess)
        // 验证已总结的日期没有被查询
        coVerify(exactly = 0) {
            conversationRepository.getLogsByContactAndDate(testContactId, "2025-12-03")
        }
        coVerify(exactly = 0) {
            conversationRepository.getLogsByContactAndDate(testContactId, "2025-12-05")
        }
    }

    // ==================== AI降级测试 ====================

    @Test
    fun `AI失败时使用本地降级方案`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery { dateRangeValidator.validate(dateRange, testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { contactRepository.getProfile(testContactId) } returns
            Result.success(createTestProfile())
        coEvery { conversationRepository.getLogsByContactAndDate(testContactId, any()) } returns
            Result.success(listOf(createTestConversation()))
        coEvery { aiProviderRepository.getDefaultProvider() } returns
            Result.failure(RuntimeException("No provider"))
        coEvery { dailySummaryRepository.saveSummary(any()) } returns Result.success(1L)
        coEvery { conversationRepository.markAsSummarized(any()) } returns Result.success(Unit)
        // Mock data sync operations for fallback scenario
        coEvery { contactRepository.updateRelationshipScore(any(), any()) } returns Result.success(Unit)

        // When
        val result = useCase(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        val summaryResult = result.getOrNull()!!
        assertTrue(summaryResult.summary.content.contains("本地统计"))
    }

    // ==================== 辅助方法 ====================

    private fun setupSuccessScenario() {
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { contactRepository.getProfile(testContactId) } returns
            Result.success(createTestProfile())
        coEvery { conversationRepository.getLogsByContactAndDate(testContactId, any()) } returns
            Result.success(listOf(createTestConversation()))
        setupAiMocks()
        coEvery { dailySummaryRepository.saveSummary(any()) } returns Result.success(1L)
        coEvery { conversationRepository.markAsSummarized(any()) } returns Result.success(Unit)
        // Mock data sync operations
        coEvery { contactRepository.updateFacts(any(), any()) } returns Result.success(Unit)
        coEvery { contactRepository.updateRelationshipScore(any(), any()) } returns Result.success(Unit)
        coEvery { brainTagRepository.saveTag(any()) } returns Result.success(1L)
    }

    private fun setupAiMocks() {
        val testModel = AiModel(
            id = "test-model",
            displayName = "Test Model"
        )
        val mockProvider = AiProvider(
            id = "provider-1",
            name = "Test Provider",
            baseUrl = "https://api.test.com",
            apiKey = "test-key",
            models = listOf(testModel),
            defaultModelId = "test-model",
            isDefault = true
        )
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(mockProvider)
        coEvery { contextBuilder.buildSummaryPrompt(any(), any()) } returns "Test prompt"
        coEvery { promptBuilder.buildSimpleInstruction(any(), any(), any()) } returns "Test instruction"
        
        val aiResponse = """
            {
                "summary": "Test summary",
                "keyEvents": [],
                "newFacts": [],
                "relationshipScoreChange": 1,
                "relationshipTrend": "STABLE"
            }
        """.trimIndent()
        coEvery { aiRepository.generateText(any(), any(), any()) } returns Result.success(aiResponse)
    }

    private fun createTestProfile() = ContactProfile(
        id = testContactId,
        name = "Test Contact",
        targetGoal = "测试目标",
        relationshipScore = 50,
        facts = emptyList(),
        lastInteractionDate = null
    )

    private fun createTestConversation() = ConversationLog(
        id = 1L,
        contactId = testContactId,
        userInput = "Hello",
        aiResponse = "Hi there",
        timestamp = System.currentTimeMillis(),
        isSummarized = false
    )
}
