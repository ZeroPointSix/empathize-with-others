package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.FloatingWindowError
import com.empathy.ai.domain.model.PolishResult
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.SessionContextService
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.UserProfileContextBuilder
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
 * PolishDraftUseCase 单元测试
 *
 * 测试润色草稿用例的各种场景
 */
class PolishDraftUseCaseTest {

    private lateinit var useCase: PolishDraftUseCase
    private lateinit var contactRepository: ContactRepository
    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var aiProviderRepository: AiProviderRepository
    private lateinit var promptBuilder: PromptBuilder
    private lateinit var sessionContextService: SessionContextService
    private lateinit var userProfileContextBuilder: UserProfileContextBuilder

    private val testContactId = "contact_123"
    private val testDraft = "你好，我想问一下关于项目的事情"
    private val testMaskedDraft = "你好，我想问一下关于项目的事情"

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
        targetGoal = "维护良好关系"
    )

    private val testRedTags = listOf(
        BrainTag(
            id = 1L,
            contactId = testContactId,
            content = "不要提加班",
            type = TagType.RISK_RED
        )
    )

    private val testPolishResult = PolishResult(
        polishedText = "您好，想请教一下关于项目的事宜",
        hasRisk = false,
        riskWarning = null
    )

    @Before
    fun setup() {
        contactRepository = mockk()
        brainTagRepository = mockk()
        privacyRepository = mockk()
        aiRepository = mockk()
        aiProviderRepository = mockk()
        promptBuilder = mockk()
        sessionContextService = mockk()
        userProfileContextBuilder = mockk()

        // 默认返回空历史上下文
        coEvery { sessionContextService.getHistoryContext(any<String>()) } returns ""
        // 默认返回空用户画像上下文
        coEvery { userProfileContextBuilder.buildAnalysisContext(any(), any()) } returns Result.success("")

        useCase = PolishDraftUseCase(
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            privacyRepository = privacyRepository,
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository,
            promptBuilder = promptBuilder,
            sessionContextService = sessionContextService,
            userProfileContextBuilder = userProfileContextBuilder
        )
    }

    @Test
    fun `when polish draft successfully, should return PolishResult`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(testRedTags)
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        val result = useCase(testContactId, testDraft)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testPolishResult, result.getOrNull())
        coVerify { privacyRepository.maskText(testDraft) }
        coVerify { aiRepository.polishDraft(testProvider, any(), any()) }
    }

    @Test
    fun `when no provider configured, should return NoProviderConfigured error`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(null)

        // When
        val result = useCase(testContactId, testDraft)

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
        val result = useCase(testContactId, testDraft)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is FloatingWindowError.ContactNotFound)
        assertEquals(testContactId, (error as FloatingWindowError.ContactNotFound).contactId)
    }

    @Test
    fun `when AI call fails, should return failure with FloatingWindowError`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(testRedTags)
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.failure(
            RuntimeException("AI调用失败")
        )

        // When
        val result = useCase(testContactId, testDraft)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `when exception thrown, should return FloatingWindowError`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } throws RuntimeException("网络超时")

        // When
        val result = useCase(testContactId, testDraft)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FloatingWindowError)
    }

    @Test
    fun `should mask text before calling AI`() = runTest {
        // Given
        val sensitiveText = "我的手机号是13812345678"
        val maskedText = "我的手机号是[手机号_1]"

        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(sensitiveText) } returns maskedText
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        useCase(testContactId, sensitiveText)

        // Then
        coVerify { privacyRepository.maskText(sensitiveText) }
    }

    @Test
    fun `should include red tags in runtime data`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(testRedTags)
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        useCase(testContactId, testDraft)

        // Then
        coVerify {
            promptBuilder.buildSystemInstruction(
                any(),
                eq(testContactId),
                any(),
                match { it.contains("雷区警告") && it.contains("不要提加班") }
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
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns historyContext
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        useCase(testContactId, testDraft)

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
    fun `should call sessionContextService to get history`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns ""
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        useCase(testContactId, testDraft)

        // Then: 验证调用了SessionContextService
        coVerify { sessionContextService.getHistoryContext(testContactId) }
    }

    @Test
    fun `should work correctly when history context is empty`() = runTest {
        // Given: 没有历史对话
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns ""
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        val result = useCase(testContactId, testDraft)

        // Then: 应该正常返回结果
        assertTrue(result.isSuccess)
        assertEquals(testPolishResult, result.getOrNull())
    }

    // ==================== TD-00013 用户画像上下文测试 ====================

    @Test
    fun `should include user profile context in runtime data when available`() = runTest {
        // Given: 有用户画像上下文
        val userProfileContext = """
            【用户画像（你的特点）】
            - 性格特点: 外向、乐观
            - 沟通风格: 直接、幽默
        """.trimIndent()

        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns ""
        coEvery { userProfileContextBuilder.buildAnalysisContext(testProfile, testDraft) } returns Result.success(userProfileContext)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        useCase(testContactId, testDraft)

        // Then: 验证用户画像上下文被包含在运行时数据中
        coVerify {
            promptBuilder.buildSystemInstruction(
                any(),
                eq(testContactId),
                any(),
                match { it.contains("【用户画像（你的特点）】") && it.contains("外向") }
            )
        }
    }

    @Test
    fun `should call userProfileContextBuilder to get user profile`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns ""
        coEvery { userProfileContextBuilder.buildAnalysisContext(testProfile, testDraft) } returns Result.success("")
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        useCase(testContactId, testDraft)

        // Then: 验证调用了UserProfileContextBuilder
        coVerify { userProfileContextBuilder.buildAnalysisContext(testProfile, testDraft) }
    }

    @Test
    fun `should work correctly when user profile context fails`() = runTest {
        // Given: 用户画像获取失败
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns ""
        coEvery { userProfileContextBuilder.buildAnalysisContext(any(), any()) } returns Result.failure(Exception("获取失败"))
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        val result = useCase(testContactId, testDraft)

        // Then: 应该正常返回结果（降级处理）
        assertTrue(result.isSuccess)
        assertEquals(testPolishResult, result.getOrNull())
    }

    @Test
    fun `should place user profile before history context in runtime data`() = runTest {
        // Given: 同时有用户画像和历史对话
        val userProfileContext = "【用户画像（你的特点）】\n- 性格特点: 外向"
        val historyContext = "【历史对话】\n- 你好"

        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        coEvery { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.maskText(testDraft) } returns testMaskedDraft
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns historyContext
        coEvery { userProfileContextBuilder.buildAnalysisContext(testProfile, testDraft) } returns Result.success(userProfileContext)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "system instruction"
        coEvery { aiRepository.polishDraft(any(), any(), any()) } returns Result.success(testPolishResult)

        // When
        useCase(testContactId, testDraft)

        // Then: 验证用户画像在历史对话之前
        coVerify {
            promptBuilder.buildSystemInstruction(
                any(),
                eq(testContactId),
                any(),
                match { 
                    val userProfileIndex = it.indexOf("【用户画像（你的特点）】")
                    val historyIndex = it.indexOf("【历史对话】")
                    userProfileIndex >= 0 && historyIndex >= 0 && userProfileIndex < historyIndex
                }
            )
        }
    }
}
