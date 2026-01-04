package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SendAdvisorMessageUseCase单元测试
 *
 * 业务背景 (PRD-00026):
 * - AI军师核心业务用例，负责处理用户消息并生成AI回复
 * - 消息发送流程: 保存用户消息 → 获取联系人画像 → 构建提示词 → 调用AI → 保存AI回复
 * - 支持联系人对话历史分析，为AI军师提供上下文
 *
 * 设计决策 (TDD-00026):
 * - 采用双阶段提示词构建: 系统提示词(系统角色定义) + 用户提示词(当前问题)
 * - 历史对话限制为最近20条（受AI模型Context Window限制）
 * - 会话上下文限制为最近10条，控制Token消耗
 * - 错误处理: 用户消息保存失败立即返回，AI调用失败仍保存失败状态消息
 *
 * 任务: TD-00026/T008
 */
class SendAdvisorMessageUseCaseTest {

    private lateinit var useCase: SendAdvisorMessageUseCase
    private lateinit var aiAdvisorRepository: AiAdvisorRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var aiProviderRepository: AiProviderRepository

    private val testContactId = "contact-1"
    private val testSessionId = "session-1"
    private val testMessage = "How should I respond to this?"

    @Before
    fun setup() {
        aiAdvisorRepository = mockk(relaxed = true)
        aiRepository = mockk()
        contactRepository = mockk()
        aiProviderRepository = mockk()

        useCase = SendAdvisorMessageUseCase(
            aiAdvisorRepository = aiAdvisorRepository,
            aiRepository = aiRepository,
            contactRepository = contactRepository,
            aiProviderRepository = aiProviderRepository
        )
    }

    /**
     * 验证消息发送流程的第一步：先保存用户消息
     *
     * 业务规则 (PRD-00026/AC-003):
     * - 用户消息必须在AI回复之前保存，确保对话历史的完整性
     * - 消息类型必须正确标记为MessageType.USER
     */
    @Test
    fun `invoke should save user message first`() = runTest {
        // Given
        setupSuccessfulMocks()
        val savedMessages = mutableListOf<AiAdvisorConversation>()
        coEvery { aiAdvisorRepository.saveMessage(capture(savedMessages)) } returns Result.success(Unit)

        // When
        useCase(testContactId, testSessionId, testMessage)

        // Then
        coVerify(atLeast = 1) { aiAdvisorRepository.saveMessage(any()) }
        // First saved message should be user message
        val userMessage = savedMessages.first()
        assertEquals(testContactId, userMessage.contactId)
        assertEquals(testSessionId, userMessage.sessionId)
        assertEquals(testMessage, userMessage.content)
        assertEquals(MessageType.USER, userMessage.messageType)
    }

