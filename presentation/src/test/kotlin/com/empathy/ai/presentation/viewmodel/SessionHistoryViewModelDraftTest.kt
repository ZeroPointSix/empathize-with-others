package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionHistoryViewModelDraftTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var aiAdvisorRepository: AiAdvisorRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var clearAdvisorDraftUseCase: ClearAdvisorDraftUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        aiAdvisorRepository = mockk()
        contactRepository = mockk()
        clearAdvisorDraftUseCase = mockk(relaxed = true)

        coEvery { contactRepository.getProfile(any()) } returns Result.success(
            ContactProfile(
                id = "contact-1",
                name = "Contact",
                targetGoal = "Goal"
            )
        )
        coEvery { aiAdvisorRepository.getSessions(any()) } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `delete session should clear draft`() = runTest {
        // Given
        val sessionId = "session-1"
        coEvery { aiAdvisorRepository.deleteSession(sessionId) } returns Result.success(Unit)
        coEvery { clearAdvisorDraftUseCase(sessionId) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.deleteSession(sessionId)
        advanceUntilIdle()

        // Then
        coVerify { clearAdvisorDraftUseCase(sessionId) }
    }

    private fun createViewModel(contactId: String = "contact-1"): SessionHistoryViewModel {
        return SessionHistoryViewModel(
            aiAdvisorRepository = aiAdvisorRepository,
            contactRepository = contactRepository,
            clearAdvisorDraftUseCase = clearAdvisorDraftUseCase,
            savedStateHandle = SavedStateHandle(
                mapOf(NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID to contactId)
            )
        )
    }
}
