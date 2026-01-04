package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.usecase.GetAdvisorSessionsUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import io.mockk.coEvery
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
 * AiAdvisorViewModel单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAdvisorViewModelTest {

    private lateinit var viewModel: AiAdvisorViewModel
    private lateinit var mockGetAllContactsUseCase: GetAllContactsUseCase
    private lateinit var mockGetAdvisorSessionsUseCase: GetAdvisorSessionsUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockGetAllContactsUseCase = mockk()
        mockGetAdvisorSessionsUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty contacts`() = runTest {
        // Given
        every { mockGetAllContactsUseCase() } returns flowOf(emptyList())

        // When
        viewModel = AiAdvisorViewModel(mockGetAllContactsUseCase, mockGetAdvisorSessionsUseCase)

        // Then - initial state before collection starts
        assertTrue(viewModel.uiState.value.contacts.isEmpty())
    }

    @Test
    fun `loadContacts should update contacts list`() = runTest {
        // Given
        val contacts = listOf(
            createContact("contact-1", "Alice"),
            createContact("contact-2", "Bob")
        )
        every { mockGetAllContactsUseCase() } returns flowOf(contacts)

        // When
        viewModel = AiAdvisorViewModel(mockGetAllContactsUseCase, mockGetAdvisorSessionsUseCase)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.contacts.size)
        assertEquals("Alice", state.contacts[0].name)
        assertEquals("Bob", state.contacts[1].name)
    }

    @Test
    fun `loadContacts with empty list should show empty state`() = runTest {
        // Given
        every { mockGetAllContactsUseCase() } returns flowOf(emptyList())

        // When
        viewModel = AiAdvisorViewModel(mockGetAllContactsUseCase, mockGetAdvisorSessionsUseCase)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.contacts.isEmpty())
    }

    @Test
    fun `loadRecentSessions should update sessions for contact`() = runTest {
        // Given
        val contacts = listOf(createContact("contact-1", "Alice"))
        val sessions = listOf(
            AiAdvisorSession.create("contact-1", "Session 1"),
            AiAdvisorSession.create("contact-1", "Session 2")
        )
        every { mockGetAllContactsUseCase() } returns flowOf(contacts)
        coEvery { mockGetAdvisorSessionsUseCase("contact-1") } returns Result.success(sessions)

        // When
        viewModel = AiAdvisorViewModel(mockGetAllContactsUseCase, mockGetAdvisorSessionsUseCase)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        // recentSessions stores only the first session per contact
        assertEquals("Session 1", state.recentSessions["contact-1"]?.title)
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // Given
        every { mockGetAllContactsUseCase() } returns flowOf(emptyList())
        viewModel = AiAdvisorViewModel(mockGetAllContactsUseCase, mockGetAdvisorSessionsUseCase)
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `refresh should reload contacts`() = runTest {
        // Given
        val contacts = listOf(createContact("contact-1", "Alice"))
        every { mockGetAllContactsUseCase() } returns flowOf(contacts)

        viewModel = AiAdvisorViewModel(mockGetAllContactsUseCase, mockGetAdvisorSessionsUseCase)
        advanceUntilIdle()

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.uiState.value.contacts.size)
    }

    // ==================== 辅助方法 ====================

    private fun createContact(id: String, name: String): ContactProfile {
        return ContactProfile(
            id = id,
            name = name,
            targetGoal = ""
        )
    }
}
