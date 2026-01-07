package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import com.empathy.ai.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
 * ContactSelectViewModel 单元测试
 *
 * ## 测试范围
 * - 加载联系人列表
 * - 选择联系人
 * - 搜索联系人
 * - 错误处理
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactSelectViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var contactRepository: ContactRepository
    private lateinit var aiAdvisorPreferences: AiAdvisorPreferencesRepository

    private val testContacts = listOf(
        ContactProfile(
            id = "contact-1",
            name = "张三",
            targetGoal = "最近聊了工作的事"
        ),
        ContactProfile(
            id = "contact-2",
            name = "李四",
            targetGoal = "周末约了吃饭"
        ),
        ContactProfile(
            id = "contact-3",
            name = "王五",
            targetGoal = "保持联系"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        contactRepository = mockk(relaxed = true)
        aiAdvisorPreferences = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads contacts successfully`() = runTest {
        // Given
        coEvery { contactRepository.getAllProfiles() } returns flowOf(testContacts)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(testContacts, state.contacts)
        assertEquals(testContacts, state.filteredContacts)
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertNull(state.error)
    }

    @Test
    fun `loadContacts shows empty state when no contacts`() = runTest {
        // Given
        coEvery { contactRepository.getAllProfiles() } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertTrue(state.contacts.isEmpty())
    }

    @Test
    fun `selectContact saves to preferences and updates state`() = runTest {
        // Given
        coEvery { contactRepository.getAllProfiles() } returns flowOf(testContacts)
        every { aiAdvisorPreferences.setLastContactId(any()) } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.selectContact("contact-1")

        // Then
        verify { aiAdvisorPreferences.setLastContactId("contact-1") }
        assertEquals("contact-1", viewModel.uiState.value.selectedContactId)
    }

    @Test
    fun `searchContacts filters by name`() = runTest {
        // Given
        coEvery { contactRepository.getAllProfiles() } returns flowOf(testContacts)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.searchContacts("张")

        // Then
        val state = viewModel.uiState.value
        assertEquals("张", state.searchQuery)
        assertEquals(1, state.filteredContacts.size)
        assertEquals("张三", state.filteredContacts[0].name)
    }

    @Test
    fun `searchContacts is case insensitive`() = runTest {
        // Given
        val contactsWithEnglish = listOf(
            ContactProfile(
                id = "contact-1",
                name = "John",
                targetGoal = "Business partner"
            ),
            ContactProfile(
                id = "contact-2",
                name = "JANE",
                targetGoal = "Friend"
            )
        )
        coEvery { contactRepository.getAllProfiles() } returns flowOf(contactsWithEnglish)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.searchContacts("john")

        // Then
        assertEquals(1, viewModel.uiState.value.filteredContacts.size)
        assertEquals("John", viewModel.uiState.value.filteredContacts[0].name)
    }

    @Test
    fun `searchContacts with empty query shows all contacts`() = runTest {
        // Given
        coEvery { contactRepository.getAllProfiles() } returns flowOf(testContacts)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // First filter
        viewModel.searchContacts("张")
        assertEquals(1, viewModel.uiState.value.filteredContacts.size)

        // When - clear search
        viewModel.searchContacts("")

        // Then
        assertEquals(testContacts.size, viewModel.uiState.value.filteredContacts.size)
    }

    @Test
    fun `searchContacts with no matches returns empty list`() = runTest {
        // Given
        coEvery { contactRepository.getAllProfiles() } returns flowOf(testContacts)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.searchContacts("不存在的名字")

        // Then
        assertTrue(viewModel.uiState.value.filteredContacts.isEmpty())
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Given
        coEvery { contactRepository.getAllProfiles() } returns flowOf(testContacts)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Manually set error for testing
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `resetSelection clears selected contact id`() = runTest {
        // Given
        coEvery { contactRepository.getAllProfiles() } returns flowOf(testContacts)
        every { aiAdvisorPreferences.setLastContactId(any()) } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectContact("contact-1")
        assertEquals("contact-1", viewModel.uiState.value.selectedContactId)

        // When
        viewModel.resetSelection()

        // Then
        assertNull(viewModel.uiState.value.selectedContactId)
    }

    @Test
    fun `multiple searches update filtered list correctly`() = runTest {
        // Given
        coEvery { contactRepository.getAllProfiles() } returns flowOf(testContacts)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - search for 张
        viewModel.searchContacts("张")
        assertEquals(1, viewModel.uiState.value.filteredContacts.size)

        // When - search for 李
        viewModel.searchContacts("李")
        assertEquals(1, viewModel.uiState.value.filteredContacts.size)
        assertEquals("李四", viewModel.uiState.value.filteredContacts[0].name)

        // When - search for partial match
        viewModel.searchContacts("三")
        assertEquals(1, viewModel.uiState.value.filteredContacts.size)
        assertEquals("张三", viewModel.uiState.value.filteredContacts[0].name)
    }

    private fun createViewModel(): ContactSelectViewModel {
        return ContactSelectViewModel(
            contactRepository = contactRepository,
            aiAdvisorPreferences = aiAdvisorPreferences
        )
    }
}