    /**
     * 验证前置条件检查：未配置AI服务商时返回错误
     *
     * 业务规则 (PRD-00026):
     * - AI军师功能依赖第三方AI服务，必须先配置服务商才能使用
     * - 这是BYOK (Bring Your Own Key) 架构的体现
     *
     * 设计权衡 (TDD-00026):
     * - 选择快速失败策略，避免无效的AI调用请求
     */
    @Test
    fun `invoke should return failure when no AI provider configured`() = runTest {
        // Given
        coEvery { aiAdvisorRepository.saveMessage(any()) } returns Result.success(Unit)
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(null)

        // When
        val result = useCase(testContactId, testSessionId, testMessage)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("未配置AI服务商") == true)
    }

    @Test
    fun `invoke should return failure when contact not found`() = runTest {
        // Given
        coEvery { aiAdvisorRepository.saveMessage(any()) } returns Result.success(Unit)
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(createTestProvider())
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)

        // When
        val result = useCase(testContactId, testSessionId, testMessage)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("联系人不存在") == true)
    }

    /**
     * 验证提示词构建：包含联系人画像和当前问题
     *
     * 业务规则 (PRD-00026/AC-005):
     * - AI军师分析时必须包含联系人的基础信息（姓名、关系阶段等）
     * - 当前问题是AI分析的直接输入
     *
     * 设计权衡 (TDD-00026):
     * - 使用双阶段提示词：系统提示词定义AI角色，用户提示词包含具体问题
     * - 联系人画像作为上下文背景，帮助AI提供个性化分析
     */
    @Test
    fun `invoke should call AI repository with correct prompt`() = runTest {
        // Given
        setupSuccessfulMocks()
        val promptSlot = slot<String>()
        coEvery {
            aiRepository.generateText(any(), capture(promptSlot), any())
        } returns Result.success("AI Response")

        // When
        useCase(testContactId, testSessionId, testMessage)

        // Then
        val prompt = promptSlot.captured
        assertTrue(prompt.contains("联系人画像"))
        assertTrue(prompt.contains("Test Contact"))
        assertTrue(prompt.contains("当前问题"))
        assertTrue(prompt.contains(testMessage))
    }

    /**
     * 验证上下文连贯性：包含会话历史对话
     *
     * 业务规则 (PRD-00026/AC-004):
     * - AI军师能理解当前对话的上下文和连续性
     * - 对话历史按时间正序排列，构建完整对话流
     *
     * 设计权衡 (TDD-00026):
     * - 会话上下文限制为最近10条（SESSION_CONTEXT_LIMIT）
     * - 平衡上下文完整性和Token消耗
     */
    @Test
    fun `invoke should include conversation history in prompt`() = runTest {
        // Given
        val history = listOf(
            createTestConversation("conv-1", MessageType.USER, "Previous question"),
            createTestConversation("conv-2", MessageType.AI, "Previous answer")
        )
        setupSuccessfulMocks(history = history)
        val promptSlot = slot<String>()
        coEvery {
            aiRepository.generateText(any(), capture(promptSlot), any())
        } returns Result.success("AI Response")

        // When
        useCase(testContactId, testSessionId, testMessage)

        // Then
        val prompt = promptSlot.captured
        assertTrue(prompt.contains("对话历史"))
        assertTrue(prompt.contains("Previous question"))
        assertTrue(prompt.contains("Previous answer"))
    }

    @Test
    fun `invoke should save AI response on success`() = runTest {
        // Given
        setupSuccessfulMocks()
        val savedMessages = mutableListOf<AiAdvisorConversation>()
        coEvery { aiAdvisorRepository.saveMessage(capture(savedMessages)) } returns Result.success(Unit)

        // When
        val result = useCase(testContactId, testSessionId, testMessage)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, savedMessages.size) // User message + AI message
        
        val aiMessage = savedMessages.last()
        assertEquals(MessageType.AI, aiMessage.messageType)
        assertEquals("AI Response", aiMessage.content)
        assertEquals(SendStatus.SUCCESS, aiMessage.sendStatus)
    }

    @Test
    fun `invoke should save failed AI message on AI error`() = runTest {
        // Given
        coEvery { aiAdvisorRepository.saveMessage(any()) } returns Result.success(Unit)
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(createTestProvider())
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(createTestContact())
        coEvery { aiAdvisorRepository.getRecentConversations(any(), any()) } returns Result.success(emptyList())
        coEvery {
            aiRepository.generateText(any(), any(), any())
        } returns Result.failure(Exception("AI Error"))

        val savedMessages = mutableListOf<AiAdvisorConversation>()
        coEvery { aiAdvisorRepository.saveMessage(capture(savedMessages)) } returns Result.success(Unit)

        // When
        val result = useCase(testContactId, testSessionId, testMessage)

        // Then
        assertTrue(result.isFailure)
        assertEquals(2, savedMessages.size)
        
        val failedMessage = savedMessages.last()
        assertEquals(MessageType.AI, failedMessage.messageType)
        assertEquals(SendStatus.FAILED, failedMessage.sendStatus)
    }

    @Test
    fun `invoke should return failure when saving user message fails`() = runTest {
        // Given
        coEvery {
            aiAdvisorRepository.saveMessage(any())
        } returns Result.failure(Exception("Database error"))

        // When
        val result = useCase(testContactId, testSessionId, testMessage)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("保存用户消息失败") == true)
    }

    /**
     * 验证Token消耗控制：会话上下文限制
     *
     * 已知限制 (TDD-00026/11.1):
     * - 受AI模型Context Window限制，会话上下文限制为最近10条
     * - 这是技术约束而非业务需求，v2.0计划引入RAG解决
     *
     * 权衡:
     * - 牺牲早期对话的上下文，换取对话的持续进行
     * - 用户可以通过新建会话来重置上下文
     */
    @Test
    fun `invoke should limit history to SESSION_CONTEXT_LIMIT`() = runTest {
        // Given
        val largeHistory = (1..30).map { i ->
            createTestConversation("conv-$i", if (i % 2 == 0) MessageType.AI else MessageType.USER, "Message $i")
        }
        setupSuccessfulMocks(history = largeHistory)
        val promptSlot = slot<String>()
        coEvery {
            aiRepository.generateText(any(), capture(promptSlot), any())
        } returns Result.success("AI Response")

        // When
        useCase(testContactId, testSessionId, testMessage)

        // Then
        val prompt = promptSlot.captured
        // Should only include last SESSION_CONTEXT_LIMIT messages
        assertTrue(prompt.contains("Message 21") || prompt.contains("Message 30"))
        // Should not include early messages
        assertTrue(!prompt.contains("Message 1") || prompt.contains("Message 10"))
    }

    /**
     * 验证个性化分析：包含用户设定的沟通目标
     *
     * 业务规则 (PRD-00026/AC-006):
     * - 分析方向与用户的攻略目标保持一致
     * - 目标设定影响AI建议的语气、深度和方向
     *
     * 场景示例:
     * - 目标"建立更亲密关系" → AI侧重情感分析
     * - 目标"保持专业距离" → AI侧重社交边界分析
     */
    @Test
    fun `invoke should include contact target goal in prompt`() = runTest {
        // Given
        val contactWithGoal = ContactProfile(
            id = testContactId,
            name = "Test Contact",
            targetGoal = "Build closer relationship"
        )
        coEvery { aiAdvisorRepository.saveMessage(any()) } returns Result.success(Unit)
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(createTestProvider())
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(contactWithGoal)
        coEvery { aiAdvisorRepository.getRecentConversations(any(), any()) } returns Result.success(emptyList())
        
        val promptSlot = slot<String>()
        coEvery {
            aiRepository.generateText(any(), capture(promptSlot), any())
        } returns Result.success("AI Response")

        // When
        useCase(testContactId, testSessionId, testMessage)

        // Then
        val prompt = promptSlot.captured
        assertTrue(prompt.contains("沟通目标"))
        assertTrue(prompt.contains("Build closer relationship"))
    }

    // ==================== 辅助方法 ====================

    private fun setupSuccessfulMocks(history: List<AiAdvisorConversation> = emptyList()) {
        coEvery { aiAdvisorRepository.saveMessage(any()) } returns Result.success(Unit)
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(createTestProvider())
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(createTestContact())
        coEvery { aiAdvisorRepository.getRecentConversations(any(), any()) } returns Result.success(history)
        coEvery { aiRepository.generateText(any(), any(), any()) } returns Result.success("AI Response")
    }

    private fun createTestProvider(): AiProvider {
        return AiProvider(
            id = "provider-1",
            name = "Test Provider",
            baseUrl = "https://api.test.com",
            apiKey = "test-key",
            models = listOf(AiModel(id = "test-model", displayName = "Test Model")),
            defaultModelId = "test-model",
            isDefault = true
        )
    }

    private fun createTestContact(): ContactProfile {
        return ContactProfile(
            id = testContactId,
            name = "Test Contact",
            targetGoal = ""
        )
    }

    private fun createTestConversation(
        id: String,
        messageType: MessageType,
        content: String
    ): AiAdvisorConversation {
        val now = System.currentTimeMillis()
        return AiAdvisorConversation(
            id = id,
            contactId = testContactId,
            sessionId = testSessionId,
            messageType = messageType,
            content = content,
            timestamp = now,
            createdAt = now,
            sendStatus = SendStatus.SUCCESS
        )
    }
}
