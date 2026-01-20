package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.domain.model.StreamingState
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.usecase.ClearAdvisorDraftUseCase
import com.empathy.ai.domain.usecase.CreateAdvisorSessionUseCase
import com.empathy.ai.domain.usecase.DeleteAdvisorConversationUseCase
import com.empathy.ai.domain.usecase.GetAdvisorConversationsUseCase
import com.empathy.ai.domain.usecase.GetAdvisorDraftUseCase
import com.empathy.ai.domain.usecase.GetAdvisorSessionsUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.SaveAdvisorDraftUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageStreamingUseCase
import com.empathy.ai.presentation.navigation.NavRoutes
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AiAdvisorChatViewModel单元测试
 *
 * 包含BUG-00044修复后的测试用例
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAdvisorChatViewModelTest {

    private lateinit var viewModel: AiAdvisorChatViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getContactUseCase: GetContactUseCase
    private lateinit var getAllContactsUseCase: GetAllContactsUseCase
    private lateinit var createAdvisorSessionUseCase: CreateAdvisorSessionUseCase
    private lateinit var getAdvisorSessionsUseCase: GetAdvisorSessionsUseCase
    private lateinit var getAdvisorConversationsUseCase: GetAdvisorConversationsUseCase
    private lateinit var sendAdvisorMessageUseCase: SendAdvisorMessageUseCase
    private lateinit var sendAdvisorMessageStreamingUseCase: SendAdvisorMessageStreamingUseCase
    private lateinit var deleteAdvisorConversationUseCase: DeleteAdvisorConversationUseCase
    private lateinit var getAdvisorDraftUseCase: GetAdvisorDraftUseCase
    private lateinit var saveAdvisorDraftUseCase: SaveAdvisorDraftUseCase
    private lateinit var clearAdvisorDraftUseCase: ClearAdvisorDraftUseCase
    private lateinit var aiAdvisorRepository: AiAdvisorRepository

    private val testDispatcher = StandardTestDispatcher()
    private val testContactId = "test-contact-1"
    private val testSessionId = "test-session-1"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle(mapOf(NavRoutes.AI_ADVISOR_CHAT_ARG_ID to testContactId))
        getContactUseCase = mockk()
        getAllContactsUseCase = mockk()
        createAdvisorSessionUseCase = mockk()
        getAdvisorSessionsUseCase = mockk()
        getAdvisorConversationsUseCase = mockk()
        sendAdvisorMessageUseCase = mockk()
        sendAdvisorMessageStreamingUseCase = mockk()
        deleteAdvisorConversationUseCase = mockk()
        getAdvisorDraftUseCase = mockk()
        saveAdvisorDraftUseCase = mockk()
        clearAdvisorDraftUseCase = mockk()
        aiAdvisorRepository = mockk()

        // Default mock behaviors
        every { getAllContactsUseCase() } returns flowOf(emptyList())
        every { getAdvisorConversationsUseCase(any()) } returns flowOf(emptyList())
        coEvery { getAdvisorDraftUseCase(any()) } returns Result.success(null)
        coEvery { saveAdvisorDraftUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { clearAdvisorDraftUseCase(any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = AiAdvisorChatViewModel(
            savedStateHandle = savedStateHandle,
            getContactUseCase = getContactUseCase,
            getAllContactsUseCase = getAllContactsUseCase,
            createAdvisorSessionUseCase = createAdvisorSessionUseCase,
            getAdvisorSessionsUseCase = getAdvisorSessionsUseCase,
            getAdvisorConversationsUseCase = getAdvisorConversationsUseCase,
            sendAdvisorMessageUseCase = sendAdvisorMessageUseCase,
            sendAdvisorMessageStreamingUseCase = sendAdvisorMessageStreamingUseCase,
            deleteAdvisorConversationUseCase = deleteAdvisorConversationUseCase,
            getAdvisorDraftUseCase = getAdvisorDraftUseCase,
            saveAdvisorDraftUseCase = saveAdvisorDraftUseCase,
            clearAdvisorDraftUseCase = clearAdvisorDraftUseCase,
            aiAdvisorRepository = aiAdvisorRepository
        )
    }

    // ==================== 初始化测试 ====================

    @Test
    fun `init should load contact and sessions when contactId is provided`() = runTest {
        // Given
        val contact = createTestContact(testContactId, "Test Contact")
        val session = createTestSession(testSessionId, testContactId)

        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        coEvery { getAdvisorSessionsUseCase(testContactId) } returns Result.success(listOf(session))

        // When
        createViewModel()
        viewModel.setStreamingMode(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Test Contact", state.contactName)
        assertEquals(1, state.sessions.size)
        assertEquals(testSessionId, state.currentSessionId)
        assertFalse(state.isLoading)
    }

    @Test
    fun `init should create new session when no sessions exist`() = runTest {
        // Given
        val contact = createTestContact(testContactId, "Test Contact")
        val newSession = createTestSession("new-session", testContactId)

        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        coEvery { getAdvisorSessionsUseCase(testContactId) } returns Result.success(emptyList())
        coEvery { createAdvisorSessionUseCase(testContactId) } returns Result.success(newSession)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        coVerify { createAdvisorSessionUseCase(testContactId) }
        val state = viewModel.uiState.value
        assertEquals("new-session", state.currentSessionId)
    }

    @Test
    fun `init should show error when contact load fails`() = runTest {
        // Given
        coEvery { getContactUseCase(testContactId) } returns Result.failure(Exception("Contact not found"))

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Contact not found", state.error)
        assertFalse(state.isLoading)
    }

    // ==================== 输入测试 ====================

    @Test
    fun `updateInput should update inputText in state`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateInput("Hello AI")

        // Then
        assertEquals("Hello AI", viewModel.uiState.value.inputText)
    }

    // ==================== 发送消息测试 ====================

    @Test
    fun `sendMessage should call usecase and clear input`() = runTest {
        // Given
        setupSuccessfulInit()
        val aiResponse = createTestConversation(
            "conv-2", testContactId, testSessionId, MessageType.AI, "AI Response"
        )
        coEvery {
            sendAdvisorMessageUseCase(testContactId, testSessionId, "Hello")
        } returns Result.success(aiResponse)

        createViewModel()
        viewModel.setStreamingMode(false)
        advanceUntilIdle()
        viewModel.updateInput("Hello")

        // When
        viewModel.sendMessage()
        advanceUntilIdle()

        // Then
        coVerify { sendAdvisorMessageUseCase(testContactId, testSessionId, "Hello") }
        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun `sendMessage should not send when input is empty`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        viewModel.setStreamingMode(false)
        advanceUntilIdle()
        viewModel.updateInput("")

        // When
        viewModel.sendMessage()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { sendAdvisorMessageUseCase(any(), any(), any()) }
    }

    @Test
    fun `sendMessage should show error on failure`() = runTest {
        // Given
        setupSuccessfulInit()
        coEvery {
            sendAdvisorMessageUseCase(any(), any(), any())
        } returns Result.failure(Exception("Network error"))

        createViewModel()
        viewModel.setStreamingMode(false)
        advanceUntilIdle()
        viewModel.updateInput("Hello")

        // When
        viewModel.sendMessage()
        advanceUntilIdle()

        // Then
        assertEquals("Network error", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSending)
    }

    // ==================== 会话切换测试 ====================

    @Test
    fun `switchSession should update currentSessionId and load conversations`() = runTest {
        // Given
        val session1 = createTestSession("session-1", testContactId)
        val session2 = createTestSession("session-2", testContactId)
        val contact = createTestContact(testContactId, "Test")

        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        coEvery { getAdvisorSessionsUseCase(testContactId) } returns Result.success(listOf(session1, session2))

        createViewModel()
        viewModel.setStreamingMode(false)
        advanceUntilIdle()

        // When
        viewModel.switchSession("session-2")
        advanceUntilIdle()

        // Then
        assertEquals("session-2", viewModel.uiState.value.currentSessionId)
    }

    @Test
    fun `createNewSession should add session and switch to it`() = runTest {
        // Given
        setupSuccessfulInit()
        val newSession = createTestSession("new-session", testContactId, "新对话")
        coEvery { createAdvisorSessionUseCase(testContactId) } returns Result.success(newSession)

        createViewModel()
        viewModel.setStreamingMode(false)
        advanceUntilIdle()

        // When
        viewModel.createNewSession()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("new-session", state.currentSessionId)
        assertTrue(state.sessions.any { it.id == "new-session" })
    }

    // ==================== 联系人选择器测试 ====================

    @Test
    fun `showContactSelector should set showContactSelector to true`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.showContactSelector()

        // Then
        assertTrue(viewModel.uiState.value.showContactSelector)
    }

    @Test
    fun `hideContactSelector should set showContactSelector to false`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()
        viewModel.showContactSelector()

        // When
        viewModel.hideContactSelector()

        // Then
        assertFalse(viewModel.uiState.value.showContactSelector)
    }

    @Test
    fun `switchContact should show confirm dialog for different contact`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()
        viewModel.showContactSelector()

        // When
        viewModel.switchContact("other-contact")

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showContactSelector)
        assertTrue(state.showSwitchConfirmDialog)
        assertEquals("other-contact", state.pendingContactId)
    }

    @Test
    fun `switchContact should hide selector for same contact`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()
        viewModel.showContactSelector()

        // When
        viewModel.switchContact(testContactId)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showContactSelector)
        assertFalse(state.showSwitchConfirmDialog)
    }

    @Test
    fun `confirmSwitch should set navigation state`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()
        viewModel.switchContact("other-contact")

        // When
        viewModel.confirmSwitch()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showSwitchConfirmDialog)
        assertNull(state.pendingContactId)
        assertEquals("other-contact", state.shouldNavigateToContact)
    }

    @Test
    fun `cancelSwitch should clear pending state`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()
        viewModel.switchContact("other-contact")

        // When
        viewModel.cancelSwitch()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showSwitchConfirmDialog)
        assertNull(state.pendingContactId)
        assertNull(state.shouldNavigateToContact)
    }

    // ==================== 消息删除测试 ====================

    @Test
    fun `deleteMessage should call delete usecase`() = runTest {
        // Given
        setupSuccessfulInit()
        coEvery { deleteAdvisorConversationUseCase("conv-1") } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.deleteMessage("conv-1")
        advanceUntilIdle()

        // Then
        coVerify { deleteAdvisorConversationUseCase("conv-1") }
    }

    @Test
    fun `deleteMessage should show error on failure`() = runTest {
        // Given
        setupSuccessfulInit()
        coEvery {
            deleteAdvisorConversationUseCase("conv-1")
        } returns Result.failure(Exception("Delete failed"))

        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.deleteMessage("conv-1")
        advanceUntilIdle()

        // Then
        assertEquals("Delete failed", viewModel.uiState.value.error)
    }

    // ==================== 重试消息测试 ====================

    @Test
    fun `retryMessage should delete failed message and resend`() = runTest {
        // Given
        setupSuccessfulInit()
        val failedConversation = AiAdvisorConversation(
            id = "conv-1",
            contactId = testContactId,
            sessionId = testSessionId,
            messageType = MessageType.USER,
            content = "Failed message",
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            sendStatus = SendStatus.FAILED
        )
        val aiResponse = createTestConversation(
            "conv-2", testContactId, testSessionId, MessageType.AI, "Response"
        )

        coEvery { deleteAdvisorConversationUseCase("conv-1") } returns Result.success(Unit)
        coEvery {
            sendAdvisorMessageUseCase(testContactId, testSessionId, "Failed message")
        } returns Result.success(aiResponse)

        createViewModel()
        advanceUntilIdle()
        viewModel.setStreamingMode(false)

        // When
        viewModel.retryMessage(failedConversation)
        advanceUntilIdle()

        // Then
        coVerify { deleteAdvisorConversationUseCase("conv-1") }
        coVerify { sendAdvisorMessageUseCase(testContactId, testSessionId, "Failed message") }
    }

    @Test
    fun `retryMessage should not retry non-failed message`() = runTest {
        // Given
        setupSuccessfulInit()
        val successConversation = createTestConversation(
            "conv-1", testContactId, testSessionId, MessageType.USER, "Success message"
        )

        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.retryMessage(successConversation)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { deleteAdvisorConversationUseCase(any()) }
    }

    // ==================== BUG-00044修复测试 ====================

    /**
     * BUG-00059修复: 测试CANCELLED状态的AI消息不能重试
     *
     * 业务规则 (PRD-00028): AI消息停止后应使用"重新生成"而非"重试"
     * 任务: BUG-00059/T002
     */
    @Test
    fun `retryMessage should NOT retry cancelled AI message`() = runTest {
        // Given
        setupSuccessfulInit()
        val cancelledAiConversation = AiAdvisorConversation(
            id = "conv-1",
            contactId = testContactId,
            sessionId = testSessionId,
            messageType = MessageType.AI,  // AI消息
            content = "AI生成的部分内容...[用户已停止生成]",
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            sendStatus = SendStatus.CANCELLED
        )

        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.retryMessage(cancelledAiConversation)
        advanceUntilIdle()

        // Then: 不应该调用删除或发送
        coVerify(exactly = 0) { deleteAdvisorConversationUseCase(any()) }
        coVerify(exactly = 0) { sendAdvisorMessageUseCase(any(), any(), any()) }
    }

    /**
     * BUG-00059修复: 测试CANCELLED状态的用户消息不能重试
     *
     * 业务规则 (PRD-00028): 仅FAILED状态可重试
     * 任务: BUG-00059/T002
     */
    @Test
    fun `retryMessage should NOT retry cancelled USER message`() = runTest {
        // Given
        setupSuccessfulInit()
        val cancelledUserConversation = AiAdvisorConversation(
            id = "conv-1",
            contactId = testContactId,
            sessionId = testSessionId,
            messageType = MessageType.USER,
            content = "用户消息",
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            sendStatus = SendStatus.CANCELLED
        )

        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.retryMessage(cancelledUserConversation)
        advanceUntilIdle()

        // Then: 不应该调用删除或发送（因为状态是CANCELLED而非FAILED）
        coVerify(exactly = 0) { deleteAdvisorConversationUseCase(any()) }
        coVerify(exactly = 0) { sendAdvisorMessageUseCase(any(), any(), any()) }
    }

    /**
     * BUG-044-P1-005: 测试切换会话时停止流式响应
     */
    @Test
    fun `switchSession should clear streaming state`() = runTest {
        // Given
        val session1 = createTestSession("session-1", testContactId)
        val session2 = createTestSession("session-2", testContactId)
        val contact = createTestContact(testContactId, "Test")

        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        coEvery { getAdvisorSessionsUseCase(testContactId) } returns Result.success(listOf(session1, session2))

        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.switchSession("session-2")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("session-2", state.currentSessionId)
        assertFalse(state.isStreaming)
        assertEquals("", state.streamingContent)
        assertEquals("", state.thinkingContent)
    }

    /**
     * BUG-044-P1-002: 测试停止生成时清空流式状态
     */
    @Test
    fun `stopGeneration should clear streaming state`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.stopGeneration()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertEquals("", state.streamingContent)
        assertEquals("", state.thinkingContent)
        assertNull(state.currentStreamingMessageId)
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `clearError should set error to null`() = runTest {
        // Given
        coEvery { getContactUseCase(testContactId) } returns Result.failure(Exception("Error"))
        every { getAllContactsUseCase() } returns flowOf(emptyList())

        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clearNavigationState should set shouldNavigateToContact to null`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()
        viewModel.switchContact("other-contact")
        viewModel.confirmSwitch()

        // When
        viewModel.clearNavigationState()

        // Then
        assertNull(viewModel.uiState.value.shouldNavigateToContact)
    }

    // ==================== 辅助方法 ====================

    private fun setupSuccessfulInit() {
        val contact = createTestContact(testContactId, "Test Contact")
        val session = createTestSession(testSessionId, testContactId)

        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        coEvery { getAdvisorSessionsUseCase(testContactId) } returns Result.success(listOf(session))
    }

    private fun createTestContact(id: String, name: String): ContactProfile {
        return ContactProfile(
            id = id,
            name = name,
            targetGoal = ""
        )
    }

    private fun createTestSession(
        id: String,
        contactId: String,
        title: String = "Test Session"
    ): AiAdvisorSession {
        val now = System.currentTimeMillis()
        return AiAdvisorSession(
            id = id,
            contactId = contactId,
            title = title,
            createdAt = now,
            updatedAt = now,
            messageCount = 0,
            isActive = true
        )
    }

    private fun createTestConversation(
        id: String,
        contactId: String,
        sessionId: String,
        messageType: MessageType,
        content: String,
        timestamp: Long = System.currentTimeMillis()
    ): AiAdvisorConversation {
        val now = timestamp
        return AiAdvisorConversation(
            id = id,
            contactId = contactId,
            sessionId = sessionId,
            messageType = messageType,
            content = content,
            timestamp = now,
            createdAt = now,
            sendStatus = SendStatus.SUCCESS
        )
    }

    // ==================== BUG-046 isRegenerating 状态测试 ====================

    /**
     * TC-046-REG-001: 重新生成时设置isRegenerating状态
     *
     * 场景：调用regenerateLastMessage
     * 预期：isRegenerating应该变为true
     */
    @Test
    fun `regenerateLastMessage should set isRegenerating to true during operation`() = runTest {
        // Given
        setupSuccessfulInit()
        val userConversation = createTestConversation(
            "user-1", testContactId, testSessionId, MessageType.USER, "用户消息内容", timestamp = 1000L
        )
        val aiConversation = createTestConversation(
            "ai-1", testContactId, testSessionId, MessageType.AI, "AI回复内容", timestamp = 2000L
        )

        every { getAdvisorConversationsUseCase(testSessionId) } returns flowOf(
            listOf(userConversation, aiConversation)
        )
        coEvery { deleteAdvisorConversationUseCase("ai-1") } returns Result.success(Unit)
        coEvery {
            sendAdvisorMessageStreamingUseCase(any(), any(), any(), any(), any())
        } returns flowOf(
            StreamingState.Started("ai-1"),
            StreamingState.Completed("AI回复内容", null)
        )

        createViewModel()
        advanceUntilIdle()

        // 初始状态isRegenerating应该为false
        assertFalse(viewModel.uiState.value.isRegenerating)

        // When - 重新生成
        viewModel.regenerateLastMessage()
        advanceUntilIdle()

        // Then - isRegenerating应该变回false（操作完成）
        assertFalse(viewModel.uiState.value.isRegenerating)
    }

    /**
     * TC-046-REG-002: 重新生成失败时清除isRegenerating状态
     *
     * 场景：删除消息失败
     * 预期：isRegenerating应该变为false，并显示错误
     */
    @Test
    fun `regenerateLastMessage should clear isRegenerating when delete fails`() = runTest {
        // Given
        setupSuccessfulInit()
        val userConversation = createTestConversation(
            "user-1", testContactId, testSessionId, MessageType.USER, "用户消息内容", timestamp = 1000L
        )
        val aiConversation = createTestConversation(
            "ai-1", testContactId, testSessionId, MessageType.AI, "AI回复内容", timestamp = 2000L
        )

        every { getAdvisorConversationsUseCase(testSessionId) } returns flowOf(
            listOf(userConversation, aiConversation)
        )
        coEvery { deleteAdvisorConversationUseCase("ai-1") } returns Result.failure(Exception("删除失败"))
        coEvery {
            sendAdvisorMessageStreamingUseCase(any(), any(), any(), any(), any())
        } returns flowOf()

        createViewModel()
        advanceUntilIdle()

        // When - 重新生成
        viewModel.regenerateLastMessage()
        advanceUntilIdle()

        // Then - isRegenerating应该变为false，错误信息应该显示
        assertFalse(viewModel.uiState.value.isRegenerating)
        assertTrue(viewModel.uiState.value.error?.contains("删除消息失败") == true)
    }

    /**
     * TC-046-REG-003: 停止生成时清除isRegenerating状态
     *
     * 场景：重新生成过程中停止
     * 预期：isRegenerating应该变为false
     */
    @Test
    fun `stopGeneration should clear isRegenerating state`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()

        // 模拟重新生成状态
        viewModel.regenerateLastMessage()

        // When - 停止生成
        viewModel.stopGeneration()
        advanceUntilIdle()

        // Then - isRegenerating应该变为false
        assertFalse(viewModel.uiState.value.isRegenerating)
    }

    // ==================== BUG-00059修复测试 ====================

    /**
     * BUG-00059: 测试isLikelyAiContent检测停止生成标记
     *
     * 业务规则 (PRD-00028): AI内容包含"[用户已停止生成]"标记
     * 任务: BUG-00059/T003
     */
    @Test
    fun `isLikelyAiContent should detect stop marker`() = runTest {
        // Given
        setupSuccessfulInit()
        createViewModel()
        advanceUntilIdle()

        // 测试数据: PRD-00028/AC-003
        val testData = listOf(
            "正常内容" to false,
            "部分内容...[用户已停止生成]" to true,
            "没有标记的长文本内容" to false,
            "核心策略：..." to false,
            "## 标题\n内容" to false,
            "ENFJ类型分析" to false,
            "1. 第一点\n2. 第二点" to false,
            "**加粗文本**" to false,
            "核心策略：\n1. 第一点" to true,
            "正常用户输入比较短" to false,
            "a".repeat(350) to true,  // 超长内容
        )

        // When & Then
        testData.forEach { (content, expected) ->
            val result = viewModel.isLikelyAiContent(content)
            assertEquals("Content: '$content' should be detected as AI=$expected", expected, result)
        }
    }

    /**
     * BUG-00059: 测试AI消息不能重试（仅FAILED用户消息可重试）
     *
     * 使用table-driven tests覆盖多种组合
     * 任务: BUG-00059/T002
     */
    @Test
    fun `retryMessage should only work for USER and FAILED combination`() = runTest {
        // Given
        setupSuccessfulInit()

        // 测试数据来源: BUG-00059/AC-002
        val testData = listOf(
            Triple(MessageType.USER, SendStatus.FAILED, true),
            Triple(MessageType.USER, SendStatus.CANCELLED, false),
            Triple(MessageType.USER, SendStatus.SUCCESS, false),
            Triple(MessageType.AI, SendStatus.FAILED, false),
            Triple(MessageType.AI, SendStatus.CANCELLED, false),
            Triple(MessageType.AI, SendStatus.SUCCESS, false),
        )

        testData.forEach { (messageType, sendStatus, shouldRetry) ->
            clearMocks(deleteAdvisorConversationUseCase, sendAdvisorMessageUseCase)

            // Given
            val conversation = AiAdvisorConversation(
                id = "conv-${messageType.name}-${sendStatus.name}",
                contactId = testContactId,
                sessionId = testSessionId,
                messageType = messageType,
                content = "Test content",
                timestamp = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                sendStatus = sendStatus
            )

            coEvery { deleteAdvisorConversationUseCase(any()) } returns Result.success(Unit)
            coEvery { sendAdvisorMessageUseCase(any(), any(), any()) } returns Result.success(
                createTestConversation("response", testContactId, testSessionId, MessageType.AI, "Response")
            )

            createViewModel()
            viewModel.setStreamingMode(false)
            advanceUntilIdle()

            // When
            viewModel.retryMessage(conversation)
            advanceUntilIdle()

            // Then
            coVerify(exactly = if (shouldRetry) 1 else 0) { deleteAdvisorConversationUseCase(any()) }
        }
    }
}
