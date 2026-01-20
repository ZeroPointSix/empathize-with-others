package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.usecase.ClearAdvisorDraftUseCase
import com.empathy.ai.presentation.navigation.NavRoutes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * SessionHistoryViewModel 单元测试
 *
 * ## 测试范围
 * - 加载联系人信息
 * - 加载会话列表
 * - 删除会话
 * - 错误处理
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var aiAdvisorRepository: AiAdvisorRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var clearAdvisorDraftUseCase: ClearAdvisorDraftUseCase
    private lateinit var savedStateHandle: SavedStateHandle

    private val testContactId = "contact-123"
    private val testContactName = "张三"

    private val testSessions = listOf(
        AiAdvisorSession(
            id = "session-1",
            contactId = testContactId,
            title = "关于工作的讨论",
            messageCount = 5,
            createdAt = System.currentTimeMillis() - 86400000,
            updatedAt = System.currentTimeMillis() - 3600000
        ),
        AiAdvisorSession(
            id = "session-2",
            contactId = testContactId,
            title = "周末计划",
            messageCount = 3,
            createdAt = System.currentTimeMillis() - 172800000,
            updatedAt = System.currentTimeMillis() - 86400000
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        aiAdvisorRepository = mockk(relaxed = true)
        contactRepository = mockk(relaxed = true)
        clearAdvisorDraftUseCase = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf(NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID to testContactId))

        coEvery { clearAdvisorDraftUseCase(any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads contact info and sessions`() = runTest {
        // Given
        val contact = ContactProfile(
            id = testContactId,
            name = testContactName,
            targetGoal = "保持联系"
        )
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(contact)
        coEvery { aiAdvisorRepository.getSessions(testContactId) } returns Result.success(testSessions)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(testContactName, state.contactName)
        assertEquals(testSessions, state.sessions)
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertNull(state.error)
    }

    @Test
    fun `loadSessions shows empty state when no sessions`() = runTest {
        // Given
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)
        coEvery { aiAdvisorRepository.getSessions(testContactId) } returns Result.success(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertTrue(state.sessions.isEmpty())
    }

    @Test
    fun `loadSessions shows error on failure`() = runTest {
        // Given
        val errorMessage = "网络错误"
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)
        coEvery { aiAdvisorRepository.getSessions(testContactId) } returns Result.failure(Exception(errorMessage))

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `deleteSession removes session and reloads list`() = runTest {
        // Given
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)
        coEvery { aiAdvisorRepository.getSessions(testContactId) } returns Result.success(testSessions)
        coEvery { aiAdvisorRepository.deleteSession("session-1") } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.deleteSession("session-1")
        advanceUntilIdle()

        // Then
        coVerify { aiAdvisorRepository.deleteSession("session-1") }
        coVerify(atLeast = 2) { aiAdvisorRepository.getSessions(testContactId) }
    }

    @Test
    fun `deleteSession shows error on failure`() = runTest {
        // Given
        val errorMessage = "删除失败"
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)
        coEvery { aiAdvisorRepository.getSessions(testContactId) } returns Result.success(testSessions)
        coEvery { aiAdvisorRepository.deleteSession("session-1") } returns Result.failure(Exception(errorMessage))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.deleteSession("session-1")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Given
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)
        coEvery { aiAdvisorRepository.getSessions(testContactId) } returns Result.failure(Exception("错误"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `getContactId returns correct contact id`() = runTest {
        // Given
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)
        coEvery { aiAdvisorRepository.getSessions(testContactId) } returns Result.success(emptyList())

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(testContactId, viewModel.getContactId())
    }

    @Test
    fun `contact name defaults to unknown when contact not found`() = runTest {
        // Given
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)
        coEvery { aiAdvisorRepository.getSessions(testContactId) } returns Result.success(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertEquals("未知联系人", viewModel.uiState.value.contactName)
    }

    @Test
    fun `contact name defaults to unknown on error`() = runTest {
        // Given
        coEvery { contactRepository.getProfile(testContactId) } returns Result.failure(Exception("错误"))
        coEvery { aiAdvisorRepository.getSessions(testContactId) } returns Result.success(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertEquals("未知联系人", viewModel.uiState.value.contactName)
    }

    private fun createViewModel(): SessionHistoryViewModel {
        return SessionHistoryViewModel(
            aiAdvisorRepository = aiAdvisorRepository,
            contactRepository = contactRepository,
            clearAdvisorDraftUseCase = clearAdvisorDraftUseCase,
            savedStateHandle = savedStateHandle
        )
    }
}
