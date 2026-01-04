package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.domain.usecase.CreateAdvisorSessionUseCase
import com.empathy.ai.domain.usecase.DeleteAdvisorConversationUseCase
import com.empathy.ai.domain.usecase.GetAdvisorConversationsUseCase
import com.empathy.ai.domain.usecase.GetAdvisorSessionsUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageUseCase
import com.empathy.ai.presentation.navigation.NavRoutes
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
    private lateinit var deleteAdvisorConversationUseCase: DeleteAdvisorConversationUseCase

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
        deleteAdvisorConversationUseCase = mockk()

        // Default mock behaviors
        every { getAllContactsUseCase() } returns flowOf(emptyList())
        every { getAdvisorConversationsUseCase(any()) } returns flowOf(emptyList())
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
            deleteAdvisorConversationUseCase = deleteAdvisorConversationUseCase
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
        content: String
    ): AiAdvisorConversation {
        val now = System.currentTimeMillis()
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
}
