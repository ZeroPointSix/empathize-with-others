package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.FloatingWindowError
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.repository.TopicRepository
import com.empathy.ai.domain.service.SessionContextService
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.UserProfileContextBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * GenerateReplyUseCase单元测试
 * 
 * 测试回复生成功能
 */
class GenerateReplyUseCaseTest {
    
    // Mock dependencies
    private lateinit var contactRepository: ContactRepository
    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var aiProviderRepository: AiProviderRepository
    private lateinit var promptBuilder: PromptBuilder
    private lateinit var sessionContextService: SessionContextService
    private lateinit var userProfileContextBuilder: UserProfileContextBuilder
    private lateinit var topicRepository: TopicRepository
    
    private lateinit var generateReplyUseCase: GenerateReplyUseCase
    
    private val testContactId = "1"
    private val testMessage = "你好，最近怎么样？"
    private val testReply = "我最近很好，谢谢关心！你呢？"
    
    @Before
    fun setup() {
        // Initialize mocks
        contactRepository = mockk()
        brainTagRepository = mockk()
        privacyRepository = mockk()
        aiRepository = mockk()
        aiProviderRepository = mockk()
        promptBuilder = mockk()
        sessionContextService = mockk()
        userProfileContextBuilder = mockk()
        topicRepository = mockk()
        
        // Create use case with mocked dependencies
        generateReplyUseCase = GenerateReplyUseCase(
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            privacyRepository = privacyRepository,
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository,
            promptBuilder = promptBuilder,
            sessionContextService = sessionContextService,
            userProfileContextBuilder = userProfileContextBuilder,
            topicRepository = topicRepository
        )
        
        // Setup default mock behaviors
        setupDefaultMocks()
    }
    
    private fun setupDefaultMocks() {
        // Mock AI provider
        val testModel = AiModel(id = "test-model", displayName = "Test Model")
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
        
        // Mock contact profile
        val mockProfile = ContactProfile(
            id = testContactId,
            name = "Test Contact",
            targetGoal = "维护良好关系",
            contextDepth = 10,
            facts = emptyList()
        )
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(mockProfile)
        
        // Mock brain tags
        val redTag = BrainTag(id = 1, contactId = testContactId, content = "不喜欢谈论工作", type = TagType.RISK_RED, isConfirmed = true)
        val greenTag = BrainTag(id = 2, contactId = testContactId, content = "喜欢美食", type = TagType.STRATEGY_GREEN, isConfirmed = true)
        every { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(listOf(redTag, greenTag))
        
        // Mock privacy - return input unchanged
        coEvery { privacyRepository.maskText(any()) } answers { firstArg() }
        
        // Mock session context
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns ""
        
        // Mock user profile context builder
        coEvery { userProfileContextBuilder.buildAnalysisContext(any(), any()) } returns Result.success("")
        
        // Mock topic repository
        coEvery { topicRepository.getActiveTopic(any()) } returns null
        
        // Mock prompt builder
        coEvery { promptBuilder.buildWithTopic(any(), any(), any(), any(), any()) } returns "test instruction"
        
        // Mock AI repository
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.success(
            ReplyResult(suggestedReply = testReply)
        )
    }
    
    @Test
    fun `回复模式应该成功生成回复`() = runTest {
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue("生成回复应该成功", result.isSuccess)
        val replyResult = result.getOrNull()
        assertNotNull("回复结果不应为空", replyResult)
    }
    
    @Test
    fun `未配置AI服务商应该返回错误`() = runTest {
        // Given - no AI provider configured
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.failure(
            Exception("未配置服务商")
        )
        
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue("未配置服务商应该返回错误", result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull("应该有异常", error)
        assertTrue("应该是NoProviderConfigured错误", error is FloatingWindowError.NoProviderConfigured)
    }
    
    @Test
    fun `联系人不存在应该返回错误`() = runTest {
        // Given - contact not found
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)
        
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue("联系人不存在应该返回错误", result.isFailure)
    }
    
    @Test
    fun `AI生成失败应该返回错误`() = runTest {
        // Given - AI generation fails
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns Result.failure(
            Exception("AI生成失败")
        )
        
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue("AI生成失败应该返回错误", result.isFailure)
    }
    
    @Test
    fun `应该正确调用PromptBuilder`() = runTest {
        // Given
        val historyContext = "之前的对话记录"
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns historyContext
        
        // When
        generateReplyUseCase(testContactId, testMessage)
        
        // Then
        coVerify {
            promptBuilder.buildWithTopic(
                scene = PromptScene.REPLY,
                contactId = testContactId,
                context = any(),
                topic = any(),
                runtimeData = any()
            )
        }
    }
    
    @Test
    fun `应该正确调用隐私脱敏`() = runTest {
        // When
        generateReplyUseCase(testContactId, testMessage)
        
        // Then
        coVerify { privacyRepository.maskText(testMessage) }
    }
    
    @Test
    fun `应该获取历史对话上下文`() = runTest {
        // When
        generateReplyUseCase(testContactId, testMessage)
        
        // Then
        coVerify { sessionContextService.getHistoryContext(testContactId) }
    }

    // ==================== TD-00013 用户画像上下文测试 ====================

    @Test
    fun `应该获取用户画像上下文`() = runTest {
        // When
        generateReplyUseCase(testContactId, testMessage)
        
        // Then
        coVerify { userProfileContextBuilder.buildAnalysisContext(any(), testMessage) }
    }

    @Test
    fun `用户画像上下文应该包含在运行时数据中`() = runTest {
        // Given
        val userProfileContext = """
            【用户画像（你的特点）】
            - 性格特点: 外向、乐观
            - 沟通风格: 直接、幽默
        """.trimIndent()
        coEvery { userProfileContextBuilder.buildAnalysisContext(any(), any()) } returns Result.success(userProfileContext)
        
        // When
        generateReplyUseCase(testContactId, testMessage)
        
        // Then
        coVerify {
            promptBuilder.buildWithTopic(
                any(),
                eq(testContactId),
                any(),
                any(),
                match { it.contains("【用户画像（你的特点）】") && it.contains("外向") }
            )
        }
    }

    @Test
    fun `用户画像获取失败应该降级处理`() = runTest {
        // Given - 用户画像获取失败
        coEvery { userProfileContextBuilder.buildAnalysisContext(any(), any()) } returns Result.failure(Exception("获取失败"))
        
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then - 应该正常返回结果（降级处理）
        assertTrue("用户画像获取失败应该降级处理", result.isSuccess)
    }

    @Test
    fun `用户画像应该在历史对话之前`() = runTest {
        // Given
        val userProfileContext = "【用户画像（你的特点）】\n- 性格特点: 外向"
        val historyContext = "【历史对话】\n- 你好"
        coEvery { userProfileContextBuilder.buildAnalysisContext(any(), any()) } returns Result.success(userProfileContext)
        coEvery { sessionContextService.getHistoryContext(testContactId) } returns historyContext
        
        // When
        generateReplyUseCase(testContactId, testMessage)
        
        // Then
        coVerify {
            promptBuilder.buildWithTopic(
                any(),
                eq(testContactId),
                any(),
                any(),
                match { runtimeData ->
                    val userProfileIndex = runtimeData.indexOf("【用户画像（你的特点）】")
                    val historyIndex = runtimeData.indexOf("【历史对话】")
                    userProfileIndex >= 0 && historyIndex >= 0 && userProfileIndex < historyIndex
                }
            )
        }
    }
}
