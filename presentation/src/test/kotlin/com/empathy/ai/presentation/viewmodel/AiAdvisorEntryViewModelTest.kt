package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorNavigationTarget
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
 * AiAdvisorEntryViewModel 单元测试
 *
 * ## 测试范围
 * - 导航目标判断逻辑
 * - 有历史联系人时导航到对话界面
 * - 无历史联系人时导航到联系人选择页面
 * - 联系人不存在时清除偏好并导航到联系人选择页面
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAdvisorEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var aiAdvisorPreferences: AiAdvisorPreferencesRepository
    private lateinit var contactRepository: ContactRepository

    private val testContactId = "contact-123"
    private val testContact = ContactProfile(
        id = testContactId,
        name = "张三",
        targetGoal = "保持联系"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        aiAdvisorPreferences = mockk(relaxed = true)
        contactRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `navigates to ContactSelect when no last contact id`() = runTest {
        // Given
        every { aiAdvisorPreferences.getLastContactId() } returns null

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)
    }

    @Test
    fun `navigates to ContactSelect when last contact id is empty`() = runTest {
        // Given
        every { aiAdvisorPreferences.getLastContactId() } returns ""

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)
    }

    @Test
    fun `navigates to Chat when contact exists`() = runTest {
        // Given
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        val target = state.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.Chat)
        assertEquals(testContactId, (target as AiAdvisorNavigationTarget.Chat).contactId)
    }

    @Test
    fun `navigates to ContactSelect and clears preferences when contact not found`() = runTest {
        // Given
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        verify { aiAdvisorPreferences.clear() }
        val state = viewModel.uiState.value
        assertTrue(state.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)
    }

    @Test
    fun `navigates to ContactSelect and clears preferences on repository error`() = runTest {
        // Given
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.failure(Exception("网络错误"))

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        verify { aiAdvisorPreferences.clear() }
        val state = viewModel.uiState.value
        assertTrue(state.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)
    }

    @Test
    fun `resetNavigationState clears navigation target`() = runTest {
        // Given
        every { aiAdvisorPreferences.getLastContactId() } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Verify navigation target is set
        assertTrue(viewModel.uiState.value.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)

        // When
        viewModel.resetNavigationState()

        // Then
        assertNull(viewModel.uiState.value.navigationTarget)
    }

    @Test
    fun `isLoading is true initially`() = runTest {
        // Given
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        // When
        val viewModel = createViewModel()
        // Don't advance - check initial state

        // Then
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `isLoading is false after navigation target is determined`() = runTest {
        // Given
        every { aiAdvisorPreferences.getLastContactId() } returns null

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun createViewModel(): AiAdvisorEntryViewModel {
        return AiAdvisorEntryViewModel(
            aiAdvisorPreferences = aiAdvisorPreferences,
            contactRepository = contactRepository
        )
    }
}
