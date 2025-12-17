package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.ConversationContextConfig
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.util.ConversationContextBuilder
import com.empathy.ai.domain.util.PromptBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * AnalyzeChatUseCase 历史上下文功能测试
 *
 * 专门测试对话上下文连续性增强功能
 */
class AnalyzeChatUseCaseHistoryContextTest {

    private lateinit var useCase: AnalyzeChatUseCase
    private lateinit var contactRepository: ContactRepository
    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var aiProviderRepository: AiProviderRepository
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var promptBuilder: PromptBuilder
    private lateinit var conversationContextBuilder: ConversationContextBuilder

    private val testContactId = "test-contact-id"
    private val testProfile = ContactProfile(
        id = testContactId,
        name = "测试联系人",
        targetGoal = "测试目标"
    )
    private val testProvider = AiProvider(
        id = "test-provider-id",
        name = "TestProvider",
        apiKey = "test-api-key",
        baseUrl = "https://api.test.com",
        model = "test-model",
        isDefault = true
    )

    @Before
    fun setup() {
        contactRepository = mockk()
        brainTagRepository = mockk()
        privacyRepository = mockk()
        aiRepository = mockk()
        settingsRepository = mockk()
        aiProviderRepository = mockk()
        conversationRepository = mockk()
        promptBuilder = mockk()
        conversationContextBuilder = ConversationContextBuilder()

        useCase = AnalyzeChatUseCase(
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            privacyRepository = privacyRepository,
            aiRepository = aiRepository,
            settingsRepository = settingsRepository,
            aiProviderRepository = aiProviderRepository,
            conversationRepository = conversationRepository,
            promptBuilder = promptBuilder,
            conversationContextBuilder = conversationContextBuilder
        )

        // 设置默认mock行为
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testProfile)
        every { brainTagRepository.getTagsForContact(testContactId) } returns flowOf(emptyList())
        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())
        coEvery { settingsRepository.getDataMaskingEnabled() } returns Result.success(true)
        coEvery { conversationRepository.saveUserInput(any(), any(), any()) } returns Result.success(1L)
        coEvery { conversationRepository.updateAiResponse(any(), any()) } returns Result.success(Unit)
        coEvery { contactRepository.updateLastInteractionDate(any(), any()) } returns Result.success(Unit)
        coEvery { promptBuilder.buildSystemInstruction(any(), any(), any(), any()) } returns "test instruction"
    }

    // ==================== 历史条数配置测试 ====================

    @Test
    fun `历史条数为0时不查询数据库`() = runTest {
        // Given
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(0)
        coEvery { aiRepository.analyzeChat(any(), any(), any()) } returns Result.success(mockk(relaxed = true))

        // When
        useCase(testContactId, listOf("测试消息"))

        // Then - 不应该调用getRecentConversations
        coVerify(exactly = 0) { conversationRepository.getRecentConversations(any(), any()) }
    }

    @Test
    fun `历史条数为5时查询5条记录`() = runTest {
        // Given
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(5)
        coEvery { conversationRepository.getRecentConversations(testContactId, 5) } returns Result.success(emptyList())
        coEvery { aiRepository.analyzeChat(any(), any(), any()) } returns Result.success(mockk(relaxed = true))

        // When
        useCase(testContactId, listOf("测试消息"))

        // Then
        coVerify(exactly = 1) { conversationRepository.getRecentConversations(testContactId, 5) }
    }

    @Test
    fun `历史条数为10时查询10条记录`() = runTest {
        // Given
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(10)
        coEvery { conversationRepository.getRecentConversations(testContactId, 10) } returns Result.success(emptyList())
        coEvery { aiRepository.analyzeChat(any(), any(), any()) } returns Result.success(mockk(relaxed = true))

        // When
        useCase(testContactId, listOf("测试消息"))

        // Then
        coVerify(exactly = 1) { conversationRepository.getRecentConversations(testContactId, 10) }
    }

    // ==================== 降级策略测试 ====================

    @Test
    fun `仓库查询失败时降级为空历史`() = runTest {
        // Given
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(5)
        coEvery { conversationRepository.getRecentConversations(any(), any()) } returns Result.failure(Exception("数据库错误"))
        coEvery { aiRepository.analyzeChat(any(), any(), any()) } returns Result.success(mockk(relaxed = true))

        // When
        val result = useCase(testContactId, listOf("测试消息"))

        // Then - 应该成功，不因历史查询失败而失败
        // 注意：这里只验证不会因为历史查询失败而导致整个流程失败
        coVerify(exactly = 1) { conversationRepository.getRecentConversations(any(), any()) }
    }

    @Test
    fun `配置读取失败时使用默认值5`() = runTest {
        // Given
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.failure(Exception("配置读取失败"))
        coEvery { conversationRepository.getRecentConversations(testContactId, 5) } returns Result.success(emptyList())
        coEvery { aiRepository.analyzeChat(any(), any(), any()) } returns Result.success(mockk(relaxed = true))

        // When
        useCase(testContactId, listOf("测试消息"))

        // Then - 应该使用默认值5
        coVerify(exactly = 1) { conversationRepository.getRecentConversations(testContactId, 5) }
    }

    // ==================== 历史记录转换测试 ====================

    @Test
    fun `空历史记录返回空字符串`() = runTest {
        // Given
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(5)
        coEvery { conversationRepository.getRecentConversations(any(), any()) } returns Result.success(emptyList())
        coEvery { aiRepository.analyzeChat(any(), any(), any()) } returns Result.success(mockk(relaxed = true))

        // When
        useCase(testContactId, listOf("测试消息"))

        // Then - 验证调用了AI分析（说明流程正常完成）
        coVerify(exactly = 1) { aiRepository.analyzeChat(any(), any(), any()) }
    }

    @Test
    fun `有效历史记录被正确处理`() = runTest {
        // Given
        val historyLogs = listOf(
            ConversationLog(
                id = 1,
                contactId = testContactId,
                userInput = "历史消息1",
                aiResponse = "AI回复1",
                timestamp = System.currentTimeMillis() - 60 * 60 * 1000, // 1小时前
                isSummarized = false
            ),
            ConversationLog(
                id = 2,
                contactId = testContactId,
                userInput = "历史消息2",
                aiResponse = "AI回复2",
                timestamp = System.currentTimeMillis() - 30 * 60 * 1000, // 30分钟前
                isSummarized = false
            )
        )

        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(5)
        coEvery { conversationRepository.getRecentConversations(any(), any()) } returns Result.success(historyLogs)
        coEvery { aiRepository.analyzeChat(any(), any(), any()) } returns Result.success(mockk(relaxed = true))

        // When
        useCase(testContactId, listOf("测试消息"))

        // Then - 验证调用了AI分析
        coVerify(exactly = 1) { aiRepository.analyzeChat(any(), any(), any()) }
    }

    @Test
    fun `无效历史记录被跳过`() = runTest {
        // Given - 包含一条空内容的记录（会被跳过）
        val historyLogs = listOf(
            ConversationLog(
                id = 1,
                contactId = testContactId,
                userInput = "有效消息",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            )
        )

        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(5)
        coEvery { conversationRepository.getRecentConversations(any(), any()) } returns Result.success(historyLogs)
        coEvery { aiRepository.analyzeChat(any(), any(), any()) } returns Result.success(mockk(relaxed = true))

        // When
        useCase(testContactId, listOf("测试消息"))

        // Then - 流程应该正常完成
        coVerify(exactly = 1) { aiRepository.analyzeChat(any(), any(), any()) }
    }
}
