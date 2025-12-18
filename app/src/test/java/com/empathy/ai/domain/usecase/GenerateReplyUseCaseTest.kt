package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.FloatingWindowError
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.SessionContextService
import com.empathy.ai.domain.util.PromptBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * GenerateReplyUseCase 单元测试
 *
 * 测试生成回复用例的各种场景
 */
class GenerateReplyUseCaseTest {

    private lateinit var useCase: GenerateReplyUseCase
    private lateinit var contactRepository: ContactRepository
    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var aiProviderRepository: AiProviderRepository
    private lateinit var promptBuilder: PromptBuilder
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var sessionContextService: SessionContextService

    private val testContactId = "contact_123"
    private val testMessage = "你最近怎么样？"
    private val testMaskedMessage = "你最近怎么样？"

    private val testProvider = AiProvider(
        id = "provider_1",
        name = "TestProvider",
        baseUrl = "https://api.test.com",
        apiKey = "test_key",
        models = listOf(AiModel(id = "gpt-4", displayName = "GPT-4")),
        defaultModelId = "gpt-4",
        isDefault = true
    )

    private val testProfile = ContactProfile(
        id = testContactId,
        name = "测试联系人",
        targetGoal = "保持友好关系"
    )

    private val testRedTags = listOf(
        BrainTag(
            id = 1L,
            contactId = testContactId,
            content = "不要提前任",
            type = TagType.RISK_RED
        )
    )

    private val testGreenTags = listOf(
        BrainTag(
            id = 2L,
            contactId = testContactId,
            content = "喜欢聊旅游",
            type = TagType.STRATEGY_GREEN
        )
    )

    private val testReplyResult = ReplyResult(
        suggestedReply = "最近挺好的，你呢？",
        strategyNote = "保持友好轻松的语气"
    )

    @Before
    fun setup() {
        contactRepository = mockk()
        brainTagRepository = mockk()
        privacyRepository = mockk()
        aiRepository = mockk()
        aiProviderRepository = mockk()
        promptBuilder = mockk()
        conversationRepository = mockk()
        sessionContextService = mockk()

        // 默认返回空历史上下文
        coEvery { sessionContextService.getHistoryContext(any<String>()) } returns ""

        useCase = GenerateReplyUseCase(
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            privacyRepository = privacyRepository,
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository,
            promptBuilder = promptBuilder,
            conversationRepository = conversationRepository,
            sessionContextService = sessionContextService
        )
    }

    @Test
    fun `when generate reply successfully, should return ReplyResult`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(testRedTags + testGreenTags)
        coEvery { privacyRepository.maskText(testMessage) } returns testMaskedMessage
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } returns Result.success(1L)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.success(testReplyResult)

        // When
        val result = useCase(testContactId, testMessage)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testReplyResult, result.getOrNull())
        coVerify { privacyRepository.maskText(testMessage) }
        coVerify { aiRepository.generateReply(testProvider, any(), any()) }
    }

    @Test
    fun `when no provider configured, should return NoProviderConfigured error`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(null)

        // When
        val result = useCase(testContactId, testMessage)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FloatingWindowError.NoProviderConfigured)
    }

    @Test
    fun `when contact not found, should return ContactNotFound error`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)

        // When
        val result = useCase(testContactId, testMessage)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is FloatingWindowError.ContactNotFound)
        assertEquals(testContactId, (error as FloatingWindowError.ContactNotFound).contactId)
    }

    @Test
    fun `when AI call fails, should return failure`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testMessage) } returns testMaskedMessage
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } returns Result.success(1L)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.failure(
            RuntimeException("AI调用失败")
        )

        // When
        val result = useCase(testContactId, testMessage)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `when exception thrown, should return FloatingWindowError`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } throws RuntimeException("timeout")

        // When
        val result = useCase(testContactId, testMessage)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FloatingWindowError)
    }

    @Test
    fun `should save conversation before calling AI`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testMessage) } returns testMaskedMessage
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } returns Result.success(1L)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.success(testReplyResult)

        // When
        useCase(testContactId, testMessage)

        // Then
        coVerify {
            conversationRepository.saveUserInput(
                contactId = testContactId,
                userInput = any(),
                timestamp = any()
            )
        }
    }

    @Test
    fun `when save conversation fails, should continue with AI call`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testMessage) } returns testMaskedMessage
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } throws RuntimeException("保存失败")
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.success(testReplyResult)

        // When
        val result = useCase(testContactId, testMessage)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testReplyResult, result.getOrNull())
    }

    @Test
    fun `should mask text before calling AI`() = runTest {
        // Given
        val sensitiveMessage = "我的手机号是13812345678"
        val maskedMessage = "我的手机号是[手机号_1]"

        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(sensitiveMessage) } returns maskedMessage
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } returns Result.success(1L)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.success(testReplyResult)

        // When
        useCase(testContactId, sensitiveMessage)

        // Then
        coVerify { privacyRepository.maskText(sensitiveMessage) }
    }

    @Test
    fun `should include both red and green tags in runtime data`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(testRedTags + testGreenTags)
        coEvery { privacyRepository.maskText(testMessage) } returns testMaskedMessage
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } returns Result.success(1L)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.success(testReplyResult)

        // When
        useCase(testContactId, testMessage)

        // Then
        coVerify {
            promptBuilder.buildSystemInstruction(
                any(),
                eq(testContactId),
                any(),
                match {
                    it.contains("雷区警告") && it.contains("不要提前任") &&
                    it.contains("策略建议") && it.contains("喜欢聊旅游")
                }
            )
        }
    }

    // ==================== BUG-00015 历史上下文测试 ====================

    @Test
    fun `should include history context in runtime data when available`() = runTest {
        // Given: 有历史对话上下文
        val historyContext = """
            【历史对话】(最近2条)
            [历史记录 - 10:00]: 【对方说】：你好
            [历史记录 - 10:05]: 【我正在回复】：你好啊
        """.trimIndent()

        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testMessage) } returns testMaskedMessage
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns historyContext
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } returns Result.success(1L)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.success(testReplyResult)

        // When
        useCase(testContactId, testMessage)

        // Then: 验证历史上下文被包含在运行时数据中
        coVerify {
            promptBuilder.buildSystemInstruction(
                any(),
                eq(testContactId),
                any(),
                match { it.contains("【历史对话】") && it.contains("你好") }
            )
        }
    }

    @Test
    fun `should call sessionContextService to get history before saving current input`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testMessage) } returns testMaskedMessage
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns ""
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } returns Result.success(1L)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.success(testReplyResult)

        // When
        useCase(testContactId, testMessage)

        // Then: 验证调用了SessionContextService
        coVerify { sessionContextService.getHistoryContext(testContactId) }
    }

    @Test
    fun `should work correctly when history context is empty`() = runTest {
        // Given: 没有历史对话
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testMessage) } returns testMaskedMessage
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns ""
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } returns Result.success(1L)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.success(testReplyResult)

        // When
        val result = useCase(testContactId, testMessage)

        // Then: 应该正常返回结果
        assertTrue(result.isSuccess)
        assertEquals(testReplyResult, result.getOrNull())
    }
}
