package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import com.empathy.ai.domain.usecase.SendAdvisorMessageStreamingUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageUseCase
import com.empathy.ai.presentation.R
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiAdvisorChatDraftTest {

    private val testDispatcher = StandardTestDispatcher()

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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getContactUseCase = mockk(relaxed = true)
        getAllContactsUseCase = mockk()
        createAdvisorSessionUseCase = mockk(relaxed = true)
        getAdvisorSessionsUseCase = mockk(relaxed = true)
        getAdvisorConversationsUseCase = mockk()
        sendAdvisorMessageUseCase = mockk(relaxed = true)
        sendAdvisorMessageStreamingUseCase = mockk(relaxed = true)
        deleteAdvisorConversationUseCase = mockk(relaxed = true)
        getAdvisorDraftUseCase = mockk(relaxed = true)
        saveAdvisorDraftUseCase = mockk(relaxed = true)
        clearAdvisorDraftUseCase = mockk(relaxed = true)
        aiAdvisorRepository = mockk(relaxed = true)

        every { getAllContactsUseCase() } returns flowOf(emptyList())
        every { getAdvisorConversationsUseCase(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateInput 触发延迟保存草稿`() = runTest {
        // Given
        val sessionId = "session-1"
        coEvery { getAdvisorDraftUseCase(sessionId) } returns Result.success(null)
        coEvery { saveAdvisorDraftUseCase(sessionId, "你好") } returns Result.success(Unit)

        val viewModel = createViewModel()

        // When
        viewModel.switchSession(sessionId)
        testDispatcher.scheduler.runCurrent()
        viewModel.updateInput("你好")
        advanceTimeBy(400)
        advanceUntilIdle()

        // Then
        coVerify { saveAdvisorDraftUseCase(sessionId, "你好") }
    }

    @Test
    fun `切换会话时恢复草稿`() = runTest {
        // Given
        val sessionId = "session-2"
        coEvery { getAdvisorDraftUseCase(sessionId) } returns Result.success("草稿内容")

        val viewModel = createViewModel()

        // When
        viewModel.switchSession(sessionId)
        testDispatcher.scheduler.runCurrent()

        // Then
        assertEquals("草稿内容", viewModel.uiState.value.inputText)
    }

    @Test
    fun `切换会话恢复草稿时显示提示`() = runTest {
        // Given
        val sessionId = "session-3"
        coEvery { getAdvisorDraftUseCase(sessionId) } returns Result.success("草稿内容")

        val viewModel = createViewModel()

        // When
        viewModel.switchSession(sessionId)
        testDispatcher.scheduler.runCurrent()

        // Then
        assertEquals("草稿内容", viewModel.uiState.value.inputText)
        assertEquals(
            R.string.advisor_draft_restored_message,
            viewModel.uiState.value.draftRestoredMessageResId
        )
    }

    @Test
    fun `草稿恢复提示自动关闭`() = runTest {
        // Given
        val sessionId = "session-4"
        coEvery { getAdvisorDraftUseCase(sessionId) } returns Result.success("草稿内容")

        val viewModel = createViewModel()

        // When
        viewModel.switchSession(sessionId)
        testDispatcher.scheduler.runCurrent()

        // Then
        assertEquals(
            R.string.advisor_draft_restored_message,
            viewModel.uiState.value.draftRestoredMessageResId
        )
        advanceTimeBy(3000)
        testDispatcher.scheduler.runCurrent()
        assertNull(viewModel.uiState.value.draftRestoredMessageResId)
    }

    @Test
    fun `更新输入后清除草稿恢复提示`() = runTest {
        // Given
        val sessionId = "session-5"
        coEvery { getAdvisorDraftUseCase(sessionId) } returns Result.success("草稿内容")

        val viewModel = createViewModel()
        viewModel.switchSession(sessionId)
        testDispatcher.scheduler.runCurrent()

        // When
        viewModel.updateInput("新的输入")
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.draftRestoredMessageResId)
    }

    private fun createViewModel(): AiAdvisorChatViewModel {
        return AiAdvisorChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf()),
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
}
