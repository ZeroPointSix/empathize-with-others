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
