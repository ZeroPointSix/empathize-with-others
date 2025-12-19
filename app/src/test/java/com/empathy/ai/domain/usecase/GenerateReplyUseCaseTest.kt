package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.FloatingWindowError
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.SessionContextService
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.domain.util.PromptBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * GenerateReplyUseCase单元测试
 * 
 * 测试回复生成功能，特别关注数据保存的完整性
 */
class GenerateReplyUseCaseTest {
    
    // Mock dependencies
    private lateinit var contactRepository: ContactRepository
    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var aiProviderRepository: AiProviderRepository
    private lateinit var promptBuilder: PromptBuilder
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var sessionContextService: SessionContextService
    
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
        conversationRepository = mockk()
        sessionContextService = mockk()
        
        // Create use case with mocked dependencies
        generateReplyUseCase = GenerateReplyUseCase(
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            privacyRepository = privacyRepository,
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository,
            promptBuilder = promptBuilder,
            conversationRepository = conversationRepository,
            sessionContextService = sessionContextService
        )
        
        // Setup default mock behaviors
        setupDefaultMocks()
    }
    
    private fun setupDefaultMocks() {
        // Mock AI provider
        val mockProvider = mockk<com.empathy.ai.domain.model.AiProvider>()
        every { aiProviderRepository.getDefaultProvider() } returns com.github.michaelbull.result.Ok(mockProvider)
        
        // Mock contact profile
        val mockProfile = mockk<com.empathy.ai.domain.model.ContactProfile>()
        every { contactRepository.getProfile(testContactId) } returns com.github.michaelbull.result.Ok(mockProfile)
        every { mockProfile.targetGoal } returns "维护良好关系"
        every { mockProfile.contextDepth } returns 10
        every { mockProfile.facts } returns emptyList()
        
        // Mock brain tags
        val redTag = BrainTag(id = 1, content = "不喜欢谈论工作", type = TagType.RISK_RED, isConfirmed = true)
        val greenTag = BrainTag(id = 2, content = "喜欢美食", type = TagType.STRATEGY_GREEN, isConfirmed = true)
        every { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(listOf(redTag, greenTag))
        
        // Mock privacy
        every { privacyRepository.maskText(any()) } returns it // No masking for test
        
        // Mock session context
        every { sessionContextService.getHistoryContext(testContactId) } returns ""
        
        // Mock prompt builder
        every { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "test instruction"
        
        // Mock conversation repository - save user input
        coEvery { conversationRepository.saveUserInput(any(), any()) } returns com.github.michaelbull.result.Ok(1L)
        
        // Mock conversation repository - update AI response
        coEvery { conversationRepository.updateAiResponse(any(), any()) } returns com.github.michaelbull.result.Ok(Unit)
        
        // Mock AI repository
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns com.github.michaelbull.result.Ok(
            ReplyResult(suggestedReply = testReply)
        )
    }
    
    @Test
    fun `回复模式应该保存用户输入和AI回复`() = runTest {
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue(result.isSuccess, "生成回复应该成功")
        
        // Verify user input was saved
        coVerify { 
            conversationRepository.saveUserInput(
                contactId = testContactId,
                userInput = IdentityPrefixHelper.addPrefix(testMessage, ActionType.REPLY)
            )
        }
        
        // Verify AI response was saved
        coVerify { 
            conversationRepository.updateAiResponse(
                logId = 1L,
                aiResponse = "【回复建议】\n$testReply"
            )
        }
    }
    
    @Test
    fun `AI回复保存失败不应影响主流程`() = runTest {
        // Given - AI response save fails
        coEvery { conversationRepository.updateAiResponse(any(), any()) } returns com.github.michaelbull.result.Err(
            Exception("保存失败")
        )
        
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue(result.isSuccess, "主流程不应该受AI回复保存失败影响")
        
        // Verify user input was still saved
        coVerify { 
            conversationRepository.saveUserInput(
                contactId = testContactId,
                userInput = IdentityPrefixHelper.addPrefix(testMessage, ActionType.REPLY)
            )
        }
        
        // Verify AI response save was attempted
        coVerify { 
            conversationRepository.updateAiResponse(
                logId = 1L,
                aiResponse = "【回复建议】\n$testReply"
            )
        }
    }
    
    @Test
    fun `用户输入保存失败不应影响AI生成`() = runTest {
        // Given - user input save fails
        coEvery { conversationRepository.saveUserInput(any(), any()) } returns com.github.michaelbull.result.Err(
            Exception("保存失败")
        )
        
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue(result.isSuccess, "AI生成不应该受用户输入保存失败影响")
        
        // Verify AI was still called
        coVerify { 
            aiRepository.generateReply(any(), any(), any())
        }
        
        // Verify AI response save was not attempted (since logId would be null)
        coVerify(exactly = 0) { 
            conversationRepository.updateAiResponse(any(), any())
        }
    }
    
    @Test
    fun `未配置AI服务商应该返回错误`() = runTest {
        // Given - no AI provider configured
        every { aiProviderRepository.getDefaultProvider() } returns com.github.michaelbull.result.Err(
            Exception("未配置服务商")
        )
        
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue(result.isFailure, "未配置服务商应该返回错误")
        assertTrue(result.exceptionOrNull() is FloatingWindowError.NoProviderConfigured)
        
        // Verify no data was saved
        coVerify(exactly = 0) { 
            conversationRepository.saveUserInput(any(), any())
        }
        coVerify(exactly = 0) { 
            conversationRepository.updateAiResponse(any(), any())
        }
    }
    
    @Test
    fun `联系人不存在应该返回错误`() = runTest {
        // Given - contact not found
        every { contactRepository.getProfile(testContactId) } returns com.github.michaelbull.result.Err(
            Exception("联系人不存在")
        )
        
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue(result.isFailure, "联系人不存在应该返回错误")
        
        // Verify no data was saved
        coVerify(exactly = 0) { 
            conversationRepository.saveUserInput(any(), any())
        }
        coVerify(exactly = 0) { 
            conversationRepository.updateAiResponse(any(), any())
        }
    }
    
    @Test
    fun `AI生成失败应该返回错误但用户输入已保存`() = runTest {
        // Given - AI generation fails
        coEvery { aiRepository.generateReply(any(), any(), any()) } returns com.github.michaelbull.result.Err(
            Exception("AI生成失败")
        )
        
        // When
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then
        assertTrue(result.isFailure, "AI生成失败应该返回错误")
        
        // Verify user input was still saved
        coVerify { 
            conversationRepository.saveUserInput(
                contactId = testContactId,
                userInput = IdentityPrefixHelper.addPrefix(testMessage, ActionType.REPLY)
            )
        }
        
        // Verify AI response save was not attempted
        coVerify(exactly = 0) { 
            conversationRepository.updateAiResponse(any(), any())
        }
    }
    
    @Test
    fun `应该正确构建运行时数据`() = runTest {
        // Given
        val redTag = BrainTag(id = 1, content = "不喜欢谈论工作", type = TagType.RISK_RED, isConfirmed = true)
        val greenTag = BrainTag(id = 2, content = "喜欢美食", type = TagType.STRATEGY_GREEN, isConfirmed = true)
        every { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(listOf(redTag, greenTag))
        
        val historyContext = "之前的对话记录"
        every { sessionContextService.getHistoryContext(testContactId) } returns historyContext
        
        val mockProfile = mockk<com.empathy.ai.domain.model.ContactProfile>()
        every { contactRepository.getProfile(testContactId) } returns com.github.michaelbull.result.Ok(mockProfile)
        every { mockProfile.targetGoal } returns "成为好朋友"
        every { mockProfile.contextDepth } returns 10
        every { mockProfile.facts } returns emptyList()
        
        // When
        generateReplyUseCase(testContactId, testMessage)
        
        // Then
        coVerify {
            promptBuilder.buildSystemInstruction(
                scene = PromptScene.REPLY,
                contactId = testContactId,
                context = any(),
                runtimeData = match { runtimeData ->
                    runtimeData.contains("【攻略目标】")
                    runtimeData.contains("成为好朋友")
                    runtimeData.contains("【对方消息】")
                    runtimeData.contains(IdentityPrefixHelper.addPrefix(testMessage, ActionType.REPLY))
                    runtimeData.contains("【雷区警告】")
                    runtimeData.contains("不喜欢谈论工作")
                    runtimeData.contains("【策略建议】")
                    runtimeData.contains("喜欢美食")
                    if (historyContext.isNotBlank()) {
                        runtimeData.contains(historyContext)
                    } else {
                        true
                    }
                }
            )
        }
    }
}